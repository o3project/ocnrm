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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LinkLayerizerBoundarySet {
    private Map<String, LinklayerizerBoundary> linklayerizerBoundaryMap
        = new HashMap<String, LinklayerizerBoundary>();
    private Logger logger = LoggerFactory.getLogger(LinkLayerizerBoundarySet.class);

    public LinkLayerizerBoundarySet() {
        linklayerizerBoundaryMap = new HashMap<String, LinklayerizerBoundary>();
    }

    public Map<String, LinklayerizerBoundary> changeJSONBoundariesToBoundaries(String jsonBoundary,
            String seqNo) {

        logger.info(seqNo + "\t" + "changeJSONBoundariestoBoundaries Start");

        linklayerizerBoundaryMap = new HashMap<String, LinklayerizerBoundary>();
        try {
            JsonFactory factory = new JsonFactory();
            JsonParser jp = factory.createParser(jsonBoundary);
            jp.nextToken();
            while (jp.nextToken() != JsonToken.END_OBJECT) {
                String mapKey = jp.getCurrentName();
                LinklayerizerBoundary llb = new LinklayerizerBoundary();
                jp.nextToken();
                while (jp.nextToken() != JsonToken.END_OBJECT) {
                    String fieldname = jp.getCurrentName();
                    jp.nextToken();
                    if ("boundary_id".equals(fieldname)) {
                        llb.setBoundary_id(jp.getText());
                    } else if ("lower_nw".equals(fieldname)) {
                        llb.setLower_nw(jp.getText());
                    } else if ("lower_nw_node".equals(fieldname)) {
                        llb.setLower_nw_node(jp.getText());
                    } else if ("lower_nw_port".equals(fieldname)) {
                        llb.setLower_nw_port(jp.getText());
                    } else if ("upper_nw".equals(fieldname)) {
                        llb.setUpper_nw(jp.getText());
                    } else if ("upper_nw_node".equals(fieldname)) {
                        llb.setUpper_nw_node(jp.getText());
                    } else if ("upper_nw_port".equals(fieldname)) {
                        llb.setUpper_nw_port(jp.getText());
                    } else if ("type".equals(fieldname)) {
                        continue;
                    } else {
                        throw new IllegalStateException(seqNo + "\t" + "Unrecognized field '"
                        + fieldname + "'!");
                    }
                }
                linklayerizerBoundaryMap.put(mapKey, llb);
            }
            jp.close();
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.info(seqNo + "\t" + "changeJSONBoundariestoBoundaries End");
        return linklayerizerBoundaryMap;
    }

    public String getUpperNWPort(String lowerNWPort, String seqNo) {
        logger.info(seqNo + "\t" + "getUpperNWPort Start");
        String upperNWPort = "";
        for (Entry<String, LinklayerizerBoundary> entry : linklayerizerBoundaryMap.entrySet()) {
            logger.debug(seqNo + "\t" + "lowerNWPort:" + lowerNWPort);
            logger.debug(seqNo + "\t" + "entry.getValue().getLower_nw_port():"
                + entry.getValue().getLower_nw_port());

            if (lowerNWPort.equals(entry.getValue().getLower_nw_port())) {
                upperNWPort = entry.getValue().getUpper_nw_port();
                break;
            }
        }
        logger.info(seqNo + "\t" + "getUpperNWPort End");
        return upperNWPort;
    }

    public String changeBoundariestoJSON() {
        logger.info("changeBoundariestoJSON Start");

        String jsonBoundaries = "";
        try {
            ObjectMapper mapper = new ObjectMapper();
            jsonBoundaries = mapper.writeValueAsString(linklayerizerBoundaryMap);
            logger.debug("jsonBoundaries:" + jsonBoundaries);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        logger.info("changeBoundariestoJSON Endt");
        return jsonBoundaries;
    }

    public void putBoundary(String key, LinklayerizerBoundary value) {
        linklayerizerBoundaryMap.put(key, value);
    }

    public LinklayerizerBoundary getBoundary(String key) {
        return linklayerizerBoundaryMap.get(key);
    }

    public void removeBoundaries(String key) {
        linklayerizerBoundaryMap.remove(key);
    }

    public Map<String, LinklayerizerBoundary> getLinklayerizerBoundaryMap() {
        return linklayerizerBoundaryMap;
    }

    public void setLinklayerizerBoundaryMap(Map<String,
            LinklayerizerBoundary> linklayerizerBoundaryMap) {
        this.linklayerizerBoundaryMap = linklayerizerBoundaryMap;
    }

}
