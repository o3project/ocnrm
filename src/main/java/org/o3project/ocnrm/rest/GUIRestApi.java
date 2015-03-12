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
package org.o3project.ocnrm.rest;

import java.util.List;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.o3project.ocnrm.lib.OcnMFSequenceLib;
import org.o3project.ocnrm.odenos.lib.OdenOsSender;
import org.o3project.odenos.core.component.network.flow.Flow;
import org.o3project.odenos.core.component.network.flow.FlowSet;
import org.o3project.odenos.core.component.network.flow.basic.BasicFlow;
import org.o3project.odenos.core.component.network.flow.basic.BasicFlowMatch;
import org.o3project.odenos.core.component.network.flow.basic.FlowAction;
import org.o3project.odenos.core.component.network.flow.basic.FlowActionOutput;
import org.o3project.odenos.core.component.network.topology.Link;
import org.o3project.odenos.core.component.network.topology.Node;
import org.o3project.odenos.core.component.network.topology.Port;
import org.o3project.odenos.core.component.network.topology.Topology;
import org.o3project.odenos.remoteobject.message.MessageBodyUnpacker.ParseBodyException;
import org.o3project.odenos.remoteobject.message.Request.Method;
import org.o3project.odenos.remoteobject.message.Response;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;

public class GUIRestApi extends ServerResource {
    private Logger logger = LoggerFactory.getLogger(GUIRestApi.class);
    private OdenOsSender sender = OdenOsSender.getInstance();
    private static final String SEQNO_PREFIX = "#";
    private OcnMFSequenceLib mf = OcnMFSequenceLib.getInstance();
    private String seqNo = "";

    /**
     * Data acquisition API of NW component.
     * Flow information, topology information,
     * and boundary information (Layerized nw is only specified) are acquired.
     *
     * GET /demo/info?NWCID=\<NW component name\>
     * <br>Example<br>
     * http://localhost:44444/demo/info?NWCID=networkcomponent012,networkcomponent01
     * @return Representation
     */
    @Get
    public Representation getNWComponentInfo() {
        seqNo = SEQNO_PREFIX + mf.requestNoToString();

        logger.info(seqNo + "\t" + "getNWComponentInfo Start");
        logger.info(seqNo + "\t" + "getQueryValue(\"NWCID\") : " + getQueryValue("NWCID"));
        JSONObject result = new JSONObject();

        String nwcIdQuery = getQueryValue("NWCID");
        if (nwcIdQuery == null) {
            return new JsonRepresentation(result);
        }

        String[] nwcIds = getQueryValue("NWCID").split(",");

        try {
            for (String nwcId : nwcIds) {
                JSONObject json = new JSONObject();

                json.put("flow", makeFlows(nwcId));
                json.put("topology", makeTopology(nwcId));

                String objectId = sender.getConnections(nwcId);
                if (objectId != null) {
                    json.put("boundaries", makeBoundary(nwcId, objectId));
                }
                result.put(nwcId, json);
            }
        } catch (JsonProcessingException e) {
            logger.error(seqNo + "\t" + "JsonProcessingException is occured: " + e.getMessage());
            e.printStackTrace();
        } catch (JSONException e) {
            logger.error(seqNo + "\t" + "JSONException is occured: " + e.getMessage());
            e.printStackTrace();
        } catch (ParseBodyException e) {
            logger.error(seqNo + "\t" + "ParseBodyException is occured: " + e.getMessage());
            e.printStackTrace();
        }
        logger.debug(seqNo + "\t" + "response to GUI : " + result);
        logger.info(seqNo + "\t" + "getNWComponentInfo End");
        return new JsonRepresentation(result);
    }

    private JSONObject makeBoundary(String nwcId, String objectId) throws ParseBodyException {
        Response response = sender.sendRequest("systemmanager", Method.GET,
                "components/" + objectId + "/settings/boundaries", "");
        logger.debug(seqNo + "\t" + "Response:" + response.statusCode);
        logger.debug(seqNo + "\t" + "Response Body:" + response.getBodyValue().toString());

        String str = response.getBody(String.class);
        logger.debug(seqNo + "\t" + "Boundaries : " + str);

        JSONObject boundaries = new JSONObject(str);

        return boundaries;
    }

    private JSONObject makeTopology(String nwcId)
            throws ParseBodyException, JsonProcessingException {
        Response response = sender.sendRequest(nwcId, Method.GET, "topology", null);
        logger.debug(seqNo + "\t" + "Response:" + response.statusCode);
        logger.debug(seqNo + "\t" + "Response Body:" + response.getBodyValue().toString());

        Topology resource = response.getBody(Topology.class);

        JSONObject elems = new JSONObject();

        JSONObject links = new JSONObject();
        for (Link link : resource.getLinkMap().values()) {
            JSONObject json = new JSONObject();
            json.put("attributes", link.getAttributes());
            json.put("dst_node", link.getDstNode());
            json.put("dst_port", link.getDstPort());
            json.put("link_id", link.getId());
            json.put("src_node", link.getSrcNode());
            json.put("src_port", link.getSrcPort());
            json.put("type", link.getType());
            json.put("version", link.getVersion());

            links.put(link.getId(), json);
        }
        elems.put("links", links);

        JSONObject nodes = new JSONObject();
        for (Node node : resource.getNodeMap().values()) {
            JSONObject json = new JSONObject();
            json.put("attributes", node.getAttributes());
            json.put("node_id", node.getId());

            JSONObject ports = new JSONObject();
            for (Port value : node.getPortMap().values()) {
                JSONObject port = new JSONObject();
                port.put("attributes", value.getAttributes());
                port.put("in_link", value.getInLink());
                port.put("node_id", value.getNode());
                port.put("out_link", value.getOutLink());
                port.put("port_id", value.getId());
                port.put("type", value.getType());
                port.put("version", value.getVersion());

                ports.put(value.getId(), port);
            }
            json.put("ports", ports);
            json.put("type", node.getType());
            json.put("version", node.getVersion());

            nodes.put(node.getId(), json);
        }
        elems.put("nodes", nodes);

        elems.put("type", resource.type);
        elems.put("version", resource.getVersion());

        logger.debug(seqNo + "\t" + "result -> " + elems);
        return elems;
    }

    private JSONObject makeFlows(String nwcId)
            throws ParseBodyException, JsonProcessingException {
        Response flowResponse = sender.sendRequest(nwcId, Method.GET, "flows", null);
        logger.debug(seqNo + "\t" + "Response:" + flowResponse.statusCode);
        logger.debug(seqNo + "\t" + "Response Body:" + flowResponse.getBodyValue().toString());

        FlowSet flowSet = flowResponse.getBody(FlowSet.class);

        JSONObject json = new JSONObject();
        for (Flow flow : flowSet.getFlows().values()) {
            BasicFlow basicFlow = (BasicFlow) flow;

            JSONObject elems = new JSONObject();

            elems.put("attributes", basicFlow.getAttributes());
            JSONObject edgeActions = new JSONObject();
            for (Entry<String, List<FlowAction>> entry : basicFlow.getEdgeActions().entrySet()) {
                JSONArray actions = new JSONArray();
                for (FlowAction value : entry.getValue()) {
                    JSONObject action = new JSONObject();
                    action.put("type", value.getType());
                    action.put("output", ((FlowActionOutput) value).getOutput());
                    actions.put(action);
                }
                edgeActions.put(entry.getKey(), actions);
            }
            elems.put("edge_actions", edgeActions);
            elems.put("enabled", basicFlow.getEnabled());
            elems.put("flow_id", basicFlow.getFlowId());

            JSONArray matches = new JSONArray();
            for (BasicFlowMatch value : basicFlow.getMatches()) {
                JSONObject match = new JSONObject();
                match.put("in_node", value.getInNode());
                match.put("in_port", value.getInPort());
                match.put("type", value.getType());
                matches.put(match);
            }
            elems.put("matches", matches);
            elems.put("owner", basicFlow.getOwner());
            elems.put("path", basicFlow.getPath());
            elems.put("priority", basicFlow.getPriority());
            elems.put("status", basicFlow.getStatus());
            elems.put("type", basicFlow.getType());
            elems.put("version", basicFlow.getVersion());

            json.put(flow.getFlowId(), elems);
        }
        JSONObject flows = new JSONObject();
        flows.put("flows", json);

        JSONObject priorities = new JSONObject(flowSet.priority);
        flows.put("priority", priorities);

        flows.put("type", flowSet.type);
        flows.put("version", flowSet.getVersion());

        logger.debug(seqNo + "\t" + "result -> " + flows.toString());
        return flows;
    }
}
