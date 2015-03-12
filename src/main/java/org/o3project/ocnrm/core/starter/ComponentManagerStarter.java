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

package org.o3project.ocnrm.core.starter;

import org.json.JSONException;
import org.json.JSONObject;
import org.o3project.ocnrm.lib.OcnRMStatusLib;
import org.o3project.ocnrm.odenos.driver.OptDriver;
import org.o3project.ocnrm.odenos.lib.OdenOsPropertyLoader;
import org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer;
import org.o3project.odenos.core.component.network.Network;
import org.o3project.odenos.remoteobject.manager.component.ComponentManager;
import org.o3project.odenos.remoteobject.messagingclient.MessageDispatcher;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class ComponentManagerStarter extends ServerResource {
    private Logger logger = LoggerFactory.getLogger(ComponentManagerStarter.class);
    private static OdenOsPropertyLoader loader = OdenOsPropertyLoader.getInstance();
    private static OcnRMStatusLib rmStatus = OcnRMStatusLib.getInstance();

    private String dispatcherHost;
    private int dispatcherPort;
    private String componentManagerId; //Do NOT update
    private String systemManagerId;

    private static final String RESTAPI_SUCCESS_MSG = loader.getRestAPIResultMsg();
    private static final String RESTAPI_ERROR_MSG = loader.getRestAPIErrorMsg();
    private static final String RESTAPI_SUCCESS = loader.getRestAPISuccess();
    private static final String RESTAPI_ERROR = loader.getRestAPIError();

    /**
     * REST API for ComponentManager start.
     *
     * <br><br>The return value is the following.
     * <br>Example<br>
     * {"ResultRevel":"0"}(Success)<br>
     * {"ResultRevel":"1","ErrorMessage":"FAILURE"}(Failure)
     * @return Representation
     * @throws JsonParseException
     * @throws JsonMappingException
     */
    @Post
    public Representation componentManagerStarter(String arg)
            throws JSONException, JsonParseException, JsonMappingException, Exception {
        logger.info("componentManagerStarter() Start");

        JSONObject result = new JSONObject();
        getProperties();

        if (rmStatus.getCMStatus()) {
            logger.debug("Component Manager has already been executed.");
            result.put(RESTAPI_SUCCESS_MSG, RESTAPI_SUCCESS);
            return new JsonRepresentation(result);
        }

        StartComponentManager scm = new StartComponentManager();
        scm.start();

        for (int index = 0; index < 10; index++) {
            Thread.sleep(500);
            if (rmStatus.getCMStatus()) {
                break;
            }
        }

        if (rmStatus.getCMStatus()) {
            result.put(RESTAPI_SUCCESS_MSG, RESTAPI_SUCCESS);
        } else {
            result.put(RESTAPI_SUCCESS_MSG, RESTAPI_ERROR);
            result.put(RESTAPI_ERROR_MSG, "FAILURE");
        }

        logger.info("response : " + result.toString());
        logger.info("componentManagerStarter() End");
        return new JsonRepresentation(result);
    }

    private class StartComponentManager extends Thread {
        @Override
        public void run() {

           MessageDispatcher dispatcher = new MessageDispatcher(
                    systemManagerId, dispatcherHost, dispatcherPort);
            dispatcher.start();

            ComponentManager cm = new ComponentManager(componentManagerId, dispatcher);

            cm.registerComponentType(Network.class);
            cm.registerComponentType(OptDriver.class);
            cm.registerComponentType(LinkLayerizer.class);

            try {
                cm.registerToSystemManager();
                rmStatus.setCMStatus(true);

            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            try {
                dispatcher.join();
            } catch (InterruptedException e) {
                logger.error("InterruptedException is occurred. {}", e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void getProperties() {
        logger.info("getProperties Start");

        componentManagerId = loader.getComponentMgrId();
        dispatcherHost = loader.getDispatcherHost();
        dispatcherPort = loader.getDispatcherPort();

        systemManagerId = loader.getSystemMgrId();

        logger.info("getProperties End");
    }

}