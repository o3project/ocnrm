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

import static org.o3project.ocnrm.odenos.lib.OdenOsCommonLib.statusLog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.json.JSONObject;
import org.o3project.ocnrm.lib.JSONParser;
import org.o3project.ocnrm.lib.OcnMFSequenceLib;
import org.o3project.ocnrm.model.UpperNodeInfo;
import org.o3project.ocnrm.odenos.lib.OdenOsTopologyLib;
import org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer;
import org.o3project.odenos.core.component.Driver;
import org.o3project.odenos.core.component.Logic;
import org.o3project.odenos.core.component.NetworkInterface;
import org.o3project.odenos.core.component.network.flow.Flow;
import org.o3project.odenos.core.component.network.flow.FlowChanged;
import org.o3project.odenos.core.component.network.flow.FlowObject;
import org.o3project.odenos.core.component.network.flow.FlowObject.FlowStatus;
import org.o3project.odenos.core.component.network.flow.basic.BasicFlow;
import org.o3project.odenos.core.component.network.packet.InPacketAdded;
import org.o3project.odenos.core.component.network.packet.OutPacketAdded;
import org.o3project.odenos.core.component.network.topology.Link;
import org.o3project.odenos.core.component.network.topology.LinkChanged;
import org.o3project.odenos.core.component.network.topology.Node;
import org.o3project.odenos.core.component.network.topology.NodeChanged;
import org.o3project.odenos.core.component.network.topology.PortChanged;
import org.o3project.odenos.core.component.network.topology.Topology;
import org.o3project.odenos.core.manager.system.ComponentConnection;
import org.o3project.odenos.core.manager.system.ComponentConnectionLogicAndNetwork;
import org.o3project.odenos.core.manager.system.event.ComponentConnectionChanged;
import org.o3project.odenos.remoteobject.RequestParser;
import org.o3project.odenos.remoteobject.message.Event;
import org.o3project.odenos.remoteobject.message.MessageBodyUnpacker.ParseBodyException;
import org.o3project.odenos.remoteobject.message.Request;
import org.o3project.odenos.remoteobject.message.Response;
import org.o3project.odenos.remoteobject.messagingclient.MessageDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public abstract class AbstractDriver extends Driver {
    private static final int AWAIT_TIME = 5 * 1000;
    private static final int THREAD_NUM = 16;
    private Logger logger = LoggerFactory.getLogger(getClass());
    protected Map<String, String> connectionIdMap;

    protected static final String OCH_LAYER = "och";
    protected static final String ODU_LAYER = "odu";

    protected final RequestParser<IActionCallback> parser;

    private ExecutorService service;

    protected static final String SEQNO_PREFIX = "#";
    protected OcnMFSequenceLib mf = OcnMFSequenceLib.getInstance();
    protected String seqNo = "";

    interface IActionCallback {
        public Response process(
                RequestParser<IActionCallback>.ParsedRequest parser)
                throws Exception;
    }

    public AbstractDriver(String objectId, String baseUri, MessageDispatcher dispatcher)
            throws Exception {
        super(objectId, dispatcher);
        this.service = Executors.newFixedThreadPool(THREAD_NUM);
        this.connectionIdMap = new HashMap<>();
        this.parser = createParser();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        try {
            service.shutdown();
            if (!service.awaitTermination(AWAIT_TIME, TimeUnit.MILLISECONDS)) {
                service.shutdownNow();
            }
        } catch (InterruptedException e) {
            logger.error("awaitTermination interrupted: " + e);
            service.shutdownNow();
        }

    }

    protected abstract RequestParser<IActionCallback> createParser();

    protected abstract Response flowMessageManager(NetworkInterface networkIf, String nwId,
            String action, Flow flowMess, String seqNo) throws JSONException, IOException;

    protected abstract void linkMessageManager(NetworkInterface networkIf, String nwId,
            String action, Link link, String seqNo)
            throws JsonParseException, JsonMappingException, JSONException, IOException;

    protected abstract void deleteFlow(NetworkInterface networkIf, String nwId,
            Flow flow, String seqNo)
            throws JSONException, IOException;

    protected abstract void deleteLink(NetworkInterface networkIf, String nwId,
            Link link, String seqNo);

    // //////////////////////////////////////////////////
    //
    // EventDispatcher
    //
    // //////////////////////////////////////////////////
    @Override
    protected void onEvent(final Event event) {
        logger.debug("onEvnet : objcetId = '" + this.getObjectId() + "'.");

        try {
            if (event.eventType.equals(ComponentConnectionChanged.TYPE)) {
                logger.debug("onEvnet ConnectionChanged : objcetId = '"
                        + this.getObjectId() + "'.");
                onEventComponentConnection(event
                        .getBody(ComponentConnectionChanged.class));
                return;
            }

            logger.debug("Recieved Message: " + event.eventType);
            if (event.eventType == null) {
                return;
            }

            switch (event.eventType) {
            case NodeChanged.TYPE:
                onNodeChanged(event.publisherId,
                        event.getBody(NodeChanged.class));
                break;
            case PortChanged.TYPE:
                onPortChanged(event.publisherId,
                        event.getBody(PortChanged.class));
                break;
            case LinkChanged.TYPE:
                onLinkChanged(event.publisherId,
                        event.getBody(LinkChanged.class));
                break;
            case FlowChanged.TYPE:
                onFlowChanged(event.publisherId,
                        event.getBody(FlowChanged.class));
                break;
            case InPacketAdded.TYPE:
                onInPacketAdded(event.publisherId,
                        event.getBody(InPacketAdded.class));
                break;
            case OutPacketAdded.TYPE:
                onOutPacketAdded(event.publisherId,
                        event.getBody(OutPacketAdded.class));
                break;
            default:
                logger.info("Unexpected event: " + event.eventType);
                break;
            }
        } catch (ParseBodyException e) {
            logger.error("Recieved Message which can't be parsed.");
        } catch (Exception e) {
            logger.error("Recieved Message Exception.", e);
        }
    }

    @Override
    protected final Response onRequest(final Request request) {
        logger.info(seqNo + "\t-- " + new Throwable().getStackTrace()[0].getMethodName()
                + " Start");
        logger.debug("received {}", request.path);
        final RequestParser<IActionCallback>.ParsedRequest parsed = parser
                .parse(request);
        if (parsed == null) {
            logger.error("ParsedRequest is null.");
            return new Response(Response.BAD_REQUEST, "Error unknown request ");
        }

        final IActionCallback callback = parsed.getResult();
        if (callback == null) {
            logger.error("IActionCallback is null.");
            return new Response(Response.BAD_REQUEST, "Error unknown request ");
        }

        service.submit(new Callable<Response>() {
            public Response call() {
                try {
                    return callback.process(parsed);
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.error("Exception is occurred.");
                    return new Response(Response.BAD_REQUEST, "Error unknown request ");
                }
            }
        });
        return new Response(Response.OK, "");
    }

    // //////////////////////////////////////////////////
    //
    // NetworkComponentConnection
    //
    // //////////////////////////////////////////////////
    private void onEventComponentConnection(final ComponentConnectionChanged message) {
        ComponentConnection curr = message.curr();
        String nwcId = curr.getProperty(ComponentConnectionLogicAndNetwork.NETWORK_ID);
        if (nwcId == null) {
            return;
        }

        if (CONN_ADD.equals(message.action())) {
            logger.debug("Message Action is add.");
            if (onConnectionChangedAddedPre(message)) {
                // Add Network Interface
                if (networkInterfaces().containsKey(nwcId)) {
                    return;
                }
                NetworkInterface networkInterface = new NetworkInterface(
                        messageDispatcher, nwcId);
                networkInterfaces().put(nwcId, networkInterface);
                onConnectionChangedAdded(message);
                return;
            }
        } else if (CONN_UPDATE.equals(message.action())) {
            logger.debug("Message Action is update.");
            if (onConnectionChangedUpdatePre(message)) {
                // Add Network Interface
                if (networkInterfaces().containsKey(nwcId)) {
                    return;
                }
                NetworkInterface networkInterface = new NetworkInterface(
                        messageDispatcher, nwcId);
                networkInterfaces().put(nwcId, networkInterface);
                onConnectionChangedUpdate(message);
                return;
            }
        } else if (CONN_DELETE.equals(message.action())) {
            logger.debug("Message Action is delete.");
            if (onConnectionChangedDeletePre(message)) {
                onConnectionChangedDelete(message);
                // Delete Network Interface
                networkInterfaces().remove(nwcId);
                return;
            }
        }
    }

    @Override
    protected final boolean onConnectionChangedAddedPre(final ComponentConnectionChanged msg) {
        seqNo = SEQNO_PREFIX + mf.requestNoToString();
        logger.info(seqNo + "\t" + "-- onConnectionChangedAddedPre Start");

        boolean ret = true;

        if (null == msg.curr()) {
            logger.error(seqNo + "\t" + "-- [add] msg.curr() is null");
            return false;
        }

        ComponentConnection compConn = msg.curr();
        String nwId = compConn
                .getProperty(ComponentConnectionLogicAndNetwork.NETWORK_ID);

        String type = msg.curr().getConnectionType();
        if (!type.equals(OCH_LAYER) && !type.equals(ODU_LAYER)) {
            logger.error(seqNo + "\t" + "-- [add] msg.curr() type mismatch");
            logger.error(seqNo + "\t" + "-- [add] type: " + type);
            return false;
        }

        String logicId = compConn.getProperty(
                ComponentConnectionLogicAndNetwork.LOGIC_ID);
        if (!this.getObjectId().equals(logicId)) {
            logger.error(seqNo + "\t" + "-- [add] mismatch logic id {} {}",
                    this.getObjectId(), logicId);
            return false;
        }

        if (nwId == null) {
            logger.error(seqNo + "\t" + "-- [add] networkComponentId is null");
            return false;
        } else if (!msg.curr().getObjectType().equals(ComponentConnectionLogicAndNetwork.TYPE)) {
            logger.debug(seqNo + "\t" + "-- [add] msg.curr().Type:" + msg.curr().getObjectType());
            logger.debug(seqNo + "\t" + "-- [add] networkComponentId:" + nwId);
            logger.error(seqNo + "\t" + "-- [add] networkComponentId not eq");
            return false;
        }

        logger.debug(seqNo + "\t" + "-- onConnectionChangedPre Ret:" + ret);
        logger.info(seqNo + "\t" + "-- onConnectionChangedAddedPre End");
        return ret;
    }

    @Override
    protected final void onConnectionChangedAdded(final ComponentConnectionChanged msg) {
        seqNo = SEQNO_PREFIX + mf.requestNoToString();
        logger.info(seqNo + "\t" + "-- onConnectionChangedAdded Start");
        logger.info(seqNo + "\t" + "eventType : " + msg.eventType);
        logger.info(seqNo + "\t" + "publisherId : " + msg.publisherId);

        String type = msg.curr().getConnectionType();
        String nwId = msg.curr().getProperty(ComponentConnectionLogicAndNetwork.NETWORK_ID);
        this.connectionIdMap.put(type, nwId);

        logger.debug(seqNo + "\t" + "-- networkCnnection type:" + type);
        logger.debug(seqNo + "\t" + "-- networkComponentId:" + nwId);
        subscribeNetworkComponent(nwId, seqNo);

        logger.info(seqNo + "\t" + "-- onConnectionChangedAdded End");
    }

    @Override
    protected final boolean onConnectionChangedUpdatePre(final ComponentConnectionChanged msg) {
        seqNo = SEQNO_PREFIX + mf.requestNoToString();
        logger.info(seqNo + "\t" + "-- onConnectionChangedUpdatePre Start");

        boolean ret = true;

        if (null == msg.curr()) {
            logger.error(seqNo + "\t" + "-- [update] msg.curr() is null");
            return false;
        }

        String type = msg.curr().getConnectionType();
        if (!type.equals(OCH_LAYER) && !type.equals(ODU_LAYER)) {
            logger.error(seqNo + "\t" + "-- [update] msg.curr() type mismatch");
            logger.error(seqNo + "\t" + "-- [update] type: " + type);
            return false;
        }

        if (!msg.curr().getObjectType().equals(ComponentConnectionLogicAndNetwork.TYPE)) {
            logger.error(seqNo + "\t" + "-- [update] msg.curr() Object Type mismatch");
            return false;
        }

        String logicId = msg.curr().getProperty(
                ComponentConnectionLogicAndNetwork.LOGIC_ID);
        if (!this.getObjectId().equals(logicId)) {
            logger.error(seqNo + "\t" + "-- [update] mismatch logic id {} {}",
                    this.getObjectId(), logicId);
            return false;
        }

        logger.debug(seqNo + "\t" + "-- onConnectionChangedPre Ret:" + ret);
        logger.info(seqNo + "\t" + "-- onConnectionChangedUpdatePre End");
        return ret;
    }

    @Override
    protected final void onConnectionChangedUpdate(final ComponentConnectionChanged msg) {
        seqNo = SEQNO_PREFIX + mf.requestNoToString();
        logger.info(seqNo + "\t" + "-- onConnectionChangedUpdate Start");
        logger.info(seqNo + "\t" + "eventType : " + msg.eventType);
        logger.info(seqNo + "\t" + "publisherId : " + msg.publisherId);

        String type = msg.curr().getConnectionType();
        String nwId = msg.curr().getProperty(ComponentConnectionLogicAndNetwork.NETWORK_ID);
        this.connectionIdMap.put(type, nwId);

        logger.debug(seqNo + "\t" + "-- networkCnnection type:" + type);
        logger.debug(seqNo + "\t" + "-- networkComponentId:" + nwId);
        subscribeNetworkComponent(nwId, seqNo);

        logger.info(seqNo + "\t" + "-- onConnectionChangedUpdate End");
    }

    @Override
    protected final boolean onConnectionChangedDeletePre(final ComponentConnectionChanged msg) {
        seqNo = SEQNO_PREFIX + mf.requestNoToString();
        logger.info(seqNo + "\t" + "-- onConnectionChangedDeletePre Start");

        boolean ret = true;
        if (null == msg.prev()) {
            logger.error(seqNo + "\t" + "-- [delete] msg.prev is null");
            return false;
        }

        ComponentConnection compConn = msg.prev();
        String networkComponentId = compConn
                .getProperty(ComponentConnectionLogicAndNetwork.NETWORK_ID);

        String objId = msg.prev().getObjectId();
        if (!(objId.equals(getObjectId()))) {
            logger.error(seqNo + "\t" + "-- [delete] msg.curr() Object ID mismatch");
            logger.error("objId:" + objId + ", getObjId(): " + getObjectId());
            return false;
        }

        if (networkComponentId == null) {
            logger.error(seqNo + "\t" + "-- [delete] networkComponentId is not null");
            return false;
        }

        logger.debug(seqNo + "\t" + "-- onConnectionChangedPre Ret:" + ret);
        logger.info(seqNo + "\t" + "-- onConnectionChangedDeletePre End");
        return ret;
    }

    @Override
    protected final void onConnectionChangedDelete(final ComponentConnectionChanged msg) {
        seqNo = SEQNO_PREFIX + mf.requestNoToString();
        logger.info(seqNo + "\t" + "-- onConnectionChangedDelete Start");
        logger.info(seqNo + "\t" + "eventType : " + msg.eventType);
        logger.info(seqNo + "\t" + "publisherId : " + msg.publisherId);

        String type = msg.prev().getConnectionType();
        String nwId = msg.prev().getProperty(ComponentConnectionLogicAndNetwork.NETWORK_ID);

        logger.debug(seqNo + "\t" + "-- networkCnnection type:" + type);
        logger.debug(seqNo + "\t" + "-- networkComponentId:" + nwId);

        unsubscribeNetworkComponent(type, nwId, seqNo);

        logger.info(seqNo + "\t" + "-- onConnectionChangedDelete End");
    }

    private void subscribeNetworkComponent(String nwId, String seqNo) {
        logger.info(seqNo + "\t" + "subscribeNetworkComponent start");
        logger.debug(seqNo + "\t" + "networkcompID : " + nwId);

        addEntryEventSubscription(NODE_CHANGED, nwId);
        addEntryEventSubscription(PORT_CHANGED, nwId);
        addEntryEventSubscription(LINK_CHANGED, nwId);
        addEntryEventSubscription(FLOW_CHANGED, nwId);

        String attrBase = AttrElements.ATTRIBUTES + "::%s";
        ArrayList<String> nodeAttributes = new ArrayList<String>(Arrays.asList(
                String.format(attrBase, AttrElements.ADMIN_STATUS)));

        ArrayList<String> portAttributes = new ArrayList<String>(Arrays.asList(
                String.format(attrBase, AttrElements.ADMIN_STATUS),
                String.format(attrBase, AttrElements.UNRESERVED_BANDWIDTH),
                String.format(attrBase, AttrElements.IS_BOUNDARY)));

        ArrayList<String> linkAttributes = new ArrayList<String>(Arrays.asList(
                String.format(attrBase, AttrElements.OPER_STATUS),
                String.format(attrBase, AttrElements.COST),
                String.format(attrBase, AttrElements.LATENCY),
                String.format(attrBase, AttrElements.REQ_LATENCY),
                String.format(attrBase, AttrElements.MAX_BANDWIDTH),
                String.format(attrBase, AttrElements.UNRESERVED_BANDWIDTH),
                String.format(attrBase, AttrElements.REQ_BANDWIDTH),
                String.format(attrBase, AttrElements.ESTABLISHMENT_STATUS),
                String.format(attrBase, LinkLayerizer.LINK_STATUS_ESTABLISHED)));

        ArrayList<String> flowAttributes = new ArrayList<String>(Arrays.asList(
                NetworkElements.STATUS,
                String.format(attrBase, AttrElements.BANDWIDTH),
                String.format(attrBase, AttrElements.REQ_BANDWIDTH),
                String.format(attrBase, AttrElements.LATENCY),
                String.format(attrBase, AttrElements.REQ_LATENCY)));

        updateEntryEventSubscription(NODE_CHANGED, nwId, nodeAttributes);
        updateEntryEventSubscription(PORT_CHANGED, nwId, portAttributes);
        updateEntryEventSubscription(LINK_CHANGED, nwId, linkAttributes);
        updateEntryEventSubscription(FLOW_CHANGED, nwId, flowAttributes);

        try {
            applyEventSubscription();
        } catch (Exception e) {
            logger.error(seqNo + "\t" + "applyEventSubscription is failed.");
            e.printStackTrace();
        }

        logger.info(seqNo + "\t" + "subscribeNetworkComponent end");
    }

    private void unsubscribeNetworkComponent(String type, String nwId, String seqNo) {
        logger.info(seqNo + "\t" + "unsubscribeNetworkComponent Start");

        removeEntryEventSubscription(NODE_CHANGED, nwId);
        removeEntryEventSubscription(PORT_CHANGED, nwId);
        removeEntryEventSubscription(LINK_CHANGED, nwId);
        removeEntryEventSubscription(FLOW_CHANGED, nwId);

        try {
            applyEventSubscription();
            this.connectionIdMap.remove(type);
        } catch (Exception e) {
            logger.error(seqNo + "\t" + "applyEventSubscription is failed.");
            e.printStackTrace();
        }

        logger.info(seqNo + "\t" + "unsubscribeNetworkComponent End");
    }

    @Override
    protected boolean onFlowAddedPre(final String networkId, final Flow flow) {
        logger.debug(">> " + new Throwable().getStackTrace()[0].getMethodName());
        logger.debug("network component ID : " + networkId);
        statusLog(flow);

        NetworkInterface networkIf = networkInterfaces().get(networkId);
        BasicFlow targetFlow = getFlow(networkIf, flow.getFlowId());

        logger.debug(seqNo + "\t" + "★flow Status:" + flow.getStatus());
        logger.debug(seqNo + "\t" + "targetFlow Status:" + targetFlow.getStatus());

        if (!flow.getStatus().equals(FlowStatus.ESTABLISHED.toString())) {
            statusLog(flow);
            return true;
        }
        statusLog(flow);
        return false;
    }

    @Override
    protected boolean onFlowUpdatePre(
            final String networkId,
            final Flow prev,
            final Flow curr,
            final ArrayList<String> attributesList) {

        logger.debug(">> " + new Throwable().getStackTrace()[0].getMethodName());
        logger.debug("network component ID : " + networkId);
        statusLog(curr);

        NetworkInterface networkIf = networkInterfaces().get(networkId);
        BasicFlow targetFlow = getFlow(networkIf, curr.getFlowId());

        logger.debug(seqNo + "\t" + "★ flow Status:" + curr.getStatus());
        logger.debug(seqNo + "\t" + "targetFlow Status:" + targetFlow.getStatus());

        if (!curr.getStatus().equals(FlowStatus.ESTABLISHED.toString())) {
            statusLog(curr);
            return true;
        }
        statusLog(curr);
        return false;
    }

    private void onFlowProcess(final String networkId, final Flow flow, String action) {
        seqNo = SEQNO_PREFIX + mf.requestNoToString();
        logger.info(seqNo + "\t" + "■onFlowProcess Strat");

        NetworkInterface networkIf = networkInterfaces().get(networkId);
        BasicFlow targetFlow = getFlow(networkIf, flow.getFlowId());

        logger.debug(seqNo + "\t" + "Status:" + flow.getStatus());
        targetFlow.setStatus(FlowObject.FlowStatus.ESTABLISHING.toString());

        try {
            flowMessageManager(networkIf, networkId, action, targetFlow, seqNo);
        } catch (JSONException e) {
            logger.error(seqNo + "\t" + "get JSONException.");
            e.printStackTrace();
            return;
        } catch (IOException e) {
            logger.error(seqNo + "\t" + "get IOException.");
            e.printStackTrace();
            return;
        } catch (Exception e) {
            logger.error(seqNo + "\t" + "get Exception: " + e.getMessage());
            e.printStackTrace();
            return;
        }
        logger.info(seqNo + "\t" + "■onFlowProcess End");
    }

    @Override
    protected void onFlowAdded(final String networkId, final Flow flow) {
        logger.debug("network component ID : " + networkId);
        if (!onFlowAddedPre(networkId, flow)) {
            return;
        }
        statusLog(flow);
        onFlowProcess(networkId, flow, Logic.CONN_ADD);
        statusLog(flow);
    }

    @Override
    protected final void onFlowUpdate(final String networkId, final Flow prev, final Flow curr,
            final ArrayList<String> attributesList) {
        logger.debug("network component ID : " + networkId);
        if (!onFlowUpdatePre(networkId, prev, curr, attributesList)) {
            return;
        }
        statusLog(curr);
        onFlowProcess(networkId, curr, Logic.CONN_UPDATE);
        statusLog(curr);
    }

    @Override
    protected boolean onFlowDeletePre(final String networkId, final Flow flow) {
        logger.debug(">> " + new Throwable().getStackTrace()[0].getMethodName());
        logger.debug("network component ID : " + networkId);
        statusLog(flow);

        if (networkId.equals(connectionIdMap.get(OCH_LAYER))) {
            return false;
        }

        statusLog(flow);
        return true;
    }

    @Override
    protected void onFlowDelete(final String networkId, final Flow flow) {
        seqNo = SEQNO_PREFIX + mf.requestNoToString();
        logger.info(seqNo + "\t" + "■onFlowDelete Strat");
        logger.debug("network component ID : " + networkId);
        if (!onFlowDeletePre(networkId, flow)) {
            return;
        }
        statusLog(flow);

        NetworkInterface networkIf = networkInterfaces().get(networkId);
        Flow targetFlow = flow;
        if (targetFlow.getStatus().equals(FlowObject.FlowStatus.ESTABLISHED.toString())
                && targetFlow.getEnabled()) {
            try {
                deleteFlow(networkIf, networkId, flow, seqNo);
            } catch (JSONException e) {
                logger.error(seqNo + "\t" + "get JSONException.");
                e.printStackTrace();
                statusLog(flow);
                return;
            } catch (IOException e) {
                logger.error(seqNo + "\t" + "get IOException.");
                e.printStackTrace();
                statusLog(flow);
                return;
            }
        }
        statusLog(flow);
        logger.info(seqNo + "\t" + "■onFlowDelete End");
    }

    private void onLinkProcess(final String networkId, final Link link, String action) {
        seqNo = SEQNO_PREFIX + mf.requestNoToString();
        logger.info(seqNo + "\t" + "■onLinkProcess Strat");
        logger.debug(seqNo + "\t" + "Link " + AttrElements.ESTABLISHMENT_STATUS + ":"
                + link.getAttribute(AttrElements.ESTABLISHMENT_STATUS));
        logger.debug(seqNo + "\t" + "action : " + action);

        NetworkInterface networkIf = networkInterfaces().get(networkId);

        if (!link.getAttribute(AttrElements.ESTABLISHMENT_STATUS)
                .equals(FlowObject.FlowStatus.ESTABLISHED.toString())) {
            logger.debug(seqNo + "\t" + "Link not established :" + link.getId());
            return;
        }

        try {
            linkMessageManager(networkIf, networkId, action, link, seqNo);
        } catch (JSONException e) {
            logger.error(seqNo + "\t" + "get JSONException.");
            e.printStackTrace();
            return;
        } catch (IOException e) {
            logger.error(seqNo + "\t" + "get IOException.");
            e.printStackTrace();
            return;
        }
        logger.info(seqNo + "\t" + "■onLinkProcess End");
    }

    @Override
    protected boolean onLinkAddedPre(final String networkId, final Link link) {
        logger.debug("onLinkAddedPre() Start");
        statusLog(link);
        if (link.getAttribute(AttrElements.ESTABLISHMENT_STATUS) == null) {
            logger.debug("This link's status is null.");
            logger.debug("onLinkAddedPre() End");
            return false;
        }
        logger.debug("onLinkAddedPre() End");
        return true;
    }

    @Override
    protected void onLinkAdded(final String networkId, final Link link) {
        logger.debug("network component ID : " + networkId);
        if (!onLinkAddedPre(networkId, link)) {
            return;
        }
        statusLog(link);
        onLinkProcess(networkId, link, Logic.CONN_ADD);
        statusLog(link);
    }

    protected boolean onLinkUpdatePre(final String networkId, final Link prev, final Link curr,
            final ArrayList<String> attributesList) {
        logger.debug(">> " + new Throwable().getStackTrace()[0].getMethodName() + "Start");

        if (curr.getAttribute(AttrElements.ESTABLISHMENT_STATUS) == null) {
            logger.debug("This link's status is null.");
            logger.debug(">> " + new Throwable().getStackTrace()[0].getMethodName() + "End");
            return false;
        }
        logger.debug("link staus: " + curr.getAttribute(AttrElements.ESTABLISHMENT_STATUS));

        logger.debug(">> " + new Throwable().getStackTrace()[0].getMethodName() + "End");
        return true;
    }

    @Override
    protected void onLinkUpdate(final String networkId, final Link prev, final Link curr,
            final ArrayList<String> attributesList) {
        logger.debug("network component ID : " + networkId);
        if (!onLinkUpdatePre(networkId, prev, curr, attributesList)) {
            return;
        }
        statusLog(curr);
        onLinkProcess(networkId, curr, Logic.CONN_UPDATE);
        statusLog(curr);
    }

    @Override
    protected void onLinkDelete(final String networkId, final Link link) {
        seqNo = SEQNO_PREFIX + mf.requestNoToString();
        logger.info(seqNo + "\t" + "■onLinkDelete Strat");
        logger.debug("network component ID : " + networkId);
        if (!onLinkDeletePre(networkId, link)) {
            return;
        }
        statusLog(link);

        NetworkInterface networkIf = networkInterfaces().get(networkId);

        if (link.getAttribute(AttrElements.ESTABLISHMENT_STATUS)
                .equals(FlowObject.FlowStatus.ESTABLISHED.toString())) {
            link.getAttributes().put(AttrElements.ESTABLISHMENT_STATUS,
                    FlowObject.FlowStatus.TEARDOWN.toString());

            deleteLink(networkIf, networkId, link, seqNo);
        }

        statusLog(link);
        logger.info(seqNo + "\t" + "■onLinkDelete End");
    }

    @Override
    protected BasicFlow getFlow(
            final NetworkInterface nwIf,
            final String flowId) {
        logger.debug(">> " + new Throwable().getStackTrace()[0].getMethodName());

        if (nwIf == null) {
            logger.error(seqNo + "\t" + "networkIF is null.");
            return null;
        }

        if (flowId == null) {
            logger.error(seqNo + "\t" + "flow ID is null.");
            return null;
        }

        Flow flow = nwIf.getFlow(flowId);
        if (flow == null) {
            logger.error(seqNo + "\t" + "flow is null.");
            return null;
        }
        if (!flow.getType().equals(BasicFlow.class.getSimpleName())) {
            logger.error(seqNo + "\t" + "flow type mismatch.");
            return null;
        }

        BasicFlow basicFlow = (BasicFlow) flow;
        if (basicFlow.getMatches() == null) {
            logger.error(seqNo + "\t" + "flow matches is null.");
            return null;
        } else if (basicFlow.getMatches().size() == 0) {
            logger.error(seqNo + "\t" + "flow matches is empty.");
            return null;
        }

        return basicFlow;
    };

    protected void sendToporogy(NetworkInterface nwIf, String nwId, Topology topo) {
        logger.info("sendToporogy Start");
        for (Node node : topo.getNodeMap().values()) {
            logger.debug(seqNo + "\t" + "Adding Node: {}", node.getId());
            node.setVersion(OdenOsTopologyLib.INITIAL_VERSION);

            Node oldNodeMessage = nwIf.getNode(node.getId());
            if (null != oldNodeMessage
                    && !(oldNodeMessage.getVersion().equals(OdenOsTopologyLib.INITIAL_VERSION))) {
                node.setVersion(oldNodeMessage.getVersion());
            }

            nwIf.putNode(node);
        }

        for (Link link : topo.getLinkMap().values()) {
            logger.debug("Adding Link: {}", link.getId());

            link.setVersion(OdenOsTopologyLib.INITIAL_VERSION);

            Link oldLinkMessage = nwIf.getLink(link.getId());
            if (null != oldLinkMessage
                    && !(oldLinkMessage.getVersion().equals(OdenOsTopologyLib.INITIAL_VERSION))) {
                link.setVersion(oldLinkMessage.getVersion());
            }

            nwIf.putLink(link);
        }
        logger.info("sendToporogy End");
    }

    protected Response sendTopologyToOdenos(String layer, String param) {
        seqNo = SEQNO_PREFIX + mf.requestNoToString();

        logger.info(seqNo + "\t" + "sendTopologyToOdenos Start");
        logger.info(seqNo + "\t" + "layer: " + layer);

        String nwId = connectionIdMap.get(layer);

        JSONObject json = new JSONObject(param);
        JSONParser parser = new JSONParser();

        try {
            UpperNodeInfo nodeInfo = parser.upperNodeInfotoPOJO(json, seqNo);

            OdenOsTopologyLib topologyCreator = new OdenOsTopologyLib();

            Topology topology = topologyCreator.createTopology(
                    nodeInfo.getUpper().getNode(),
                    nodeInfo.getUpper().getPort(),
                    nodeInfo.getUpper().getLink());
            sendToporogy(networkInterfaces().get(nwId), nwId, topology);
        } catch (Exception e) {
            logger.error(seqNo + "\t" + e + "occurred.");
            e.printStackTrace();
            return new Response(Response.BAD_REQUEST, "");
        }

        logger.info(seqNo + "\t" + "sendTopologyToOdenos End");
        return new Response(Response.OK, "");
    }

}
