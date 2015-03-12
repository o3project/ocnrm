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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.msgpack.type.Value;
import org.o3project.ocnrm.odenos.lib.OdenOsSender;
import org.o3project.odenos.core.component.network.flow.Flow;
import org.o3project.odenos.core.component.network.flow.FlowObject;
import org.o3project.odenos.core.component.network.flow.FlowSet;
import org.o3project.odenos.core.component.network.flow.basic.BasicFlow;
import org.o3project.odenos.core.component.network.flow.basic.BasicFlowMatch;
import org.o3project.odenos.core.component.network.flow.basic.FlowAction;
import org.o3project.odenos.core.component.network.flow.basic.FlowActionOutput;
import org.o3project.odenos.core.component.network.topology.Link;
import org.o3project.odenos.core.component.network.topology.Node;
import org.o3project.odenos.core.component.network.topology.Port;
import org.o3project.odenos.core.component.network.topology.Topology;
import org.o3project.odenos.remoteobject.message.MessageBodyUnpacker.ParseBodyException;
import org.o3project.odenos.remoteobject.message.Request.Method;
import org.o3project.odenos.remoteobject.message.Response;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.restlet.representation.Representation;
import org.slf4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ GUIRestApi.class, OdenOsSender.class })
public class GUIRestApiTest {
    private GUIRestApi target;

    private String nwId = "nwId";
    private String objectId = "objectId";

    @Before
    public void setUp() throws Exception {
        target = PowerMockito.spy(new GUIRestApi());
    }

    @After
    public void tearDown() throws Exception {
        target = null;
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.rest.GUIRestApi#getNWComponentInfo()}
     */
    @Test
    public void testGetNWComponentInfo() throws Exception {
        PowerMockito.doReturn(nwId).when(target, "getQueryValue", anyString());
        OdenOsSender sender = spy(OdenOsSender.getInstance());
        doReturn(objectId).when(sender).getConnections(nwId);
        Whitebox.setInternalState(target, "sender", sender);

        JSONObject testResult = new JSONObject("{test:result}");

        PowerMockito.doReturn(testResult).when(target, "makeFlows", eq(nwId));
        PowerMockito.doReturn(testResult).when(target, "makeTopology", eq(nwId));
        PowerMockito.doReturn(testResult).when(target, "makeBoundary", eq(nwId), eq(objectId));

        Representation response = target.getNWComponentInfo();

        JSONObject result = new JSONObject(response.getText());

        assertThat(result.getJSONObject(nwId).get("flow").toString(),
                is(testResult.toString()));
        assertThat(result.getJSONObject(nwId).get("topology").toString(),
                is(testResult.toString()));
        assertThat(result.getJSONObject(nwId).get("boundaries").toString(),
                is(testResult.toString()));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.rest.GUIRestApi#getNWComponentInfo()}
     */
    @Test
    public void testGetNWComponentInfoWithEmptyNwId() throws Exception {
        Logger dummyLogger = mock(Logger.class);
        Field field = target.getClass().getSuperclass().getDeclaredField("logger");
        field.setAccessible(true);
        field.set(target, dummyLogger);

        Representation response = target.getNWComponentInfo();
        JSONObject result = new JSONObject(response.getText());

        assertThat(result.toString(), is("{}"));
        PowerMockito.verifyPrivate(target, never()).invoke("makeFlows", anyString());
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.rest.GUIRestApi#getNWComponentInfo()}
     */
    @Test
    public void testGetNWComponentInfoWithNotLayerizedNw() throws Exception {
        PowerMockito.doReturn(nwId).when(target, "getQueryValue", anyString());
        OdenOsSender sender = spy(OdenOsSender.getInstance());
        doReturn(null).when(sender).getConnections(nwId);
        Whitebox.setInternalState(target, "sender", sender);

        JSONObject testResult = new JSONObject("{test:result}");

        PowerMockito.doReturn(testResult).when(target, "makeFlows", eq(nwId));
        PowerMockito.doReturn(testResult).when(target, "makeTopology", eq(nwId));

        Representation response = target.getNWComponentInfo();

        JSONObject result = new JSONObject(response.getText());

        assertThat(result.getJSONObject(nwId).get("flow").toString(),
                is(testResult.toString()));
        assertThat(result.getJSONObject(nwId).get("topology").toString(),
                is(testResult.toString()));
        PowerMockito.verifyPrivate(target, never())
                .invoke("makeBoundary", anyString(), anyString());
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.rest.GUIRestApi#getNWComponentInfo()}
     */
    @Test
    public void testGetNWComponentInfoWithJsonProcessingException() throws Exception {
        Logger dummyLogger = mock(Logger.class);
        Field field = target.getClass().getSuperclass().getDeclaredField("logger");
        field.setAccessible(true);
        field.set(target, dummyLogger);

        Field seqNo = target.getClass().getSuperclass().getDeclaredField("seqNo");
        seqNo.setAccessible(true);

        PowerMockito.doReturn(nwId).when(target, "getQueryValue", anyString());

        JsonProcessingException mock = mock(JsonProcessingException.class);
        when(mock.getMessage()).thenReturn("message");
        PowerMockito.doThrow(mock).when(target, "makeFlows", eq(nwId));

        Representation response = target.getNWComponentInfo();
        JSONObject result = new JSONObject(response.getText());

        assertThat(result.toString(), is("{}"));
        verify(dummyLogger, times(1))
                .error((String) seqNo.get(target) + "\t" + "JsonProcessingException is occured: "
                        + mock.getMessage());
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.rest.GUIRestApi#getNWComponentInfo()}
     */
    @Test
    public void testGetNWComponentInfoWithJSONException() throws Exception {
        Logger dummyLogger = mock(Logger.class);
        Field field = target.getClass().getSuperclass().getDeclaredField("logger");
        field.setAccessible(true);
        field.set(target, dummyLogger);

        Field seqNo = target.getClass().getSuperclass().getDeclaredField("seqNo");
        seqNo.setAccessible(true);

        PowerMockito.doReturn(nwId).when(target, "getQueryValue", anyString());

        JSONException mock = mock(JSONException.class);
        when(mock.getMessage()).thenReturn("message");

        PowerMockito.doThrow(mock).when(target, "makeFlows", eq(nwId));

        Representation response = target.getNWComponentInfo();
        JSONObject result = new JSONObject(response.getText());

        assertThat(result.toString(), is("{}"));
        verify(dummyLogger, times(1))
                .error((String) seqNo.get(target) + "\t" + "JSONException is occured: "
                        + mock.getMessage());
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.rest.GUIRestApi#getNWComponentInfo()}
     */
    @Test
    public void testGetNWComponentInfoWithParseBodyException() throws Exception {
        Logger dummyLogger = mock(Logger.class);
        Field field = target.getClass().getSuperclass().getDeclaredField("logger");
        field.setAccessible(true);
        field.set(target, dummyLogger);

        Field seqNo = target.getClass().getSuperclass().getDeclaredField("seqNo");
        seqNo.setAccessible(true);

        PowerMockito.doReturn(nwId).when(target, "getQueryValue", anyString());

        ParseBodyException mock = mock(ParseBodyException.class);
        when(mock.getMessage()).thenReturn("message");
        PowerMockito.doThrow(mock).when(target, "makeFlows", eq(nwId));

        Representation response = target.getNWComponentInfo();
        JSONObject result = new JSONObject(response.getText());

        assertThat(result.toString(), is("{}"));
        verify(dummyLogger, times(1))
                .error((String) seqNo.get(target) + "\t" + "ParseBodyException is occured: "
                        + mock.getMessage());
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.rest.GUIRestApi#makeBoundary()}
     */
    @Test
    public void testMakeBoundary() throws Exception {
        Response response = spy(new Response(Response.OK, "{\"1\":\"boundary\"}"));

        OdenOsSender sender = spy(OdenOsSender.getInstance());

        doReturn(response).when(sender).sendRequest(eq("systemmanager"), eq(Method.GET),
                eq("components/" + objectId + "/settings/boundaries"), eq(""));
        Whitebox.setInternalState(target, "sender", sender);

        JSONObject result =
                Whitebox.invokeMethod(target, "makeBoundary", nwId, objectId);

        assertThat(result.toString(), is("{\"1\":\"boundary\"}"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.rest.GUIRestApi#makeTopology()}
     */
    @Test
    public void testMakeTopology() throws Exception {
        String nodeId = "nodeId";
        String linkId = "linkId";
        String portId = "portId";

        Map<String, String> attribute = new HashMap<>();
        attribute.put("testAttribute", "testAttributeParam");

        Map<String, Port> ports = new HashMap<>();
        Port port = new Port("1", portId, nodeId, "outLink", "inLink", attribute);
        ports.put(portId, port);

        Map<String, Node> nodes = new HashMap<>();
        Node node = new Node("0", nodeId, ports, attribute);
        nodes.put(nodeId, node);

        Link link = new Link(linkId, "srcNode", "srcPort", "dstNode", "dstPort");
        Map<String, Link> links = new HashMap<>();
        links.put(linkId, link);

        Topology topology = spy(new Topology("1", nodes, links));

        Response response = spy(new Response(Response.OK, topology));
        Value value = mock(Value.class);
        when(value.toString()).thenReturn("testValue");
        doReturn(value).when(response).getBodyValue();

        OdenOsSender sender = spy(OdenOsSender.getInstance());
        doReturn(response).when(sender).sendRequest(anyString(), (Method) anyObject(),
                anyString(), anyObject());
        Whitebox.setInternalState(target, "sender", sender);

        JSONObject result = Whitebox.invokeMethod(target, "makeTopology", nwId);

        assertThat(result.getJSONObject("nodes").getJSONObject("nodeId").get("node_id").toString(),
                is("nodeId"));

        assertThat(result.getJSONObject("nodes").getJSONObject("nodeId").getJSONObject("ports")
                .getJSONObject("portId").get("port_id").toString(),
                is("portId"));

        assertThat(result.getJSONObject("links").getJSONObject("linkId").get("link_id").toString(),
                is("linkId"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.rest.GUIRestApi#makeTopology()}
     */
    @Test
    public void testMakeTopologyWithEmptyLink() throws Exception {
        String nodeId = "nodeId";
        String portId = "portId";

        Map<String, String> attribute = new HashMap<>();
        attribute.put("testAttribute", "testAttributeParam");

        Map<String, Port> ports = new HashMap<>();
        Port port = new Port("1", portId, nodeId, "outLink", "inLink", attribute);
        ports.put(portId, port);

        Map<String, Node> nodes = new HashMap<>();
        Node node = new Node("0", nodeId, ports, attribute);
        nodes.put(nodeId, node);

        Map<String, Link> links = new HashMap<>();

        Topology topology = spy(new Topology("1", nodes, links));

        Response response = spy(new Response(Response.OK, topology));
        Value value = mock(Value.class);
        when(value.toString()).thenReturn("testValue");
        doReturn(value).when(response).getBodyValue();

        OdenOsSender sender = spy(OdenOsSender.getInstance());
        doReturn(response).when(sender).sendRequest(anyString(), (Method) anyObject(),
                anyString(), anyObject());
        Whitebox.setInternalState(target, "sender", sender);

        JSONObject result = Whitebox.invokeMethod(target, "makeTopology", nwId);

        assertThat(result.getJSONObject("nodes").getJSONObject("nodeId").get("node_id").toString(),
                is("nodeId"));

        assertThat(result.getJSONObject("nodes").getJSONObject("nodeId").getJSONObject("ports")
                .getJSONObject("portId").get("port_id").toString(),
                is("portId"));

        assertThat(result.getJSONObject("links").toString(),
                is("{}"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.rest.GUIRestApi#makeTopology()}
     */
    @Test
    public void testMakeTopologyWithEmptyNode() throws Exception {
        String linkId = "linkId";

        Map<String, String> attribute = new HashMap<>();
        attribute.put("testAttribute", "testAttributeParam");

        Map<String, Node> nodes = new HashMap<>();

        Link link = new Link(linkId, "srcNode", "srcPort", "dstNode", "dstPort");
        Map<String, Link> links = new HashMap<>();
        links.put(linkId, link);

        Topology topology = spy(new Topology("1", nodes, links));

        Response response = spy(new Response(Response.OK, topology));
        Value value = mock(Value.class);
        when(value.toString()).thenReturn("testValue");
        doReturn(value).when(response).getBodyValue();

        OdenOsSender sender = spy(OdenOsSender.getInstance());
        doReturn(response).when(sender).sendRequest(anyString(), (Method) anyObject(),
                anyString(), anyObject());
        Whitebox.setInternalState(target, "sender", sender);

        JSONObject result = Whitebox.invokeMethod(target, "makeTopology", nwId);

        assertThat(result.getJSONObject("nodes").toString(),
                is("{}"));

        assertThat(result.getJSONObject("links").getJSONObject("linkId").get("link_id").toString(),
                is("linkId"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.rest.GUIRestApi#makeTopology()}
     */
    @Test
    public void testMakeTopologyWithemptyPort() throws Exception {
        String nodeId = "nodeId";
        String linkId = "linkId";

        Map<String, String> attribute = new HashMap<>();
        attribute.put("testAttribute", "testAttributeParam");

        Map<String, Port> ports = new HashMap<>();

        Map<String, Node> nodes = new HashMap<>();
        Node node = new Node("0", nodeId, ports, attribute);
        nodes.put(nodeId, node);

        Link link = new Link(linkId, "srcNode", "srcPort", "dstNode", "dstPort");
        Map<String, Link> links = new HashMap<>();
        links.put(linkId, link);

        Topology topology = spy(new Topology("1", nodes, links));

        Response response = spy(new Response(Response.OK, topology));
        Value value = mock(Value.class);
        when(value.toString()).thenReturn("testValue");
        doReturn(value).when(response).getBodyValue();

        OdenOsSender sender = spy(OdenOsSender.getInstance());
        doReturn(response).when(sender).sendRequest(anyString(), (Method) anyObject(),
                anyString(), anyObject());
        Whitebox.setInternalState(target, "sender", sender);

        JSONObject result = Whitebox.invokeMethod(target, "makeTopology", nwId);

        assertThat(result.getJSONObject("nodes").getJSONObject("nodeId").get("node_id").toString(),
                is("nodeId"));

        assertThat(result.getJSONObject("nodes").getJSONObject("nodeId").getJSONObject("ports")
                .toString(),
                is("{}"));

        assertThat(result.getJSONObject("links").getJSONObject("linkId").get("link_id").toString(),
                is("linkId"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.rest.GUIRestApi#makeFlows()}
     */
    @Test
    public void testMakeFlows() throws Exception {
        String flowId = "flowId";

        List<BasicFlowMatch> matches = new ArrayList<>();
        matches.add(new BasicFlowMatch("inNode", "inPort"));

        List<String> path = new ArrayList<>();
        path.add("linkId");

        Map<String, List<FlowAction>> edgeActions = new HashMap<>();
        List<FlowAction> actions = new ArrayList<>();
        actions.add(new FlowActionOutput("outPort"));
        edgeActions.put("nodeId", actions);

        Map<String, String> attribute = new HashMap<>();
        attribute.put("testAttribute", "testAttributeParam");

        Flow flow = new BasicFlow("1", flowId, "ANY", true, "1",
                FlowObject.FlowStatus.ESTABLISHED.toString(), matches, path, edgeActions,
                attribute);

        Map<String, Flow> flows = new HashMap<>();
        flows.put(flowId, flow);

        Map<String, List<String>> priority = new HashMap<>();
        FlowSet flowSet = new FlowSet("1", priority, flows);

        Response response = spy(new Response(Response.OK, flowSet));
        Value value = mock(Value.class);
        when(value.toString()).thenReturn("testValue");
        doReturn(value).when(response).getBodyValue();

        OdenOsSender sender = spy(OdenOsSender.getInstance());
        doReturn(response).when(sender).sendRequest(anyString(), (Method) anyObject(),
                anyString(), anyObject());
        Whitebox.setInternalState(target, "sender", sender);

        JSONObject result = Whitebox.invokeMethod(target, "makeFlows", nwId);

        assertThat(result.getJSONObject("flows").getJSONObject("flowId").get("flow_id").toString(),
                is("flowId"));

        assertThat(result.getJSONObject("flows").getJSONObject("flowId").getJSONArray("matches")
                .getJSONObject(0).get("in_port").toString(),
                is("inPort"));

        assertThat(result.getJSONObject("flows").getJSONObject("flowId")
                .getJSONObject("edge_actions").getJSONArray("nodeId").getJSONObject(0)
                .get("output").toString(),
                is("outPort"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.rest.GUIRestApi#makeFlows()}
     */
    @Test
    public void testMakeFlowsWithEmptyFlow() throws Exception {
        Map<String, Flow> flows = new HashMap<>();

        Map<String, List<String>> priority = new HashMap<>();
        FlowSet flowSet = new FlowSet("1", priority, flows);

        Response response = spy(new Response(Response.OK, flowSet));
        Value value = mock(Value.class);
        when(value.toString()).thenReturn("testValue");
        doReturn(value).when(response).getBodyValue();

        OdenOsSender sender = spy(OdenOsSender.getInstance());
        doReturn(response).when(sender).sendRequest(anyString(), (Method) anyObject(),
                anyString(), anyObject());
        Whitebox.setInternalState(target, "sender", sender);

        JSONObject result = Whitebox.invokeMethod(target, "makeFlows", nwId);

        assertThat(result.getJSONObject("flows").toString(),
                is("{}"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.rest.GUIRestApi#makeFlows()}
     */
    @Test
    public void testMakeFlowsWithEmptyEdgeAction() throws Exception {
        String flowId = "flowId";

        List<BasicFlowMatch> matches = new ArrayList<>();
        matches.add(new BasicFlowMatch("inNode", "inPort"));

        List<String> path = new ArrayList<>();
        path.add("linkId");

        Map<String, List<FlowAction>> edgeActions = new HashMap<>();

        Map<String, String> attribute = new HashMap<>();
        attribute.put("testAttribute", "testAttributeParam");

        Flow flow = new BasicFlow("1", flowId, "ANY", true, "1",
                FlowObject.FlowStatus.ESTABLISHED.toString(), matches, path, edgeActions,
                attribute);

        Map<String, Flow> flows = new HashMap<>();
        flows.put(flowId, flow);

        Map<String, List<String>> priority = new HashMap<>();
        FlowSet flowSet = new FlowSet("1", priority, flows);

        Response response = spy(new Response(Response.OK, flowSet));
        Value value = mock(Value.class);
        when(value.toString()).thenReturn("testValue");
        doReturn(value).when(response).getBodyValue();

        OdenOsSender sender = spy(OdenOsSender.getInstance());
        doReturn(response).when(sender).sendRequest(anyString(), (Method) anyObject(),
                anyString(), anyObject());
        Whitebox.setInternalState(target, "sender", sender);

        JSONObject result = Whitebox.invokeMethod(target, "makeFlows", nwId);

        assertThat(result.getJSONObject("flows").getJSONObject("flowId").get("flow_id").toString(),
                is("flowId"));

        assertThat(result.getJSONObject("flows").getJSONObject("flowId").getJSONArray("matches")
                .getJSONObject(0).get("in_port").toString(),
                is("inPort"));

        assertThat(result.getJSONObject("flows").getJSONObject("flowId")
                .getJSONObject("edge_actions").toString(),
                is("{}"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.rest.GUIRestApi#makeFlows()}
     */
    @Test
    public void testMakeFlowsWithEmptyFlowActionOutput() throws Exception {
        String flowId = "flowId";

        List<BasicFlowMatch> matches = new ArrayList<>();
        matches.add(new BasicFlowMatch("inNode", "inPort"));

        List<String> path = new ArrayList<>();
        path.add("linkId");

        Map<String, List<FlowAction>> edgeActions = new HashMap<>();
        List<FlowAction> actions = new ArrayList<>();
        edgeActions.put("nodeId", actions);

        Map<String, String> attribute = new HashMap<>();
        attribute.put("testAttribute", "testAttributeParam");

        Flow flow = new BasicFlow("1", flowId, "ANY", true, "1",
                FlowObject.FlowStatus.ESTABLISHED.toString(), matches, path, edgeActions,
                attribute);

        Map<String, Flow> flows = new HashMap<>();
        flows.put(flowId, flow);

        Map<String, List<String>> priority = new HashMap<>();
        FlowSet flowSet = new FlowSet("1", priority, flows);

        Response response = spy(new Response(Response.OK, flowSet));
        Value value = mock(Value.class);
        when(value.toString()).thenReturn("testValue");
        doReturn(value).when(response).getBodyValue();

        OdenOsSender sender = spy(OdenOsSender.getInstance());
        doReturn(response).when(sender).sendRequest(anyString(), (Method) anyObject(),
                anyString(), anyObject());
        Whitebox.setInternalState(target, "sender", sender);

        JSONObject result = Whitebox.invokeMethod(target, "makeFlows", nwId);

        assertThat(result.getJSONObject("flows").getJSONObject("flowId").get("flow_id").toString(),
                is("flowId"));

        assertThat(result.getJSONObject("flows").getJSONObject("flowId").getJSONArray("matches")
                .getJSONObject(0).get("in_port").toString(),
                is("inPort"));

        assertThat(result.getJSONObject("flows").getJSONObject("flowId")
                .getJSONObject("edge_actions").getJSONArray("nodeId").toString(),
                is("[]"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.rest.GUIRestApi#makeFlows()}
     */
    @Test
    public void testMakeFlowsWithEmptyMatch() throws Exception {
        String flowId = "flowId";

        List<BasicFlowMatch> matches = new ArrayList<>();

        List<String> path = new ArrayList<>();
        path.add("linkId");

        Map<String, List<FlowAction>> edgeActions = new HashMap<>();
        List<FlowAction> actions = new ArrayList<>();
        actions.add(new FlowActionOutput("outPort"));
        edgeActions.put("nodeId", actions);

        Map<String, String> attribute = new HashMap<>();
        attribute.put("testAttribute", "testAttributeParam");

        Flow flow = new BasicFlow("1", flowId, "ANY", true, "1",
                FlowObject.FlowStatus.ESTABLISHED.toString(), matches, path, edgeActions,
                attribute);

        Map<String, Flow> flows = new HashMap<>();
        flows.put(flowId, flow);

        Map<String, List<String>> priority = new HashMap<>();
        FlowSet flowSet = new FlowSet("1", priority, flows);

        Response response = spy(new Response(Response.OK, flowSet));
        Value value = mock(Value.class);
        when(value.toString()).thenReturn("testValue");
        doReturn(value).when(response).getBodyValue();

        OdenOsSender sender = spy(OdenOsSender.getInstance());
        doReturn(response).when(sender).sendRequest(anyString(), (Method) anyObject(),
                anyString(), anyObject());
        Whitebox.setInternalState(target, "sender", sender);

        JSONObject result = Whitebox.invokeMethod(target, "makeFlows", nwId);

        assertThat(result.getJSONObject("flows").getJSONObject("flowId").get("flow_id").toString(),
                is("flowId"));

        assertThat(result.getJSONObject("flows").getJSONObject("flowId").getJSONArray("matches")
                .toString(),
                is("[]"));

        assertThat(result.getJSONObject("flows").getJSONObject("flowId")
                .getJSONObject("edge_actions").getJSONArray("nodeId").getJSONObject(0)
                .get("output").toString(),
                is("outPort"));
    }

}
