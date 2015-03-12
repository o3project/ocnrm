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
package org.o3project.ocnrm.odenos.driver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONException;
import org.json.JSONObject;
import org.o3project.ocnrm.lib.JSONParser;
import org.o3project.ocnrm.lib.table.ResourceInfoFlomMf;
import org.o3project.ocnrm.lib.table.TableManager;
import org.o3project.ocnrm.model.LowerNodeInfo;
import org.o3project.ocnrm.model.TerminationPoints;
import org.o3project.ocnrm.model.odu.OduFlowCreationResponse;
import org.o3project.ocnrm.odenos.driver.controller.ResourceSendController;
import org.o3project.ocnrm.odenos.lib.OdenOsCommonLib;
import org.o3project.ocnrm.odenos.lib.OdenOsTopologyLib;
import org.o3project.odenos.core.component.Logic;
import org.o3project.odenos.core.component.NetworkInterface;
import org.o3project.odenos.core.component.network.flow.Flow;
import org.o3project.odenos.core.component.network.flow.FlowObject.FlowStatus;
import org.o3project.odenos.core.component.network.flow.basic.BasicFlow;
import org.o3project.odenos.core.component.network.flow.basic.BasicFlowMatch;
import org.o3project.odenos.core.component.network.flow.basic.FlowActionOutput;
import org.o3project.odenos.core.component.network.topology.Link;
import org.o3project.odenos.core.component.network.topology.Topology;
import org.o3project.odenos.remoteobject.RequestParser;
import org.o3project.odenos.remoteobject.message.MessageBodyUnpacker.ParseBodyException;
import org.o3project.odenos.remoteobject.message.Request.Method;
import org.o3project.odenos.remoteobject.message.Response;
import org.o3project.odenos.remoteobject.messagingclient.MessageDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class OptDriver extends AbstractDriver {
    private TableManager tableManager = TableManager.getInstance();
    private Logger logger = LoggerFactory.getLogger(OptDriver.class);

    private final String driverName = "optDriver";
    private static final String FLOW_PRIORITY = "256";

    public OptDriver(String objectId, String baseUri, MessageDispatcher dispatcher)
            throws Exception {
        super(objectId, baseUri, dispatcher);
    }

    @Override
    protected String getDescription() {
        return this.driverName;
    }

    @Override
    protected RequestParser<IActionCallback> createParser() {
        return new RequestParser<IActionCallback>() {
            {
                addRule(Method.PUT, "settings/och/topology", new IActionCallback() {
                    @Override
                    public Response process(
                            final RequestParser<IActionCallback>.ParsedRequest parsed) {
                        String seqNo = SEQNO_PREFIX + mf.requestNoToString();
                        String nwId = connectionIdMap.get(OCH_LAYER);
                        NetworkInterface nwIf = networkInterfaces().get(nwId);

                        try {
                            JSONObject json = new JSONObject(
                                    parsed.getRequest().getBody(String.class));

                            JSONParser parser = new JSONParser();
                            LowerNodeInfo nodeInfo = parser.lowerNodeInfotoPOJO(json, seqNo);

                            OdenOsTopologyLib topologyCreator = new OdenOsTopologyLib();
                            Topology topology = topologyCreator.
                                    createTopology(nodeInfo.getLower().getNode(),
                                            nodeInfo.getLower().getPort(),
                                            nodeInfo.getLower().getLink());
                            sendToporogy(nwIf, nwId, topology);
                            createOdenosFlow(nwIf, nodeInfo.getLower().getFlow(), seqNo);
                        } catch (Exception e) {
                            logger.error("in [settings/och/topology] Exception occurred.");
                            e.printStackTrace();
                            return new Response(Response.BAD_REQUEST, "");
                        }
                        return new Response(Response.OK, "");
                    }
                });
                addRule(Method.PUT, "settings/odu/topology", new IActionCallback() {
                    @Override
                    public Response process(
                            final RequestParser<IActionCallback>.ParsedRequest parsed) {
                        try {
                            return sendTopologyToOdenos(ODU_LAYER,
                                    parsed.getRequest().getBody(String.class));
                        } catch (ParseBodyException e) {
                            logger.error("in [settings/odu/topology] ParseBodyException occurred.");
                            e.printStackTrace();
                            return new Response(Response.BAD_REQUEST, "");
                        }
                    }
                });
            }
        };
    }

    protected Response flowMessageManager(NetworkInterface networkIf, String nwId, String action,
            Flow flowMess, String seqNo)
            throws JSONException, IOException {
        logger.info(seqNo + "\t" + "flowMessageManager Start");
        logger.info(seqNo + "\t" + "nw_id : " + nwId);

        if (!(flowMess.getType()).equals("BasicFlow")) {
            logger.debug(seqNo + "\t" + "getType() == " + flowMess.getType());
            return null;
        }

        logger.debug(seqNo + "\t" + "getType() == BasicFlow");

        BasicFlow flm = (BasicFlow) flowMess;

        org.o3project.ocnrm.lib.table.Event event = tableManager.createEvent(nwId,
                flm.getAttribute(TableManager.TRANSACTION_ID), Logic.FLOW_CHANGED,
                flm.getFlowId(), action, driverName);

        OduFlowCreationResponse resourceInfofromMF = registerOdu(seqNo, flm, event);

        Map<String, Link> links = networkIf.getLinks();

        formFlowMessage(flm, resourceInfofromMF, links, event.getTransactionId());

        Response ret = networkIf.putFlow(flm);
        logger.debug(seqNo + "\t" + "Response:" + ret.statusCode);
        logger.debug(seqNo + "\t" + "Response Body:" + ret.getBodyValue().toString());
        logger.info(seqNo + "\t" + "flowMessageManager End");

        return ret;
    }

    private OduFlowCreationResponse registerOdu(String seqNo, BasicFlow flm,
            org.o3project.ocnrm.lib.table.Event event)
            throws JsonParseException, JsonMappingException, IOException {
        ResourceSendController rsc = new ResourceSendController();
        OduFlowCreationResponse resourceInfofromMF = rsc
                .registerNewOduFlow(flm, event, seqNo);
        return resourceInfofromMF;
    }

    private void formFlowMessage(BasicFlow flm, OduFlowCreationResponse resourceInfofromMF,
            Map<String, Link> linkSetMessage, String transactionId) {
        logger.debug("formFlowMessage Start");

        List<TerminationPoints> points = makeCtpPoints(resourceInfofromMF);

        for (TerminationPoints point : points) {
            logger.debug("ctpp link-> srcPort: " + point.getInPoint()
                    + ", dstPort: " + point.getOutPoint());
            for (Entry<String, Link> entry : linkSetMessage.entrySet()) {
                logger.debug("link from odnos-> srcPort: " + entry.getValue().getSrcPort()
                        + ", dstPort: " + entry.getValue().getDstPort());
                if (point.getInPoint().equals(entry.getValue().getSrcPort())
                        && point.getOutPoint().equals(entry.getValue().getDstPort())) {
                    logger.debug("add path linkId: " + entry.getValue().getId());
                    flm.addPath(entry.getValue().getId());
                    break;
                }
            }
        }

        flm.getAttributes().put(TableManager.TRANSACTION_ID, transactionId);
        flm.setStatus(FlowStatus.ESTABLISHED.toString());
        flm.putAttribute(Logic.AttrElements.BANDWIDTH,
                resourceInfofromMF.getConstraint().getBandwidth());
        flm.putAttribute(Logic.AttrElements.LATENCY,
                resourceInfofromMF.getConstraint().getLatency());
        logger.debug("formFlowMessage End");
    }

    private List<TerminationPoints> makeCtpPoints(OduFlowCreationResponse resourceInfofromMF) {
        List<TerminationPoints> cutThroughPoints = new ArrayList<>();
        TerminationPoints startPoint = null;
        TerminationPoints endPoint = null;
        for (TerminationPoints point : resourceInfofromMF.getTerminationPointPairs()) {
            logger.debug("target points -> src: " + point.getInPoint()
                    + ", dst: " + point.getOutPoint());
            if (checkStartPoint(point)) {
                startPoint = point;
                logger.debug("startPoint addded.");
            } else if (checkEndPoint(point)) {
                endPoint = point;
                logger.debug("endPoint addded.");
            } else {
                cutThroughPoints.add(point);
                logger.debug("cutThroughPoints addded.");
            }
        }

        if (startPoint == null || endPoint == null) {
            return new ArrayList<TerminationPoints>();
        }

        List<TerminationPoints> points = new ArrayList<>();
        TerminationPoints start = new TerminationPoints();

        start.setInPoint(startPoint.getOutPoint());
        points.add(start);

        if (!cutThroughPoints.isEmpty()) {
            for (TerminationPoints point : cutThroughPoints) {
                points.get(points.size() - 1).setOutPoint(point.getInPoint());

                TerminationPoints nextPoint = new TerminationPoints();
                nextPoint.setInPoint(point.getOutPoint());

                points.add(nextPoint);
            }
        }

        points.get(points.size() - 1).setOutPoint(endPoint.getInPoint());

        return points;
    }

    private boolean checkStartPoint(TerminationPoints point) {
        boolean inPointResult = checkCtp(point.getInPoint());

        if (!inPointResult) {
            return true;
        }
        return false;
    }

    private boolean checkEndPoint(TerminationPoints point) {
        boolean outPointResult = checkCtp(point.getOutPoint());
        if (!outPointResult) {
            return true;
        }
        return false;
    }

    private boolean checkCtp(String param) {
        String[] strParse1 = param.split(",");
        String[] strParse2 = strParse1[3].split("=");
        if (strParse2[0].equals("CTP")) {
            return true;
        }
        return false;
    }

    @Override
    protected void deleteFlow(NetworkInterface networkIf, String nwId, Flow flow, String seqNo)
            throws JSONException, IOException {
        ResourceInfoFlomMf resource = tableManager.checkExistingEvent(
                flow.getAttribute(TableManager.TRANSACTION_ID), nwId, flow.getFlowId(), driverName);

        if (!deleteResource(flow, seqNo, resource)) {
            return;
        }

        tableManager.delete(flow.getAttribute(TableManager.TRANSACTION_ID),
                nwId, flow.getFlowId());
    }

    private boolean deleteResource(Flow flow, String seqNo, ResourceInfoFlomMf resource)
            throws IOException {
        ResourceSendController sender = new ResourceSendController();
        return sender.deleteOduFlow(resource, flow, seqNo);
    }

    @Override
    protected void linkMessageManager(NetworkInterface networkIf, String nwId, String action,
            Link link, String seqNo)
            throws JsonParseException, JsonMappingException, JSONException, IOException {
    }

    @Override
    protected void deleteLink(NetworkInterface networkIf, String nwId, Link link, String seqNo) {
    }

    private void createOdenosFlow(NetworkInterface nwIf,
            List<org.o3project.ocnrm.model.Flow> flowList, String seqNo) {
        if (flowList == null) {
            logger.debug(seqNo + "\t" + "flow is empty.");
            return;
        }

        logger.debug(seqNo + "\t" + "flow_list.size():" + flowList.size());

        for (org.o3project.ocnrm.model.Flow flow : flowList) {
            logger.debug(seqNo + "\t" + "flow.getFlowId():" + flow.getFlowId());

            BasicFlow odenosFlow = new BasicFlow(flow.getFlowId(), "ANY", true, FLOW_PRIORITY);
            odenosFlow.setStatus(FlowStatus.ESTABLISHED.toString());
            logger.debug(seqNo + "\t" + "odenos_flow.getStatus():" + odenosFlow.getStatus()
                    .toString());

            BasicFlowMatch match = new BasicFlowMatch(OdenOsCommonLib
                    .getNodeId(flow.getSrcTTP(), seqNo), flow.getSrcTTP());

            logger.debug(seqNo + "\t" + "match:" + match.toString());

            odenosFlow.addMatch(match);

            for (String linkId : flow.getLinkId()) {
                odenosFlow.addPath(linkId);
                logger.debug("link ID: " + linkId);
            }

            FlowActionOutput actionOutput = new FlowActionOutput(flow.getDstTTP());
            odenosFlow.addEdgeAction(OdenOsCommonLib.getNodeId(flow.getDstTTP(), seqNo),
                    actionOutput);

            nwIf.putFlow(odenosFlow);
        }
    }
}
