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

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;
import org.o3project.ocnrm.lib.OcnMFSequenceLib;
import org.o3project.ocnrm.odenos.lib.OdenOsPropertyLoader;
import org.o3project.ocnrm.odenos.lib.OdenOsSender;
import org.o3project.odenos.remoteobject.message.Request.Method;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class RMNodeRestApi extends ServerResource {
    private Logger logger = LoggerFactory.getLogger(RMNodeRestApi.class);
    private static OdenOsPropertyLoader loader = OdenOsPropertyLoader.getInstance();

    private static final String RESTAPI_SUCCESS_MSG = loader.getRestAPIResultMsg();
    private static final String RESTAPI_ERROR_MSG = loader.getRestAPIErrorMsg();
    private static final String RESTAPI_SUCCESS = loader.getRestAPISuccess();
    private static final String RESTAPI_ERROR = loader.getRestAPIError();

    private static final String DRIVER = "optDriver";

    private static final String SEQNO_PREFIX = "#";
    private OcnMFSequenceLib mf = OcnMFSequenceLib.getInstance();
    private String seqNo = "";

    /**
     * Creation REST API of initial resource information.
     *
     * <br><br>The return value is the following.
     * <br>Example<br>
     * {"ResultRevel":"0"}(Success)<br>
     * {"ResultRevel":"1","ErrorMessage":"100　An error has occurred."}(Failure)
     *
     * @param nodeInfoFromMF   Registered physical resource information
     * @return 要求を受け付けた結果。
     * @throws Representation
     * @throws IOException
     * @throws JsonMappingException
     * @throws JsonParseException
     */
    @Post
    public Representation createNodeInfo(String nodeInfoFromMF)
            throws JSONException, JsonParseException, JsonMappingException, IOException {
        seqNo = SEQNO_PREFIX + mf.requestNoToString();
        logger.info(seqNo + "\t" + "createNodeInfo() Start");
        logger.info(seqNo + "\t" + "getParam : " + nodeInfoFromMF);
        final JSONObject jsonObj = new JSONObject(nodeInfoFromMF);
        JSONObject result = new JSONObject();

        if (jsonObj.has("lower")) {
            logger.debug("make lower topology.");
            Thread task = new Thread(new Runnable() {
                @Override
                public void run() {
                    OdenOsSender.getInstance()
                            .sendRequest("systemmanager", Method.PUT,
                                    "components/" + DRIVER + "/settings/och/topology",
                                    jsonObj.toString());
                }
            });
            task.start();

            result.put(RESTAPI_SUCCESS_MSG, RESTAPI_SUCCESS);
        } else if (jsonObj.has("upper")) {
            logger.debug("make upper topology.");
            Thread task = new Thread(new Runnable() {
                @Override
                public void run() {
                    OdenOsSender.getInstance()
                            .sendRequest("systemmanager",
                                    Method.PUT,
                                    "components/" + DRIVER + "/settings/odu/topology",
                                    jsonObj.toString());
                }
            });
            task.start();

            result.put(RESTAPI_SUCCESS_MSG, RESTAPI_SUCCESS);
        } else {
            result.put(RESTAPI_SUCCESS_MSG, RESTAPI_ERROR);
            result.put(RESTAPI_ERROR_MSG, "100 An error has occurred.");
            logger.info("物理リソース情報に不備があります");
        }

        logger.info(seqNo + "\t" + "response : " + result.toString());
        logger.info(seqNo + "\t" + "createNodeInfo() End");

        return new JsonRepresentation(result);
    }

}
