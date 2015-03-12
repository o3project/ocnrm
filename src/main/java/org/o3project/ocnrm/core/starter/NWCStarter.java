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

import org.json.JSONArray;
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

public class NWCStarter extends ServerResource {
    private Logger logger = LoggerFactory.getLogger(NWCStarter.class);
    private static OdenOsPropertyLoader loader = OdenOsPropertyLoader.getInstance();
    private static OcnRMStatusLib rmStatus = OcnRMStatusLib.getInstance();

    private static final String RESTAPI_SUCCESS_MSG = loader.getRestAPIResultMsg();
    private static final String RESTAPI_ERROR_MSG = loader.getRestAPIErrorMsg();
    private static final String RESTAPI_SUCCESS = loader.getRestAPISuccess();
    private static final String RESTAPI_ERROR = loader.getRestAPIError();

    /**
     * REST API for NetworkComponent start.
     *
     * It starts based on NetworkComponentID information that has been sent with JSON.
     * NetworkComponentID information is the following.
     * <br>Example<br>
     * {"nwid":["networkcomponent0","networkcomponent1"]}
     *
     * <br><br>The return value is the following.
     * <br>Example<br>
     * {"ResultRevel":"0"}(Success)<br>
     * {"ResultRevel":"1","ErrorMessage":"FAILURE"}(Failure)
     * @param networkComponentID Registered NetworkComponentID information
     * @return Representation
     * @throws JsonParseException
     * @throws JsonMappingException
     */
    @Post
    public Representation nwcStarter(String networkComponentID)
            throws JsonParseException, JsonMappingException {
        logger.info("nwcStarter() Start");
        logger.info("getParam : " + networkComponentID);

        JSONObject jsonObj = new JSONObject(networkComponentID);
        JSONObject result = new JSONObject();

        if (!rmStatus.getCMStatus()) {
            result.put(RESTAPI_SUCCESS_MSG, RESTAPI_ERROR);
            result.put(RESTAPI_ERROR_MSG, "FAILURE");
            logger.error("Not running RM Component Manager");
            logger.info("nwcStarter() End");
            return new JsonRepresentation(result);
        }

        result.put(RESTAPI_SUCCESS_MSG, RESTAPI_SUCCESS);

        JSONArray jsonArray =  jsonObj.getJSONArray("nwid");

        OdenOsStartUpLib osl = new OdenOsStartUpLib();
        for (int index = 0; index < jsonArray.length(); index++) {
            String nwId = jsonArray.get(index).toString();
            if (!osl.createNWComponent(nwId)) {
                logger.error("startup failure nwid:" + nwId);
                result.put(RESTAPI_SUCCESS_MSG, RESTAPI_ERROR);
                result.put(RESTAPI_ERROR_MSG, "FAILURE");
            }
        }

        logger.info("response : " + result.toString());
        logger.info("nwcStarter() End");

        return new JsonRepresentation(result);
    }
}