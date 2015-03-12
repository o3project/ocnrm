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
package org.o3project.ocnrm.odenos.linklayerizer;

import static org.o3project.ocnrm.odenos.lib.OdenOsCommonLib.statusLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.o3project.ocnrm.lib.table.TableManager;
import org.o3project.ocnrm.odenos.lib.OdenOsSender;
import org.o3project.odenos.core.component.Logic;
import org.o3project.odenos.core.component.NetworkInterface;
import org.o3project.odenos.core.component.network.flow.Flow;
import org.o3project.odenos.core.component.network.flow.FlowChanged;
import org.o3project.odenos.core.component.network.flow.FlowObject.FlowStatus;
import org.o3project.odenos.core.component.network.flow.FlowObject.FlowType;
import org.o3project.odenos.core.component.network.flow.FlowSet;
import org.o3project.odenos.core.component.network.flow.basic.BasicFlow;
import org.o3project.odenos.core.component.network.flow.basic.BasicFlowMatch;
import org.o3project.odenos.core.component.network.flow.basic.FlowAction;
import org.o3project.odenos.core.component.network.flow.basic.FlowActionOutput;
import org.o3project.odenos.core.component.network.packet.InPacketAdded;
import org.o3project.odenos.core.component.network.packet.OutPacketAdded;
import org.o3project.odenos.core.component.network.topology.Link;
import org.o3project.odenos.core.component.network.topology.LinkChanged;
import org.o3project.odenos.core.component.network.topology.Node;
import org.o3project.odenos.core.component.network.topology.NodeChanged;
import org.o3project.odenos.core.component.network.topology.Port;
import org.o3project.odenos.core.component.network.topology.PortChanged;
import org.o3project.odenos.core.manager.system.ComponentConnection;
import org.o3project.odenos.core.manager.system.ComponentConnectionLogicAndNetwork;
import org.o3project.odenos.core.manager.system.event.ComponentConnectionChanged;
import org.o3project.odenos.remoteobject.RequestParser;
import org.o3project.odenos.remoteobject.message.Event;
import org.o3project.odenos.remoteobject.message.MessageBodyUnpacker.ParseBodyException;
import org.o3project.odenos.remoteobject.message.Request;
import org.o3project.odenos.remoteobject.message.Request.Method;
import org.o3project.odenos.remoteobject.message.Response;
import org.o3project.odenos.remoteobject.messagingclient.MessageDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LinkLayerizer extends Logic {
    private Logger logger = LoggerFactory.getLogger(LinkLayerizer.class);

    private LinkLayerizerBoundarySet boundaryset;
    private Map<String, ArrayList<String>> lowerflows;
    private Map<String, String> layerizedlinks;

    private static final String UPPER = "upper";
    private static final String LOWER = "lower";
    private static final String LAYERIZER = "layerized";

    public static final String STATUS_UP = "UP";
    public static final String STATUS_DOWN = "DOWN";
    public static final String LINK_STATUS_ESTABLISHED = "established";
    public static final String LINK_DEFAULT_UNRESERVED_BANDWIDTH = "0 Mbps";

    private static final String FLOW_PRIORITY = "256";
    RequestParser<IActionCallback> parser;

    public LinkLayerizer(final String objectId, final MessageDispatcher dispatcher)
            throws Exception {
        super(objectId, dispatcher);
        logger.info("** LinkLayerizer Start");
        logger.info("** objectId:" + objectId);

        parser = createParser();

        lowerflows = new HashMap<String, ArrayList<String>>();
        layerizedlinks = new HashMap<String, String>();
        boundaryset = new LinkLayerizerBoundarySet();

        logger.info("** LinkLayerizer End");
    }

    @Override
    protected String getSuperType() {
        return LinkLayerizer.class.getSimpleName();
    }

    private static final String DESCRIPTION = "LinkLayerzer Component";

    @Override
    protected String getDescription() {
        return DESCRIPTION;
    }

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
    protected boolean onConnectionChangedAddedPre(
            final ComponentConnectionChanged msg) {
        callStartLog((msg.curr()) == null ? null : msg.curr().getObjectId());

        if (null == msg.curr()) {
            logger.error("** [add] msg.curr is null");
            return false;
        }
        if (!msg.curr().getObjectType()
                .equals(ComponentConnectionLogicAndNetwork.TYPE)) {
            logger.error("** [add] msg.curr Object Type mismatch");
            return false;
        }

        ComponentConnection compConn = msg.curr();
        String logicId = compConn.getProperty(
                ComponentConnectionLogicAndNetwork.LOGIC_ID);
        String networkId = compConn.getProperty(
                ComponentConnectionLogicAndNetwork.NETWORK_ID);
        String status = compConn.getObjectState();
        String type = compConn.getConnectionType();

        if (!this.getObjectId().equals(logicId)) {
            logger.error("** [add] msg.curr Object ID mismatch");
            return false;
        }

        if (null == networkId) {
            logger.error("** [add] networkId is null");
            return false;
        }

        // Type Check
        if (UPPER.equals(type)) {
            // Double registration check
            if (conversionTable().isConnectionType(UPPER)) {
                logger.debug("** [add] {} is already registered. networkid={}", UPPER, networkId);
                status = ComponentConnection.State.ERROR;
            }
        } else if (LOWER.equals(type)) {
            // Double registration check
            if (conversionTable().isConnectionType(LOWER)) {
                logger.debug("** [add] {} is already registered. networkid={}", LOWER, networkId);
                status = ComponentConnection.State.ERROR;
            }
        } else if (LAYERIZER.equals(type)) {
            // Double registration check
            if (conversionTable().isConnectionType(LAYERIZER)) {
                logger.debug("** [add] {} is already registered. networkid={}",
                        LAYERIZER, networkId);
                status = ComponentConnection.State.ERROR;
            }
        } else {
            logger.debug("** [add] connectionType:" + type);
            logger.debug("** [add] networkId:" + networkId);
            logger.error("** [add] Connection Type mismatch");
            status = ComponentConnection.State.ERROR;
        }

        // Registration of the abnormal component
        if (ComponentConnection.State.ERROR.equals(status)) {
            // Changed ConectionProperty's status.
            compConn.setConnectionState(status);
            systemMngInterface().putConnection(compConn);
            return false;
        }

        callEndLog(msg.curr().getObjectId());
        return true;
    }

    @Override
    protected boolean onConnectionChangedUpdatePre(
            final ComponentConnectionChanged msg) {
        if (null == msg.curr()) {
            logger.error("** [update] msg.curr is null");
            return false;
        }

        callStartLog(msg.curr().getObjectId());

        if (!msg.curr().getObjectType()
                .equals(ComponentConnectionLogicAndNetwork.TYPE)) {
            logger.error("getObjectType mismatch. expect="
                    + ComponentConnectionLogicAndNetwork.TYPE
                    + ", but type=" + msg.curr().getObjectType());
            callEndLog(msg.curr().getObjectId());
            return false;
        }
        String logicId = msg.curr().getProperty(
                ComponentConnectionLogicAndNetwork.LOGIC_ID);
        if (!this.getObjectId().equals(logicId)) {
            logger.error("logicId mismatch. expect=" + this.getObjectId()
                    + ", but logicId=" + logicId);
            callEndLog(msg.curr().getObjectId());
            return false;
        }

        String type = msg.curr().getConnectionType();
        String networkId = msg.curr().getProperty(
                ComponentConnectionLogicAndNetwork.NETWORK_ID);

        // Type Check
        if (UPPER.equals(type)) {
            // Double registration check
            if (conversionTable().isConnectionType(UPPER)) {
                logger.debug("** [update] {} is already registered. networkid={}",
                        UPPER, networkId);
                return false;
            }
        } else if (LOWER.equals(type)) {
            // Double registration check
            if (conversionTable().isConnectionType(LOWER)) {
                logger.debug("** [update] {} is already registered. networkid={}",
                        LOWER, networkId);
                return false;
            }
        } else if (LAYERIZER.equals(type)) {
            // Double registration check
            if (conversionTable().isConnectionType(LAYERIZER)) {
                logger.debug("** [update] {} is already registered. networkid={}",
                        LAYERIZER, networkId);
                return false;
            }
        } else {
            logger.debug("** [update] connectionType:" + type);
            logger.debug("** [update] networkId:" + networkId);
            logger.error("** [update] Connection Type mismatch");
            return false;
        }

        callEndLog(msg.curr().getObjectId());
        return true;
    }

    @Override
    protected final boolean onConnectionChangedDeletePre(
            final ComponentConnectionChanged msg) {
        callStartLog(msg.curr().getObjectId());

        if (null == msg.prev()) {
            logger.error("** [delete] msg.prev is null");
            callEndLog(msg.curr().getObjectId());
            return false;
        }
        if (!msg.prev().getObjectType()
                .equals(ComponentConnectionLogicAndNetwork.TYPE)) {
            logger.error("** [delete] msg.prev Object Type mismatch");
            callEndLog(msg.curr().getObjectId());
            return false;
        }

        ComponentConnection compConn = msg.prev();
        String logicId = compConn.getProperty(
                ComponentConnectionLogicAndNetwork.LOGIC_ID);

        if (!this.getObjectId().equals(logicId)) {
            logger.error("** [delete] msg.prev Object ID mismatch");
            callEndLog(msg.curr().getObjectId());
            return false;
        }

        callEndLog(msg.curr().getObjectId());
        return true;
    }

    @Override
    protected final void onConnectionChangedAdded(
            final ComponentConnectionChanged msg) {
        callStartLog(msg.curr().getObjectId());

        ComponentConnection compConn = msg.curr();
        String networkId = compConn.getProperty(
                ComponentConnectionLogicAndNetwork.NETWORK_ID);
        String type = compConn.getConnectionType();

        compConn.getObjectId();

        // Registration of the network ID
        conversionTable().addEntryConnectionType(networkId, type);
        logger.debug("** set conversionTable networkId={}, connectionType={}",
                networkId, type);
        compConn.setConnectionState(ComponentConnection.State.RUNNING);

        if (UPPER.equals(type)) {
            if (!getLayerizerNetworkId().isEmpty()) {
                conversionTable().addEntryNetwork(networkId, getLayerizerNetworkId());
            }
        } else if (LAYERIZER.equals(type)) {
            OdenOsSender.getInstance().addConnection(networkId, this.getObjectId());
            if (!getUpperNetworkId().isEmpty()) {
                logger.debug("upper id: " + getUpperNetworkId());
                conversionTable().addEntryNetwork(networkId, getUpperNetworkId());
            }
        }
        try {
            subscribeNetworkComponent(networkId);
            syncLinkLayerizer();
            systemMngInterface().putConnection(compConn);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("** Failed to subscribe " + networkId);
            callEndLog(msg.curr().getObjectId());
            return;
        }
        callEndLog(msg.curr().getObjectId());
    }

    @Override
    protected void onConnectionChangedUpdate(final ComponentConnectionChanged msg) {
        callStartLog(msg.curr().getObjectId());

        ComponentConnection compConn = msg.curr();
        String networkId = compConn.getProperty(
                ComponentConnectionLogicAndNetwork.NETWORK_ID);
        String type = compConn.getConnectionType();

        // Update of the network ID
        conversionTable().addEntryConnectionType(networkId, type);
        logger.debug("** set conversionTable networkId={}, connectionType={}", networkId, type);
        compConn.setConnectionState(ComponentConnection.State.RUNNING);

        if (UPPER.equals(type)) {
            logger.debug("type: " + UPPER + ", " + type);
            if (!getLayerizerNetworkId().isEmpty()) {
                conversionTable().addEntryNetwork(networkId, getLayerizerNetworkId());
            }
        } else if (LAYERIZER.equals(type)) {
            OdenOsSender.getInstance().addConnection(networkId, this.getObjectId());
            if (!getUpperNetworkId().isEmpty()) {
                logger.debug("layerized id: " + getLayerizerNetworkId());
                conversionTable().addEntryNetwork(networkId, getUpperNetworkId());
            }
        }
        try {
            subscribeNetworkComponent(networkId);
            syncLinkLayerizer();
            systemMngInterface().putConnection(compConn);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("** Failed to subscribe " + networkId);
            callEndLog(msg.curr().getObjectId());
            return;
        }

        callEndLog(msg.curr().getObjectId());
    }

    @Override
    protected final void onConnectionChangedDelete(
            final ComponentConnectionChanged msg) {
        callStartLog(msg.curr().getObjectId());

        ComponentConnection prev = msg.prev();
        String networkId = prev.getProperty(
                ComponentConnectionLogicAndNetwork.NETWORK_ID);

        // Changed ConectionProperty's status.
        prev.setConnectionState(ComponentConnection.State.FINALIZING);
        systemMngInterface().putConnection(prev);

        // Delete flows.
        conversionTable().getFlow().clear();
        // Delete network's topology.
        conversionTable().getLink().clear();
        conversionTable().getNode().clear();
        conversionTable().getPort().clear();
        // Delete networkId from conversionTable.
        conversionTable().delEntryNetwork(getUpperNetworkId());
        conversionTable().delEntryNetwork(getLowerNetworkId());
        conversionTable().delEntryNetwork(getLayerizerNetworkId());

        try {
            unsubscribeNetworkComponent(getUpperNetworkId());
            unsubscribeNetworkComponent(getLowerNetworkId());
            unsubscribeNetworkComponent(getLayerizerNetworkId());

            conversionTable().delEntryConnectionType(getUpperNetworkId());
            conversionTable().delEntryConnectionType(getLowerNetworkId());
            conversionTable().delEntryConnectionType(getLayerizerNetworkId());

            prev.setConnectionState(ComponentConnection.State.NONE);
            systemMngInterface().putConnection(prev);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("** Failed to onConnectionChangedDelete {}", networkId);
        }
        callEndLog(msg.prev().getObjectId());
    }

    private void unsubscribeNetworkComponent(String networkId) {
        callStartLog(networkId);

        removeEntryEventSubscription(NODE_CHANGED, networkId);
        removeEntryEventSubscription(PORT_CHANGED, networkId);
        removeEntryEventSubscription(LINK_CHANGED, networkId);
        removeEntryEventSubscription(FLOW_CHANGED, networkId);

        try {
            this.applyEventSubscription();
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("** Failed to unsubscribeNetworkComponent {}", networkId);
        }
        callEndLog(networkId);
    }

    private void subscribeNetworkComponent(String networkcompID) {
        callStartLog(networkcompID);

        String attrBase = AttrElements.ATTRIBUTES + "::%s";

        addEntryEventSubscription(NODE_CHANGED, networkcompID);
        addEntryEventSubscription(PORT_CHANGED, networkcompID);
        addEntryEventSubscription(LINK_CHANGED, networkcompID);
        addEntryEventSubscription(FLOW_CHANGED, networkcompID);

        updateEntryEventSubscription(NODE_CHANGED, networkcompID, null);
        updateEntryEventSubscription(PORT_CHANGED, networkcompID, null);

        ArrayList<String> linkAttributes = new ArrayList<String>(Arrays.asList(
                String.format(attrBase, AttrElements.OPER_STATUS),
                String.format(attrBase, AttrElements.COST),
                String.format(attrBase, AttrElements.LATENCY),
                String.format(attrBase, AttrElements.REQ_LATENCY),
                String.format(attrBase, AttrElements.MAX_BANDWIDTH),
                String.format(attrBase, AttrElements.UNRESERVED_BANDWIDTH),
                String.format(attrBase, AttrElements.REQ_BANDWIDTH),
                String.format(attrBase, AttrElements.ESTABLISHMENT_STATUS)));
        updateEntryEventSubscription(LINK_CHANGED, networkcompID, linkAttributes);

        ArrayList<String> flowAttributes = new ArrayList<String>(Arrays.asList(
                NetworkElements.STATUS,
                String.format(attrBase, AttrElements.BANDWIDTH),
                String.format(attrBase, AttrElements.REQ_BANDWIDTH),
                String.format(attrBase, AttrElements.LATENCY),
                String.format(attrBase, AttrElements.REQ_LATENCY)));

        updateEntryEventSubscription(FLOW_CHANGED, networkcompID, flowAttributes);

        try {
            applyEventSubscription();
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("** Failed to subscribeNetworkComponent {}", networkcompID);
        }

        callEndLog(networkcompID);
    }

    @Override
    protected boolean onNodeAddedPre(final String networkId, final Node node) {
        callStartLog(networkId);

        boolean result = checkUpperEvent(networkId);

        callEndLog(networkId);
        return result;
    }

    @Override
    protected void onNodeAdded(final String networkId, final Node node) {
        if (!onNodeAddedPre(networkId, node)) {
            return;
        }
        callStartLog(networkId);

        logger.debug("** upper_nw:" + getUpperNetworkId());
        logger.debug("** compID:" + networkId);

        NetworkInterface layerizerNetworkIf = networkInterfaces().get(getLayerizerNetworkId());

        Node layerizerNode = layerizerNetworkIf.getNode(node.getId());

        layerizerNode = node.clone();
        Response ret = layerizerNetworkIf.putNode(layerizerNode);
        logger.debug("** Response:" + ret.statusCode);
        logger.debug("** Response Body:" + ret.getBodyValue().toString());

        conversionTable().addEntryNode(
                networkId, node.getId(),
                getLayerizerNetworkId(), node.getId());

        if (node.getPortMap() != null && node.getPortMap().size() > 0) {
            for (String portId : node.getPortMap().keySet()) {
                conversionTable().addEntryPort(
                        networkId, node.getId(), portId,
                        getLayerizerNetworkId(), node.getId(), portId);
            }
        }

        callEndLog(networkId);
    }

    @Override
    protected boolean onNodeUpdatePre(String networkId, Node prev, Node curr,
            ArrayList<String> attributesList) {
        callStartLog(networkId);

        boolean result = checkUpperEvent(networkId);

        callEndLog(networkId);
        return result;
    }

    @Override
    protected boolean onNodeDeletePre(String networkId, Node node) {
        callStartLog(networkId);

        boolean result = checkUpperEvent(networkId);

        callEndLog(networkId);
        return result;
    }

    @Override
    protected boolean onPortAddedPre(String networkId, Port port) {
        callStartLog(networkId);

        boolean result = checkUpperEvent(networkId);

        callEndLog(networkId);
        return result;
    }

    @Override
    protected boolean onPortUpdatePre(String networkId, Port prev, Port curr,
            ArrayList<String> attributesList) {
        callStartLog(networkId);

        boolean result = checkUpperEvent(networkId);

        callEndLog(networkId);
        return result;
    }

    @Override
    protected boolean onPortDeletePre(String networkId, Port port) {
        callStartLog(networkId);

        boolean result = checkUpperEvent(networkId);

        callEndLog(networkId);
        return result;
    }

    @Override
    protected boolean onLinkAddedPre(String networkId, Link link) {
        callStartLog(networkId, link);

        boolean result = checkLayerizerUpperEvent(networkId, link);

        callEndLog(networkId, link);
        return result;
    }

    @Override
    protected void onLinkAdded(final String networkId, final Link link) {
        if (!onLinkAddedPre(networkId, link)) {
            return;
        }
        callStartLog(networkId, link);

        String type = conversionTable().getConnectionType(networkId);

        if (UPPER.equals(type)) {
            if ((link.getAttribute(AttrElements.ESTABLISHMENT_STATUS))
                    .equals(LINK_STATUS_ESTABLISHED)) {
                logger.debug("*** Upper OnLinkAdded stop : status:"
                        + link.getAttribute(AttrElements.ESTABLISHMENT_STATUS));
                return;
            }

            addLayerizedLink(networkId, link);
        } else if (LAYERIZER.equals(type)) {
            addUpperLink(networkId, link);
            uppdateLowerFlowFromLayerizerLink(link);
        }
        logger.debug("[" + networkId + "] links in conversionTable: "
                + conversionTable().getLink(networkId, link.getId()).toString());

        callEndLog(networkId, link);
    }

    @Override
    protected boolean onLinkUpdatePre(String networkId, Link prev, Link curr,
            ArrayList<String> attributesList) {
        callStartLog(networkId, prev, curr);

        boolean result = checkLayerizerEvent(networkId, curr);

        callEndLog(networkId, prev, curr);
        return result;
    }

    @Override
    protected void onLinkUpdate(String networkId, Link prev, Link curr,
            ArrayList<String> attributesList) {
        if (!onLinkUpdatePre(networkId, prev, curr, attributesList)) {
            return;
        }
        callStartLog(networkId, prev, curr);

        onSuperLinkUpdate(networkId, prev, curr, attributesList);
        uppdateLowerFlowFromLayerizerLink(curr);

        callEndLog(networkId, prev, curr);
    }

    private void onSuperLinkUpdate(String networkId, Link prev, Link curr,
            ArrayList<String> attributesList) {
        super.onLinkUpdate(networkId, prev, curr, attributesList);
    }

    @Override
    protected boolean onLinkDeletePre(String networkId, Link link) {
        callStartLog(networkId, link);

        boolean result = checkLayerizerUpperEvent(networkId, link);

        callEndLog(networkId, link);
        return result;
    }

    @Override
    protected void onLinkDelete(String networkId, Link link) {
        if (!onLinkDeletePre(networkId, link)) {
            return;
        }
        callStartLog(networkId, link);

        logger.debug("[" + networkId + "] links in conversionTable: "
                + conversionTable().getLink(networkId, link.getId()).toString());
        onSuperLinkDelete(networkId, link);
        delLowerFlows(link.getId());

        callEndLog(networkId, link);
    }

    private void onSuperLinkDelete(String networkId, Link link) {
        super.onLinkDelete(networkId, link);
    }

    @Override
    protected boolean onFlowAddedPre(String networkId, Flow flow) {
        callStartLog(networkId, flow);

        boolean result = checkFlowType(flow);

        callEndLog(networkId, flow);
        return result;
    }

    @Override
    protected void onFlowAdded(final String networkId, final Flow flow) {
        if (!onFlowAddedPre(networkId, flow)) {
            return;
        }
        callStartLog(networkId, flow);

        String type = conversionTable().getConnectionType(networkId);

        if (UPPER.equals(type)) {
            logger.debug("type: " + UPPER);
        } else if (LOWER.equals(type)) {
            addLinkOfLayerizerFromFlowOfLower(networkId, flow);
        } else if (LAYERIZER.equals(type)) {
            addFlowToUpper(networkId, flow);
        }

        callEndLog(networkId, flow);
    }

    @Override
    protected boolean onFlowUpdatePre(String networkId, Flow prev, Flow curr,
            ArrayList<String> attributesList) {
        callStartLog(networkId, prev, curr);

        logger.debug("** updated flow status: {}", curr.getStatus());
        boolean result = checkFlowType(curr);

        logger.debug("■** prev status:" + prev.getStatus());
        logger.debug("■** curr status:" + curr.getStatus());

        if (result) {
            if ((prev.getStatus().equals(FlowStatus.ESTABLISHED.toString()))
                    && ((curr.getStatus()).equals(FlowStatus.ESTABLISHING.toString()))) {
                return false;
            }
            if (((prev.getStatus()).equals(FlowStatus.ESTABLISHED.toString()))
                    && (curr.getStatus().equals(FlowStatus.ESTABLISHED.toString()))) {
                return false;
            }
            if ((prev.getStatus().equals(FlowStatus.ESTABLISHING.toString()))
                    && (curr.getStatus().equals(FlowStatus.ESTABLISHING.toString()))) {
                return false;
            }
        }

        callEndLog(networkId, prev, curr);
        return result;
    }

    @Override
    protected void onFlowUpdate(final String networkId, final Flow prev,
            final Flow curr, final ArrayList<String> attributesList) {
        if (!onFlowUpdatePre(networkId, prev, curr, attributesList)) {
            return;
        }
        callStartLog(networkId, prev, curr);

        String type = conversionTable().getConnectionType(networkId);

        logger.debug("** type:" + type);

        if (UPPER.equals(type)) {
            if (!curr.getStatus().equals(FlowStatus.ESTABLISHED.toString())) {
                logger.debug("** updated flow status: {}", curr.getStatus());
                return;
            }
            updateFlowToLayerizer(networkId, prev, curr);
        } else if (LAYERIZER.equals(type)) {
            super.onFlowUpdate(networkId, prev, curr, attributesList);
        } else if (LOWER.equals(type)) {
            if (curr.getStatus().equals(FlowStatus.ESTABLISHING.toString())) {
                logger.debug("** ■updated flow status: {}", curr.getStatus());
                return;
            }
            addLinkOfLayerizerFromFlowOfLower(networkId, curr);
        }

        callEndLog(networkId, prev, curr);
    }

    private void updateFlowToLayerizer(String networkId, Flow prev, Flow curr) {
        logger.info("** " + new Throwable().getStackTrace()[0].getMethodName() + " Start");

        NetworkInterface targetNetworkIf = networkInterfaces().get(getLayerizerNetworkId());

        BasicFlow flowWithPath = (BasicFlow) networkInterfaces().get(networkId)
                .getFlow(curr.getFlowId());

        Flow targetFlow = flowWithPath.clone();
        targetFlow.setVersion(targetNetworkIf.getFlow(curr.getFlowId()).getVersion());

        targetNetworkIf.putFlow(targetFlow);

        conversionTable().addEntryFlow(
                networkId, curr.getFlowId(),
                getLayerizerNetworkId(), targetFlow.getFlowId());
        logger.info("** " + new Throwable().getStackTrace()[0].getMethodName() + " End");
    }

    @Override
    protected boolean onFlowDeletePre(String networkId, Flow flow) {
        callStartLog(networkId, flow);

        boolean result = checkFlowType(flow);

        callEndLog(networkId, flow);
        return result;
    }

    @Override
    protected void onFlowDelete(String networkId, Flow flow) {
        if (!onFlowDeletePre(networkId, flow)) {
            return;
        }
        callStartLog(networkId, flow);

        String type = conversionTable().getConnectionType(networkId);

        if (LAYERIZER.equals(type)) {
            NetworkInterface networkIf = networkInterfaces().get(getUpperNetworkId());
            Flow upperFlow = networkIf.getFlow(flow.getFlowId());

            if (null != upperFlow) {
                Response ret = networkIf.delFlow(upperFlow.getFlowId());
                logger.debug("** Response:" + ret.statusCode);
                logger.debug("** Response Body:" + ret.getBodyValue().toString());
            }
        } else if (LOWER.equals(type)) {
            String linkId = null;
            if (null != layerizedlinks) {
                linkId = layerizedlinks.get(flow.getFlowId());
            }
            NetworkInterface networkIf = networkInterfaces().get(getLayerizerNetworkId());
            Link link = networkIf.getLink(linkId);

            if (null != link) {
                Response ret = networkIf.delLink(linkId);
                logger.debug("** Response:" + ret.statusCode);
                logger.debug("** Response Body:" + ret.getBodyValue().toString());
            }
            layerizedlinks.remove(flow.getFlowId());
            lowerflows.remove(linkId);
        }
        callEndLog(networkId, flow);
    }

    private void syncLinkLayerizer() {
        logger.info("** " + new Throwable().getStackTrace()[0].getMethodName() + " Start");
        if (getUpperNetworkId().isEmpty()
                || getLowerNetworkId().isEmpty()
                || getLayerizerNetworkId().isEmpty()) {
            logger.debug("** Network components either does not exist.");
            return;
        }

        NetworkInterface upNetworkIf = networkInterfaces().get(getUpperNetworkId());
        NetworkInterface lowNetworkIf = networkInterfaces().get(getLowerNetworkId());
        NetworkInterface layerizerNetworkIf = networkInterfaces().get(getLayerizerNetworkId());

        // Upper.Node -> Layerizer.Node Upper.Port -> Layerizer.Port
        Map<String, Node> upNodes = upNetworkIf.getNodes();
        if (null != upNodes && !upNodes.isEmpty()) {
            for (Entry<String, Node> nodes : upNodes.entrySet()) {
                Node node = nodes.getValue();
                logger.debug("** put node target={}, nodeid={}",
                        layerizerNetworkIf.getNetworkId(), node.getId());
                layerizerNetworkIf.putNode(node);
                conversionTable().addEntryNode(getUpperNetworkId(), node.getId(),
                        getLayerizerNetworkId(), node.getId());

                for (Entry<String, Port> ports : node.getPortMap().entrySet()) {
                    Port port = ports.getValue();
                    conversionTable().addEntryPort(getUpperNetworkId(), node.getId(), port.getId(),
                            getLayerizerNetworkId(), node.getId(), port.getId());
                }
            }
        }

        // Lower.Flow -> Layerizer.Link
        // Layerizer.Link -> Upper.Link
        FlowSet lowFlowSet = lowNetworkIf.getFlowSet();
        if (null != lowFlowSet
                && null != lowFlowSet.getFlows()
                && !lowFlowSet.getFlows().isEmpty()) {
            for (Entry<String, Flow> entry : lowFlowSet.getFlows().entrySet()) {
                logger.debug("** put Link target={}, linkid={}",
                        getLayerizerNetworkId(), entry.getValue().getFlowId());
                addLinkOfLayerizerFromFlowOfLower(getLayerizerNetworkId(), entry.getValue());
            }
        }

    }

    private void addLinkOfLayerizerFromFlowOfLower(final String networkId, final Flow flow) {
        String flowStatus = flow.getStatus();

        if (FlowStatus.ESTABLISHING.equals(FlowStatus.messageValueOf(flowStatus))) {
            addLinkOfLayerizerFromFlow(networkId, flow);
        } else if (FlowStatus.ESTABLISHED.equals(FlowStatus.messageValueOf(flowStatus))) {
            updateLinkOfLayerizerFromFlow(networkId, flow);
        }
    }

    private void addLinkOfLayerizerFromFlow(String networkId, Flow flow) {
        logger.info("** " + new Throwable().getStackTrace()[0].getMethodName() + " Start");
        String linkId = null;
        if (null != layerizedlinks) {
            linkId = layerizedlinks.get(flow.getFlowId());
            logger.debug("** get linkId={}", linkId);
        }

        NetworkInterface layerizerNetworkIf = networkInterfaces().get(getLayerizerNetworkId());

        if (null == linkId) {
            Link link = createLinkFromFlow(flow);
            link.setId(getUniqueLinkId(getLayerizerNetworkId()));
            if (null == link || !link.validate()) {
                logger.debug("** link create failure.");
                return;
            }

            Response ret = layerizerNetworkIf.postLink(link);
            logger.debug("** Response:" + ret.statusCode);
            logger.debug("** Response Body:" + ret.getBodyValue().toString());

            try {
                linkId = ret.getBody(Link.class).getId();
                putLowerFlows(linkId, flow.getFlowId());
                putLayerizedlinks(linkId, flow.getFlowId());
            } catch (Exception e) {
                logger.error("post link error.");
            }
        } else {
            logger.debug("** flow status:" + flow.getStatus());

            Link link = layerizerNetworkIf.getLink(linkId);

            Map<String, String> linkAttributes = link.getAttributes();
            linkAttributes.put(AttrElements.OPER_STATUS, STATUS_UP);
            linkAttributes.put(AttrElements.ESTABLISHMENT_STATUS, flow.getStatus());
            linkAttributes.put(AttrElements.LATENCY, flow.getAttribute(AttrElements.LATENCY));
            linkAttributes.put(AttrElements.MAX_BANDWIDTH,
                    flow.getAttribute(AttrElements.BANDWIDTH));
            linkAttributes.put(
                    AttrElements.UNRESERVED_BANDWIDTH, LINK_DEFAULT_UNRESERVED_BANDWIDTH);
            linkAttributes.put(TableManager.TRANSACTION_ID,
                    flow.getAttribute(TableManager.TRANSACTION_ID));
            link.putAttributes(linkAttributes);

            Response ret = layerizerNetworkIf.putLink(link);

            logger.debug("** Response:" + ret.statusCode);
            logger.debug("** Response Body:" + ret.getBodyValue().toString());

            return;
        }
        logger.info("** " + new Throwable().getStackTrace()[0].getMethodName() + " End");
    }

    private void updateLinkOfLayerizerFromFlow(String networkId, Flow flow) {
        logger.info("** " + new Throwable().getStackTrace()[0].getMethodName() + " Start");
        String linkId = null;
        if (null != layerizedlinks) {
            linkId = layerizedlinks.get(flow.getFlowId());
            logger.debug("** get linkId={}", linkId);
        }

        NetworkInterface layerizerNetworkIf = networkInterfaces().get(getLayerizerNetworkId());

        if (null == linkId) {
            Link link = createLinkFromFlow(flow);
            if (null == link) {
                logger.debug("** link create failure.");
                return;
            }
            link.setId(getUniqueLinkId(getLayerizerNetworkId()));
            link.putAttribute(AttrElements.OPER_STATUS, STATUS_UP);

            if (!link.validate()) {
                logger.debug("** link create failure.");
                return;
            }
            Response ret = layerizerNetworkIf.postLink(link);

            logger.debug("** Response:" + ret.statusCode);
            logger.debug("** Response Body:" + ret.getBodyValue().toString());

            try {
                linkId = ret.getBody(Link.class).getId();
                putLowerFlows(linkId, flow.getFlowId());
                putLayerizedlinks(linkId, flow.getFlowId());
            } catch (Exception e) {
                logger.error("post link error.");
            }

        } else {
            Link link = layerizerNetworkIf.getLink(linkId);

            Map<String, String> linkAttributes = link.getAttributes();
            linkAttributes.put(AttrElements.OPER_STATUS, STATUS_UP);
            linkAttributes.put(AttrElements.ESTABLISHMENT_STATUS, flow.getStatus());
            linkAttributes.put(AttrElements.LATENCY, flow.getAttribute(AttrElements.LATENCY));
            linkAttributes.put(AttrElements.MAX_BANDWIDTH,
                    flow.getAttribute(AttrElements.BANDWIDTH));
            linkAttributes.put(
                    AttrElements.UNRESERVED_BANDWIDTH, LINK_DEFAULT_UNRESERVED_BANDWIDTH);
            linkAttributes.put(TableManager.TRANSACTION_ID,
                    flow.getAttribute(TableManager.TRANSACTION_ID));
            link.putAttributes(linkAttributes);

            Response ret = layerizerNetworkIf.putLink(link);
            logger.debug("** Response:" + ret.statusCode);
            logger.debug("** Response Body:" + ret.getBodyValue().toString());
        }
        logger.info("** " + new Throwable().getStackTrace()[0].getMethodName() + " End");
    }

    private Link createLinkFromFlow(Flow flow) {
        logger.info("** " + new Throwable().getStackTrace()[0].getMethodName() + " Start");

        BasicFlow bfm = (BasicFlow) flow;
        Map<String, List<FlowAction>> edgeActions = bfm.getEdgeActions();

        String dstNode = "";
        String dstPort = "";
        String srcNode = "";
        String srcPort = "";

        for (Entry<String, List<FlowAction>> entry : edgeActions.entrySet()) {
            if (edgeActions.entrySet().size() != 1) {
                logger.error("edgeActions size over. size:{}", edgeActions.entrySet().size());
                return null;
            }
            String nodeId = entry.getKey();
            String portId = ((FlowActionOutput) entry.getValue().get(0)).output;

            String[] str = changePortBoundaryLowToUpper(nodeId, portId);
            if (str[0] != null && str[1] != null
                    && str.length != 0 && str[1].length() != 0) {
                logger.debug("** str[0]:" + str[0]);
                logger.debug("** str[1]:" + str[1]);
                dstNode = str[0];
                dstPort = str[1];
                break;
            } else {
                logger.error("boundary not found. NodeId:{}, PortId:{}: ", nodeId, portId);
                return null;
            }
        }

        for (BasicFlowMatch matches : bfm.getMatches()) {
            String[] str = changePortBoundaryLowToUpper(matches.inNode, matches.inPort);
            if (str[0] != null && str[1] != null
                    && str.length != 0 && str[1].length() != 0) {
                srcNode = str[0];
                srcPort = str[1];
                break;
            } else {
                logger.error("boundary not found. matches.in_node:{}, matches.in_port:{}: ",
                        matches.inNode, matches.inPort);
                return null;
            }
        }

        Link link = new Link();
        // set link ports
        link.setPorts(srcNode, srcPort, dstNode, dstPort);

        link.putAttribute(AttrElements.MAX_BANDWIDTH,
                StringUtils.defaultString(flow.getAttribute(AttrElements.BANDWIDTH)));
        link.putAttribute(AttrElements.REQ_BANDWIDTH,
                StringUtils.defaultString(flow.getAttribute(AttrElements.REQ_BANDWIDTH)));
        link.putAttribute(AttrElements.LATENCY,
                StringUtils.defaultString(flow.getAttribute(AttrElements.LATENCY)));
        link.putAttribute(AttrElements.REQ_LATENCY,
                StringUtils.defaultString(flow.getAttribute(AttrElements.REQ_LATENCY)));
        link.putAttribute(TableManager.TRANSACTION_ID,
                StringUtils.defaultString(flow.getAttribute(TableManager.TRANSACTION_ID)));

        link.putAttribute(AttrElements.ESTABLISHMENT_STATUS, flow.getStatus());
        link.putAttribute(AttrElements.OPER_STATUS, STATUS_DOWN);
        return link;
    }

    private String getUniqueLinkId(final String targetNetworkId) {
        String id;
        NetworkInterface networkIf = networkInterfaces().get(targetNetworkId);
        while (true) {
            id = UUID.randomUUID().toString();
            if (null == networkIf.getLink(id)) {
                break;
            }
        }
        return id;
    }

    private void addFlowToUpper(final String networkId, final Flow flow) {
        logger.info("** " + new Throwable().getStackTrace()[0].getMethodName() + " Start");
        addFlowAndSync(networkId, flow, getUpperNetworkId());
    }

    private void addFlowAndSync(final String orgNetworkId, final Flow orgFlow,
            final String targetNetworkId) {
        NetworkInterface targetNetworkIf = networkInterfaces().get(targetNetworkId);

        BasicFlow flowWithPath = (BasicFlow) networkInterfaces().get(orgNetworkId)
                .getFlow(orgFlow.getFlowId());

        Flow targetFlow = flowWithPath.clone();

        targetNetworkIf.putFlow(targetFlow);

        conversionTable().addEntryFlow(
                orgNetworkId, orgFlow.getFlowId(),
                targetNetworkId, targetFlow.getFlowId());
    }

    private void addUpperLink(final String networkId, final Link link) {
        logger.info("** " + new Throwable().getStackTrace()[0].getMethodName() + " Start");

        Map<String, String> attributes = link.getAttributes();
        String establishmentStatus = attributes.get(AttrElements.ESTABLISHMENT_STATUS);
        if (null == establishmentStatus) {
            logger.debug("** Link Attribute establishment_status non");
            return;
        }
        logger.info("** link_id:" + link.getId() + " status:" + establishmentStatus);
        logger.debug("** Link Attribute establishment_status:" + establishmentStatus);

        registerLinkAndSync(networkId, getUpperNetworkId(), link);

    }

    private void addLayerizedLink(final String networkId, final Link link) {
        logger.info("** " + new Throwable().getStackTrace()[0].getMethodName() + " Start");

        Map<String, String> attributes = link.getAttributes();
        String establishmentStatus = attributes.get(AttrElements.ESTABLISHMENT_STATUS);
        if (null == establishmentStatus) {
            logger.debug("** Link Attribute establishment_status non");
            return;
        }
        logger.info("** link_id:" + link.getId() + " status:" + establishmentStatus);
        logger.debug("** Link Attribute establishment_status:" + establishmentStatus);

        registerLinkAndSync(networkId, getLayerizerNetworkId(), link);

    }

    private void uppdateLowerFlowFromLayerizerLink(final Link link) {
        logger.info("** " + new Throwable().getStackTrace()[0].getMethodName() + " Start");

        String establishmentStatus = link.getAttribute(AttrElements.ESTABLISHMENT_STATUS);
        NetworkInterface lowerNetworkIf = networkInterfaces().get(getLowerNetworkId());

        if (!LINK_STATUS_ESTABLISHED.equals(establishmentStatus)) {
            logger.debug("** Lower_nw Add Flow Start");
            if (lowerflows.containsKey(link.getId())) {
                for (String lowerFlowId : lowerflows.get(link.getId())) {
                    Flow lowerFlow = lowerNetworkIf.getFlow(lowerFlowId);
                    if (null != lowerFlow) {
                        lowerFlow.setStatus(establishmentStatus);
                        lowerFlow.putAttribute(AttrElements.BANDWIDTH,
                                StringUtils.defaultString(
                                        link.getAttribute(AttrElements.MAX_BANDWIDTH)));
                        lowerFlow.putAttribute(AttrElements.REQ_BANDWIDTH,
                                StringUtils.defaultString(
                                        link.getAttribute(AttrElements.REQ_BANDWIDTH)));
                        lowerFlow.putAttribute(AttrElements.LATENCY,
                                StringUtils.defaultString(
                                        link.getAttribute(AttrElements.LATENCY)));
                        lowerFlow.putAttribute(AttrElements.REQ_LATENCY,
                                StringUtils.defaultString(
                                        link.getAttribute(AttrElements.REQ_LATENCY)));
                        lowerFlow.putAttribute(TableManager.TRANSACTION_ID,
                                StringUtils.defaultString(
                                        link.getAttribute(TableManager.TRANSACTION_ID)));
                        lowerNetworkIf.putFlow(lowerFlow);
                    }
                }
            } else {
                BasicFlow flow = linkToFlow(lowerNetworkIf, link);
                logger.debug("** layerized_nw:" + getLayerizerNetworkId());
                logger.debug("** lower_nw:" + getLowerNetworkId());

                if (flow.validate()) {
                    logger.debug("** flow True");
                    lowerNetworkIf.putFlow(flow);
                    putLowerFlows(link.getId(), flow.getFlowId());
                    putLayerizedlinks(link.getId(), flow.getFlowId());
                }
            }
        }
        logger.info("** " + new Throwable().getStackTrace()[0].getMethodName() + " End");
    }

    private void registerLinkAndSync(final String networkId, final String targetNetworkId,
            final Link link) {
        logger.info("** " + new Throwable().getStackTrace()[0].getMethodName() + " Start");

        NetworkInterface networkIf = networkInterfaces().get(targetNetworkId);
        Link targetLink = link.clone();
        Response ret = networkIf.putLink(targetLink);
        logger.debug("** Response:" + ret.statusCode);
        logger.debug("** Response Body:" + ret.getBodyValue().toString());

        // Update conversionTable
        conversionTable().addEntryLink(
                networkId, link.getId(),
                targetNetworkId, link.getId());

        logger.info("** " + new Throwable().getStackTrace()[0].getMethodName() + " End");
    }

    private void delLowerFlows(final String linkId) {
        logger.info("** " + new Throwable().getStackTrace()[0].getMethodName() + " Start");
        logger.debug("** linkId={}, lowerflows=[{}]", linkId, lowerflows);

        if (!lowerflows.containsKey(linkId)) {
            return;
        }

        for (String flowId : lowerflows.get(linkId)) {
            logger.debug("** Lower flow delete. flowId={}", flowId);
            networkInterfaces().get(getLowerNetworkId()).delFlow(flowId);
            layerizedlinks.remove(flowId);
        }
        lowerflows.remove(linkId);
        logger.info("** " + new Throwable().getStackTrace()[0].getMethodName() + " End");
    }

    private void putLowerFlows(String layerLinkId, String lowFlowId) {
        logger.info("** " + new Throwable().getStackTrace()[0].getMethodName() + " Start");
        ArrayList<String> list = lowerflows.get(layerLinkId);
        if (null == list) {
            list = new ArrayList<String>();
        }

        list.add(lowFlowId);
        lowerflows.put(layerLinkId, list);
        logger.info("** lowerflows [{}]", lowerflows);
        logger.info("** " + new Throwable().getStackTrace()[0].getMethodName() + " End");
    }

    private void putLayerizedlinks(String layerLinkId, String lowFlowId) {
        logger.info("** " + new Throwable().getStackTrace()[0].getMethodName() + " Start");
        layerizedlinks.put(lowFlowId, layerLinkId);
        logger.info("** " + new Throwable().getStackTrace()[0].getMethodName() + " End");
    }

    private BasicFlow linkToFlow(NetworkInterface networkInterface, Link link) {
        logger.info("** " + new Throwable().getStackTrace()[0].getMethodName() + " Start");

        String srcPortId = link.getSrcPort();
        String srcNodeId = link.getSrcNode();
        String dstPortId = link.getDstPort();
        String dstNodeId = link.getDstNode();

        FlowSet flowSet = networkInterface.getFlowSet();
        if (null == flowSet) {
            flowSet = new FlowSet();
        }

        BasicFlow flow = (BasicFlow) flowSet.createFlow(FlowType.BASIC_FLOW, FLOW_PRIORITY);
        flow.setStatus(FlowStatus.ESTABLISHING.toString());
        flow.setOwner("Any");
        flow.setEnabled(true);
        flow.setPriority(FLOW_PRIORITY);

        String[] lowerMatch = changePortBoundaryUppreToLow(srcNodeId, srcPortId);
        BasicFlowMatch match = new BasicFlowMatch(lowerMatch[0], lowerMatch[1]);

        logger.debug("** match:{},{}", match.inNode, match.inPort);

        flow.addMatch(match);

        String[] upperEdgeAction = changePortBoundaryUppreToLow(dstNodeId, dstPortId);
        if (null != upperEdgeAction[0]
                && null != upperEdgeAction[1]) {
            FlowActionOutput actionOutput = new FlowActionOutput(upperEdgeAction[1]);
            flow.addEdgeAction(upperEdgeAction[0], actionOutput);
        }

        Map<String, String> flowAttributes = new HashMap<String, String>();

        flowAttributes.put(AttrElements.BANDWIDTH,
                StringUtils.defaultString(
                        link.getAttribute(AttrElements.MAX_BANDWIDTH)));
        flowAttributes.put(AttrElements.REQ_BANDWIDTH,
                StringUtils.defaultString(
                        link.getAttribute(AttrElements.REQ_BANDWIDTH)));
        flowAttributes.put(AttrElements.LATENCY,
                StringUtils.defaultString(
                        link.getAttribute(AttrElements.LATENCY)));
        flowAttributes.put(AttrElements.REQ_LATENCY,
                StringUtils.defaultString(
                        link.getAttribute(AttrElements.REQ_LATENCY)));
        flowAttributes.put(TableManager.TRANSACTION_ID,
                StringUtils.defaultString(
                        link.getAttribute(TableManager.TRANSACTION_ID)));

        flow.putAttributes(flowAttributes);
        flow.setVersion("0");

        logger.info("** " + new Throwable().getStackTrace()[0].getMethodName() + " End");
        return flow;
    }

    private String[] changePortBoundaryUppreToLow(String nodeId, String portId) {
        logger.info("** " + new Throwable().getStackTrace()[0].getMethodName() + " Start");

        logger.debug("** nodeId:" + nodeId);
        logger.debug("** portId:" + portId);

        String[] ret = { "", "" };

        Set<String> keySet = boundaryset.getLinklayerizerBoundaryMap().keySet();
        Iterator<String> boundaryIterator = keySet.iterator();
        while (boundaryIterator.hasNext()) {
            String key = boundaryIterator.next();
            logger.debug("** key:" + key);
            LinklayerizerBoundary lb = boundaryset.getLinklayerizerBoundaryMap().get(key);
            logger.debug("** lb:" + lb.getUpper_nw_port());
            if (lb.getUpper_nw_node().equals(nodeId)
                    && lb.getUpper_nw_port().equals(portId)) {
                ret[0] = lb.getLower_nw_node();
                ret[1] = lb.getLower_nw_port();
                logger.debug("** match ok");
                break;
            }
        }

        logger.info("** " + new Throwable().getStackTrace()[0].getMethodName() + " End");
        return ret;
    }

    private String[] changePortBoundaryLowToUpper(String nodeId, String portId) {
        logger.info("** " + new Throwable().getStackTrace()[0].getMethodName() + " Start");

        logger.debug("** nodeId:" + nodeId);
        logger.debug("** portId:" + portId);

        String[] ret = { "", "" };

        Set<String> keySet = boundaryset.getLinklayerizerBoundaryMap().keySet();
        Iterator<String> boundaryIterator = keySet.iterator();
        while (boundaryIterator.hasNext()) {
            String key = boundaryIterator.next();
            logger.debug("** key:" + key);
            LinklayerizerBoundary lb = boundaryset.getLinklayerizerBoundaryMap().get(key);
            logger.debug("** lb:" + lb.getUpper_nw_port());
            if (lb.getLower_nw_node().equals(nodeId)
                    && lb.getLower_nw_port().equals(portId)) {
                ret[0] = lb.getUpper_nw_node();
                ret[1] = lb.getUpper_nw_port();
                break;
            }
        }

        logger.info("** " + new Throwable().getStackTrace()[0].getMethodName() + " End");
        return ret;
    }

    private boolean checkUpperEvent(String networkId) {
        logger.info("** " + new Throwable().getStackTrace()[0].getMethodName() + " Start");
        String type = conversionTable().getConnectionType(networkId);

        // check connection type
        if (!UPPER.equals(type)) {
            logger.debug("type not upper.");
            return false;
        }

        // check layerizer has been registered
        if (!conversionTable().isConnectionType(LAYERIZER)) {
            logger.debug("layerizer networkInterface Not found.");
            return false;
        }
        return true;
    }

    private boolean checkLayerizerUpperEvent(String networkId, Link link) {
        logger.info("** " + new Throwable().getStackTrace()[0].getMethodName() + " Start");

        if (checkLayerizerEvent(networkId, link) || checkUpperEvent(networkId)) {
            logger.info("** " + new Throwable().getStackTrace()[0].getMethodName() + " True End");
            return true;
        }
        logger.info("** " + new Throwable().getStackTrace()[0].getMethodName() + " False End");
        return false;
    }

    private boolean checkLayerizerEvent(String networkId, Link link) {
        logger.info("** " + new Throwable().getStackTrace()[0].getMethodName() + " Start");
        String type = conversionTable().getConnectionType(networkId);

        // check connection type
        if (!LAYERIZER.equals(type)) {
            logger.debug("type not layerizer.");
            return false;
        }

        // check upper has been registered
        if (!conversionTable().isConnectionType(UPPER)) {
            logger.debug("upper networkInterface Not found.");
            return false;
        }

        // check validate
        if (!link.validate()) {
            logger.debug("link validate Failure");
            return false;
        }

        return true;
    }

    private boolean checkFlowType(Flow flow) {
        return FlowType.BASIC_FLOW.equals(FlowType.messageValueOf(flow.getType()));
    }

    private String getUpperNetworkId() {
        return getNetworkIdSpecifyType(UPPER);
    }

    private String getLowerNetworkId() {
        return getNetworkIdSpecifyType(LOWER);
    }

    private String getLayerizerNetworkId() {
        return getNetworkIdSpecifyType(LAYERIZER);
    }

    private String getNetworkIdSpecifyType(String type) {
        List<String> networkIds = conversionTable().getConnectionList(type);
        if (networkIds.size() != 1) {
            return "";
        }
        return networkIds.get(0);
    }

    @Override
    protected final Response onRequest(
            final Request request) {
        logger.info("** " + new Throwable().getStackTrace()[0].getMethodName() + " Start");
        logger.debug("received {}", request.path);
        RequestParser<IActionCallback>.ParsedRequest parsed = parser
                .parse(request);
        if (parsed == null) {
            return new Response(Response.BAD_REQUEST, "Error unknown request ");
        }

        IActionCallback callback = parsed.getResult();
        if (callback == null) {
            return new Response(Response.BAD_REQUEST, "Error unknown request ");
        }

        try {
            return callback.process(parsed);
        } catch (Exception e) {
            logger.error("Error unknown request");
            return new Response(Response.BAD_REQUEST, "Error unknown request ");
        }
    }

    private String postBoundary(String bodyInfo) throws Exception {
        logger.info("** " + new Throwable().getStackTrace()[0].getMethodName() + " Start");

        LinklayerizerBoundary boundary = createBoundary(new JSONObject(bodyInfo));
        if (!checkBoundary(boundary)) {
            return "Undefined Boundary. NetworkID is Lower=" + boundary.getLower_nw()
                    + " Upper=" + boundary.getUpper_nw();
        }

        boundaryset.putBoundary(boundary.getBoundary_id(), boundary);

        String rtnJsonVal = "";
        try {
            ObjectMapper mapper = new ObjectMapper();
            rtnJsonVal = mapper.writeValueAsString(boundary);
            logger.debug("jsonBoundary:" + rtnJsonVal);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            logger.error("** Failed to postBoundary {}", bodyInfo);
        }

        logger.info("** " + new Throwable().getStackTrace()[0].getMethodName() + " End");
        return rtnJsonVal;
    }

    private String putBoundary(String boundaryId, String bodyInfo) throws Exception {
        logger.info("putBoundary Start");

        LinklayerizerBoundary boundary = createBoundary(new JSONObject(bodyInfo));
        if (!checkBoundary(boundary)) {
            logger.error("expect NetworkID is Lower=" + getLowerNetworkId()
                    + " Upper=" + getUpperNetworkId());
            return "Undefined Boundary. NetworkID is Lower=" + boundary.getLower_nw()
                    + " Upper=" + boundary.getUpper_nw();
        }

        boundaryset.putBoundary(boundaryId, boundary);

        String rtnJsonVal = "";
        try {
            ObjectMapper mapper = new ObjectMapper();
            rtnJsonVal = mapper.writeValueAsString(boundary);
            logger.debug("jsonBoundary:" + rtnJsonVal);
        } catch (JsonProcessingException e) {
            logger.error("** Failed to postBoundary {}", bodyInfo);
            e.printStackTrace();
        }

        logger.info("putBoundary End");
        return rtnJsonVal;
    }

    private String delBoundary(String boundaryId) {
        logger.info("delBoundary End");
        boundaryset.removeBoundaries(boundaryId);
        String rtnJsonVal = "";
        logger.info("delBoundary End");
        return rtnJsonVal;
    }

    private String getFlows() {
        logger.info("getFlows Start");

        String rtnJsonVal = "";
        try {
            ObjectMapper mapper = new ObjectMapper();
            rtnJsonVal = mapper.writeValueAsString(lowerflows);
            logger.debug("jsonFlows:" + rtnJsonVal);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            logger.error("** Failed to getFlows {}", lowerflows);
        }

        logger.info("getFlows End");
        return rtnJsonVal;
    }

    private String getFlows(String linkId) {
        logger.info("getFlows Start");
        logger.info("link id :" + linkId);

        if (null == lowerflows.get(linkId)) {
            return "";
        }

        String rtnJsonVal = "";
        try {
            ObjectMapper mapper = new ObjectMapper();
            rtnJsonVal = mapper.writeValueAsString(lowerflows.get(linkId));
            logger.debug("jsonFlows:" + rtnJsonVal);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            logger.error("** Failed to getFlows {}", lowerflows);
        }

        logger.info("getFlows End");
        return rtnJsonVal;
    }

    private String getLinks() {
        logger.info("getLinks Start");

        String rtnJsonVal = "";
        try {
            ObjectMapper mapper = new ObjectMapper();
            rtnJsonVal = mapper.writeValueAsString(layerizedlinks);
            logger.debug("jsonLinks:" + rtnJsonVal);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            logger.error("** Failed to getLinks {}", layerizedlinks);
        }

        logger.info("getLinks End");
        return rtnJsonVal;
    }

    private String getLinks(String flowId) {
        logger.info("getLinks Start");
        logger.info("flow id :" + flowId);

        logger.info("getLinks End");
        return layerizedlinks.get(flowId);
    }

    private boolean checkBoundary(LinklayerizerBoundary boundary) {
        String lowerNwid = boundary.getLower_nw();
        String upperNwid = boundary.getUpper_nw();
        return (getLowerNetworkId().equals(lowerNwid) && getUpperNetworkId().equals(upperNwid));
    }

    private LinklayerizerBoundary createBoundary(JSONObject jsonObject) {
        LinklayerizerBoundary boundary = new LinklayerizerBoundary();
        boundary.setBoundary_id(jsonObject.getString("boundary_id"));

        boundary.setLower_nw(jsonObject.getString("lower_nw"));
        boundary.setLower_nw_node(jsonObject.getString("lower_nw_node"));
        boundary.setLower_nw_port(jsonObject.getString("lower_nw_port"));

        boundary.setUpper_nw(jsonObject.getString("upper_nw"));
        boundary.setUpper_nw_node(jsonObject.getString("upper_nw_node"));
        boundary.setUpper_nw_port(jsonObject.getString("upper_nw_port"));

        return boundary;
    }

    private interface IActionCallback {
        public Response process(
                RequestParser<IActionCallback>.ParsedRequest parser)
                throws Exception;
    }

    /**
     * createParser
     *
     * @return RequestParser&lt;IActionCallback&gt;
     */
    private RequestParser<IActionCallback> createParser() {
        return new RequestParser<IActionCallback>() {
            {
                addRule(Method.GET, "settings/boundaries",
                        new IActionCallback() {
                            public Response process(
                                    final RequestParser<IActionCallback>.
                                    ParsedRequest parsed) throws Exception {
                                return new Response(Response.OK,
                                        boundaryset.changeBoundariestoJSON());
                            }
                        });

                addRule(Method.GET, "settings/boundaries/<boundary_id>",
                        new IActionCallback() {
                            public Response process(
                                    final RequestParser<IActionCallback>.
                                    ParsedRequest parsed) throws Exception {
                                return new Response(Response.OK,
                                        new ObjectMapper().writeValueAsString(boundaryset
                                                .getBoundary(parsed.getParam("boundary_id"))));
                            }
                        });

                addRule(Method.POST, "settings/boundaries",
                        new IActionCallback() {
                            public Response process(
                                    final RequestParser<IActionCallback>.
                                    ParsedRequest parsed) throws Exception {
                                return new Response(Response.OK, postBoundary(
                                        parsed.getRequest().getBody(String.class)));
                            }
                        });

                addRule(Method.PUT, "settings/boundaries/<boundary_id>",
                        new IActionCallback() {
                            public Response process(
                                    final RequestParser<IActionCallback>.
                                    ParsedRequest parsed) throws Exception {
                                return new Response(Response.OK, putBoundary(
                                        parsed.getParam("boundary_id"), parsed.getRequest()
                                                .getBody(String.class)));
                            }
                        });

                addRule(Method.DELETE, "settings/boundaries/<boundary_id>",
                        new IActionCallback() {
                            public Response process(
                                    final RequestParser<IActionCallback>.
                                    ParsedRequest parsed) throws Exception {
                                return new Response(Response.OK, delBoundary(parsed
                                        .getParam("boundary_id")));
                            }
                        });

                addRule(Method.GET, "lower_flows",
                        new IActionCallback() {
                            public Response process(
                                    final RequestParser<IActionCallback>.
                                    ParsedRequest parsed) throws Exception {
                                return new Response(Response.OK, getFlows());
                            }
                        });

                addRule(Method.GET, "lower_flows/<link_id>",
                        new IActionCallback() {
                            public Response process(
                                    final RequestParser<IActionCallback>.
                                    ParsedRequest parsed) throws Exception {
                                return new Response(Response.OK, getFlows(parsed
                                        .getParam("link_id")));
                            }
                        });

                addRule(Method.GET, "layerized_links",
                        new IActionCallback() {
                            public Response process(
                                    final RequestParser<IActionCallback>.
                                    ParsedRequest parsed) throws Exception {
                                return new Response(Response.OK, getLinks());
                            }
                        });

                addRule(Method.GET, "layerized_links/<flow_id>",
                        new IActionCallback() {
                            public Response process(
                                    final RequestParser<IActionCallback>.
                                    ParsedRequest parsed) throws Exception {
                                return new Response(Response.OK, getLinks(parsed
                                        .getParam("flow_id")));
                            }
                        });
            }
        };
    }

    private void callStartLog(String networkId) {
        startLog(networkId);
    }

    private void callStartLog(String networkId, Link link) {
        startLog(networkId);
        statusLog(link);
    }

    private void callStartLog(String networkId, Link prev, Link curr) {
        startLog(networkId);
        statusLog(prev, "previous");
        statusLog(curr, "current");
    }

    private void callStartLog(String networkId, Flow flow) {
        startLog(networkId);
        statusLog(flow);
    }

    private void callStartLog(String networkId, Flow prev, Flow curr) {
        startLog(networkId);
        statusLog(prev, "previous");
        statusLog(curr, "current");
    }

    private void callEndLog(String networkId) {
        endLog(networkId);
    }

    private void callEndLog(String networkId, Link link) {
        statusLog(link);
        endLog(networkId);
    }

    private void callEndLog(String networkId, Link prev, Link curr) {
        statusLog(prev, "previous");
        statusLog(curr, "current");
        endLog(networkId);
    }

    private void callEndLog(String networkId, Flow flow) {
        statusLog(flow);
        endLog(networkId);
    }

    private void callEndLog(String networkId, Flow prev, Flow curr) {
        statusLog(prev, "previous");
        statusLog(curr, "current");
        endLog(networkId);
    }

    private void startLog(String networkId) {
        logger.info("** {} Start", new Throwable().getStackTrace()[2].getMethodName());
        logger.info("** networkcomponentID:{}", networkId);
    }

    private void endLog(String networkId) {
        logger.info("** networkcomponentID:{}", networkId);
        logger.info("** {} End", new Throwable().getStackTrace()[2].getMethodName());
    }
}
