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
package org.o3project.ocnrm.lib;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;
import org.o3project.ocnrm.model.IdExchangeRequest;
import org.o3project.ocnrm.model.ResourceRequest;
import org.o3project.ocnrm.odenos.lib.OdenOsPropertyLoader;
import org.restlet.Client;
import org.restlet.Context;
import org.restlet.data.Protocol;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ClientResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MFApiCaller {

    private Logger logger = LoggerFactory.getLogger(MFApiCaller.class);
    private static OdenOsPropertyLoader loader = OdenOsPropertyLoader.getInstance();
    private static final String REQUEST_ODU_FLOW_URL = loader.getRequestOduFlowURL();
    private static final String REQUEST_ODU_REPLACEMENT_PIECE_URL = loader
            .getRequestOduReplacementPieceUrl();
    private static final String DELETE_ODU_FLOW_URL = loader.getDeleteOduFlowUrl();
    private static final String REQUEST_OCH_REPLACEMENT_PIECE_URL = loader
            .getRequestOchReplacementPieceUrl();

    /**
     * The resource of ODU Flow is demanded.
     *
     * @param resourceRequest  End point of resource allocation object and
     *                         information on demand bandwidth and delay.
     * @param seqNo  Sequence number
     * @return JSONObject
     * @throws JSONException
     * @throws IOException
     */
    public JSONObject createFlow(ResourceRequest resourceRequest, String seqNo)
            throws JSONException, IOException {
        logger.debug(seqNo + "\t" + "createFlow() Start**");
        logger.debug(seqNo + "\t" + "REQUEST_ODU_FLOW_URL:" + REQUEST_ODU_FLOW_URL);
        return createFlow(REQUEST_ODU_FLOW_URL, resourceRequest, seqNo);
    }

    private <T> JSONObject createFlow(String path, T resourceRequesttoMF, String seqNo)
            throws JSONException, IOException {
        logger.info(seqNo + "\t" + "createFlow() Start");
        logger.info(seqNo + "\t" + "path: " + path);

        JSONParser toJSONData = new JSONParser();
        JSONObject jsonObj = new JSONObject(toJSONData.convertToJson(resourceRequesttoMF, seqNo));
        String sendParam = jsonObj.toString();

        logger.info(seqNo + "\t" + "postToMF() Start");
        String result = postToMF(path, sendParam, seqNo);
        logger.info(seqNo + "\t" + "postToMF() End");

        logger.debug(seqNo + "\t" + "getParam" + result);

        JSONObject jsonObjfromMF = new JSONObject(result);
        logger.info(seqNo + "\t" + "createFlow() End");
        return jsonObjfromMF;
    }

    /**
     * The result of doing POST to MF is returned.
     *
     * @param path path of rest api
     * @param sendParam parameter to send
     * @param seqNo sequence No.
     * @return result that client posted
     * @throws IOException
     */
    private String postToMF(String path, String sendParam, String seqNo) throws IOException {
        ClientResource client = new ClientResource(path);
        Client ct = new Client(new Context(), Protocol.HTTP);
        client.setNext(ct);
        logger.info(seqNo + "\t" + "Send path:" + path);
        logger.info(seqNo + "\t" + "Send Param:" + sendParam);
        StringRepresentation srp = new StringRepresentation(sendParam.toCharArray());

        Representation representation = client.post(srp);
        try {
            return representation.getText();
        } finally {
            if (representation != null) {
                representation.release();
            }
            srp.release();
            client.release();

            representation = null;
            srp = null;
            client = null;
            try {
                ct.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
            ct = null;
        }
    }

    /**
     * ID of OCH is converted.
     *
     * @param request  ID conversion demand
     * @param seqNo    Sequence number
     * @return JSONObject
     * @throws IOException
     */
    public JSONObject requestOchCorrespondingTable(IdExchangeRequest request, String seqNo)
            throws IOException {
        logger.debug(seqNo + "\t" + "requestOchCorrespondingTable() Start **");
        return getTable(REQUEST_OCH_REPLACEMENT_PIECE_URL, request, seqNo);
    }

    /**
     * ID of ODU is converted.
     *
     * @param request   ID conversion demand
     * @param seqNo     Sequence number
     * @return JSONObject
     * @throws IOException
     */
    public JSONObject requestOduCorrespondingTable(IdExchangeRequest request, String seqNo)
            throws IOException {
        logger.debug(seqNo + "\t" + "requestOduCorrespondingTable() Start **");
        return getTable(REQUEST_ODU_REPLACEMENT_PIECE_URL, request, seqNo);
    }

    private JSONObject getTable(String path, IdExchangeRequest request, String seqNo)
            throws IOException {
        logger.info(seqNo + "\t" + "getTable() Start");

        JSONParser parser = new JSONParser();
        logger.info(seqNo + "\t" + "postToMF() Start");
        String result = postToMF(path, parser.convertToJson(request, seqNo).toString(), seqNo);
        logger.info(seqNo + "\t" + "postToMF() End");

        JSONObject flowMap = new JSONObject(result);
        logger.debug(seqNo + "\t" + "getInfoModelBindMap" + flowMap.toString());
        logger.info(seqNo + "\t" + "getTable() End");
        return flowMap;
    }

    /**
     * ODU Flow registered in MF is deleted.
     *
     * @param sendParam  Deleted Flow
     * @param seqNo      Sequence number
     * @return boolean
     * @throws IOException
     */
    public boolean deleteOduFlow(String sendParam, String seqNo) throws IOException {
        postToMF(DELETE_ODU_FLOW_URL, sendParam, seqNo);
        return true;
    }
}
