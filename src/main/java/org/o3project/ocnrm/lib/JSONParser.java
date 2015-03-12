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
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.o3project.ocnrm.model.FjFlowId;
import org.o3project.ocnrm.model.LowerNodeInfo;
import org.o3project.ocnrm.model.UpperNodeInfo;
import org.o3project.ocnrm.model.och.OchFlowCreationResponse;
import org.o3project.ocnrm.model.odu.OduFlowCreationResponse;
import org.o3project.ocnrm.model.odu.OptFlow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JSONParser {
    private Logger logger = LoggerFactory.getLogger(JSONParser.class);

    public LowerNodeInfo lowerNodeInfotoPOJO(JSONObject jsonObj, String seqNo)
            throws JsonParseException, JsonMappingException, IOException {
        logger.info(seqNo + "\t" + "lowerNodeInfotoPOJO() Start");
        logger.info(seqNo + "\t" + "getParam : " + jsonObj.toString());
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(Include.NON_NULL);
        LowerNodeInfo nodeInfo = mapper.readValue(jsonObj.toString(), LowerNodeInfo.class);
        logger.info(seqNo + "\t" + "lowerNodeInfotoPOJO() End");
        return nodeInfo;
    }

    public UpperNodeInfo upperNodeInfotoPOJO(JSONObject jsonObj, String seqNo)
            throws JsonParseException, JsonMappingException, IOException {
        logger.info(seqNo + "\t" + "upperNodeInfotoPOJO() Start");
        logger.info(seqNo + "\t" + "param : " + jsonObj.toString());
        ObjectMapper mapper = new ObjectMapper();
        UpperNodeInfo nodeInfo = mapper.readValue(jsonObj.toString(), UpperNodeInfo.class);
        logger.info(seqNo + "\t" + "upperNodeInfotoPOJO() End");
        return nodeInfo;
    }

    public OptFlow optFlowtoPOJO(JSONObject jsonObj, String seqNo)
            throws JsonParseException, JsonMappingException, IOException {
        logger.info(seqNo + "\t" + "optFlowtoPOJO() Start");
        logger.info(seqNo + "\t" + "param : " + jsonObj.toString());
        ObjectMapper mapper = new ObjectMapper();
        OptFlow optFlow = mapper.readValue(jsonObj.toString(), OptFlow.class);
        logger.info(seqNo + "\t" + "optFlowtoPOJO() End");
        return optFlow;
    }

    public <K> String convertToJson(K target, String seqNo)
            throws JSONException, JsonProcessingException {
        logger.info(seqNo + "\t" + "convertToJson() Start");
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(Include.NON_NULL);
        String jsonArray = mapper.writeValueAsString(target);
        logger.info(seqNo + "\t" + "convertToJson() End");
        return jsonArray;
    }

    public OchFlowCreationResponse ochFlowCreationResponseToPOJO(JSONObject jsonObj, String seqNo)
            throws JsonParseException, JsonMappingException, IOException {
        logger.info(seqNo + "\t" + "ochFlowToPOJO() Start");
        logger.info(seqNo + "\t" + "param : " + jsonObj.toString());
        ObjectMapper mapper = new ObjectMapper();
        OchFlowCreationResponse response = mapper
                .readValue(jsonObj.toString(), OchFlowCreationResponse.class);
        logger.info(seqNo + "\t" + "ochFlowToPOJO() End");
        return response;
    }

    public OduFlowCreationResponse oduFlowCreationResponseToPOJO(JSONObject jsonObj, String seqNo)
            throws JsonParseException, JsonMappingException, IOException {
        logger.info(seqNo + "\t" + "oduFlowCreationResponseToPOJO() Start");
        logger.info(seqNo + "\t" + "param : " + jsonObj.toString());
        ObjectMapper mapper = new ObjectMapper();
        OduFlowCreationResponse response = mapper
                .readValue(jsonObj.toString(), OduFlowCreationResponse.class);
        logger.info(seqNo + "\t" + "oduFlowCreationResponseToPOJO() End");
        return response;
    }

    public FjFlowId fjFlowIdToPOJO(JSONObject jsonObj, String seqNo)
            throws JsonParseException, JsonMappingException, IOException {
        logger.info(seqNo + "\t" + "oduFlowCreationResponseToPOJO() Start");
        logger.info(seqNo + "\t" + "param : " + jsonObj.toString());
        ObjectMapper mapper = new ObjectMapper();
        FjFlowId response = mapper
                .readValue(jsonObj.toString(), FjFlowId.class);
        logger.info(seqNo + "\t" + "oduFlowCreationResponseToPOJO() End");
        return response;
    }

    public List<String> convertToList(JSONArray target, String seqNo)
            throws JSONException, JsonProcessingException {
        logger.info(seqNo + "\t" + "convertToList() Start");
        logger.info(seqNo + "\t" + "param : " + target.toString());
        List<String> list = new ArrayList<>();
        for (int index = 0; index < target.length(); index++) {
            list.add(target.getJSONObject(index).toString());
        }
        logger.info(seqNo + "\t" + "convertToList() End");
        return list;
    }

}
