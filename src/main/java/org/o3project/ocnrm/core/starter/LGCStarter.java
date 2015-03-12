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

public class LGCStarter extends ServerResource {
    private Logger logger = LoggerFactory.getLogger(LGCStarter.class);
    private static OdenOsPropertyLoader loader = OdenOsPropertyLoader.getInstance();
    private static OcnRMStatusLib rmStatus = OcnRMStatusLib.getInstance();

    private static final String RESTAPI_SUCCESS_MSG = loader.getRestAPIResultMsg();
    private static final String RESTAPI_ERROR_MSG = loader.getRestAPIErrorMsg();
    private static final String RESTAPI_SUCCESS = loader.getRestAPISuccess();
    private static final String RESTAPI_ERROR = loader.getRestAPIError();

    /**
     * REST API for LogicComponent start.
     *
     * It starts based on LogicComponent information that has been sent with JSON.
     * LogicComponent information is the following.
     * <br>Example<br>
     * {"type":"OptDriver","id":"opt_driver_01"}
     *
     * <br><br>The return value is the following.
     * <br>Example<br>
     * {"ResultRevel":"0"}(Success)<br>
     * {"ResultRevel":"1","ErrorMessage":"FAILURE"}(Failure)
     * @param logicComponentInfo Registered LogicComponent information.
     * @return Representation
     * @throws JsonParseException
     * @throws JsonMappingException
     */
    @Post
    public Representation lgcStarter(String logicComponentInfo)
            throws JsonParseException, JsonMappingException {
        logger.info("lgcStarter() Start");
        logger.info("getParam : " + logicComponentInfo);

        JSONObject jsonObj = new JSONObject(logicComponentInfo);
        JSONObject result = new JSONObject();

        if (!rmStatus.getCMStatus()) {
            result.put(RESTAPI_SUCCESS_MSG, RESTAPI_ERROR);
            result.put(RESTAPI_ERROR_MSG, "FAILURE");
            logger.error("Not running RM Component Manager");
            logger.info("lgcStarter() End");
            return new JsonRepresentation(result);
        }

        OdenOsStartUpLib osl = new OdenOsStartUpLib();

        String componentType = jsonObj.getString("type");
        String componentId = jsonObj.getString("id");
        if (osl.createLogicComponent(componentType, componentId)) {
            result.put(RESTAPI_SUCCESS_MSG, RESTAPI_SUCCESS);
        } else {
            result.put(RESTAPI_SUCCESS_MSG, RESTAPI_ERROR);
            result.put(RESTAPI_ERROR_MSG, "FAILURE");
        }

        logger.info("response : " + result.toString());
        logger.info("lgcStarter() End");

        return new JsonRepresentation(result);
    }
}