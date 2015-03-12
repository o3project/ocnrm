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

import org.json.JSONObject;
import org.o3project.ocnrm.lib.OcnRMStatusLib;
import org.o3project.ocnrm.odenos.lib.OdenOsPropertyLoader;
import org.o3project.ocnrm.odenos.lib.OdenOsStartUpLib;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class ConnectionCreator extends ServerResource {
    private Logger logger = LoggerFactory.getLogger(ConnectionCreator.class);
    private static OdenOsPropertyLoader loader = OdenOsPropertyLoader.getInstance();
    private static OcnRMStatusLib rmStatus = OcnRMStatusLib.getInstance();

    private static final String RESTAPI_SUCCESS_MSG = loader.getRestAPIResultMsg();
    private static final String RESTAPI_ERROR_MSG = loader.getRestAPIErrorMsg();
    private static final String RESTAPI_SUCCESS = loader.getRestAPISuccess();
    private static final String RESTAPI_ERROR = loader.getRestAPIError();

    /**
     * REST API for connection making.
     *
     * It starts based on Connection information that has been sent with JSON.
     * Connection information is the following.
     * <br>Example<br>
     * {"type":"OptDriver","nwid":"networkcomponent0"
     * ,"id":"opt_driver_01","connection_type":"layerizer"}
     *
     * <br><br>The return value is the following.
     * <br>Example<br>
     * {"ResultRevel":"0"}(Success)<br>
     * {"ResultRevel":"1","ErrorMessage":"FAILURE"}(Failure)
     * @param ConnectionInfo Registered Connection information.
     * @return Representation
     * @throws JsonParseException
     * @throws JsonMappingException
     */
    @Post
    public Representation connectionCreate(String connectionInfo)
            throws JsonParseException, JsonMappingException {
        logger.info("connectionCreate() Start");
        logger.info("getParam : " + connectionInfo);

        JSONObject jsonObj = new JSONObject(connectionInfo);
        JSONObject result = new JSONObject();

        if (!rmStatus.getCMStatus()) {
            result.put(RESTAPI_SUCCESS_MSG, RESTAPI_ERROR);
            result.put(RESTAPI_ERROR_MSG, "FAILURE");
            logger.error("Not running RM Component Manager");
            logger.info("connectionCreate() End");
            return new JsonRepresentation(result);
        }

        OdenOsStartUpLib osl = new OdenOsStartUpLib();

        String componentType = jsonObj.getString("type");
        String networkId = jsonObj.getString("nwid");
        String logicId = jsonObj.getString("id");
        String connectionType = jsonObj.getString("connection_type");
        if (osl.createSubscribe(componentType, networkId, logicId, connectionType)) {
            result.put(RESTAPI_SUCCESS_MSG, RESTAPI_SUCCESS);
        } else {
            result.put(RESTAPI_SUCCESS_MSG, RESTAPI_ERROR);
            result.put(RESTAPI_ERROR_MSG, "FAILURE");
        }

        logger.info("response : " + result.toString());
        logger.info("connectionCreate() End");

        return new JsonRepresentation(result);
    }
}