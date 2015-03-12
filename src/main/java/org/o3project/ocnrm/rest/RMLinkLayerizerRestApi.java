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
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.o3project.ocnrm.lib.OcnMFSequenceLib;
import org.o3project.ocnrm.odenos.lib.OdenOsPropertyLoader;
import org.o3project.ocnrm.odenos.lib.OdenOsSender;
import org.o3project.odenos.remoteobject.message.Request.Method;
import org.o3project.odenos.remoteobject.message.Response;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class RMLinkLayerizerRestApi extends ServerResource {
    private Logger logger = LoggerFactory.getLogger(RMLinkLayerizerRestApi.class);
    private static OdenOsPropertyLoader loader = OdenOsPropertyLoader.getInstance();
    private OdenOsSender odenos = OdenOsSender.getInstance();

    private static final String RESTAPI_SUCCESS_MSG = loader.getRestAPIResultMsg();
    private static final String RESTAPI_ERROR_MSG = loader.getRestAPIErrorMsg();
    private static final String RESTAPI_SUCCESS = loader.getRestAPISuccess();
    private static final String RESTAPI_ERROR = loader.getRestAPIError();

    private static final String SEQNO_PREFIX = "#";
    private OcnMFSequenceLib mf = OcnMFSequenceLib.getInstance();
    private String seqNo = "";

    /**
     * Creation REST API of Linklayerizer information.(POST)
     *
     * Linklayerizer information that has been sent with JSON registration/is deleted.
     *
     * <br><br>The return value is the following.
     * <br>Example<br>
     * {"ResultRevel":"0"}(Success)<br>
     * {"ResultRevel":"1","ErrorMessage":"100　An error has occurred."}(Failure)
     * @param layerizerInfoFromMF   Registered physical resource information
     * @return Representation
     * @throws JSONException
     * @throws IOException
     * @throws JsonMappingException
     * @throws JsonParseException
     */
    @Post
    public Representation postLayerizerInfo(String layerizerInfoFromMF)
            throws JSONException, JsonParseException, JsonMappingException, IOException {
        seqNo = SEQNO_PREFIX + mf.requestNoToString();
        logger.info(seqNo + "\t" + "postLayerizerInfo() Start");
        logger.info(seqNo + "\t" + "getParam : " + layerizerInfoFromMF);
        JSONObject result = sendLayerizerRest(Method.POST, layerizerInfoFromMF);
        logger.info(seqNo + "\t" + "postLayerizerInfo() End");

        return new JsonRepresentation(result);
    }

    /**
     * Creation REST API of Linklayerizer information.(PUT)
     *
     * Linklayerizer information that has been sent with JSON is registered.
     *
     * <br><br>The return value is the following.
     * <br>Example<br>
     * {"ResultRevel":"0"}(Success)<br>
     * {"ResultRevel":"1","ErrorMessage":"100　An error has occurred."}(Failure)
     * @param layerizerInfoFromMF   Registered physical resource information
     * @return Representation
     * @throws JSONException
     * @throws IOException
     * @throws JsonMappingException
     * @throws JsonParseException
     */
    @Put
    public Representation putLayerizerInfo(String layerizerInfoFromMF)
            throws JSONException, JsonParseException, JsonMappingException, IOException {
        seqNo = SEQNO_PREFIX + mf.requestNoToString();
        logger.info(seqNo + "\t" + "putLayerizerInfo() Start");
        logger.info(seqNo + "\t" + "getParam : " + layerizerInfoFromMF);
        JSONObject result = sendLayerizerRest(Method.PUT, layerizerInfoFromMF);
        logger.info(seqNo + "\t" + "putLayerizerInfo() End");

        return new JsonRepresentation(result);
    }

    /**
     * Acquisition REST API of Linklayerizer information.
     *
     * Linklayerizer information is returned.
     *
     * <br><br>The return value is the following.
     * <br>Example<br>
     * {"ResultRevel":"0"}(Success)<br>
     * {"ResultRevel":"1","ErrorMessage":"100　An error has occurred."}(Failure)
     *
     * @return Representation
     * @throws JSONException
     * @throws IOException
     * @throws JsonMappingException
     * @throws JsonParseException
     */
    @Get
    public Representation getLayerizerInfo()
            throws JSONException, JsonParseException, JsonMappingException, IOException {
        seqNo = SEQNO_PREFIX + mf.requestNoToString();
        logger.info(seqNo + "\t" + "getLayerizerInfo() Start");
        JSONObject result = sendLayerizerRest(Method.GET, null);
        logger.info(seqNo + "\t" + "getLayerizerInfo() End");

        return new JsonRepresentation(result);
    }

    /**
     * Deletion REST API of Linklayerizer information.
     *
     * Linklayerizer information is deleted.
     *
     * <br><br>The return value is the following.
     * <br>Example<br>
     * {"ResultRevel":"0"}(Success)<br>
     * {"ResultRevel":"1","ErrorMessage":"100　An error has occurred."}(Failure)
     *
     * @return Representation
     * @throws JSONException
     * @throws IOException
     * @throws JsonMappingException
     * @throws JsonParseException
     */
    @Delete
    public Representation delLayerizerInfo()
            throws JSONException, JsonParseException, JsonMappingException, IOException {

        seqNo = SEQNO_PREFIX + mf.requestNoToString();
        logger.info(seqNo + "\t" + "delLayerizerInfo() Start");
        JSONObject result = sendLayerizerRest(Method.DELETE, null);
        logger.info(seqNo + "\t" + "delLayerizerInfo() End");

        return new JsonRepresentation(result);
    }

    private JSONObject sendLayerizerRest(Method method, String layerizerInfoFromMF) {
        JSONObject result = new JSONObject();
        result.put(RESTAPI_SUCCESS_MSG, RESTAPI_SUCCESS);

        Map<String, Object> requestParam = this.getRequestAttributes();

        String layerizerId = (String) requestParam.get("linklayerizer_id");
        String path = this.getOriginalRef().getPath();

        String uri = path.substring(
                path.indexOf(layerizerId) + layerizerId.length(), path.length());

        if (!isValue(uri)) {
            logger.debug("Failed. request is {}", requestParam);
            result.put(RESTAPI_SUCCESS_MSG, RESTAPI_ERROR);
            result.put(RESTAPI_ERROR_MSG, Response.BAD_REQUEST.toString());
            return result;
        }

        Response rsp = odenos.sendRequest("systemmanager", method,
                "components/" + layerizerId + uri, layerizerInfoFromMF);

        if (rsp == null) {
            logger.debug("Failed.");
            result.put(RESTAPI_SUCCESS_MSG, RESTAPI_ERROR);
            result.put(RESTAPI_ERROR_MSG, "no response");
            return result;
        }

        String returnVal = "";
        try {
            returnVal = rsp.getBody(String.class);
        } catch (Exception e) {
            returnVal = rsp.getBodyValue().toString();
        }

        if (isJSONObject(returnVal)) {
            JSONObject body = new JSONObject(returnVal);
            result.put("body", body);
        } else if (isJSONArray(returnVal)) {
            JSONArray body = new JSONArray(returnVal);
            result.put("body", body);
        } else {
            result.put("body", returnVal);
        }

        if (!rsp.statusCode.equals(Response.OK)) {
            result.put(RESTAPI_SUCCESS_MSG, RESTAPI_ERROR);
        }

        logger.debug("response get body {}",result.get("body"));

        logger.info(seqNo + "\t" + "response : " + result.toString());
        return result;
    }

    private boolean isJSONObject(String jsonStr) {
        try {
            new JSONObject(jsonStr);
        } catch (JSONException ex) {
            return false;
        }
        return true;
    }

    private boolean isJSONArray(String jsonStr) {
        try {
            new JSONArray(jsonStr);
        } catch (JSONException ex) {
            return false;
        }
        return true;
    }

    private boolean isValue(String string) {
        return string != null && !string.isEmpty();
    }

}
