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
package org.o3project.ocnrm.odenos.driver.controller;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.o3project.ocnrm.lib.JSONParser;
import org.o3project.ocnrm.lib.MFApiCaller;
import org.o3project.ocnrm.lib.flow.OduFlowCreator;
import org.o3project.ocnrm.lib.table.Event;
import org.o3project.ocnrm.lib.table.ResourceInfoFlomMf;
import org.o3project.ocnrm.lib.table.TableManager;
import org.o3project.ocnrm.model.Constraint;
import org.o3project.ocnrm.model.IdExchangeRequest;
import org.o3project.ocnrm.model.ResourceRequest;
import org.o3project.ocnrm.model.TerminationPoints;
import org.o3project.ocnrm.model.bind.OduBindingData;
import org.o3project.ocnrm.model.bind.RmData;
import org.o3project.ocnrm.model.odu.OduFlow;
import org.o3project.ocnrm.model.odu.OduFlowCreationResponse;
import org.o3project.ocnrm.model.odu.OptFlow;
import org.o3project.ocnrm.ofctl.OfCtlSender;
import org.o3project.odenos.core.component.Logic;
import org.o3project.odenos.core.component.network.flow.Flow;
import org.o3project.odenos.core.component.network.flow.basic.BasicFlow;
import org.o3project.odenos.core.component.network.flow.basic.BasicFlowMatch;
import org.o3project.odenos.core.component.network.flow.basic.FlowAction;
import org.o3project.odenos.core.component.network.flow.basic.FlowActionOutput;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.slf4j.Logger;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ ResourceSendController.class, Thread.class })
public class ResourceSendControllerTest {
    private ResourceSendController target;
    private String seqNo = "#1";
    String transactionId = "transactionId";
    String nwId = "nwId";
    String eventName = "eventName";
    String flowId = "flowId";
    String mfId = "mfId";
    String action = "action";
    String time = "time";
    String driver = "driver";

    @Before
    public void setUp() throws Exception {
        target = PowerMockito.spy(new ResourceSendController());
    }

    @After
    public void tearDown() throws Exception {
        target = null;
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.controller.ResourceSendController
     * #registerNewOduFlow()}
     */
    @Test
    public void testRegisterNewOduFlow() throws Exception {
        OduFlow dummyOduFlow = new OduFlow();
        dummyOduFlow.setFjFlowId(flowId);
        List<TerminationPoints> points = new ArrayList<TerminationPoints>();
        dummyOduFlow.setTerminationPointPairs(points);
        dummyOduFlow.setConstraint(new Constraint());

        Event event = new Event(transactionId, nwId, eventName, flowId, action, time,
                driver);

        ResourceInfoFlomMf dummyResource = spy(new ResourceInfoFlomMf(transactionId,
                nwId, flowId, mfId, dummyOduFlow, time, driver));

        TableManager dummyManager = spy(TableManager.getInstance());
        PowerMockito.doReturn(dummyResource).when(dummyManager).checkIncompleteResource(eq(event));
        Whitebox.setInternalState(target, "manager", dummyManager);

        RmData dummyData = mock(RmData.class);
        PowerMockito.doReturn(dummyData).when(target, "getOduReplacementTable",
                eq(flowId), eq(points), eq(seqNo));

        List<String> dummyJson = spy(new ArrayList<String>());
        PowerMockito.doReturn(dummyJson).when(target, "makeOduFlow",
                eq(dummyData), anyObject(), eq(seqNo));

        PowerMockito.doNothing().when(target, "sendOfCtlSender",
                eq(dummyJson), eq(OfCtlSender.FLOW_ADD_METHOD), eq(2000), eq(seqNo));

        BasicFlow basicFlow = mock(BasicFlow.class);

        OduFlowCreationResponse result = target.registerNewOduFlow(basicFlow, event, seqNo);

        assertThat(result.getFjFlowId(), is(flowId));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.controller.ResourceSendController
     * #registerNewOduFlow()}
     */
    @Test
    public void testRegisterNewOduFlowWithNullResource() throws Exception {
        TerminationPoints point = new TerminationPoints();
        point.setInPoint("inPoint");
        point.setOutPoint("outPoint");
        List<TerminationPoints> points = new ArrayList<TerminationPoints>();
        points.add(point);

        Event event = new Event(transactionId, nwId, eventName, flowId, action, time, driver);

        TableManager dummyManager = spy(TableManager.getInstance());
        PowerMockito.doReturn(null).when(dummyManager).checkIncompleteResource(eq(event));
        Whitebox.setInternalState(target, "manager", dummyManager);

        Constraint constraint = new Constraint();
        constraint.setBandwidth("bandwidth");
        constraint.setLatency("letency");

        OduFlowCreationResponse dummyOduFlow = spy(new OduFlowCreationResponse());
        dummyOduFlow.setFjFlowId(flowId);
        dummyOduFlow.setTerminationPointPairs(points);
        dummyOduFlow.setConstraint(constraint);

        PowerMockito.doReturn(dummyOduFlow).when(target, "sendMf",
                eq(seqNo), (ResourceRequest) anyObject());

        RmData dummyData = mock(RmData.class);
        PowerMockito.doReturn(dummyData).when(target, "getOduReplacementTable",
                eq(flowId), eq(points), eq(seqNo));

        List<String> dummyJson = spy(new ArrayList<String>());
        PowerMockito.doReturn(dummyJson).when(target, "makeOduFlow",
                eq(dummyData), anyObject(), eq(seqNo));

        PowerMockito.doNothing().when(target, "sendOfCtlSender",
                eq(dummyJson), eq(OfCtlSender.FLOW_ADD_METHOD), anyInt(), eq(seqNo));

        BasicFlow basicFlow = spy(new BasicFlow(flowId));
        BasicFlowMatch match = new BasicFlowMatch("inNode", "inPort");
        basicFlow.addMatch(match);

        OduFlowCreationResponse result = target.registerNewOduFlow(basicFlow, event, seqNo);

        assertThat(result.getFjFlowId(), is(flowId));
        assertThat(result.getTerminationPointPairs().get(0).getInPoint(), is("inPoint"));
        assertThat(result.getTerminationPointPairs().get(0).getOutPoint(), is("outPoint"));
        assertThat(result.getConstraint().getBandwidth(), is("bandwidth"));
        assertThat(result.getConstraint().getLatency(), is("letency"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.controller.ResourceSendController
     * #getOduReplacementTable()}
     */
    @Test
    public void testGetOduReplacementTable() throws Exception {
        String srcName = "srcName";
        String dstName = "dstName";
        String resource = "{\"test\":\"test\"}";
        JSONObject json = new JSONObject(resource);

        TerminationPoints point = new TerminationPoints();
        point.setInPoint(srcName);
        point.setOutPoint(dstName);

        List<TerminationPoints> points = new ArrayList<>();
        points.add(point);

        IdExchangeRequest dummyIdExchangeRequest = mock(IdExchangeRequest.class);
        PowerMockito.whenNew(IdExchangeRequest.class).withAnyArguments()
                .thenReturn(dummyIdExchangeRequest);

        MFApiCaller dummyCaller = mock(MFApiCaller.class);
        when(dummyCaller.requestOduCorrespondingTable(eq(dummyIdExchangeRequest), eq(seqNo)))
                .thenReturn(json);
        PowerMockito.whenNew(MFApiCaller.class).withNoArguments().thenReturn(dummyCaller);

        OduBindingData dummyBinder = mock(OduBindingData.class);
        doNothing().when(dummyBinder).bind(srcName, resource);
        doNothing().when(dummyBinder).bind(dstName, resource);
        PowerMockito.whenNew(OduBindingData.class).withNoArguments().thenReturn(dummyBinder);

        RmData result = Whitebox.invokeMethod(target, "getOduReplacementTable",
                flowId, points, seqNo);
        assertThat(result, is((RmData) dummyBinder));
        verify(dummyBinder, times(1)).bind(eq(srcName), eq(resource));
        verify(dummyBinder, times(1)).bind(eq(dstName), eq(resource));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.controller.ResourceSendController
     * #getOduReplacementTable()}
     */
    @Test
    public void testGetOduReplacementTableWithEmptyTerminationPoints() throws Exception {
        List<TerminationPoints> points = new ArrayList<>();

        RmData result = Whitebox.invokeMethod(target, "getOduReplacementTable",
                flowId, points, seqNo);

        assertThat(result.getBindMap().size(), is(0));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.controller.ResourceSendController#sendOfCtlSender()}
     */
    @Test
    public void testSendOfCtlSender() throws Exception {
        JSONParser parser = new JSONParser();
        List<String> flow = parser.convertToList(new JSONArray("[]"), seqNo);

        OfCtlSender dummySender = mock(OfCtlSender.class);
        doNothing().when(dummySender).ofCtlSend(eq(flow), eq("add"), eq(seqNo));
        PowerMockito.whenNew(OfCtlSender.class).withNoArguments().thenReturn(dummySender);

        Whitebox.invokeMethod(target, "sendOfCtlSender", flow, "add", 500, seqNo);
        verify(dummySender, times(1)).ofCtlSend(eq(flow), eq("add"), eq(seqNo));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.controller.ResourceSendController#sendOfCtlSender()}
     */
    @Test
    public void testSendOfCtlSenderWithInterruptedException() throws Exception {
        Logger dummyLogger = mock(Logger.class);
        Field field = target.getClass().getSuperclass().getDeclaredField("logger");
        field.setAccessible(true);
        field.set(target, dummyLogger);

        PowerMockito.mockStatic(Thread.class);
        PowerMockito.doThrow(mock(InterruptedException.class)).when(Thread.class);
        Thread.sleep(500);

        List<String> flow = spy(new ArrayList<String>());
        Whitebox.invokeMethod(target, "sendOfCtlSender", flow, OfCtlSender.FLOW_ADD_METHOD, 500,
                seqNo);

        verify(dummyLogger, times(1)).error(seqNo + "\t" + "get InterruptedException with "
                + "500" + "ms sleep.");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.controller.ResourceSendController#makeOduFlow()}
     */
    @Test
    public void testMakeOduFlow() throws Exception {
        JSONParser parser = new JSONParser();
        List<String> flow = parser.convertToList(new JSONArray("[]"), seqNo);

        RmData data = mock(RmData.class);
        OptFlow optFlow = mock(OptFlow.class);

        OduFlowCreator dummyCreator = mock(OduFlowCreator.class);
        when(dummyCreator.createFlow(eq(optFlow), eq(data), eq(seqNo)))
                .thenReturn(flow);
        PowerMockito.whenNew(OduFlowCreator.class).withNoArguments().thenReturn(dummyCreator);

        List<String> result = Whitebox.invokeMethod(target, "makeOduFlow", data, optFlow, seqNo);

        assertThat(result.toString(), is(flow.toString()));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.controller.ResourceSendController#sendMf()}
     */
    @Test
    public void testSendMf() throws Exception {
        JSONObject json = new JSONObject("{test:test}");
        ResourceRequest request = mock(ResourceRequest.class);
        OduFlowCreationResponse dummyResponse = mock(OduFlowCreationResponse.class);

        MFApiCaller dummyCaller = mock(MFApiCaller.class);
        when(dummyCaller.createFlow(eq(request), eq(seqNo))).thenReturn(json);
        PowerMockito.whenNew(MFApiCaller.class).withNoArguments().thenReturn(dummyCaller);

        JSONParser dummyParser = mock(JSONParser.class);
        when(dummyParser.oduFlowCreationResponseToPOJO(eq(json), eq(seqNo)))
                .thenReturn(dummyResponse);
        PowerMockito.whenNew(JSONParser.class).withNoArguments().thenReturn(dummyParser);

        OduFlowCreationResponse result = Whitebox.invokeMethod(target, "sendMf", seqNo, request);

        assertThat(result, is(dummyResponse));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.controller.ResourceSendController#makeDirection()}
     */
    @Test
    public void testMakeDirection() throws Exception {
        String direction = "direction";
        BasicFlow flow = new BasicFlow(flowId);
        flow.getAttributes().put("direction", direction);

        String result = Whitebox.invokeMethod(target, "makeDirection", flow);

        assertThat(result, is(direction));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.controller.ResourceSendController#makeDirection()}
     */
    @Test
    public void testMakeDirectionWithNullDirection() throws Exception {
        String direction = "unidirectional";
        BasicFlow flow = new BasicFlow(flowId);

        String result = Whitebox.invokeMethod(target, "makeDirection", flow);

        assertThat(result, is(direction));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.controller.ResourceSendController#makeConstraint()}
     */
    @Test
    public void testMakeConstraint() throws Exception {
        String bandwidth = "bandwidth";
        String latency = "latency";
        BasicFlow flow = new BasicFlow(flowId);
        flow.getAttributes().put(Logic.AttrElements.REQ_BANDWIDTH, bandwidth);
        flow.getAttributes().put(Logic.AttrElements.REQ_LATENCY, latency);

        Constraint result = Whitebox.invokeMethod(target, "makeConstraint", flow);

        assertThat(result.getBandwidth(), is(bandwidth));
        assertThat(result.getLatency(), is(latency));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.controller.ResourceSendController
     * #makeTerminationPoints()}
     */
    @Test
    public void testMakeTerminationPoints() throws Exception {
        String inPoint = "inPoint";
        String outPoint = "outPoint";

        BasicFlow flow = new BasicFlow(flowId);
        BasicFlowMatch match = new BasicFlowMatch("inNode", inPoint);
        flow.matches.add(match);

        FlowActionOutput action = new FlowActionOutput(outPoint);
        List<FlowAction> actions = new ArrayList<>();
        actions.add(action);
        flow.edgeActions.put("key", actions);

        TerminationPoints result = Whitebox.invokeMethod(target, "makeTerminationPoints",
                flow, new TerminationPoints());

        assertThat(result.getInPoint(), is(inPoint));
        assertThat(result.getOutPoint(), is(outPoint));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.controller.ResourceSendController
     * #makeTerminationPoints()}
     */
    @Test
    public void testMakeTerminationPointsWithEmptyOutput() throws Exception {
        String inPoint = "inPoint";

        BasicFlow flow = new BasicFlow(flowId);
        BasicFlowMatch match = new BasicFlowMatch("inNode", inPoint);
        flow.matches.add(match);

        TerminationPoints result = Whitebox.invokeMethod(target, "makeTerminationPoints",
                flow, new TerminationPoints());

        assertThat(result.getInPoint(), is(inPoint));
        assertThat(result.getOutPoint(), is(nullValue()));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.controller.ResourceSendController#deleteOduFlow()}
     */
    @Test
    public void testDeleteOduFlow() throws Exception {
        Flow flow = new BasicFlow(flowId);

        OduFlow oduFlow = new OduFlow();
        oduFlow.setFjFlowId(flowId);

        Constraint constraint = new Constraint();
        oduFlow.setConstraint(constraint);

        TerminationPoints point = new TerminationPoints();
        point.setInPoint("inPoint");
        point.setOutPoint("outPoint");
        List<TerminationPoints> points = new ArrayList<>();
        points.add(point);
        oduFlow.setTerminationPointPairs(points);

        ResourceInfoFlomMf resource = new ResourceInfoFlomMf(transactionId, nwId, flowId, mfId,
                oduFlow, time, driver);

        RmData data = mock(RmData.class);
        List<String> json = new ArrayList<>();
        OptFlow optFlow = spy(new OptFlow());
        PowerMockito.whenNew(OptFlow.class).withNoArguments().thenReturn(optFlow);

        PowerMockito.doReturn(data).when(target, "getOduReplacementTable",
                eq(flowId), eq(points), eq(seqNo));
        PowerMockito.doReturn(json).when(target, "makeDeleteOduFlow",
                eq(data), eq(optFlow), eq(seqNo));
        PowerMockito.doNothing().when(target, "sendOfCtlSender",
                eq(json), eq(OfCtlSender.FLOW_ADD_METHOD), anyInt(), eq(seqNo));

        MFApiCaller dummyCaller = mock(MFApiCaller.class);
        doReturn(true).when(dummyCaller)
                .deleteOduFlow(eq("{\"fjFlowId\":\"flowId\"}"), eq(seqNo));
        PowerMockito.whenNew(MFApiCaller.class).withNoArguments().thenReturn(dummyCaller);

        boolean result = target.deleteOduFlow(resource, flow, seqNo);

        assertThat(result, is(true));

        PowerMockito.verifyPrivate(target, times(1)).invoke("sendOfCtlSender",
                eq(json), eq(OfCtlSender.FLOW_DELETE_METHOD), eq(2000), eq(seqNo));
        verify(dummyCaller, times(1)).deleteOduFlow(eq("{\"fjFlowId\":\"mfId\"}"), eq(seqNo));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.controller.ResourceSendController
     * #makeDeleteOduFlow()}
     */
    @Test
    public void testMakeDeleteOduFlow() throws Exception {
        RmData data = mock(RmData.class);
        OptFlow flow = mock(OptFlow.class);
        JSONParser parser = new JSONParser();
        List<String> array = parser.convertToList(new JSONArray("[]"), seqNo);

        OduFlowCreator dummyCreator = mock(OduFlowCreator.class);
        when(dummyCreator.createFlow(eq(flow), eq(data), eq(seqNo))).thenReturn(array);
        PowerMockito.whenNew(OduFlowCreator.class).withNoArguments().thenReturn(dummyCreator);

        List<String> result = Whitebox
                .invokeMethod(target, "makeDeleteOduFlow", data, flow, seqNo);

        assertThat(result.toString(), is(array.toString()));
    }
}
