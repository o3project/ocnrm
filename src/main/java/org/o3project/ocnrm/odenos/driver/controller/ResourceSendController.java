/*
* Copyright 2015 FUJITSU LIMITED.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.o3project.ocnrm.odenos.driver.controller;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.json.JSONException;
import org.o3project.ocnrm.lib.JSONParser;
import org.o3project.ocnrm.lib.MFApiCaller;
import org.o3project.ocnrm.lib.flow.AbstractFlowCreator;
import org.o3project.ocnrm.lib.flow.OduFlowCreator;
import org.o3project.ocnrm.lib.table.Event;
import org.o3project.ocnrm.lib.table.ResourceInfoFlomMf;
import org.o3project.ocnrm.lib.table.TableManager;
import org.o3project.ocnrm.model.Constraint;
import org.o3project.ocnrm.model.FlowResponse;
import org.o3project.ocnrm.model.IdExchangeRequest;
import org.o3project.ocnrm.model.ResourceRequest;
import org.o3project.ocnrm.model.TerminationPoints;
import org.o3project.ocnrm.model.bind.OduBindingData;
import org.o3project.ocnrm.model.bind.RmData;
import org.o3project.ocnrm.model.odu.OduFlow;
import org.o3project.ocnrm.model.odu.OduFlowCreationResponse;
import org.o3project.ocnrm.model.odu.OptFlow;
import org.o3project.ocnrm.model.odu.OptLink;
import org.o3project.ocnrm.odenos.lib.OdenOsPropertyLoader;
import org.o3project.ocnrm.ofctl.OfCtlSender;
import org.o3project.odenos.core.component.Logic;
import org.o3project.odenos.core.component.network.flow.Flow;
import org.o3project.odenos.core.component.network.flow.basic.BasicFlow;
import org.o3project.odenos.core.component.network.flow.basic.FlowAction;
import org.o3project.odenos.core.component.network.flow.basic.FlowActionOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class ResourceSendController {
    private Logger logger = LoggerFactory.getLogger(ResourceSendController.class);

    private static OdenOsPropertyLoader loader = OdenOsPropertyLoader.getInstance();
    private static final int ODU_SEND_TIME = Integer.valueOf(loader.getOduSendTime());

    private TableManager manager = TableManager.getInstance();

    public OduFlowCreationResponse registerNewOduFlow(BasicFlow basicFlow, Event event,
            String seqNo)
            throws JsonParseException, JsonMappingException, IOException, JSONException {
        logger.info(seqNo + "\t" + "registerNewOduFlow() Start");

        OduFlowCreationResponse resourceInfoFromMf = new OduFlowCreationResponse();

        ResourceInfoFlomMf resource = manager.checkIncompleteResource(event);
        if (resource != null) {
            logger.debug(seqNo + "\t" + "resource is not null");
            resourceInfoFromMf.setFjFlowId(resource.getResourse().getFjFlowId());
            resourceInfoFromMf.setConstraint(resource.getResourse().getConstraint());

            resourceInfoFromMf.setTerminationPointPairs(
                    ((OduFlow) resource.getResourse()).getTerminationPointPairs());
        } else {
            logger.debug(seqNo + "\t" + "resource is null");
            ResourceRequest rr = new ResourceRequest();
            rr.setRequest(convertBasicFlowToOptLink(basicFlow));

            resourceInfoFromMf = sendMf(seqNo, rr);

            OduFlow oduFlow = new OduFlow();
            oduFlow.setFjFlowId(resourceInfoFromMf.getFjFlowId());
            oduFlow.setTerminationPointPairs(resourceInfoFromMf.getTerminationPointPairs());
            oduFlow.setConstraint(resourceInfoFromMf.getConstraint());

            resource = new ResourceInfoFlomMf(event.getTransactionId(),
                    event.getSrcNetworkComponent(),
                    event.getEventId(),
                    resourceInfoFromMf.getFjFlowId(),
                    oduFlow,
                    TableManager.date.format(new Date()),
                    event.getDriver());
            TableManager.getInstance().addResource(resource);

        }
        RmData data = getOduReplacementTable(resourceInfoFromMf.getFjFlowId(),
                resourceInfoFromMf.getTerminationPointPairs(),
                seqNo);

        sendOfCtlSender(makeOduFlow(data, resourceInfoFromMf, seqNo), OfCtlSender.FLOW_ADD_METHOD,
                ODU_SEND_TIME, seqNo);

        logger.info(seqNo + "\t" + "registerNewOduFlow() End");
        return resourceInfoFromMf;
    }

    private RmData getOduReplacementTable(String flowId, List<TerminationPoints> points,
            String seqNo)
            throws JSONException, IOException {
        MFApiCaller sender = new MFApiCaller();
        RmData data = new OduBindingData();
        for (TerminationPoints point : points) {
            data.bind(point.getInPoint(),
                    sender.requestOduCorrespondingTable(
                            new IdExchangeRequest(flowId, point.getInPoint()), seqNo).toString());
            data.bind(point.getOutPoint(),
                    sender.requestOduCorrespondingTable(
                            new IdExchangeRequest(flowId, point.getOutPoint()), seqNo).toString());
        }

        return data;
    }

    private void sendOfCtlSender(List<String> flows, String method, int time, String seqNo) {
        OfCtlSender ofc = new OfCtlSender();
        ofc.ofCtlSend(flows, method, seqNo);

        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            logger.error(seqNo + "\t" + "get InterruptedException with " + time + "ms sleep.");
            e.printStackTrace();
        }
    }

    private List<String> makeOduFlow(RmData data, OptFlow optFlow, String seqNo) {
        AbstractFlowCreator flowCreator = new OduFlowCreator();
        List<String> flows = flowCreator.createFlow(optFlow, data, seqNo);
        logger.debug(seqNo + "\t" + "getFlow : " + flows.toString());
        return flows;
    }

    private OduFlowCreationResponse sendMf(String seqNo, ResourceRequest rr)
            throws JsonParseException, JsonMappingException, IOException {
        MFApiCaller rmFlowRestApi = new MFApiCaller();
        JSONParser jsonParse = new JSONParser();
        OduFlowCreationResponse optFlow = jsonParse.oduFlowCreationResponseToPOJO(
                rmFlowRestApi.createFlow(rr, seqNo), seqNo);
        return optFlow;
    }

    private OptLink convertBasicFlowToOptLink(BasicFlow basicFlowMessage) {
        OptLink optlink = new OptLink();

        optlink.setTerminationPoints(makeTerminationPoints(basicFlowMessage,
                new TerminationPoints()));
        optlink.setDirection(makeDirection(basicFlowMessage));
        optlink.setConstraint(makeConstraint(basicFlowMessage));

        return optlink;
    }

    private String makeDirection(BasicFlow basicFlow) {
        if (null == basicFlow.getAttribute("direction")) {
            return "unidirectional";
        }
        return basicFlow.getAttribute("direction");
    }

    private TerminationPoints makeTerminationPoints(BasicFlow basicFlow, TerminationPoints point) {
        point.setInPoint(basicFlow.getMatches().get(0).getInPort());

        for (List<FlowAction> action : basicFlow.getEdgeActions().values()) {
            FlowActionOutput flowActionOutput = (FlowActionOutput) action.get(0);
            point.setOutPoint(flowActionOutput.getOutput());
        }

        return point;
    }

    private Constraint makeConstraint(BasicFlow basicFlow) {
        Constraint cons = new Constraint();
        cons.setBandwidth(basicFlow.getAttribute(Logic.AttrElements.REQ_BANDWIDTH));
        cons.setLatency(basicFlow.getAttribute(Logic.AttrElements.REQ_LATENCY));
        return cons;
    }

    public boolean deleteOduFlow(ResourceInfoFlomMf resourceInfoFromMf,
            Flow flow, String seqNo)
            throws JSONException, IOException {
        logger.info(seqNo + "\t" + "deleteOduFlow() Start");

        RmData data = getOduReplacementTable(resourceInfoFromMf.getResourse().getFjFlowId(),
                ((OduFlow) resourceInfoFromMf.getResourse()).getTerminationPointPairs(),
                seqNo);

        OptFlow optFlow = new OptFlow();
        optFlow.setTerminationPointPairs(((OduFlow) resourceInfoFromMf.getResourse())
                .getTerminationPointPairs());

        JSONParser parser = new JSONParser();

        sendOfCtlSender(makeDeleteOduFlow(data, optFlow, seqNo),
                OfCtlSender.FLOW_DELETE_METHOD, ODU_SEND_TIME, seqNo);

        FlowResponse targetflow = new FlowResponse();
        targetflow.setFjFlowId(resourceInfoFromMf.getMfId());
        String sendParam = parser.convertToJson(targetflow, seqNo);
        MFApiCaller mfSender = new MFApiCaller();
        mfSender.deleteOduFlow(sendParam, seqNo);

        logger.info(seqNo + "\t" + "deleteOduFlow() End");
        return true;
    }

    private List<String> makeDeleteOduFlow(RmData data, OptFlow optFlow, String seqNo) {
        AbstractFlowCreator flowCreator = new OduFlowCreator();
        List<String> flows = flowCreator.createFlow(optFlow, data, seqNo);
        logger.debug(seqNo + "\t" + "getFlow : " + flows.toString());
        return flows;
    }
}
