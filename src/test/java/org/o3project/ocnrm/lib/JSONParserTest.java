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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.o3project.ocnrm.model.LowerNodeInfo;
import org.o3project.ocnrm.model.UpperNodeInfo;
import org.o3project.ocnrm.model.och.OchFlowCreationResponse;
import org.o3project.ocnrm.model.odu.OduFlowCreationResponse;
import org.o3project.ocnrm.model.odu.OptFlow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class JSONParserTest {
    private Logger logger = LoggerFactory.getLogger(JSONParserTest.class);

    private String testData = "";
    JSONParser jsonParse = new JSONParser();
    private static final String SEQUENCE_NO = "#1";

    /**
     * Test method for
     * {@link org.o3project.ocnrm.lib.JSONParser#lowerNodeInfotoPOJO()}
     * @throws IOException
     * @throws JsonMappingException
     * @throws JsonParseException
     */
    @Test
    public void testLowerNodeInfotoPOJO()
            throws JsonParseException, JsonMappingException, IOException {
        testData = "{"
                + "\"lower\":"
                + "{"
                + "\"flow\":["
                + "{\"flowId\":\"Layar=ODU, TL=1\",\"linkId\":[\"Layer=OMS,TL=1\"],"
                + "\"srcTTP\":\"NE=FW1, Layer=OMS, TTP=1\","
                + "\"dstTTP\":\"NE=FW2, Layer=OMS, TTP=1\"},"
                + "{\"flowId\":\"Layar=ODU, TL=2\",\"linkId\":[\"Layer=OMS,TL=2\"],"
                + "\"srcTTP\":\"NE=FW2, Layer=OMS, TTP=2\", \"dstTTP\":\"NE=FW3, Layer=OMS,"
                + "TTP=1\"}],"
                + "\"link\":["
                + "{\"linkId\":\"Layer=OMS, TL=1\",\"srcTTP\":\"NE=FW1, Layer=OMS,"
                + "TTP=1\",\"dstTTP\":\"NE=FW2, Layer=OMS, TTP=1\"},"
                + "{\"linkId\":\"Layer=OMS, TL=2\",\"srcTTP\":\"NE=FW2, Layer=OMS,"
                + "TTP=2\",\"dstTTP\":\"NE=FW3, Layer=OMS, TTP=1\"}],"
                + "\"node\":["
                + "{\"nodeId\":\"NE=FW1\"},"
                + "{\"nodeId\":\"NE=FW2\"},"
                + "{\"nodeId\":\"NE=FW3\"}],"
                + "\"port\":["
                + "{\"portId\":\"NE=FW1, Layer=OCh, TTP=1\"},"
                + "{\"portId\":\"NE=FW1, Layer=OCh, CTP=1\"},"
                + "{\"portId\":\"NE=FW2, Layer=OCh, TTP=1\"},"
                + "{\"portId\":\"NE=FW2, Layer=OCh, TTP=2\"},"
                + "{\"portId\":\"NE=FW2, Layer=OCh, CTP=1\"},"
                + "{\"portId\":\"NE=FW2, Layer=OCh, CTP=2\"},"
                + "{\"portId\":\"NE=FW3, Layer=OCh, TTP=1\"},"
                + "{\"portId\":\"NE=FW3, Layer=OCh, CTP=1\"}]"
                + "}"
                + "}";

        JSONObject jsonObj = new JSONObject(testData);
        logger.debug(jsonObj.toString());

        LowerNodeInfo lower = jsonParse.lowerNodeInfotoPOJO(jsonObj, SEQUENCE_NO);
        assertEquals(lower.getLower().getFlow().get(0).getFlowId(), "Layar=ODU, TL=1");
        assertEquals(lower.getLower().getFlow().get(1).getLinkId().get(0), "Layer=OMS,TL=2");
        assertEquals(lower.getLower().getLink().get(0).getDstTTP(), "NE=FW2, Layer=OMS, TTP=1");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.lib.JSONParser#upperNodeInfotoPOJO()}
     */
    @Test
    public void testUpperNodeInfotoPOJO()
            throws JsonParseException, JsonMappingException, IOException {
        testData = "{"
                + "\"upper\":"
                + "{"
                + "\"link\":["
                + "{\"linkId\":\"Layer=OCh, TL=1\",\"srcTTP\":\"NE=FW1, Layer=OCh,"
                + "TTP=1\",\"dstTTP\":\"NE=FW2, Layer=OCh, TTP=1\"},"
                + "{\"linkId\":\"Layer=OCh, TL=2\",\"srcTTP\":\"NE=FW2, Layer=OCh,"
                + "TTP=2\",\"dstTTP\":\"NE=FW3, Layer=OCh, TTP=1\"}],"
                + "\"node\":["
                + "{\"nodeId\":\"NE=FW1\"},"
                + "{\"nodeId\":\"NE=FW2\"},"
                + "{\"nodeId\":\"NE=FW3\"}],"
                + "\"port\":["
                + "{\"portId\":\"NE=FW1, Layer=OCh, TTP=1\"},"
                + "{\"portId\":\"NE=FW1, Layer=OCh, TTP=2\"},"
                + "{\"portId\":\"NE=FW1, Layer=OCh, CTP=1\"},"
                + "{\"portId\":\"NE=FW1, Layer=OCh, CTP=2\"},"
                + "{\"portId\":\"NE=FW2, Layer=OCh, TTP=1\"},"
                + "{\"portId\":\"NE=FW2, Layer=OCh, TTP=2\"},"
                + "{\"portId\":\"NE=FW2, Layer=OCh, CTP=1\"},"
                + "{\"portId\":\"NE=FW2, Layer=OCh, CTP=2\"},"
                + "{\"portId\":\"NE=FW2, Layer=OCh, CTP=3\"},"
                + "{\"portId\":\"NE=FW2, Layer=OCh, CTP=4\"},"
                + "{\"portId\":\"NE=FW3, Layer=OCh, TTP=1\"},"
                + "{\"portId\":\"NE=FW3, Layer=OCh, TTP=2\"},"
                + "{\"portId\":\"NE=FW3, Layer=OCh, CTP=1\"},"
                + "{\"portId\":\"NE=FW3, Layer=OCh, CTP=1\"}]"
                + "}"
                + "}";

        JSONObject jsonObj = new JSONObject(testData);
        UpperNodeInfo upper = jsonParse.upperNodeInfotoPOJO(jsonObj, SEQUENCE_NO);
        assertEquals(upper.getUpper().getLink().get(0).getLinkId(), "Layer=OCh, TL=1");
        assertEquals(upper.getUpper().getPort().get(13).getPortId(), "NE=FW3, Layer=OCh, "
                + "CTP=1");

    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.lib.JSONParser#convertToJson()}
     */
    @Test
    public void convertToJsonWithLowerNodeInfo()
            throws JSONException, JsonParseException, JsonMappingException, IOException {

        testData = "{"
                + "\"lower\":"
                + "{"
                + "\"flow\":["
                + "{\"flowId\":\"Layar=ODU, TL=1\",\"linkId\":[\"Layer=OMS,TL=1\"],"
                + "\"srcTTP\":\"NE=FW1, Layer=OMS, TTP=1\", \"dstTTP\":\"NE=FW2, Layer=OMS,"
                + "TTP=1\"},"
                + "{\"flowId\":\"Layar=ODU, TL=2\",\"linkId\":[\"Layer=OMS,TL=2\"],"
                + "\"srcTTP\":\"NE=FW2, Layer=OMS, TTP=2\", \"dstTTP\":\"NE=FW3, Layer=OMS,"
                + "TTP=1\"}],"
                + "\"link\":["
                + "{\"linkId\":\"Layer=OMS, TL=1\",\"srcTTP\":\"NE=FW1, Layer=OMS,"
                + "TTP=1\",\"dstTTP\":\"NE=FW2, Layer=OMS, TTP=1\"},"
                + "{\"linkId\":\"Layer=OMS, TL=2\",\"srcTTP\":\"NE=FW2, Layer=OMS,"
                + "TTP=2\",\"dstTTP\":\"NE=FW3, Layer=OMS, TTP=1\"}],"
                + "\"node\":["
                + "{\"nodeId\":\"NE=FW1\"},"
                + "{\"nodeId\":\"NE=FW2\"},"
                + "{\"nodeId\":\"NE=FW3\"}],"
                + "\"port\":["
                + "{\"portId\":\"NE=FW1, Layer=OCh, TTP=1\"},"
                + "{\"portId\":\"NE=FW1, Layer=OCh, CTP=1\"},"
                + "{\"portId\":\"NE=FW2, Layer=OCh, TTP=1\"},"
                + "{\"portId\":\"NE=FW2, Layer=OCh, TTP=2\"},"
                + "{\"portId\":\"NE=FW2, Layer=OCh, CTP=1\"},"
                + "{\"portId\":\"NE=FW2, Layer=OCh, CTP=2\"},"
                + "{\"portId\":\"NE=FW3, Layer=OCh, TTP=1\"},"
                + "{\"portId\":\"NE=FW3, Layer=OCh, CTP=1\"}]"
                + "}"
                + "}";

        JSONObject jsonObj = new JSONObject(testData);
        LowerNodeInfo lower = jsonParse.lowerNodeInfotoPOJO(jsonObj, SEQUENCE_NO);
        jsonObj.remove("lower");
        jsonObj = new JSONObject(jsonParse.convertToJson(lower, SEQUENCE_NO));
        logger.debug("-----↓testNodeInfotoJSONLowerNodeInfo()結果-----");
        logger.debug(jsonObj.get("lower").toString());

    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.lib.JSONParser#convertToJson()}
     * @throws IOException
     * @throws JsonMappingException
     * @throws JsonParseException
     */
    @Test
    public void testConvertToJsonWithUpperNodeInfo()
            throws JsonParseException, JsonMappingException, IOException {
        testData = "{"
                + "\"upper\":"
                + "{"
                + "\"link\":["
                + "{\"linkId\":\"Layer=OCh, TL=1\",\"srcTTP\":\"NE=FW1, Layer=OCh,"
                + "TTP=1\",\"dstTTP\":\"NE=FW2, Layer=OCh, TTP=1\"},"
                + "{\"linkId\":\"Layer=OCh, TL=2\",\"srcTTP\":\"NE=FW2, Layer=OCh,"
                + "TTP=2\",\"dstTTP\":\"NE=FW3, Layer=OCh, TTP=1\"}],"
                + "\"node\":["
                + "{\"nodeId\":\"NE=FW1\"},"
                + "{\"nodeId\":\"NE=FW2\"},"
                + "{\"nodeId\":\"NE=FW3\"}],"
                + "\"port\":["
                + "{\"portId\":\"NE=FW1, Layer=OCh, TTP=1\"},"
                + "{\"portId\":\"NE=FW1, Layer=OCh, TTP=2\"},"
                + "{\"portId\":\"NE=FW1, Layer=OCh, CTP=1\"},"
                + "{\"portId\":\"NE=FW1, Layer=OCh, CTP=2\"},"
                + "{\"portId\":\"NE=FW2, Layer=OCh, TTP=1\"},"
                + "{\"portId\":\"NE=FW2, Layer=OCh, TTP=2\"},"
                + "{\"portId\":\"NE=FW2, Layer=OCh, CTP=1\"},"
                + "{\"portId\":\"NE=FW2, Layer=OCh, CTP=2\"},"
                + "{\"portId\":\"NE=FW2, Layer=OCh, CTP=3\"},"
                + "{\"portId\":\"NE=FW2, Layer=OCh, CTP=4\"},"
                + "{\"portId\":\"NE=FW3, Layer=OCh, TTP=1\"},"
                + "{\"portId\":\"NE=FW3, Layer=OCh, TTP=2\"},"
                + "{\"portId\":\"NE=FW3, Layer=OCh, CTP=1\"},"
                + "{\"portId\":\"NE=FW3, Layer=OCh, CTP=1\"}]"
                + "}"
                + "}";

        JSONObject jsonObj = new JSONObject(testData);
        UpperNodeInfo upper = jsonParse.upperNodeInfotoPOJO(jsonObj, SEQUENCE_NO);

        JSONObject test = new JSONObject(jsonParse.convertToJson(upper, SEQUENCE_NO));
        logger.debug("-----↓testNodeInfotoJSONUpperNodeInfo()結果-----");
        logger.debug(test.get("upper").toString());
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.lib.JSONParser#optFlowtoPOJO()}
     * @throws IOException
     * @throws JsonMappingException
     * @throws JsonParseException
     */
    @Test
    public void testOptFlowtoPOJO()
            throws JsonParseException, JsonMappingException, IOException {
        testData = "{\"terminationPointPairs\": [{"
                + "\"inPoint\": \"NW=SDN,NE=FW1,Layer=ODU,TTP=1\","
                + "\"outPoint\": \"NW=SDN,NE=FW1,Layer=ODU,CTP=1\"},"
                + "{"
                + "\"inPoint\": \"NW=SDN,NE=FW2,Layer=ODU,CTP=2\","
                + "\"outPoint\": \"NW=SDN,NE=FW1,Layer=ODU,CTP=3\""
                + "}]}";

        JSONObject jsonObj = new JSONObject(testData);
        logger.debug(jsonObj.toString());
        OptFlow optFlow = jsonParse.optFlowtoPOJO(jsonObj, SEQUENCE_NO);
        assertEquals(optFlow.getTerminationPointPairs().get(0).getInPoint(),
                "NW=SDN,NE=FW1,Layer=ODU,TTP=1");
        assertEquals(optFlow.getTerminationPointPairs().get(0).getOutPoint(),
                "NW=SDN,NE=FW1,Layer=ODU,CTP=1");
        assertEquals(optFlow.getTerminationPointPairs().get(1).getInPoint(),
                "NW=SDN,NE=FW2,Layer=ODU,CTP=2");
        assertEquals(optFlow.getTerminationPointPairs().get(1).getOutPoint(),
                "NW=SDN,NE=FW1,Layer=ODU,CTP=3");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.lib.JSONParser#ochFlowCreationResponseToPOJO()}
     */
    @Test
    public void testOchFlowCreationResponseToPOJO()
            throws JsonParseException, JsonMappingException, IOException {
        testData = "{"
                + "\"lsw\": {"
                + "\"fjFlowId\": \"1111111111\","
                + "\"terminationPointPairs\": ["
                + "{"
                + "\"inPoint\": \"lswInPoint1\","
                + "\"outPoint\": \"lswOutPoint1\","
                + "\"bandwidth\": \"lswBandwidth1\""
                + "},"
                + "{"
                + "\"inPoint\": \"NW=SDN, NE=FW2, Layer=OCH, CTPP=1\","
                + "\"outPoint\": \"NW=SDN, NE=FW2, Layer=OCH, CTPP=2\","
                + "\"bandwidth\": \"100G\""
                + "}]},"
                + "\"och\": ["
                + "{"
                + "\"fjFlowId\": \"ochFlowId1\","
                + "\"terminationPointPairs\": {"
                + "\"inPoint\": \"ochInPoint1\","
                + "\"outPoint\": \"ochOutPoint1\","
                + "\"bandwidth\": \"ochBandwidth1\""
                + "}"
                + "},"
                + "{"
                + "\"fjFlowId\": \"ochFlowId2\","
                + "\"terminationPointPairs\": {"
                + "\"inPoint\": \"ochInPoint2\","
                + "\"outPoint\": \"ochOutPoint2\","
                + "\"bandwidth\": \"ochBandwidth2\""
                + "}"
                + "}"
                + "]"
                + "}";

        JSONObject json = new JSONObject(testData);
        logger.debug(json.toString());
        JSONParser parser = new JSONParser();

        OchFlowCreationResponse result = parser.ochFlowCreationResponseToPOJO(json, SEQUENCE_NO);

        assertThat(result.getLsw().getFjFlowId(), is("1111111111"));
        assertThat(result.getLsw().getTerminationPointPairs().get(0).getInPoint(),
                is("lswInPoint1"));
        assertThat(result.getLsw().getTerminationPointPairs().get(0).getOutPoint(),
                is("lswOutPoint1"));
        assertThat(result.getLsw().getTerminationPointPairs().get(0).getBandwidth(),
                is("lswBandwidth1"));

        assertThat(result.getOch().get(0).getFjFlowId(), is("ochFlowId1"));
        assertThat(result.getOch().get(0).getTerminationPointPairs().getInPoint(),
                is("ochInPoint1"));
        assertThat(result.getOch().get(0).getTerminationPointPairs().getOutPoint(),
                is("ochOutPoint1"));
        assertThat(result.getOch().get(0).getTerminationPointPairs().getBandwidth(),
                is("ochBandwidth1"));

        assertThat(result.getOch().get(1).getFjFlowId(), is("ochFlowId2"));
        assertThat(result.getOch().get(1).getTerminationPointPairs().getInPoint(),
                is("ochInPoint2"));
        assertThat(result.getOch().get(1).getTerminationPointPairs().getOutPoint(),
                is("ochOutPoint2"));
        assertThat(result.getOch().get(1).getTerminationPointPairs().getBandwidth(),
                is("ochBandwidth2"));

    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.lib.JSONParser#oduFlowCreationResponseToPOJO()}
     */
    @Test
    public void testOduFlowCreationResponseToPOJO()
            throws JsonParseException, JsonMappingException, IOException {
        testData = "{"
                + "\"fjFlowId\": \"flowId\","
                + "\"terminationPointPairs\": ["
                + "{"
                + "\"inPoint\": \"NW=SDNNP-POC,NW=SDN,NE=FW1,Layer=ODU,TTP=xxxx\","
                + "\"outPoint\": \"NW=SDNNP-POC,NW=SDN,NE=FW1,Layer=ODU,CTPP=xxxxx\""
                + "}"
                + "],"
                + "\"constraint\": {"
                + "\"bandwidth\": \"bandwidth\","
                + "\"latency\": \"latency\""
                + "}"
                + "}";

        JSONObject json = new JSONObject(testData);
        logger.debug(json.toString());
        JSONParser parser = new JSONParser();

        OduFlowCreationResponse result = parser.oduFlowCreationResponseToPOJO(json, SEQUENCE_NO);
        assertThat(result.getFjFlowId(), is("flowId"));

        assertThat(result.getTerminationPointPairs().get(0).getInPoint(),
                is("NW=SDNNP-POC,NW=SDN,NE=FW1,Layer=ODU,TTP=xxxx"));
        assertThat(result.getTerminationPointPairs().get(0).getOutPoint(),
                is("NW=SDNNP-POC,NW=SDN,NE=FW1,Layer=ODU,CTPP=xxxxx"));

        assertThat(result.getConstraint().getBandwidth(), is("bandwidth"));
        assertThat(result.getConstraint().getLatency(), is("latency"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.lib.JSONParser#convertToList()}
     */
    @Test
    public void testConvertToList() throws JSONException, JsonProcessingException {
        testData = "[{\"test1\":\"1\"},{\"test2\":\"2\"}]";

        JSONArray json = new JSONArray(testData);
        logger.debug(json.toString());
        JSONParser parser = new JSONParser();

        List<String> result = parser.convertToList(json, SEQUENCE_NO);
        assertThat(result.toString(), is("[{\"test1\":\"1\"}, {\"test2\":\"2\"}]"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.lib.JSONParser#convertToList()}
     */
    @Test
    public void testConvertToListWithEmptyList() throws JSONException, JsonProcessingException {
        testData = "[]";

        JSONArray json = spy(new JSONArray(testData));
        logger.debug(json.toString());
        JSONParser parser = new JSONParser();

        List<String> result = parser.convertToList(json, SEQUENCE_NO);
        assertThat(result.toString(), is("[]"));

        verify(json, never()).getJSONObject(anyInt());
    }
}
