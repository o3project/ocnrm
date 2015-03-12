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
package org.o3project.ocnrm.odenos.lib;

import java.util.Map;

import org.o3project.odenos.core.component.network.Network;
import org.o3project.odenos.core.manager.system.ComponentConnection;
import org.o3project.odenos.core.manager.system.ComponentConnectionLogicAndNetwork;
import org.o3project.odenos.remoteobject.ObjectProperty;
import org.o3project.odenos.remoteobject.message.MessageBodyUnpacker.ParseBodyException;
import org.o3project.odenos.remoteobject.message.Request.Method;
import org.o3project.odenos.remoteobject.message.Response;
import org.o3project.odenos.remoteobject.messagingclient.MessageDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OdenOsStartUpLib {
    private Logger logger = LoggerFactory.getLogger(OdenOsStartUpLib.class);
    private static OdenOsPropertyLoader loader = OdenOsPropertyLoader.getInstance();
    private OdenOsSender odenos = OdenOsSender.getInstance();

    private String systemManagerId;

    private String dispatcherHost;
    private int dispatcherPort;

    private String componentManagerId; //Do NOT update

    public OdenOsStartUpLib() {
        getProperties();
        createDispatcher();
    }

    public boolean createNWComponent(String nwId) {
        logger.info("createNWComponent() start");
        logger.info("nwId : " + nwId);

        ObjectProperty sendProperty = new ObjectProperty(
                Network.class.getSimpleName(), nwId);
        sendProperty.setProperty("cm_id", componentManagerId);

        Response rsp = odenos.sendRequest(systemManagerId, Method.PUT,
                "components/" + nwId, sendProperty);
        if (rsp == null) {
            logger.debug("Failed.");
            return false;
        }

        logger.debug("NetworkComponent created...");
        logger.info("createNWComponent() end");
        return true;
    }

    public boolean createLogicComponent(String componentType, String componentId) {
        logger.debug("Creating " + componentType + "...");
        logger.debug("component_id : " + componentId);

        ObjectProperty sendProperty = new ObjectProperty(
                componentType, componentId);
        sendProperty.setProperty("cm_id", componentManagerId);

        Response rsp = odenos.sendRequest(systemManagerId, Method.PUT,
                "components/" + componentId, sendProperty);
        if (rsp == null) {
            logger.debug("Failed.");
            return false;
        }
        Map<String, String> receiveDict = null;
        try {
            receiveDict = rsp.getBodyAsStringMap();
        } catch (ParseBodyException e) {
            logger.error("Recieved ChangedMessage which can't be parsed.");
            e.printStackTrace();
            return false;
        }
        logger.debug("  -Received: " + rsp.statusCode + " " + receiveDict);
        logger.debug("created...");

        return true;
    }

    public boolean createSubscribe(String componentType, String networkId,
            String logicId, String connectionType) {
        logger.info("createSubscribe start From " + componentType);
        logger.info("-NetworkComponentID:" + networkId);

        ComponentConnection conn1 = new ComponentConnectionLogicAndNetwork(
                logicId + "-" + networkId,
                connectionType,
                null,
                logicId,
                networkId);

        Response rsp = odenos.sendRequest(systemManagerId, Method.PUT,
                "connections/" + conn1.getObjectId(), conn1);

        if (rsp == null) {
            logger.debug("Failed.");
            return false;
        }
        Map<String, String> receiveDict = null;
        try {
            receiveDict = rsp.getBodyAsStringMap();
        } catch (ParseBodyException e) {
            logger.error("Recieved ChangedMessage which can't be parsed.");
            logger.error("rsp.toString() : " + rsp.toString());
            e.printStackTrace();
            return false;
        }
        logger.debug("  -Received: " + rsp.statusCode + " " + receiveDict);
        logger.info("createSubscribe end");
        return true;
    }

    private void createDispatcher() {
        logger.info("MessageDispatcher Start");

        MessageDispatcher dispatcher = odenos.getDispatcher();
        if (dispatcher != null) {
            logger.debug("Dispatcher already exists.");
            logger.info("MessageDispatcher End");
            return;
        }
        dispatcher = new MessageDispatcher(systemManagerId, dispatcherHost, dispatcherPort);
        dispatcher.start();
        logger.debug("Dispatcher created.");
        logger.info("MessageDispatcher End");
        odenos.setDispatcher(dispatcher);
    }

    private void getProperties() {
        logger.info("getProperties Start");

        systemManagerId = loader.getSystemMgrId();

        dispatcherHost = loader.getDispatcherHost();
        dispatcherPort = loader.getDispatcherPort();

        componentManagerId = loader.getComponentMgrId(); //変更不可

        logger.info("getProperties End");
    }
}
