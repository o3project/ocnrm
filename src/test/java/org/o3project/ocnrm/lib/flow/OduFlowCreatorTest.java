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
package org.o3project.ocnrm.lib.flow;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.o3project.ocnrm.lib.JSONParser;
import org.o3project.ocnrm.lib.flow.model.ActionsData;
import org.o3project.ocnrm.lib.flow.model.OduMatchData;
import org.o3project.ocnrm.model.FlowData;
import org.o3project.ocnrm.model.TerminationPoints;
import org.o3project.ocnrm.model.bind.OduBindingData;
import org.o3project.ocnrm.model.bind.RmData;
import org.o3project.ocnrm.model.odu.OptFlow;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ OduFlowCreator.class })
public class OduFlowCreatorTest {
    private OduFlowCreator target;
    private RmData data;
    private TerminationPoints point;
    private String seqNo = "#1";

    @Before
    public void setUp() throws Exception {
        target = spy(new OduFlowCreator());
    }

    @After
    public void tearDown() throws Exception {
        point = null;
        target = null;
        data = null;
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.lib.flow.OduFlowCreator#makeMatch()}
     */
    @Test
    public void testMakeMatch() throws JsonParseException, JsonMappingException, IOException {
        point = new TerminationPoints();
        point.setInPoint("src");

        String srcJson = "{"
                + "\"dpid\": \"srcDpid\","
                + "\"odutype\": \"srcOdutype\","
                + "\"port\": \"srcPort\","
                + "\"tpn\": \"-1\","
                + "\"ts\": \"srcTs,ts2\""
                + "}";

        data = new OduBindingData();
        data.bind(point.getInPoint(), srcJson);

        OduMatchData result = (OduMatchData) target.makeMatch(data, point);

        assertThat(result.getIn_port(), is("srcPort"));
        assertThat(result.getOdu_sigtype(), is("srcOdutype"));
        assertThat(result.getOdu_sigid().getTpn(), is("4095"));
        assertThat(result.getOdu_sigid().getTsmap(), is("srcTs,ts2"));
        assertThat(result.getOdu_sigid().getTslen(), is("8"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.lib.flow.OduFlowCreator#makeMatch()}
     */
    @Test
    public void testMakeMatchWithEmptyPort()
            throws JsonParseException, JsonMappingException, IOException {
        point = new TerminationPoints();
        point.setInPoint("src");

        String srcJson = "{"
                + "\"dpid\": \"srcDpid\","
                + "\"odutype\": \"srcOdutype\","
                + "\"tpn\": \"-1\","
                + "\"ts\": \"srcTs,ts2\""
                + "}";

        data = new OduBindingData();
        data.bind(point.getInPoint(), srcJson);

        OduMatchData result = (OduMatchData) target.makeMatch(data, point);

        assertThat(result.getIn_port(), is(nullValue()));
        assertThat(result.getOdu_sigtype(), is("srcOdutype"));
        assertThat(result.getOdu_sigid().getTpn(), is("4095"));
        assertThat(result.getOdu_sigid().getTsmap(), is("srcTs,ts2"));
        assertThat(result.getOdu_sigid().getTslen(), is("8"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.lib.flow.OduFlowCreator#makeMatch()}
     */
    @Test
    public void testMakeMatchWithEmptyOduType()
            throws JsonParseException, JsonMappingException, IOException {
        point = new TerminationPoints();
        point.setInPoint("src");

        String srcJson = "{"
                + "\"dpid\": \"srcDpid\","
                + "\"port\": \"srcPort\","
                + "\"tpn\": \"-1\","
                + "\"ts\": \"srcTs,ts2\""
                + "}";

        data = new OduBindingData();
        data.bind(point.getInPoint(), srcJson);

        OduMatchData result = (OduMatchData) target.makeMatch(data, point);

        assertThat(result.getIn_port(), is("srcPort"));
        assertThat(result.getOdu_sigtype(), is(nullValue()));
        assertThat(result.getOdu_sigid().getTpn(), is("4095"));
        assertThat(result.getOdu_sigid().getTsmap(), is("srcTs,ts2"));
        assertThat(result.getOdu_sigid().getTslen(), is("8"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.lib.flow.OduFlowCreator#makeMatch()}
     */
    @Test
    public void testMakeMatchWithEmptyTs()
            throws JsonParseException, JsonMappingException, IOException {
        point = new TerminationPoints();
        point.setInPoint("src");

        String srcJson = "{"
                + "\"dpid\": \"srcDpid\","
                + "\"odutype\": \"srcOdutype\","
                + "\"port\": \"srcPort\","
                + "\"tpn\": \"-1\""
                + "}";

        data = new OduBindingData();
        data.bind(point.getInPoint(), srcJson);

        OduMatchData result = (OduMatchData) target.makeMatch(data, point);

        assertThat(result.getIn_port(), is("srcPort"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.lib.flow.OduFlowCreator#makeMatch()}
     */
    @Test
    public void testMakeMatchWithEmptyTpn()
            throws JsonParseException, JsonMappingException, IOException {
        point = new TerminationPoints();
        point.setInPoint("src");

        String srcJson = "{"
                + "\"dpid\": \"srcDpid\","
                + "\"odutype\": \"srcOdutype\","
                + "\"port\": \"srcPort\","
                + "\"ts\": \"srcTs,ts2\""
                + "}";

        data = new OduBindingData();
        data.bind(point.getInPoint(), srcJson);

        OduMatchData result = (OduMatchData) target.makeMatch(data, point);

        assertThat(result.getIn_port(), is("srcPort"));
        assertThat(result.getOdu_sigtype(), is("srcOdutype"));
        assertThat(result.getOdu_sigid().getTpn(), is(nullValue()));
        assertThat(result.getOdu_sigid().getTsmap(), is("srcTs,ts2"));
        assertThat(result.getOdu_sigid().getTslen(), is("8"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.lib.flow.OduFlowCreator#makeInstruments()}
     */
    @Test
    public void testMakeInstruments() throws JsonParseException, JsonMappingException, IOException {
        point = new TerminationPoints();
        point.setOutPoint("dst");

        String dstJson = "{"
                + "\"dpid\": \"dstDpid\","
                + "\"odutype\": \"dstOdutype\","
                + "\"port\": \"dstPort\","
                + "\"tpn\": \"-1\","
                + "\"ts\": \"dstTs,ts2\""
                + "}";

        data = new OduBindingData();
        data.bind(point.getOutPoint(), dstJson);

        List<ActionsData> actions = target.makeActions(data, point);

        assertThat(actions.size(), is(2));

        assertThat(actions.get(0).getType(), is("OUTPUT"));
        assertThat(actions.get(0).getPort(), is("dstPort"));

        assertThat(actions.get(1).getType(), is("SET_FIELD"));
        assertThat(actions.get(1).getField().get(0), is("tsmap"));
        assertThat(actions.get(1).getValue().get(0), is("dstTs,ts2"));

        assertThat(actions.get(1).getField().get(1), is("tpn"));
        assertThat(actions.get(1).getValue().get(1), is("4095"));

        assertThat(actions.get(1).getField().get(2), is("tslen"));
        assertThat(actions.get(1).getValue().get(2), is("8"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.lib.flow.OduFlowCreator#makeInstruments()}
     */
    @Test
    public void testMakeInstrumentsWithEmptyTpn()
            throws JsonParseException, JsonMappingException, IOException {
        point = new TerminationPoints();
        point.setOutPoint("dst");

        String dstJson = "{"
                + "\"dpid\": \"dstDpid\","
                + "\"odutype\": \"dstOdutype\","
                + "\"port\": \"dstPort\","
                + "\"ts\": \"dstTs,ts2\""
                + "}";

        data = new OduBindingData();
        data.bind(point.getOutPoint(), dstJson);

        List<ActionsData> actions = target.makeActions(data, point);

        assertThat(actions.size(), is(2));

        assertThat(actions.get(0).getType(), is("OUTPUT"));
        assertThat(actions.get(0).getPort(), is("dstPort"));

        assertThat(actions.get(1).getType(), is("SET_FIELD"));

        assertThat(actions.get(1).getField().get(0), is("tsmap"));
        assertThat(actions.get(1).getValue().get(0), is("dstTs,ts2"));

        assertThat(actions.get(1).getField().get(1), is("tslen"));
        assertThat(actions.get(1).getValue().get(1), is("8"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.lib.flow.OduFlowCreator#makeInstruments()}
     */
    @Test
    public void testMakeInstrumentsWithEmptyTs()
            throws JsonParseException, JsonMappingException, IOException {
        point = new TerminationPoints();
        point.setOutPoint("dst");

        String dstJson = "{"
                + "\"dpid\": \"dstDpid\","
                + "\"odutype\": \"dstOdutype\","
                + "\"port\": \"dstPort\","
                + "\"tpn\": \"-1\""
                + "}";

        data = new OduBindingData();
        data.bind(point.getOutPoint(), dstJson);

        List<ActionsData> actions = target.makeActions(data, point);

        assertThat(actions.size(), is(1));

        assertThat(actions.get(0).getType(), is("OUTPUT"));
        assertThat(actions.get(0).getPort(), is("dstPort"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.lib.flow.OduFlowCreator#makeInstruments()}
     */
    @Test
    public void testMakeInstrumentsWithEmptyActionField()
            throws JsonParseException, JsonMappingException, IOException {
        point = new TerminationPoints();
        point.setOutPoint("dst");

        String dstJson = "{"
                + "\"dpid\": \"dstDpid\","
                + "\"port\": \"dstPort\""
                + "}";

        data = new OduBindingData();
        data.bind(point.getOutPoint(), dstJson);

        List<ActionsData> actions = target.makeActions(data, point);

        assertThat(actions.size(), is(1));

        assertThat(actions.get(0).getType(), is("OUTPUT"));
        assertThat(actions.get(0).getPort(), is("dstPort"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.lib.flow.OduFlowCreator#makeInstruments()}
     */
    @Test
    public void testMakeInstrumentsWithEmptyPort()
            throws JsonParseException, JsonMappingException, IOException {
        point = new TerminationPoints();
        point.setOutPoint("dst");

        String dstJson = "{"
                + "\"dpid\": \"dstDpid\","
                + "\"odutype\": \"dstOdutype\","
                + "\"tpn\": \"-1\","
                + "\"ts\": \"dstTs,ts2\""
                + "}";

        data = new OduBindingData();
        data.bind(point.getOutPoint(), dstJson);

        List<ActionsData> actions = target.makeActions(data, point);

        assertThat(actions.size(), is(1));

        assertThat(actions.get(0).getType(), is("SET_FIELD"));
        assertThat(actions.get(0).getField().get(0), is("tsmap"));
        assertThat(actions.get(0).getValue().get(0), is("dstTs,ts2"));

        assertThat(actions.get(0).getField().get(1), is("tpn"));
        assertThat(actions.get(0).getValue().get(1), is("4095"));

        assertThat(actions.get(0).getField().get(2), is("tslen"));
        assertThat(actions.get(0).getValue().get(2), is("8"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.lib.flow.OduFlowCreator#createFlow()}
     */
    @Test
    public void testCreateFlow()
            throws JsonParseException, JsonMappingException, IOException {
        point = new TerminationPoints();
        point.setInPoint("src");
        point.setOutPoint("dst");

        String srcJson = "{"
                + "\"dpid\": \"srcDpid\","
                + "\"odutype\": \"srcOdutype\","
                + "\"port\": \"srcPort\","
                + "\"tpn\": \"-1\","
                + "\"ts\": \"srcTs,ts2\""
                + "}";

        String dstJson = "{"
                + "\"dpid\": \"dstDpid\","
                + "\"odutype\": \"dstOdutype\","
                + "\"port\": \"dstPort\","
                + "\"tpn\": \"-1\","
                + "\"ts\": \"dstTs,ts2\""
                + "}";

        data = new OduBindingData();
        data.bind(point.getInPoint(), srcJson);
        data.bind(point.getOutPoint(), dstJson);

        TerminationPoints point2 = new TerminationPoints();
        point2.setInPoint("dst");
        point2.setOutPoint("src");

        List<TerminationPoints> points = new ArrayList<TerminationPoints>();
        points.add(point);
        points.add(point2);

        OptFlow flow = new OptFlow();
        flow.setTerminationPointPairs(points);

        List<String> result = target.createFlow(flow, data, seqNo);

        JSONObject resultJson1 = new JSONObject(result.get(0));

        assertThat(resultJson1.get("dpid").toString(), is("srcDpid"));
        assertThat(resultJson1.getJSONObject("match").get("in_port").toString(),
                is("srcPort"));
        assertThat(resultJson1.getJSONArray("actions")
                .getJSONObject(0).get("port").toString(),
                is("dstPort"));

        JSONObject resultJson2 = new JSONObject(result.get(1));

        assertThat(resultJson2.get("dpid").toString(), is("dstDpid"));
        assertThat(resultJson2.getJSONObject("match").get("in_port").toString(),
                is("dstPort"));
        assertThat(resultJson2.getJSONArray("actions")
                .getJSONObject(0).get("port").toString(),
                is("srcPort"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.lib.flow.OduFlowCreator#createFlow()}
     */
    @Test
    public void testCreateFlowWithEmptyTerminationPoints()
            throws JsonParseException, JsonMappingException, IOException {
        point = new TerminationPoints();

        String srcJson = "{"
                + "\"dpid\": \"srcDpid\","
                + "\"odutype\": \"srcOdutype\","
                + "\"port\": \"srcPort\","
                + "\"tpn\": \"-1\","
                + "\"ts\": \"srcTs,ts2\""
                + "}";

        String dstJson = "{"
                + "\"dpid\": \"dstDpid\","
                + "\"odutype\": \"dstOdutype\","
                + "\"port\": \"dstPort\","
                + "\"tpn\": \"-1\","
                + "\"ts\": \"dstTs,ts2\""
                + "}";

        data = new OduBindingData();
        data.bind("src", srcJson);
        data.bind("dst", dstJson);

        OptFlow flow = new OptFlow();
        flow.setTerminationPointPairs(new ArrayList<TerminationPoints>());

        List<String> result = target.createFlow(flow, data, seqNo);

        assertThat(result.toString(),
                is("[]"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.lib.flow.OduFlowCreator#createFlow()}
     */
    @Test
    public void testCreateFlowWithEmptyDpid()
            throws JsonParseException, JsonMappingException, IOException {
        point = new TerminationPoints();
        point.setInPoint("src");
        point.setOutPoint("dst");

        String srcJson = "{"
                + "\"odutype\": \"srcOdutype\","
                + "\"port\": \"srcPort\","
                + "\"tpn\": \"-1\","
                + "\"ts\": \"srcTs,ts2\""
                + "}";

        String dstJson = "{"
                + "\"odutype\": \"dstOdutype\","
                + "\"port\": \"dstPort\","
                + "\"tpn\": \"-1\","
                + "\"ts\": \"dstTs,ts2\""
                + "}";

        data = new OduBindingData();
        data.bind(point.getInPoint(), srcJson);
        data.bind(point.getOutPoint(), dstJson);

        TerminationPoints point2 = new TerminationPoints();
        point2.setInPoint("dst");
        point2.setOutPoint("src");

        List<TerminationPoints> points = new ArrayList<TerminationPoints>();
        points.add(point);
        points.add(point2);

        OptFlow flow = new OptFlow();
        flow.setTerminationPointPairs(points);

        List<String> result = target.createFlow(flow, data, seqNo);

        JSONObject resultJson1 = new JSONObject(result.get(0));

        assertFalse(resultJson1.has("dpid"));
        assertThat(resultJson1.getJSONObject("match").get("in_port").toString(),
                is("srcPort"));
        assertThat(resultJson1.getJSONArray("actions").getJSONObject(0)
                .get("port").toString(),
                is("dstPort"));

        JSONObject resultJson2 = new JSONObject(result.get(1));

        assertFalse(resultJson2.has("dpid"));
        assertThat(resultJson2.getJSONObject("match").get("in_port").toString(),
                is("dstPort"));
        assertThat(resultJson2.getJSONArray("actions").getJSONObject(0)
                .get("port").toString(),
                is("srcPort"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.lib.flow.OduFlowCreator#createFlow()}
     */
    @Test
    public void testCreateFlowWithJsonProcessingException() throws Exception {
        Logger dummyLogger = mock(Logger.class);
        Field field = target.getClass().getSuperclass().getSuperclass().getDeclaredField("logger");
        field.setAccessible(true);
        field.set(target, dummyLogger);

        JSONParser parser = mock(JSONParser.class);
        PowerMockito.whenNew(JSONParser.class).withNoArguments().thenReturn(parser);

        OptFlow flow = new OptFlow();
        flow.setTerminationPointPairs(new ArrayList<TerminationPoints>());

        PowerMockito.doThrow(mock(JsonProcessingException.class)).when(parser)
                .convertToJson(anyList(), eq(seqNo));
        List<String> result = target.createFlow(flow, data, seqNo);

        assertThat(result.toString(), is("[]"));
        verify(dummyLogger, times(1)).error(seqNo + "\t" + "JsonProcessingException occured.");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.lib.flow.OduFlowCreator#createFlow()}
     */
    @Test
    public void testCreateFlowWithJSONException() throws Exception {
        Logger dummyLogger = mock(Logger.class);
        Field field = target.getClass().getSuperclass().getSuperclass().getDeclaredField("logger");
        field.setAccessible(true);
        field.set(target, dummyLogger);

        JSONParser parser = mock(JSONParser.class);
        PowerMockito.whenNew(JSONParser.class).withNoArguments().thenReturn(parser);

        OptFlow flow = new OptFlow();
        flow.setTerminationPointPairs(new ArrayList<TerminationPoints>());

        PowerMockito.doThrow(mock(JSONException.class)).when(parser)
                .convertToJson(anyListOf(FlowData.class), eq(seqNo));
        List<String> result = target.createFlow(flow, data, seqNo);

        assertThat(result.toString(), is("[]"));
        verify(dummyLogger, times(1)).error(seqNo + "\t" + "JSONException occured.");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.lib.flow.OduFlowCreator#makeOutput()}
     */
    @Test
    public void testMakeOutput() {
        String type = "type";
        String output = "output";

        ActionsData result = target.makeOutput(type, output);

        assertThat(result.getType(), is(type));
        assertThat(result.getPort(), is(output));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.lib.flow.OduFlowCreator#makeAction()}
     */
    @Test
    public void testMakeAction() {
        String type = "type";
        List<String> field = new ArrayList<>();
        List<String> value = new ArrayList<>();

        ActionsData result = target.makeAction(type, field, value);

        assertThat(result.getType(), is(type));
        assertThat(result.getField(), is(field));
        assertThat(result.getValue(), is(value));
    }
}
