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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.o3project.ocnrm.lib.table.TableManager;
import org.o3project.odenos.core.component.ConversionTable;
import org.o3project.odenos.core.component.Logic;
import org.o3project.odenos.core.component.Logic.AttrElements;
import org.o3project.odenos.core.component.NetworkInterface;
import org.o3project.odenos.core.component.SystemManagerInterface;
import org.o3project.odenos.core.component.network.flow.Flow;
import org.o3project.odenos.core.component.network.flow.FlowObject.FlowStatus;
import org.o3project.odenos.core.component.network.flow.FlowObject.FlowType;
import org.o3project.odenos.core.component.network.flow.FlowSet;
import org.o3project.odenos.core.component.network.flow.basic.BasicFlow;
import org.o3project.odenos.core.component.network.flow.basic.BasicFlowMatch;
import org.o3project.odenos.core.component.network.flow.basic.FlowAction;
import org.o3project.odenos.core.component.network.flow.basic.FlowActionOutput;
import org.o3project.odenos.core.component.network.topology.Link;
import org.o3project.odenos.core.component.network.topology.Node;
import org.o3project.odenos.core.component.network.topology.Port;
import org.o3project.odenos.core.component.network.topology.Topology;
import org.o3project.odenos.core.manager.system.ComponentConnection;
import org.o3project.odenos.core.manager.system.ComponentConnectionLogicAndNetwork;
import org.o3project.odenos.core.manager.system.event.ComponentConnectionChanged;
import org.o3project.odenos.core.util.PathCalculator;
import org.o3project.odenos.remoteobject.ObjectProperty;
import org.o3project.odenos.remoteobject.message.Request;
import org.o3project.odenos.remoteobject.message.Request.Method;
import org.o3project.odenos.remoteobject.message.Response;
import org.o3project.odenos.remoteobject.messagingclient.MessageDispatcher;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.powermock.reflect.internal.WhiteboxImpl;

import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ LinkLayerizer.class, ConversionTable.class, PathCalculator.class,
        ComponentConnection.class, ComponentConnectionLogicAndNetwork.class,
        SystemManagerInterface.class, NetworkInterface.class, Link.class })
public class LinkLayerizerTest {

    private LinkLayerizer target;
    private MessageDispatcher dispatcher;
    private ConversionTable conversionTable;
    private SystemManagerInterface systemInterface;
    private Map<String, NetworkInterface> networkInterfaces =
            new HashMap<String, NetworkInterface>();

    private static final String LAYERIZER_ID = "layerizer";

    private static final String METHOD_CONVERSITON_TABLE = "conversionTable";
    private static final String METHOD_NETWOEK_IF = "networkInterfaces";
    private static final String METHOD_SYSTEM_MNG_IF = "systemMngInterface";

    private static final String TYPE_LOWER = "lower";
    private static final String TYPE_UPPER = "upper";
    private static final String TYPE_LAYERIZER = "layerized";

    private static final String NETWORK_ID_LOWER = "networkcomponent0";
    private static final String NETWORK_ID_UPPER = "networkcomponent1";
    private static final String NETWORK_ID_LAYERIZER = "networkcomponent2";

    private Topology targetTopo;
    private Map<String, Port> ports1;
    private Map<String, Port> ports2;
    private Map<String, Node> nodes;
    private Map<String, Link> links;
    private Port port1;
    private Port port2;
    private Port port3;
    private Port port4;
    private Node node1;
    private Node node2;
    private Link link1;
    private Link link2;

    private FlowSet flowSet;
    private Flow flow1;
    private Flow flow2;

    private Map<String, List<FlowAction>> edgeActions;

    private String boundary1 = "{\"boundary_id\": \"boundary1\","
            + "\"lower_nw\": \"networkcomponent0\",\"lower_nw_node\": \"NW=SDN,NE=PT1\","
            + "\"lower_nw_port\": \"NW=SDN,NE=PT1,Layer=Ether,TTP=1\","
            + "\"upper_nw\": \"networkcomponent1\",\"upper_nw_node\": \"NW=SDN,NE=PT11\","
            + "\"upper_nw_port\": \"NW=SDN,NE=PT11,Layer=Ether,TTP=1\"}";
    private String boundary2 = "{\"boundary_id\": \"boundary2\","
            + "\"lower_nw\": \"networkcomponent0\",\"lower_nw_node\": \"NW=SDN,NE=PT1\","
            + "\"lower_nw_port\": \"NW=SDN,NE=PT1,Layer=Ether,TTP=2\","
            + "\"upper_nw\": \"networkcomponent1\",\"upper_nw_node\": \"NW=SDN,NE=PT11\","
            + "\"upper_nw_port\": \"NW=SDN,NE=PT11,Layer=Ether,TTP=2\"}";
    private String boundary3 = "{\"boundary_id\": \"boundary3\","
            + "\"lower_nw\": \"networkcomponent0\",\"lower_nw_node\": \"NW=SDN,NE=PT2\","
            + "\"lower_nw_port\": \"NW=SDN,NE=PT2,Layer=Ether,TTP=1\","
            + "\"upper_nw\": \"networkcomponent1\",\"upper_nw_node\": \"NW=SDN,NE=PT21\","
            + "\"upper_nw_port\": \"NW=SDN,NE=PT21,Layer=Ether,TTP=1\"}";
    private String boundary4 = "{\"boundary_id\": \"boundary4\","
            + "\"lower_nw\": \"networkcomponent0\",\"lower_nw_node\": \"NW=SDN,NE=PT2\","
            + "\"lower_nw_port\": \"NW=SDN,NE=PT2,Layer=Ether,TTP=2\","
            + "\"upper_nw\": \"networkcomponent1\",\"upper_nw_node\": \"NW=SDN,NE=PT21\","
            + "\"upper_nw_port\": \"NW=SDN,NE=PT21,Layer=Ether,TTP=2\"}";
    private String boundary5 = "{\"boundary_id\": \"boundary5\","
            + "\"lower_nw\": \"networkcomponent0\",\"lower_nw_node\": \"NW=SDN,NE=PT11\","
            + "\"lower_nw_port\": \"NW=SDN,NE=PT11,Layer=Ether,TTP=1\","
            + "\"upper_nw\": \"networkcomponent1\",\"upper_nw_node\": \"NW=SDN,NE=PT1\","
            + "\"upper_nw_port\": \"NW=SDN,NE=PT1,Layer=Ether,TTP=1\"}";
    private String boundary6 = "{\"boundary_id\": \"boundary6\","
            + "\"lower_nw\": \"networkcomponent0\",\"lower_nw_node\": \"NW=SDN,NE=PT11\","
            + "\"lower_nw_port\": \"NW=SDN,NE=PT11,Layer=Ether,TTP=2\","
            + "\"upper_nw\": \"networkcomponent1\",\"upper_nw_node\": \"NW=SDN,NE=PT1\","
            + "\"upper_nw_port\": \"NW=SDN,NE=PT1,Layer=Ether,TTP=2\"}";
    private String boundary7 = "{\"boundary_id\": \"boundary7\","
            + "\"lower_nw\": \"networkcomponent0\",\"lower_nw_node\": \"NW=SDN,NE=PT21\","
            + "\"lower_nw_port\": \"NW=SDN,NE=PT21,Layer=Ether,TTP=1\","
            + "\"upper_nw\": \"networkcomponent1\",\"upper_nw_node\": \"NW=SDN,NE=PT2\","
            + "\"upper_nw_port\": \"NW=SDN,NE=PT2,Layer=Ether,TTP=1\"}";
    private String boundary8 = "{\"boundary_id\": \"boundary8\","
            + "\"lower_nw\": \"networkcomponent0\",\"lower_nw_node\": \"NW=SDN,NE=PT21\","
            + "\"lower_nw_port\": \"NW=SDN,NE=PT21,Layer=Ether,TTP=2\","
            + "\"upper_nw\": \"networkcomponent1\",\"upper_nw_node\": \"NW=SDN,NE=PT2\","
            + "\"upper_nw_port\": \"NW=SDN,NE=PT2,Layer=Ether,TTP=2\"}";

    private static final Response DEFAULT_RESPONSE = new Response(Response.OK, new String("OK"));

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {

        dispatcher = Mockito.mock(MessageDispatcher.class);
        target = PowerMockito.spy(new LinkLayerizer("objectId", dispatcher) {
            @Override
            protected String getSuperType() {
                return "SuperType";
            }

            @Override
            protected String getDescription() {
                return "Description";
            }
        });

        conversionTable = PowerMockito.spy(new ConversionTable());
        conversionTable.addEntryConnectionType(NETWORK_ID_LOWER, TYPE_LOWER);
        conversionTable.addEntryConnectionType(NETWORK_ID_UPPER, TYPE_UPPER);
        conversionTable.addEntryConnectionType(NETWORK_ID_LAYERIZER, TYPE_LAYERIZER);

        systemInterface = PowerMockito.spy(new SystemManagerInterface(dispatcher));

        PowerMockito.doReturn(DEFAULT_RESPONSE).when(systemInterface,
                "putConnection", anyObject());

        NetworkInterface lowerNwif = PowerMockito.spy(new NetworkInterface(
                dispatcher, NETWORK_ID_LOWER));
        NetworkInterface upperNwif = PowerMockito.spy(new NetworkInterface(
                dispatcher, NETWORK_ID_UPPER));
        NetworkInterface layerizerNwif = PowerMockito.spy(new NetworkInterface(
                dispatcher, NETWORK_ID_LAYERIZER));
        networkInterfaces.put(NETWORK_ID_LOWER, lowerNwif);
        networkInterfaces.put(NETWORK_ID_UPPER, upperNwif);
        networkInterfaces.put(NETWORK_ID_LAYERIZER, layerizerNwif);

        targetTopo = new Topology();
        ports1 = new HashMap<String, Port>();
        ports2 = new HashMap<String, Port>();
        nodes = new HashMap<String, Node>();
        links = new HashMap<String, Link>();
        port1 = new Port("401", "NW=SDN,NE=PT1,Layer=Ether,TTP=1", "NW=SDN,NE=PT1", "link1_id123",
                "", new HashMap<String, String>());
        port2 = new Port("402", "NW=SDN,NE=PT1,Layer=Ether,TTP=2", "NW=SDN,NE=PT1", "",
                "link2_id123", new HashMap<String, String>());
        port3 = new Port("403", "NW=SDN,NE=PT2,Layer=Ether,TTP=1", "NW=SDN,NE=PT2", "",
                "link1_id123", new HashMap<String, String>());
        port4 = new Port("404", "NW=SDN,NE=PT2,Layer=Ether,TTP=2", "NW=SDN,NE=PT2", "link2_id123",
                "", new HashMap<String, String>());

        ports1.put("NW=SDN,NE=PT1,Layer=Ether,TTP=1", port1);
        ports1.put("NW=SDN,NE=PT1,Layer=Ether,TTP=2", port2);
        ports2.put("NW=SDN,NE=PT2,Layer=Ether,TTP=1", port3);
        ports2.put("NW=SDN,NE=PT2,Layer=Ether,TTP=2", port4);
        node1 = new Node("301", "NW=SDN,NE=PT1", ports1, new HashMap<String, String>());
        node2 = new Node("302", "NW=SDN,NE=PT2", ports2, new HashMap<String, String>());
        nodes.put("NW=SDN,NE=PT1", node1);
        nodes.put("NW=SDN,NE=PT2", node2);
        link1 = new Link("201", "link1_id123", "NW=SDN,NE=PT1", "NW=SDN,NE=PT1,Layer=Ether,TTP=1",
                "NW=SDN,NE=PT2", "NW=SDN,NE=PT2,Layer=Ether,TTP=1", new HashMap<String, String>());
        link2 = new Link("202", "link2_id123", "NW=SDN,NE=PT2", "NW=SDN,NE=PT2,Layer=Ether,TTP=2",
                "NW=SDN,NE=PT1", "NW=SDN,NE=PT1,Layer=Ether,TTP=2", new HashMap<String, String>());

        Map<String, String> linkAttributes = link1.getAttributes();
        linkAttributes.put(AttrElements.OPER_STATUS, LinkLayerizer.STATUS_UP);
        linkAttributes.put(AttrElements.ESTABLISHMENT_STATUS,
                LinkLayerizer.LINK_STATUS_ESTABLISHED);
        linkAttributes.put(AttrElements.LATENCY, linkAttributes.get(AttrElements.REQ_LATENCY));
        linkAttributes.put(AttrElements.MAX_BANDWIDTH,
                linkAttributes.get(AttrElements.REQ_BANDWIDTH));
        linkAttributes.put(
                AttrElements.UNRESERVED_BANDWIDTH, LinkLayerizer.LINK_DEFAULT_UNRESERVED_BANDWIDTH);
        linkAttributes.put(TableManager.TRANSACTION_ID, "999");
        link1.putAttributes(linkAttributes);

        links.put("link1_id123", link1);
        links.put("link2_id123", link2);

        targetTopo.nodes = nodes;
        targetTopo.links = links;

        flowSet = new FlowSet();
        flow1 = flowSet.createFlow(FlowType.BASIC_FLOW, Flow.DEFAULT_PRIORITY);
        flow2 = flowSet.createFlow(FlowType.BASIC_FLOW, Flow.DEFAULT_PRIORITY);

        edgeActions = new HashMap<String, List<FlowAction>>();
        List<FlowAction> actions = new ArrayList<FlowAction>() {
            {
                add(new FlowActionOutput() {
                    {
                        output = "NW=SDN,NE=PT1,Layer=Ether,TTP=1";
                    }
                });
                add(new FlowActionOutput() {
                    {
                        output = "NW=SDN,NE=PT1,Layer=Ether,TTP=2";
                    }
                });
            }
        };

        edgeActions.put("NW=SDN,NE=PT1", actions);
        ((BasicFlow) flow1).putEdgeActions(edgeActions);

        List<BasicFlowMatch> matches = new ArrayList<BasicFlowMatch>();
        BasicFlowMatch match1 = new BasicFlowMatch("NW=SDN,NE=PT2",
                "NW=SDN,NE=PT2,Layer=Ether,TTP=1");
        BasicFlowMatch match2 = new BasicFlowMatch("NW=SDN,NE=PT2",
                "NW=SDN,NE=PT2,Layer=Ether,TTP=2");
        matches.add(match1);
        matches.add(match2);

        ((BasicFlow) flow1).putMatches(matches);

        flowSet.flows.put(flow1.getFlowId(), flow1);
    }

    @After
    public void tearDown() throws Exception {
        target = null;
        dispatcher = null;
    }

    @Test
    public void testLinkLayerizer() throws Exception {

        String objId = LAYERIZER_ID;

        target = PowerMockito.spy(new LinkLayerizer(objId, dispatcher));
        assertThat(target.getObjectId(), is(objId));
        assertThat(target, is(instanceOf(LinkLayerizer.class)));

    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#getSuperType()}
     *
     * @throws Exception
     */
    @Test
    public final void testGetSuperType() throws Exception {
        assertThat(target.getSuperType(), is("SuperType"));
    }

    @Test
    public void testGetDescription() throws Exception {
        assertThat(target.getDescription(), is("Description"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onConnectionChangedAddedPre()}
     * @throws Exception
     */
    @Test
    public void testOnConnectionChangedAddedPreSuccessLower() throws Exception {

        ComponentConnection prev = new ComponentConnectionLogicAndNetwork(
                LAYERIZER_ID + "->" + NETWORK_ID_LOWER,
                TYPE_LOWER,
                "initializing",
                LAYERIZER_ID,
                NETWORK_ID_LOWER);

        ComponentConnection curr = new ComponentConnectionLogicAndNetwork(
                LAYERIZER_ID + "->" + NETWORK_ID_LOWER,
                TYPE_LOWER,
                "running",
                LAYERIZER_ID,
                NETWORK_ID_LOWER);

        ComponentConnectionChanged msg = new ComponentConnectionChanged("add", prev, curr);
        PowerMockito.doReturn(systemInterface).when(target, METHOD_SYSTEM_MNG_IF);

        when(target.getObjectId()).thenReturn(LAYERIZER_ID);

        assertThat(target.onConnectionChangedAddedPre(msg), is(true));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onConnectionChangedAddedPre()}
     * @throws Exception
     */
    @Test
    public void testOnConnectionChangedAddedPreSuccessUpper() throws Exception {

        ComponentConnection prev = new ComponentConnectionLogicAndNetwork(
                LAYERIZER_ID + "->" + NETWORK_ID_UPPER,
                TYPE_UPPER,
                "initializing",
                LAYERIZER_ID,
                NETWORK_ID_UPPER);

        ComponentConnection curr = new ComponentConnectionLogicAndNetwork(
                LAYERIZER_ID + "->" + NETWORK_ID_UPPER,
                TYPE_UPPER,
                "running",
                LAYERIZER_ID,
                NETWORK_ID_UPPER);

        ComponentConnectionChanged msg = new ComponentConnectionChanged("add", prev, curr);
        PowerMockito.doReturn(systemInterface).when(target, METHOD_SYSTEM_MNG_IF);

        when(target.getObjectId()).thenReturn(LAYERIZER_ID);

        assertThat(target.onConnectionChangedAddedPre(msg), is(true));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onConnectionChangedAddedPre()}
     * @throws Exception
     */
    @Test
    public void testOnConnectionChangedAddedPreSuccessLayerizer() throws Exception {

        ComponentConnection prev = new ComponentConnectionLogicAndNetwork(
                LAYERIZER_ID + "->" + NETWORK_ID_LAYERIZER,
                TYPE_LAYERIZER,
                "initializing",
                LAYERIZER_ID,
                NETWORK_ID_LAYERIZER);

        ComponentConnection curr = new ComponentConnectionLogicAndNetwork(
                LAYERIZER_ID + "->" + NETWORK_ID_LAYERIZER,
                TYPE_LAYERIZER,
                "running",
                LAYERIZER_ID,
                NETWORK_ID_LAYERIZER);

        ComponentConnectionChanged msg = new ComponentConnectionChanged("add", prev, curr);
        PowerMockito.doReturn(systemInterface).when(target, METHOD_SYSTEM_MNG_IF);

        when(target.getObjectId()).thenReturn(LAYERIZER_ID);

        assertThat(target.onConnectionChangedAddedPre(msg), is(true));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onConnectionChangedAddedPre()}
     */
    @Test
    public void testOnConnectionChangedAddedPreWithCurrNullError() {

        ComponentConnection prev = new ComponentConnectionLogicAndNetwork(
                LAYERIZER_ID + "->" + NETWORK_ID_LOWER,
                TYPE_LOWER,
                "initializing",
                LAYERIZER_ID,
                NETWORK_ID_LOWER);

        ComponentConnection curr = null;
        ComponentConnectionChanged msg = new ComponentConnectionChanged("add", prev, curr);

        when(target.getObjectId()).thenReturn(LAYERIZER_ID);

        assertThat(target.onConnectionChangedAddedPre(msg), is(false));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onConnectionChangedAddedPre()}
     */
    @Test
    public void testOnConnectionChangedAddedPreWithObjectTypeError() {

        ComponentConnection prev = new ComponentConnectionLogicAndNetwork(
                LAYERIZER_ID + "->" + NETWORK_ID_LOWER,
                TYPE_LOWER,
                "initializing",
                LAYERIZER_ID,
                NETWORK_ID_LOWER);

        ComponentConnection curr = PowerMockito.spy(new ComponentConnectionLogicAndNetwork(
                LAYERIZER_ID + "->" + NETWORK_ID_LOWER,
                TYPE_LOWER,
                "running",
                LAYERIZER_ID,
                NETWORK_ID_LOWER));

        when(curr.getObjectType()).thenReturn("UndefinedObjectType");
        when(target.getObjectId()).thenReturn(LAYERIZER_ID);

        ComponentConnectionChanged msg = new ComponentConnectionChanged("add", prev, curr);

        assertThat(target.onConnectionChangedAddedPre(msg), is(false));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onConnectionChangedAddedPre()}
     */
    @Test
    public void testOnConnectionChangedAddedPreWithLogicIdError() {

        ComponentConnection prev = new ComponentConnectionLogicAndNetwork(
                LAYERIZER_ID + "->" + NETWORK_ID_LOWER,
                TYPE_LOWER,
                "initializing",
                LAYERIZER_ID,
                NETWORK_ID_LOWER);

        ComponentConnection curr = new ComponentConnectionLogicAndNetwork(
                LAYERIZER_ID + "->" + NETWORK_ID_LOWER,
                TYPE_LOWER,
                "running",
                LAYERIZER_ID,
                NETWORK_ID_LOWER);

        when(target.getObjectId()).thenReturn("UndefinedLogicId");

        ComponentConnectionChanged msg = new ComponentConnectionChanged("add", prev, curr);

        assertThat(target.onConnectionChangedAddedPre(msg), is(false));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onConnectionChangedAddedPre()}
     * @throws Exception
     */
    @Test
    public void testOnConnectionChangedAddedPreUndefinedTypeError() throws Exception {

        ComponentConnection prev = new ComponentConnectionLogicAndNetwork(
                LAYERIZER_ID + "->" + NETWORK_ID_LOWER,
                "Sliver",
                "initializing",
                LAYERIZER_ID,
                NETWORK_ID_LOWER);

        ComponentConnection curr = new ComponentConnectionLogicAndNetwork(
                LAYERIZER_ID + "->" + NETWORK_ID_LOWER,
                "Sliver",
                "running",
                LAYERIZER_ID,
                NETWORK_ID_LOWER);

        ComponentConnectionChanged msg = new ComponentConnectionChanged("add", prev, curr);

        when(target.getObjectId()).thenReturn(LAYERIZER_ID);
        PowerMockito.doReturn(DEFAULT_RESPONSE).when(systemInterface)
                .putConnection((ComponentConnection) anyObject());
        PowerMockito.doReturn(systemInterface).when(target, METHOD_SYSTEM_MNG_IF);

        assertThat(target.onConnectionChangedAddedPre(msg), is(false));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onConnectionChangedAddedPre()}
     * @throws Exception
     */
    @Test
    public void testOnConnectionChangedAddedPreLowerTypeError() throws Exception {

        ComponentConnection prev = new ComponentConnectionLogicAndNetwork(
                LAYERIZER_ID + "->" + NETWORK_ID_LOWER,
                TYPE_LOWER,
                "initializing",
                LAYERIZER_ID,
                NETWORK_ID_LOWER);

        ComponentConnection curr = new ComponentConnectionLogicAndNetwork(
                LAYERIZER_ID + "->" + NETWORK_ID_LOWER,
                TYPE_LOWER,
                "running",
                LAYERIZER_ID,
                NETWORK_ID_LOWER);

        ComponentConnectionChanged msg = new ComponentConnectionChanged("add", prev, curr);

        when(target.getObjectId()).thenReturn(LAYERIZER_ID);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);
        PowerMockito.doReturn(DEFAULT_RESPONSE).when(systemInterface)
                .putConnection((ComponentConnection) anyObject());
        PowerMockito.doReturn(systemInterface).when(target, METHOD_SYSTEM_MNG_IF);

        assertThat(target.onConnectionChangedAddedPre(msg), is(false));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onConnectionChangedAddedPre()}
     * @throws Exception
     */
    @Test
    public void testOnConnectionChangedAddedPreUpperTypeError() throws Exception {

        ComponentConnection prev = new ComponentConnectionLogicAndNetwork(
                LAYERIZER_ID + "->" + NETWORK_ID_UPPER,
                TYPE_UPPER,
                "initializing",
                LAYERIZER_ID,
                NETWORK_ID_UPPER);

        ComponentConnection curr = new ComponentConnectionLogicAndNetwork(
                LAYERIZER_ID + "->" + NETWORK_ID_UPPER,
                TYPE_UPPER,
                "running",
                LAYERIZER_ID,
                NETWORK_ID_UPPER);

        ComponentConnectionChanged msg = new ComponentConnectionChanged("add", prev, curr);

        when(target.getObjectId()).thenReturn(LAYERIZER_ID);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);
        PowerMockito.doReturn(DEFAULT_RESPONSE).when(systemInterface)
                .putConnection((ComponentConnection) anyObject());
        PowerMockito.doReturn(systemInterface).when(target, METHOD_SYSTEM_MNG_IF);
        assertThat(target.onConnectionChangedAddedPre(msg), is(false));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onConnectionChangedAddedPre()}
     * @throws Exception
     */
    @Test
    public void testOnConnectionChangedAddedPreLayerizerTypeError() throws Exception {

        ComponentConnection prev = new ComponentConnectionLogicAndNetwork(
                LAYERIZER_ID + "->" + NETWORK_ID_LAYERIZER,
                TYPE_LAYERIZER,
                "initializing",
                LAYERIZER_ID,
                NETWORK_ID_LAYERIZER);

        ComponentConnection curr = new ComponentConnectionLogicAndNetwork(
                LAYERIZER_ID + "->" + NETWORK_ID_LAYERIZER,
                TYPE_LAYERIZER,
                "running",
                LAYERIZER_ID,
                NETWORK_ID_LAYERIZER);

        ComponentConnectionChanged msg = new ComponentConnectionChanged("add", prev, curr);

        when(target.getObjectId()).thenReturn(LAYERIZER_ID);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);
        PowerMockito.doReturn(DEFAULT_RESPONSE).when(systemInterface)
                .putConnection((ComponentConnection) anyObject());
        PowerMockito.doReturn(systemInterface).when(target, METHOD_SYSTEM_MNG_IF);

        assertThat(target.onConnectionChangedAddedPre(msg), is(false));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onConnectionChangedUpdatePre()}
     */
    @Test
    public void testOnConnectionChangedUpdatePreSuccessLower() {

        ComponentConnection prev = new ComponentConnectionLogicAndNetwork(
                LAYERIZER_ID + "->" + NETWORK_ID_LOWER,
                TYPE_LOWER,
                "initializing",
                LAYERIZER_ID,
                NETWORK_ID_LOWER);

        ComponentConnection curr = new ComponentConnectionLogicAndNetwork(
                LAYERIZER_ID + "->" + NETWORK_ID_LOWER,
                TYPE_LOWER,
                "running",
                LAYERIZER_ID,
                NETWORK_ID_LOWER);

        ComponentConnectionChanged msg = new ComponentConnectionChanged("update", prev, curr);

        when(target.getObjectId()).thenReturn(LAYERIZER_ID);

        assertThat(target.onConnectionChangedUpdatePre(msg), is(true));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onConnectionChangedUpdatePre()}
     */
    @Test
    public void testOnConnectionChangedUpdatePreSuccessUpper() {

        ComponentConnection prev = new ComponentConnectionLogicAndNetwork(
                LAYERIZER_ID + "->" + NETWORK_ID_UPPER,
                TYPE_UPPER,
                "initializing",
                LAYERIZER_ID,
                NETWORK_ID_UPPER);

        ComponentConnection curr = new ComponentConnectionLogicAndNetwork(
                LAYERIZER_ID + "->" + NETWORK_ID_UPPER,
                TYPE_UPPER,
                "running",
                LAYERIZER_ID,
                NETWORK_ID_UPPER);

        ComponentConnectionChanged msg = new ComponentConnectionChanged("update", prev, curr);

        when(target.getObjectId()).thenReturn(LAYERIZER_ID);

        assertThat(target.onConnectionChangedUpdatePre(msg), is(true));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onConnectionChangedUpdatePre()}
     */
    @Test
    public void testOnConnectionChangedUpdatePreSuccessLayerizer() {

        ComponentConnection prev = new ComponentConnectionLogicAndNetwork(
                LAYERIZER_ID + "->" + NETWORK_ID_LAYERIZER,
                TYPE_LAYERIZER,
                "initializing",
                LAYERIZER_ID,
                NETWORK_ID_LAYERIZER);

        ComponentConnection curr = new ComponentConnectionLogicAndNetwork(
                LAYERIZER_ID + "->" + NETWORK_ID_LAYERIZER,
                TYPE_LAYERIZER,
                "running",
                LAYERIZER_ID,
                NETWORK_ID_LAYERIZER);

        ComponentConnectionChanged msg = new ComponentConnectionChanged("update", prev, curr);

        when(target.getObjectId()).thenReturn(LAYERIZER_ID);

        assertThat(target.onConnectionChangedUpdatePre(msg), is(true));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onConnectionChangedUpdatePre()}
     */
    @Test
    public void testOnConnectionChangedUpdatePreWithObjectTypeError() {

        ComponentConnection prev = new ComponentConnectionLogicAndNetwork(
                LAYERIZER_ID + "->" + NETWORK_ID_LAYERIZER,
                TYPE_LAYERIZER,
                "initializing",
                LAYERIZER_ID,
                NETWORK_ID_LAYERIZER);

        ComponentConnection curr = PowerMockito.spy(new ComponentConnectionLogicAndNetwork(
                LAYERIZER_ID + "->" + NETWORK_ID_LAYERIZER,
                TYPE_LAYERIZER,
                "running",
                LAYERIZER_ID,
                NETWORK_ID_LAYERIZER));

        when(curr.getObjectType()).thenReturn("UndefinedObjectType");

        ComponentConnectionChanged msg = new ComponentConnectionChanged("update", prev, curr);

        when(target.getObjectId()).thenReturn(LAYERIZER_ID);

        assertThat(target.onConnectionChangedUpdatePre(msg), is(false));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onConnectionChangedUpdatePre()}
     */
    @Test
    public void testOnConnectionChangedUpdatePreWithLogicIdError() {

        ComponentConnection prev = new ComponentConnectionLogicAndNetwork(
                LAYERIZER_ID + "->" + NETWORK_ID_LAYERIZER,
                TYPE_LAYERIZER,
                "initializing",
                LAYERIZER_ID,
                NETWORK_ID_LAYERIZER);

        ComponentConnection curr = new ComponentConnectionLogicAndNetwork(
                LAYERIZER_ID + "->" + NETWORK_ID_LAYERIZER,
                TYPE_LAYERIZER,
                "running",
                LAYERIZER_ID,
                NETWORK_ID_LAYERIZER);

        ComponentConnectionChanged msg = new ComponentConnectionChanged("update", prev, curr);

        when(target.getObjectId()).thenReturn("slicer_1");

        assertThat(target.onConnectionChangedUpdatePre(msg), is(false));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onConnectionChangedDeletePre()}
     */
    @Test
    public void testOnConnectionChangedDeletePreSuccessLower() {

        ComponentConnection prev = new ComponentConnectionLogicAndNetwork(
                LAYERIZER_ID + "->" + NETWORK_ID_LOWER,
                TYPE_LOWER,
                "initializing",
                LAYERIZER_ID,
                NETWORK_ID_LOWER);

        ComponentConnection curr = new ComponentConnectionLogicAndNetwork(
                LAYERIZER_ID + "->" + NETWORK_ID_LOWER,
                TYPE_LOWER,
                "running",
                LAYERIZER_ID,
                NETWORK_ID_LOWER);

        ComponentConnectionChanged msg = new ComponentConnectionChanged("delete", prev, curr);

        when(target.getObjectId()).thenReturn(LAYERIZER_ID);

        assertThat(target.onConnectionChangedDeletePre(msg), is(true));

    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onConnectionChangedDeletePre()}
     */
    @Test
    public void testOnConnectionChangedDeletePreSuccessUpper() {

        ComponentConnection prev = new ComponentConnectionLogicAndNetwork(
                LAYERIZER_ID + "->" + NETWORK_ID_UPPER,
                TYPE_UPPER,
                "initializing",
                LAYERIZER_ID,
                NETWORK_ID_UPPER);

        ComponentConnection curr = new ComponentConnectionLogicAndNetwork(
                LAYERIZER_ID + "->" + NETWORK_ID_UPPER,
                TYPE_UPPER,
                "running",
                LAYERIZER_ID,
                NETWORK_ID_UPPER);

        ComponentConnectionChanged msg = new ComponentConnectionChanged("delete", prev, curr);

        when(target.getObjectId()).thenReturn(LAYERIZER_ID);

        assertThat(target.onConnectionChangedDeletePre(msg), is(true));

    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onConnectionChangedDeletePre()}
     */
    @Test
    public void testOnConnectionChangedDeletePreSuccessLayerizer() {

        ComponentConnection prev = new ComponentConnectionLogicAndNetwork(
                LAYERIZER_ID + "->" + NETWORK_ID_LAYERIZER,
                TYPE_LAYERIZER,
                "initializing",
                LAYERIZER_ID,
                NETWORK_ID_LAYERIZER);

        ComponentConnection curr = new ComponentConnectionLogicAndNetwork(
                LAYERIZER_ID + "->" + NETWORK_ID_LAYERIZER,
                TYPE_LAYERIZER,
                "running",
                LAYERIZER_ID,
                NETWORK_ID_LAYERIZER);

        ComponentConnectionChanged msg = new ComponentConnectionChanged("delete", prev, curr);

        when(target.getObjectId()).thenReturn(LAYERIZER_ID);

        assertThat(target.onConnectionChangedDeletePre(msg), is(true));

    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onConnectionChangedDeletePre()}
     */
    @Test
    public void testOnConnectionChangedDeletePreWithPrevNullError() {
        ComponentConnection prev = null;

        ComponentConnection curr = new ComponentConnectionLogicAndNetwork(
                LAYERIZER_ID + "->" + NETWORK_ID_LAYERIZER,
                TYPE_LAYERIZER,
                "running",
                LAYERIZER_ID,
                NETWORK_ID_LAYERIZER);

        ComponentConnectionChanged msg = new ComponentConnectionChanged("delete", prev, curr);

        when(target.getObjectId()).thenReturn(LAYERIZER_ID);

        assertThat(target.onConnectionChangedDeletePre(msg), is(false));

    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onConnectionChangedDeletePre()}
     */
    @Test
    public void testOnConnectionChangedDeletePreWithObjectTypeError() {

        ComponentConnection prev = PowerMockito.spy(new ComponentConnectionLogicAndNetwork(
                LAYERIZER_ID + "->" + NETWORK_ID_LOWER,
                TYPE_LOWER,
                "initializing",
                LAYERIZER_ID,
                NETWORK_ID_LOWER));

        ComponentConnection curr = new ComponentConnectionLogicAndNetwork(
                LAYERIZER_ID + "->" + NETWORK_ID_LOWER,
                TYPE_LOWER,
                "running",
                LAYERIZER_ID,
                NETWORK_ID_LOWER);

        when(prev.getObjectType()).thenReturn("UndefinedObjectType");

        ComponentConnectionChanged msg = new ComponentConnectionChanged("delete", prev, curr);

        when(target.getObjectId()).thenReturn(LAYERIZER_ID);

        assertThat(target.onConnectionChangedDeletePre(msg), is(false));

    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onConnectionChangedDeletePre()}
     */
    @Test
    public void testOnConnectionChangedDeletePreWithLogicIdError() {

        ComponentConnection prev = new ComponentConnectionLogicAndNetwork(
                LAYERIZER_ID + "->" + NETWORK_ID_LOWER,
                TYPE_LOWER,
                "initializing",
                LAYERIZER_ID,
                NETWORK_ID_LOWER);

        ComponentConnection curr = new ComponentConnectionLogicAndNetwork(
                LAYERIZER_ID + "->" + NETWORK_ID_LOWER,
                TYPE_LOWER,
                "running",
                LAYERIZER_ID,
                NETWORK_ID_LOWER);

        ComponentConnectionChanged msg = new ComponentConnectionChanged("delete", prev, curr);

        when(target.getObjectId()).thenReturn("slicer_1");
        assertThat(target.onConnectionChangedDeletePre(msg), is(false));

    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onConnectionChangedAdded()}
     * @throws Exception
     */
    @Test
    public void testOnConnectionChangedAddedSuccessLower() throws Exception {
        ComponentConnection prev = new ComponentConnectionLogicAndNetwork(
                LAYERIZER_ID + "->" + NETWORK_ID_LOWER,
                TYPE_LOWER,
                "initializing",
                LAYERIZER_ID,
                NETWORK_ID_LOWER);

        ComponentConnection curr = new ComponentConnectionLogicAndNetwork(
                LAYERIZER_ID + "->" + NETWORK_ID_LOWER,
                TYPE_LOWER,
                "running",
                LAYERIZER_ID,
                NETWORK_ID_LOWER);

        ComponentConnectionChanged msg =
                new ComponentConnectionChanged("add", prev, curr);

        PowerMockito.doReturn(systemInterface).when(target, METHOD_SYSTEM_MNG_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);
        PowerMockito.doNothing().when(target, "syncLinkLayerizer");

        target.onConnectionChangedAdded(msg);

        PowerMockito.verifyPrivate(conversionTable, never()).invoke("addEntryNetwork", anyString(),
                anyString());

        PowerMockito.verifyPrivate(target, times(1)).invoke("subscribeNetworkComponent",
                anyString());
        PowerMockito.verifyPrivate(target, times(1)).invoke("syncLinkLayerizer");

    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onConnectionChangedAdded()}
     * @throws Exception
     */
    @Test
    public void testOnConnectionChangedAddedSuccessUpper() throws Exception {
        ComponentConnection prev = new ComponentConnectionLogicAndNetwork(
                LAYERIZER_ID + "->" + NETWORK_ID_UPPER,
                TYPE_UPPER,
                "initializing",
                LAYERIZER_ID,
                NETWORK_ID_UPPER);

        ComponentConnection curr = new ComponentConnectionLogicAndNetwork(
                LAYERIZER_ID + "->" + NETWORK_ID_UPPER,
                TYPE_UPPER,
                "running",
                LAYERIZER_ID,
                NETWORK_ID_UPPER);

        ComponentConnectionChanged msg =
                new ComponentConnectionChanged("add", prev, curr);

        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);
        PowerMockito.doNothing().when(target, "syncLinkLayerizer");

        target.onConnectionChangedAdded(msg);

        PowerMockito.verifyPrivate(conversionTable, times(1)).invoke("addEntryNetwork", anyString(),
                anyString());

        PowerMockito.verifyPrivate(target, times(1)).invoke("subscribeNetworkComponent",
                anyString());
        PowerMockito.verifyPrivate(target, times(1)).invoke("syncLinkLayerizer");

    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onConnectionChangedAdded()}
     * @throws Exception
     */
    @Test
    public void testOnConnectionChangedAddedSuccessLayerizer() throws Exception {
        ComponentConnection prev = new ComponentConnectionLogicAndNetwork(
                LAYERIZER_ID + "->" + NETWORK_ID_LAYERIZER,
                TYPE_LAYERIZER,
                "initializing",
                LAYERIZER_ID,
                NETWORK_ID_LAYERIZER);

        ComponentConnection curr = new ComponentConnectionLogicAndNetwork(
                LAYERIZER_ID + "->" + NETWORK_ID_LAYERIZER,
                TYPE_LAYERIZER,
                "running",
                LAYERIZER_ID,
                NETWORK_ID_LAYERIZER);

        ComponentConnectionChanged msg =
                new ComponentConnectionChanged("add", prev, curr);

        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);
        PowerMockito.doReturn(systemInterface).when(target, METHOD_SYSTEM_MNG_IF);
        PowerMockito.doNothing().when(target, "syncLinkLayerizer");

        target.onConnectionChangedAdded(msg);

        PowerMockito.verifyPrivate(conversionTable, times(1)).invoke("addEntryNetwork", anyString(),
                anyString());

        PowerMockito.verifyPrivate(target, times(1)).invoke("subscribeNetworkComponent",
                anyString());
        PowerMockito.verifyPrivate(target, times(1)).invoke("syncLinkLayerizer");

    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onConnectionChangedAdded()}
     * @throws Exception
     */
    @Test
    public void testOnConnectionChangedAddedUpperNotSetLayerizer() throws Exception {
        ComponentConnection prev = new ComponentConnectionLogicAndNetwork(
                LAYERIZER_ID + "->" + NETWORK_ID_UPPER,
                TYPE_UPPER,
                "initializing",
                LAYERIZER_ID,
                NETWORK_ID_UPPER);

        ComponentConnection curr = new ComponentConnectionLogicAndNetwork(
                LAYERIZER_ID + "->" + NETWORK_ID_UPPER,
                TYPE_UPPER,
                "running",
                LAYERIZER_ID,
                NETWORK_ID_UPPER);

        ComponentConnectionChanged msg =
                new ComponentConnectionChanged("add", prev, curr);

        conversionTable.delEntryConnectionType(NETWORK_ID_LAYERIZER);

        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);
        PowerMockito.doReturn(systemInterface).when(target, METHOD_SYSTEM_MNG_IF);
        PowerMockito.doNothing().when(target, "syncLinkLayerizer");

        target.onConnectionChangedAdded(msg);

        PowerMockito.verifyPrivate(conversionTable, never()).invoke("addEntryNetwork", anyString(),
                anyString());

        PowerMockito.verifyPrivate(target, times(1)).invoke("subscribeNetworkComponent",
                anyString());
        PowerMockito.verifyPrivate(target, times(1)).invoke("syncLinkLayerizer");

    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onConnectionChangedAdded()}
     * @throws Exception
     */
    @Test
    public void testOnConnectionChangedAddedLayerizerNotSetUpper() throws Exception {
        ComponentConnection prev = new ComponentConnectionLogicAndNetwork(
                LAYERIZER_ID + "->" + NETWORK_ID_LAYERIZER,
                TYPE_LAYERIZER,
                "initializing",
                LAYERIZER_ID,
                NETWORK_ID_LAYERIZER);

        ComponentConnection curr = new ComponentConnectionLogicAndNetwork(
                LAYERIZER_ID + "->" + NETWORK_ID_LAYERIZER,
                TYPE_LAYERIZER,
                "running",
                LAYERIZER_ID,
                NETWORK_ID_LAYERIZER);

        ComponentConnectionChanged msg =
                new ComponentConnectionChanged("add", prev, curr);

        conversionTable.delEntryConnectionType(NETWORK_ID_UPPER);

        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);
        PowerMockito.doReturn(systemInterface).when(target, METHOD_SYSTEM_MNG_IF);
        PowerMockito.doNothing().when(target, "syncLinkLayerizer");

        target.onConnectionChangedAdded(msg);

        PowerMockito.verifyPrivate(conversionTable, never()).invoke("addEntryNetwork", anyString(),
                anyString());

        PowerMockito.verifyPrivate(target, times(1)).invoke("subscribeNetworkComponent",
                anyString());
        PowerMockito.verifyPrivate(target, times(1)).invoke("syncLinkLayerizer");

    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onConnectionChangedDelete()}
     * @throws Exception
     */
    @Test
    public void testOnConnectionChangedDeleteSuccessUpper() throws Exception {

        ComponentConnection prev = new ComponentConnectionLogicAndNetwork(
                LAYERIZER_ID + "->" + NETWORK_ID_UPPER,
                TYPE_UPPER,
                "initializing",
                LAYERIZER_ID,
                NETWORK_ID_UPPER);

        ComponentConnection curr = new ComponentConnectionLogicAndNetwork(
                LAYERIZER_ID + "->" + NETWORK_ID_UPPER,
                TYPE_UPPER,
                "running",
                LAYERIZER_ID,
                NETWORK_ID_UPPER);

        ComponentConnectionChanged msg =
                new ComponentConnectionChanged("add", prev, curr);

        PowerMockito.doReturn(new ArrayList<Object>() {
            {
                add(DEFAULT_RESPONSE);
            }
        }).when(networkInterfaces.get(NETWORK_ID_UPPER), "deleteAllFlow");
        PowerMockito.doReturn(DEFAULT_RESPONSE).when(systemInterface, "delConnection", anyString());
        PowerMockito.doNothing().when(target, "unsubscribeNetworkComponent", anyString());

        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);
        PowerMockito.doReturn(systemInterface).when(target, METHOD_SYSTEM_MNG_IF);

        target.onConnectionChangedDelete(msg);

        PowerMockito.verifyPrivate(target, times(3)).invoke("unsubscribeNetworkComponent",
                anyString());
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onConnectionChangedDelete()}
     * @throws Exception
     */
    @Test
    public void testOnConnectionChangedDeleteSuccessLower() throws Exception {

        ComponentConnection prev = new ComponentConnectionLogicAndNetwork(
                LAYERIZER_ID + "->" + NETWORK_ID_LOWER,
                TYPE_LOWER,
                "initializing",
                LAYERIZER_ID,
                NETWORK_ID_LOWER);

        ComponentConnection curr = new ComponentConnectionLogicAndNetwork(
                LAYERIZER_ID + "->" + NETWORK_ID_LOWER,
                TYPE_LOWER,
                "running",
                LAYERIZER_ID,
                NETWORK_ID_LOWER);

        ComponentConnectionChanged msg =
                new ComponentConnectionChanged("add", prev, curr);

        PowerMockito.doReturn(new ArrayList<Object>() {
            {
                add(DEFAULT_RESPONSE);
            }
        }).when(networkInterfaces.get(NETWORK_ID_LOWER), "deleteAllFlow");
        PowerMockito.doReturn(DEFAULT_RESPONSE).when(systemInterface, "delConnection", anyString());
        PowerMockito.doNothing().when(target, "unsubscribeNetworkComponent", anyString());

        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);
        PowerMockito.doReturn(systemInterface).when(target, METHOD_SYSTEM_MNG_IF);

        target.onConnectionChangedDelete(msg);

        PowerMockito.verifyPrivate(target, times(3)).invoke("unsubscribeNetworkComponent",
                anyString());
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onConnectionChangedDelete()}
     * @throws Exception
     */
    @Test
    public void testOnConnectionChangedDeleteSuccessLayerizer() throws Exception {

        ComponentConnection prev = new ComponentConnectionLogicAndNetwork(
                LAYERIZER_ID + "->" + NETWORK_ID_LAYERIZER,
                TYPE_LAYERIZER,
                "initializing",
                LAYERIZER_ID,
                NETWORK_ID_LAYERIZER);

        ComponentConnection curr = new ComponentConnectionLogicAndNetwork(
                LAYERIZER_ID + "->" + NETWORK_ID_LAYERIZER,
                TYPE_LAYERIZER,
                "running",
                LAYERIZER_ID,
                NETWORK_ID_LAYERIZER);

        ComponentConnectionChanged msg =
                new ComponentConnectionChanged("add", prev, curr);

        PowerMockito.doReturn(new ArrayList<Object>() {
            {
                add(DEFAULT_RESPONSE);
            }
        }).when(networkInterfaces.get(NETWORK_ID_LAYERIZER), "deleteAllFlow");
        PowerMockito.doReturn(DEFAULT_RESPONSE).when(systemInterface, "delConnection", anyString());
        PowerMockito.doNothing().when(target, "unsubscribeNetworkComponent", anyString());

        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);
        PowerMockito.doReturn(systemInterface).when(target, METHOD_SYSTEM_MNG_IF);

        target.onConnectionChangedDelete(msg);

        PowerMockito.verifyPrivate(target, times(3)).invoke("unsubscribeNetworkComponent",
                anyString());
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#unsubscribeNetworkComponent()}
     * @throws Exception
     */
    @Test
    public void testUnsubscribeNetworkComponentSuccessUpper() throws Exception {
        String networkId = NETWORK_ID_UPPER;

        // conversion table settings
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        Whitebox.invokeMethod(target, "unsubscribeNetworkComponent", networkId);

        // Method execution check
        PowerMockito.verifyPrivate(target, times(4)).invoke("removeEntryEventSubscription",
                anyString(), anyString());
        // ComponentとLogicのコンストラクタでも実行されているため3回
        PowerMockito.verifyPrivate(target, times(3)).invoke("applyEventSubscription");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#unsubscribeNetworkComponent()}
     * @throws Exception
     */
    @Test
    public void testUnsubscribeNetworkComponentSuccessLower() throws Exception {
        String networkId = NETWORK_ID_LOWER;

        // conversion table settings
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        Whitebox.invokeMethod(target, "unsubscribeNetworkComponent", networkId);

        // Method execution check
        PowerMockito.verifyPrivate(target, times(4)).invoke("removeEntryEventSubscription",
                anyString(), anyString());
        // ComponentとLogicのコンストラクタでも実行されているため3回
        PowerMockito.verifyPrivate(target, times(3)).invoke("applyEventSubscription");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#unsubscribeNetworkComponent()}
     * @throws Exception
     */
    @Test
    public void testUnsubscribeNetworkComponentSuccessLayerizer() throws Exception {
        String networkId = NETWORK_ID_LAYERIZER;

        // conversion table settings
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        Whitebox.invokeMethod(target, "unsubscribeNetworkComponent", networkId);

        // Method execution check
        PowerMockito.verifyPrivate(target, times(4)).invoke("removeEntryEventSubscription",
                anyString(), anyString());
        // ComponentとLogicのコンストラクタでも実行されているため3回
        PowerMockito.verifyPrivate(target, times(3)).invoke("applyEventSubscription");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#subscribeNetworkComponent()}
     * @throws Exception
     */
    @Test
    public void testSubscribeNetworkComponent() throws Exception {
        String networkId = NETWORK_ID_UPPER;

        Whitebox.invokeMethod(target, "subscribeNetworkComponent", networkId);

        // Method execution check
        PowerMockito.verifyPrivate(target, times(4)).invoke("addEntryEventSubscription",
                anyString(), anyString());
        PowerMockito.verifyPrivate(target, times(4)).invoke("updateEntryEventSubscription",
                anyString(), anyString(), any());
        PowerMockito.verifyPrivate(target, times(3)).invoke("applyEventSubscription");

    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onNodeAddedPre()}
     * @throws Exception
     */
    @Test
    public void testOnNodeAddedPreUpper() throws Exception {
        String networkId = NETWORK_ID_UPPER;
        Node node = new Node();

        /* default network setting for the conversion table */
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        assertTrue(target.onNodeAddedPre(networkId, node));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onNodeAddedPre()}
     * @throws Exception
     */
    @Test
    public void testOnNodeAddedPreLower() throws Exception {
        String networkId = NETWORK_ID_LOWER;
        Node node = new Node();

        /* default network setting for the conversion table */
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        assertFalse(target.onNodeAddedPre(networkId, node));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onNodeAddedPre()}
     * @throws Exception
     */
    @Test
    public void testOnNodeAddedPreLayerizer() throws Exception {
        String networkId = NETWORK_ID_LAYERIZER;
        Node node = new Node();

        /* default network setting for the conversion table */
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        assertFalse(target.onNodeAddedPre(networkId, node));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onNodeAdded()}
     * @throws Exception
     */
    @Test
    public void testOnNodeAdded() throws Exception {
        String networkId = NETWORK_ID_UPPER;

        Node layerizerNode = new Node("nodeIdLay");
        Map<String, Port> layerizerPorts = new HashMap<String, Port>() {
            {
                put("port1", new Port("0", "port1", "nodeIdLay"));
                put("port2", new Port("0", "port2", "nodeIdLay"));
            }
        };
        layerizerNode.setPorts(layerizerPorts);

        PowerMockito.doNothing().when(conversionTable, "addEntryNode",
                anyString(), anyString(), anyString(), anyString());
        PowerMockito.doReturn(DEFAULT_RESPONSE).when(networkInterfaces.get(NETWORK_ID_LAYERIZER),
                "putNode", anyObject());
        PowerMockito.doReturn(layerizerNode).when(networkInterfaces.get(NETWORK_ID_LAYERIZER),
                "getNode", anyObject());

        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        Node node = new Node("nodeId");
        Map<String, Port> ports = new HashMap<String, Port>() {
            {
                put("port1", new Port("0", "port1", "nodeId"));
                put("port2", new Port("0", "port2", "nodeId"));
            }
        };
        node.setPorts(ports);

        target.onNodeAdded(networkId, node);

        PowerMockito.verifyPrivate(conversionTable, times(1)).invoke("addEntryNode",
                anyString(), anyString(), anyString(), anyString());
        PowerMockito.verifyPrivate(conversionTable, times(2)).invoke("addEntryPort",
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
        PowerMockito.verifyPrivate(networkInterfaces.get(NETWORK_ID_LAYERIZER), times(1)).invoke(
                "putNode", anyObject());
        PowerMockito.verifyPrivate(networkInterfaces.get(NETWORK_ID_LAYERIZER), times(1)).invoke(
                "getNode", anyObject());
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onNodeAdded()}
     * @throws Exception
     */
    @Test
    public void testOnNodeAddedNotSetPort() throws Exception {
        String networkId = NETWORK_ID_UPPER;

        Node layerizerNode = new Node("nodeIdLay");
        Map<String, Port> layerizerPorts = new HashMap<String, Port>() {
            {
                put("port1", new Port("0", "port1", "nodeIdLay"));
                put("port2", new Port("0", "port2", "nodeIdLay"));
            }
        };
        layerizerNode.setPorts(layerizerPorts);

        PowerMockito.doNothing().when(conversionTable, "addEntryNode",
                anyString(), anyString(), anyString(), anyString());
        PowerMockito.doReturn(DEFAULT_RESPONSE).when(networkInterfaces.get(NETWORK_ID_LAYERIZER),
                "putNode", anyObject());
        PowerMockito.doReturn(layerizerNode).when(networkInterfaces.get(NETWORK_ID_LAYERIZER),
                "getNode", anyObject());

        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        Node node = new Node("nodeId");
        Map<String, Port> ports = new HashMap<String, Port>() {
            {

            }
        };
        node.setPorts(ports);

        target.onNodeAdded(networkId, node);

        PowerMockito.verifyPrivate(conversionTable, times(1)).invoke("addEntryNode",
                anyString(), anyString(), anyString(), anyString());
        PowerMockito.verifyPrivate(conversionTable, never()).invoke("addEntryPort",
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
        PowerMockito.verifyPrivate(networkInterfaces.get(NETWORK_ID_LAYERIZER), times(1)).invoke(
                "putNode", anyObject());
        PowerMockito.verifyPrivate(networkInterfaces.get(NETWORK_ID_LAYERIZER), times(1)).invoke(
                "getNode", anyObject());
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onNodeUpdatePre()}
     * @throws Exception
     */
    @Test
    public void testOnNodeUpdatePreUpper() throws Exception {
        String networkId = NETWORK_ID_UPPER;
        Node prev = new Node();
        Node curr = new Node();

        /* default network setting for the conversion table */
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        assertTrue(target.onNodeUpdatePre(networkId, prev, curr, new ArrayList<String>()));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onNodeUpdatePre()}
     * @throws Exception
     */
    @Test
    public void testOnNodeUpdatePreLower() throws Exception {
        String networkId = NETWORK_ID_LOWER;
        Node prev = new Node();
        Node curr = new Node();

        /* default network setting for the conversion table */
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        assertFalse(target.onNodeUpdatePre(networkId, prev, curr, new ArrayList<String>()));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onNodeUpdatePre()}
     * @throws Exception
     */
    @Test
    public void testOnNodeUpdatePreLayerizer() throws Exception {
        String networkId = NETWORK_ID_LAYERIZER;
        Node prev = new Node();
        Node curr = new Node();

        /* default network setting for the conversion table */
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        assertFalse(target.onNodeUpdatePre(networkId, prev, curr, new ArrayList<String>()));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onNodeDeletePre()}
     * @throws Exception
     */
    @Test
    public void testOnNodeDeletePreUpper() throws Exception {
        String networkId = NETWORK_ID_UPPER;
        Node node = new Node();

        /* default network setting for the conversion table */
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        assertTrue(target.onNodeDeletePre(networkId, node));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onNodeDeletePre()}
     * @throws Exception
     */
    @Test
    public void testOnNodeDeletePreLower() throws Exception {
        String networkId = NETWORK_ID_LOWER;
        Node node = new Node();

        /* default network setting for the conversion table */
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        assertFalse(target.onNodeDeletePre(networkId, node));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onNodeDeletePre()}
     * @throws Exception
     */
    @Test
    public void testOnNodeDeletePreLayerizer() throws Exception {
        String networkId = NETWORK_ID_LAYERIZER;
        Node node = new Node();

        /* default network setting for the conversion table */
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        assertFalse(target.onNodeDeletePre(networkId, node));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onPortAddedPre()}
     * @throws Exception
     */
    @Test
    public void testOnPortAddedPreUpper() throws Exception {
        String networkId = NETWORK_ID_UPPER;
        Port port = new Port();

        /* default network setting for the conversion table */
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        assertTrue(target.onPortAddedPre(networkId, port));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onPortAddedPre()}
     * @throws Exception
     */
    @Test
    public void testOnPortAddedPreLower() throws Exception {
        String networkId = NETWORK_ID_LOWER;
        Port port = new Port();

        /* default network setting for the conversion table */
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        assertFalse(target.onPortAddedPre(networkId, port));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onPortAddedPre()}
     * @throws Exception
     */
    @Test
    public void testOnPortAddedPreLayerizer() throws Exception {
        String networkId = NETWORK_ID_LAYERIZER;
        Port port = new Port();

        /* default network setting for the conversion table */
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        assertFalse(target.onPortAddedPre(networkId, port));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onPortUpdatePre()}
     * @throws Exception
     */
    @Test
    public void testOnPortUpdatePreUpper() throws Exception {
        String networkId = NETWORK_ID_UPPER;
        Port prev = new Port();
        Port curr = new Port();

        /* default network setting for the conversion table */
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        assertTrue(target.onPortUpdatePre(networkId, prev, curr, new ArrayList<String>()));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onPortUpdatePre()}
     * @throws Exception
     */
    @Test
    public void testOnPortUpdatePreLower() throws Exception {
        String networkId = NETWORK_ID_LOWER;
        Port prev = new Port();
        Port curr = new Port();

        /* default network setting for the conversion table */
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        assertFalse(target.onPortUpdatePre(networkId, prev, curr, new ArrayList<String>()));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onPortUpdatePre()}
     * @throws Exception
     */
    @Test
    public void testOnPortUpdatePreLayerizer() throws Exception {
        String networkId = NETWORK_ID_LAYERIZER;
        Port prev = new Port();
        Port curr = new Port();

        /* default network setting for the conversion table */
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        assertFalse(target.onPortUpdatePre(networkId, prev, curr, new ArrayList<String>()));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onPortDeletePre()}
     * @throws Exception
     */
    @Test
    public void testOnPortDeletePreUpper() throws Exception {
        String networkId = NETWORK_ID_UPPER;
        Port port = new Port();

        /* default network setting for the conversion table */
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        assertTrue(target.onPortDeletePre(networkId, port));

    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onPortDeletePre()}
     * @throws Exception
     */
    @Test
    public void testOnPortDeletePreLower() throws Exception {
        String networkId = NETWORK_ID_LOWER;
        Port port = new Port();

        /* default network setting for the conversion table */
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        assertFalse(target.onPortDeletePre(networkId, port));

    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onPortDeletePre()}
     * @throws Exception
     */
    @Test
    public void testOnPortDeletePreLayerizer() throws Exception {
        String networkId = NETWORK_ID_LAYERIZER;
        Port port = new Port();

        /* default network setting for the conversion table */
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        assertFalse(target.onPortDeletePre(networkId, port));

    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onLinkAddedPre()}
     * @throws Exception
     */
    @Test
    public void testOnLinkAddedPreUpper() throws Exception {
        String networkId = NETWORK_ID_UPPER;
        Link link = PowerMockito.spy(new Link());
        link.putAttribute(AttrElements.ESTABLISHMENT_STATUS, "Establishing");
        when(link.validate()).thenReturn(true);

        /* default network setting for the conversion table */
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        assertTrue(target.onLinkAddedPre(networkId, link));

    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onLinkAddedPre()}
     * @throws Exception
     */
    @Test
    public void testOnLinkAddedPreLower() throws Exception {
        String networkId = NETWORK_ID_LOWER;
        Link link = PowerMockito.spy(new Link());
        link.putAttribute(AttrElements.ESTABLISHMENT_STATUS, "Establishing");
        when(link.validate()).thenReturn(true);

        /* default network setting for the conversion table */
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        assertFalse(target.onLinkAddedPre(networkId, link));

    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onLinkAddedPre()}
     * @throws Exception
     */
    @Test
    public void testOnLinkAddedPreLayerizer() throws Exception {
        String networkId = NETWORK_ID_LAYERIZER;
        Link link = PowerMockito.spy(new Link());
        link.putAttribute(AttrElements.ESTABLISHMENT_STATUS, "Establishing");
        when(link.validate()).thenReturn(true);

        /* default network setting for the conversion table */
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        assertTrue(target.onLinkAddedPre(networkId, link));

    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onLinkAdded()}
     * @throws Exception
     */
    @Test
    public void testOnLinkAdded() throws Exception {
        String networkId = NETWORK_ID_LAYERIZER;
        Link link = PowerMockito.spy(new Link());
        link.putAttribute(AttrElements.ESTABLISHMENT_STATUS, "Establishing");
        when(link.validate()).thenReturn(true);

        /* default network setting for the conversion table */
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        PowerMockito.doNothing().when(target, "registerLinkAndSync", anyString(),
                anyString(), anyObject());
        PowerMockito.doNothing().when(target, "uppdateLowerFlowFromLayerizerLink",
                anyObject());

        target.onLinkAdded(networkId, link);

        PowerMockito.verifyPrivate(target, times(1)).invoke("registerLinkAndSync", anyString(),
                anyString(), anyObject());
        PowerMockito.verifyPrivate(target, times(1)).invoke("uppdateLowerFlowFromLayerizerLink",
                anyObject());

    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onLinkUpdatePre()}
     * @throws Exception
     */
    @Test
    public void testOnLinkUpdatePreUpper() throws Exception {
        String networkId = NETWORK_ID_UPPER;
        Link prev = new Link();
        Link curr = PowerMockito.spy(new Link());
        curr.putAttribute(AttrElements.ESTABLISHMENT_STATUS, "Establishing");
        when(curr.validate()).thenReturn(true);

        /* default network setting for the conversion table */
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        assertFalse(target.onLinkUpdatePre(networkId, prev, curr, new ArrayList<String>()));

    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onLinkUpdatePre()}
     * @throws Exception
     */
    @Test
    public void testOnLinkUpdatePreLower() throws Exception {
        String networkId = NETWORK_ID_LOWER;
        Link prev = new Link();
        Link curr = PowerMockito.spy(new Link());
        curr.putAttribute(AttrElements.ESTABLISHMENT_STATUS, "Establishing");
        when(curr.validate()).thenReturn(true);

        /* default network setting for the conversion table */
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        assertFalse(target.onLinkUpdatePre(networkId, prev, curr, new ArrayList<String>()));

    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onLinkUpdatePre()}
     * @throws Exception
     */
    @Test
    public void testOnLinkUpdatePreLayerizer() throws Exception {
        String networkId = NETWORK_ID_LAYERIZER;
        Link prev = new Link();
        Link curr = PowerMockito.spy(new Link());
        curr.putAttribute(AttrElements.ESTABLISHMENT_STATUS, "Establishing");
        when(curr.validate()).thenReturn(true);

        /* default network setting for the conversion table */
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        assertTrue(target.onLinkUpdatePre(networkId, prev, curr, new ArrayList<String>()));

    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onLinkUpdate()}
     * @throws Exception
     */
    @Test
    public void testOnLinkUpdate() throws Exception {
        String networkId = NETWORK_ID_LAYERIZER;
        Link prev = new Link();
        Link curr = new Link();
        curr.putAttribute(AttrElements.ESTABLISHMENT_STATUS, "Establishing");

        /* default network setting for the conversion table */
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);
        PowerMockito.doReturn(true).when(target, "onLinkUpdatePre", anyString(),
                anyObject(), anyObject(), anyObject());
        PowerMockito.doNothing().when(target, "onSuperLinkUpdate", anyString(),
                anyObject(), anyObject(), anyObject());
        PowerMockito.doNothing().when(target, "uppdateLowerFlowFromLayerizerLink",
                anyObject());

        target.onLinkUpdate(networkId, prev, curr, new ArrayList<String>());

        PowerMockito.verifyPrivate(target, times(1)).invoke("uppdateLowerFlowFromLayerizerLink",
                anyObject());
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onLinkDeletePre()}
     * @throws Exception
     */
    @Test
    public void testOnLinkDeletePreUpper() throws Exception {
        String networkId = NETWORK_ID_UPPER;
        Link link = spy(new Link());
        link.putAttribute(AttrElements.ESTABLISHMENT_STATUS, "Establishing");
        when(link.validate()).thenReturn(true);

        /* default network setting for the conversion table */
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        assertTrue(target.onLinkDeletePre(networkId, link));

    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onLinkDeletePre()}
     * @throws Exception
     */
    @Test
    public void testOnLinkDeletePreLower() throws Exception {
        String networkId = NETWORK_ID_LOWER;
        Link link = spy(new Link());
        link.putAttribute(AttrElements.ESTABLISHMENT_STATUS, "Establishing");
        when(link.validate()).thenReturn(true);

        /* default network setting for the conversion table */
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        assertFalse(target.onLinkDeletePre(networkId, link));

    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onLinkDeletePre()}
     * @throws Exception
     */
    @Test
    public void testOnLinkDeletePreLayerizer() throws Exception {
        String networkId = NETWORK_ID_LAYERIZER;
        Link link = spy(new Link());
        link.putAttribute(AttrElements.ESTABLISHMENT_STATUS, "Establishing");
        when(link.validate()).thenReturn(true);

        /* default network setting for the conversion table */
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        assertTrue(target.onLinkDeletePre(networkId, link));

    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onLinkDelete()}
     * @throws Exception
     */
    @Test
    public void testOnLinkDelete() throws Exception {
        String networkId = NETWORK_ID_LAYERIZER;
        Link link = new Link();
        link.putAttribute(AttrElements.ESTABLISHMENT_STATUS, "Establishing");

        /* default network setting for the conversion table */
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);
        PowerMockito.doNothing().when(target, "onSuperLinkDelete", anyString(), anyObject());
        PowerMockito.doNothing().when(target, "delLowerFlows", anyString());
        PowerMockito.doReturn(true).when(target, "onLinkDeletePre", anyString(),
                anyObject());

        target.onLinkDelete(networkId, link);

        PowerMockito.verifyPrivate(target, times(1)).invoke("delLowerFlows", anyString());
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onFlowAdded()}
     * @throws Exception
     */
    @Test
    public void testOnFlowAddedUpper() throws Exception {
        String networkId = NETWORK_ID_UPPER;
        Flow flow = new BasicFlow();

        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        target.onFlowAdded(networkId, flow);

        PowerMockito.verifyPrivate(target, never()).invoke("addLinkOfLayerizerFromFlowOfLower",
                anyString(), anyObject());
        PowerMockito.verifyPrivate(target, never()).invoke("addFlowToUpper",
                anyString(), anyObject());
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onFlowAdded()}
     * @throws Exception
     */
    @Test
    public void testOnFlowAddedLower() throws Exception {
        String networkId = NETWORK_ID_LOWER;
        Flow flow = new BasicFlow();

        PowerMockito.doNothing().when(target, "addLinkOfLayerizerFromFlowOfLower", anyString(),
                anyObject());
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        target.onFlowAdded(networkId, flow);

        PowerMockito.verifyPrivate(target, times(1)).invoke("addLinkOfLayerizerFromFlowOfLower",
                anyString(),
                anyObject());
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onFlowAdded()}
     * @throws Exception
     */
    @Test
    public void testOnFlowAddedLayerizer() throws Exception {
        String networkId = NETWORK_ID_LAYERIZER;
        Flow flow = new BasicFlow();

        PowerMockito.doNothing().when(target, "addFlowToUpper", anyString(), anyObject());
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        target.onFlowAdded(networkId, flow);

        PowerMockito.verifyPrivate(target, times(1)).invoke("addFlowToUpper", anyString(),
                anyObject());
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onFlowUpdate()}
     * @throws Exception
     */
    @Test
    public void testOnFlowUpdateUpper() throws Exception {
        String networkId = NETWORK_ID_UPPER;
        Flow prev = new BasicFlow();
        Flow curr = new BasicFlow();

        PowerMockito.doNothing().when((Logic) target, "onFlowUpdate", anyString(), anyObject(),
                anyObject(), anyObject());
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        target.onFlowUpdate(networkId, prev, curr, new ArrayList<String>());

        PowerMockito.verifyPrivate((Logic) target, times(1)).invoke("onFlowUpdate", anyString(),
                anyObject(), anyObject(), anyObject());
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onFlowUpdate()}
     * @throws Exception
     */
    @Test
    public void testOnFlowUpdateLower() throws Exception {
        String networkId = NETWORK_ID_LOWER;
        Flow prev = new BasicFlow();
        prev.setStatus(FlowStatus.ESTABLISHED.toString());
        Flow curr = new BasicFlow();
        curr.setStatus(FlowStatus.NONE.toString());

        PowerMockito.doNothing().when((Logic) target, "addLinkOfLayerizerFromFlowOfLower",
                anyString(), anyObject());
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        target.onFlowUpdate(networkId, prev, curr, new ArrayList<String>());

        PowerMockito.verifyPrivate((Logic) target, times(1)).invoke(
                "addLinkOfLayerizerFromFlowOfLower", anyString(), anyObject());
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onFlowUpdate()}
     * @throws Exception
     */
    @Test
    public void testOnFlowUpdateLayerizer() throws Exception {
        String networkId = NETWORK_ID_LAYERIZER;
        Flow prev = new BasicFlow();
        Flow curr = new BasicFlow();

        PowerMockito.doNothing().when((Logic) target, "onFlowUpdate", anyString(), anyObject(),
                anyObject(), anyObject());
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        target.onFlowUpdate(networkId, prev, curr, new ArrayList<String>());

        PowerMockito.verifyPrivate((Logic) target, times(1)).invoke("onFlowUpdate", anyString(),
                anyObject(), anyObject(), anyObject());
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onFlowDelete()}
     * @throws Exception
     */
    @Test
    public void testOnFlowDeleteUpper() throws Exception {
        String networkId = NETWORK_ID_UPPER;
        Flow flow = new BasicFlow();
        Flow returnFlow = new FlowSet().createFlow(FlowType.BASIC_FLOW, Flow.DEFAULT_PRIORITY);

        PowerMockito.doReturn(DEFAULT_RESPONSE).when(networkInterfaces.get(NETWORK_ID_LAYERIZER)
                , "delFlow", anyString());
        PowerMockito.doReturn(returnFlow).when(networkInterfaces.get(NETWORK_ID_LAYERIZER)
                , "getFlow", anyString());
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);
        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);

        target.onFlowDelete(networkId, flow);

        PowerMockito.verifyPrivate(networkInterfaces.get(NETWORK_ID_LAYERIZER), never()).invoke(
                "delFlow", anyString());
        PowerMockito.verifyPrivate(networkInterfaces.get(NETWORK_ID_LAYERIZER), never()).invoke(
                "delLink", anyString());
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onFlowDelete()}
     * @throws Exception
     */
    @Test
    public void testOnFlowDeleteLower() throws Exception {
        String networkId = NETWORK_ID_LOWER;
        Flow flow = new BasicFlow();
        flow.setFlowId("FlowId");

        Link returnLink = new Link();

        PowerMockito.doReturn(DEFAULT_RESPONSE).when(networkInterfaces.get(NETWORK_ID_LAYERIZER)
                , "delLink", anyString());
        PowerMockito.doReturn(returnLink).when(networkInterfaces.get(NETWORK_ID_LAYERIZER)
                , "getLink", anyString());

        Map<String, String> layerizedlinks = new HashMap<String, String>();
        layerizedlinks.put("FlowId", "linkid");

        Whitebox.setInternalState(target, "layerizedlinks", layerizedlinks);
        Whitebox.setInternalState(target, "lowerflows", new HashMap<String, String>());
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);
        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);

        target.onFlowDelete(networkId, flow);

        PowerMockito.verifyPrivate(networkInterfaces.get(NETWORK_ID_LAYERIZER), never()).invoke(
                "delFlow", anyString());
        PowerMockito.verifyPrivate(networkInterfaces.get(NETWORK_ID_LAYERIZER), times(1)).invoke(
                "delLink", anyString());
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onFlowDelete()}
     * @throws Exception
     */
    @Test
    public void testOnFlowDeleteLowerNotSetLink() throws Exception {
        String networkId = NETWORK_ID_LOWER;
        Flow flow = new BasicFlow();
        flow.setFlowId("FlowId");

        PowerMockito.doReturn(DEFAULT_RESPONSE).when(networkInterfaces.get(NETWORK_ID_LAYERIZER)
                , "delLink", anyString());
        PowerMockito.doReturn(null).when(networkInterfaces.get(NETWORK_ID_LAYERIZER)
                , "getLink", anyString());

        Map<String, String> layerizedlinks = new HashMap<String, String>();
        layerizedlinks.put("FlowId", "linkid");

        Whitebox.setInternalState(target, "layerizedlinks", layerizedlinks);
        Whitebox.setInternalState(target, "lowerflows", new HashMap<String, String>());
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);
        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);

        target.onFlowDelete(networkId, flow);

        PowerMockito.verifyPrivate(networkInterfaces.get(NETWORK_ID_LAYERIZER), never()).invoke(
                "delFlow", anyString());
        PowerMockito.verifyPrivate(networkInterfaces.get(NETWORK_ID_LAYERIZER), never()).invoke(
                "delLink", anyString());
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onFlowDelete()}
     * @throws Exception
     */
    @Test
    public void testOnFlowDeleteLayerizer() throws Exception {
        String networkId = NETWORK_ID_LAYERIZER;
        Flow flow = new BasicFlow();
        flow.setFlowId("FlowId");

        Flow returnFlow = new FlowSet().createFlow(FlowType.BASIC_FLOW, Flow.DEFAULT_PRIORITY);

        PowerMockito.doReturn(DEFAULT_RESPONSE).when(networkInterfaces.get(NETWORK_ID_UPPER)
                , "delFlow", anyString());
        PowerMockito.doReturn(returnFlow).when(networkInterfaces.get(NETWORK_ID_UPPER)
                , "getFlow", anyString());

        Map<String, String> layerizedlinks = new HashMap<String, String>();
        layerizedlinks.put("FlowId", "linkid");

        Whitebox.setInternalState(target, "layerizedlinks", layerizedlinks);
        Whitebox.setInternalState(target, "lowerflows", new HashMap<String, String>());
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);
        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);

        target.onFlowDelete(networkId, flow);

        PowerMockito.verifyPrivate(networkInterfaces.get(NETWORK_ID_UPPER), times(1)).invoke(
                "delFlow", anyString());
        PowerMockito.verifyPrivate(networkInterfaces.get(NETWORK_ID_UPPER), never()).invoke(
                "delLink", anyString());
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onFlowDelete()}
     * @throws Exception
     */
    @Test
    public void testOnFlowDeleteLayerizerNotSetFlow() throws Exception {
        String networkId = NETWORK_ID_LAYERIZER;
        Flow flow = new BasicFlow();
        flow.setFlowId("FlowId");

        PowerMockito.doReturn(DEFAULT_RESPONSE).when(networkInterfaces.get(NETWORK_ID_UPPER)
                , "delFlow", anyString());
        PowerMockito.doReturn(null).when(networkInterfaces.get(NETWORK_ID_UPPER)
                , "getFlow", anyString());

        Map<String, String> layerizedlinks = new HashMap<String, String>();
        layerizedlinks.put("FlowId", "linkid");

        Whitebox.setInternalState(target, "layerizedlinks", layerizedlinks);
        Whitebox.setInternalState(target, "lowerflows", new HashMap<String, String>());
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);
        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);

        target.onFlowDelete(networkId, flow);

        PowerMockito.verifyPrivate(networkInterfaces.get(NETWORK_ID_UPPER), never()).invoke(
                "delFlow", anyString());
        PowerMockito.verifyPrivate(networkInterfaces.get(NETWORK_ID_UPPER), never()).invoke(
                "delLink", anyString());
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#syncLinkLayerizer()}
     * @throws Exception
     */
    @Test
    public void testSyncLinkLayerizerSuccess() throws Exception {

        // networkinterface settings
        PowerMockito.doReturn(nodes).when(networkInterfaces.get(NETWORK_ID_UPPER), "getNodes");
        PowerMockito.doReturn(DEFAULT_RESPONSE).when(networkInterfaces.get(NETWORK_ID_LAYERIZER),
                "putNode", anyObject());
        PowerMockito.doReturn(flowSet).when(networkInterfaces.get(NETWORK_ID_LOWER), "getFlowSet");
        PowerMockito.doNothing().when(target, "addLinkOfLayerizerFromFlowOfLower", anyString(),
                anyObject());

        // Set the mockito to layerizer
        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        Whitebox.invokeMethod(target, "syncLinkLayerizer");

        PowerMockito.verifyPrivate(networkInterfaces.get(NETWORK_ID_LAYERIZER), times(2)).invoke(
                "putNode", anyObject());
        PowerMockito.verifyPrivate(target, times(2)).invoke("addLinkOfLayerizerFromFlowOfLower",
                anyString(), anyObject());
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#syncLinkLayerizer()}
     * @throws Exception
     */
    @Test
    public void testSyncLinkLayerizerNotSetUpper() throws Exception {

        // networkinterface settings
        PowerMockito.doReturn(nodes).when(networkInterfaces.get(NETWORK_ID_UPPER), "getNodes");
        PowerMockito.doReturn(DEFAULT_RESPONSE).when(networkInterfaces.get(NETWORK_ID_LAYERIZER),
                "putNode", anyObject());
        PowerMockito.doReturn(flowSet).when(networkInterfaces.get(NETWORK_ID_LOWER), "getFlowSet");
        PowerMockito.doNothing().when(target, "addLinkOfLayerizerFromFlowOfLower", anyString(),
                anyObject());

        // Set the mockito to layerizer
        conversionTable.delEntryConnectionType(NETWORK_ID_UPPER);
        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        Whitebox.invokeMethod(target, "syncLinkLayerizer");

        PowerMockito.verifyPrivate(networkInterfaces.get(NETWORK_ID_LAYERIZER), never()).invoke(
                "putNode", anyObject());
        PowerMockito.verifyPrivate(target, never()).invoke("addLinkOfLayerizerFromFlowOfLower",
                anyString(), anyObject());
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#syncLinkLayerizer()}
     * @throws Exception
     */
    @Test
    public void testSyncLinkLayerizerNotSetLower() throws Exception {

        // networkinterface settings
        PowerMockito.doReturn(nodes).when(networkInterfaces.get(NETWORK_ID_UPPER), "getNodes");
        PowerMockito.doReturn(DEFAULT_RESPONSE).when(networkInterfaces.get(NETWORK_ID_LAYERIZER),
                "putNode", anyObject());
        PowerMockito.doReturn(flowSet).when(networkInterfaces.get(NETWORK_ID_LOWER), "getFlowSet");
        PowerMockito.doNothing().when(target, "addLinkOfLayerizerFromFlowOfLower", anyString(),
                anyObject());

        // Set the mockito to layerizer
        conversionTable.delEntryConnectionType(NETWORK_ID_LOWER);
        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        Whitebox.invokeMethod(target, "syncLinkLayerizer");

        PowerMockito.verifyPrivate(networkInterfaces.get(NETWORK_ID_LAYERIZER), never()).invoke(
                "putNode", anyObject());
        PowerMockito.verifyPrivate(target, never()).invoke("addLinkOfLayerizerFromFlowOfLower",
                anyString(), anyObject());
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#syncLinkLayerizer()}
     * @throws Exception
     */
    @Test
    public void testSyncLinkLayerizerNotSetLayerizer() throws Exception {

        // networkinterface settings
        PowerMockito.doReturn(nodes).when(networkInterfaces.get(NETWORK_ID_UPPER), "getNodes");
        PowerMockito.doReturn(DEFAULT_RESPONSE).when(networkInterfaces.get(NETWORK_ID_LAYERIZER),
                "putNode", anyObject());
        PowerMockito.doReturn(flowSet).when(networkInterfaces.get(NETWORK_ID_LOWER), "getFlowSet");
        PowerMockito.doNothing().when(target, "addLinkOfLayerizerFromFlowOfLower", anyString(),
                anyObject());

        // Set the mockito to layerizer
        conversionTable.delEntryConnectionType(NETWORK_ID_LAYERIZER);
        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        Whitebox.invokeMethod(target, "syncLinkLayerizer");

        PowerMockito.verifyPrivate(networkInterfaces.get(NETWORK_ID_LAYERIZER), never()).invoke(
                "putNode", anyObject());
        PowerMockito.verifyPrivate(target, never()).invoke("addLinkOfLayerizerFromFlowOfLower",
                anyString(), anyObject());
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#syncLinkLayerizer()}
     * @throws Exception
     */
    @Test
    public void testSyncLinkLayerizerNotSetNodes() throws Exception {

        // networkinterface settings
        PowerMockito.doReturn(null).when(networkInterfaces.get(NETWORK_ID_UPPER), "getNodes");
        PowerMockito.doReturn(DEFAULT_RESPONSE).when(networkInterfaces.get(NETWORK_ID_LAYERIZER),
                "putNode", anyObject());
        PowerMockito.doReturn(flowSet).when(networkInterfaces.get(NETWORK_ID_LOWER), "getFlowSet");
        PowerMockito.doNothing().when(target, "addLinkOfLayerizerFromFlowOfLower", anyString(),
                anyObject());

        // Set the mockito to layerizer
        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        Whitebox.invokeMethod(target, "syncLinkLayerizer");

        PowerMockito.verifyPrivate(networkInterfaces.get(NETWORK_ID_LAYERIZER), never()).invoke(
                "putNode", anyObject());
        PowerMockito.verifyPrivate(target, times(2)).invoke("addLinkOfLayerizerFromFlowOfLower",
                anyString(), anyObject());
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#syncLinkLayerizer()}
     * @throws Exception
     */
    @Test
    public void testSyncLinkLayerizerNotSetFlowSet() throws Exception {

        // networkinterface settings
        PowerMockito.doReturn(nodes).when(networkInterfaces.get(NETWORK_ID_UPPER), "getNodes");
        PowerMockito.doReturn(DEFAULT_RESPONSE).when(networkInterfaces.get(NETWORK_ID_LAYERIZER),
                "putNode", anyObject());
        PowerMockito.doReturn(null).when(networkInterfaces.get(NETWORK_ID_LOWER), "getFlowSet");
        PowerMockito.doNothing().when(target, "addLinkOfLayerizerFromFlowOfLower", anyString(),
                anyObject());

        // Set the mockito to layerizer
        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        Whitebox.invokeMethod(target, "syncLinkLayerizer");

        PowerMockito.verifyPrivate(networkInterfaces.get(NETWORK_ID_LAYERIZER), times(2)).invoke(
                "putNode", anyObject());
        PowerMockito.verifyPrivate(target, never()).invoke("addLinkOfLayerizerFromFlowOfLower",
                anyString(), anyObject());
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#syncLinkLayerizer()}
     * @throws Exception
     */
    @Test
    public void testSyncLinkLayerizerNotSetFlow() throws Exception {

        // networkinterface settings
        PowerMockito.doReturn(nodes).when(networkInterfaces.get(NETWORK_ID_UPPER), "getNodes");
        PowerMockito.doReturn(DEFAULT_RESPONSE).when(networkInterfaces.get(NETWORK_ID_LAYERIZER),
                "putNode", anyObject());
        flowSet = new FlowSet();
        PowerMockito.doReturn(flowSet).when(networkInterfaces.get(NETWORK_ID_LOWER), "getFlowSet");
        PowerMockito.doNothing().when(target, "addLinkOfLayerizerFromFlowOfLower", anyString(),
                anyObject());

        // Set the mockito to layerizer
        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        Whitebox.invokeMethod(target, "syncLinkLayerizer");

        PowerMockito.verifyPrivate(networkInterfaces.get(NETWORK_ID_LAYERIZER), times(2)).invoke(
                "putNode", anyObject());
        PowerMockito.verifyPrivate(target, never()).invoke("addLinkOfLayerizerFromFlowOfLower",
                anyString(), anyObject());
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#
     * addLinkOfLayerizerFromFlowOfLower()}
     *
     * @throws Exception
     */
    @Test
    public void testAddLinkOfLayerizerFromFlowOfLowerFlowStatusEstablishing() throws Exception {
        String networkId = NETWORK_ID_UPPER;
        flow1.setStatus(FlowStatus.ESTABLISHING.toString());

        PowerMockito.doNothing().when(target, "addLinkOfLayerizerFromFlow", anyString(),
                anyObject());
        PowerMockito.doNothing().when(target, "updateLinkOfLayerizerFromFlow", anyString(),
                anyObject());

        Whitebox.invokeMethod(target, "addLinkOfLayerizerFromFlowOfLower", networkId, flow1);

        PowerMockito.verifyPrivate(target, times(1)).invoke("addLinkOfLayerizerFromFlow",
                anyString(), anyObject());

    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#
     * addLinkOfLayerizerFromFlowOfLower()}
     *
     * @throws Exception
     */
    @Test
    public void testAddLinkOfLayerizerFromFlowOfLowerFlowStatusEstablished() throws Exception {
        String networkId = NETWORK_ID_UPPER;
        flow1.setStatus(FlowStatus.ESTABLISHED.toString());

        PowerMockito.doNothing().when(target, "addLinkOfLayerizerFromFlow", anyString(),
                anyObject());
        PowerMockito.doNothing().when(target, "updateLinkOfLayerizerFromFlow", anyString(),
                anyObject());

        Whitebox.invokeMethod(target, "addLinkOfLayerizerFromFlowOfLower", networkId, flow1);

        PowerMockito.verifyPrivate(target, times(1)).invoke("updateLinkOfLayerizerFromFlow",
                anyString(), anyObject());

    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#addLinkOfLayerizerFromFlow()}
     * @throws Exception
     */
    @Test
    public void testAddLinkOfLayerizerFromFlowStatusEstablishing() throws Exception {
        String networkId = NETWORK_ID_LOWER;
        flow1.setStatus(FlowStatus.ESTABLISHING.toString());

        Map<String, String> layerizedlinks = new HashMap<String, String>();
        Whitebox.setInternalState(target, "layerizedlinks", layerizedlinks);
        Whitebox.setInternalState(target, "lowerflows", new HashMap<String, String>());
        PowerMockito.doReturn(DEFAULT_RESPONSE).when(networkInterfaces.get(NETWORK_ID_LAYERIZER),
                "postLink", anyObject());

        PowerMockito.doReturn(link1).when(target, "createLinkFromFlow", anyObject());
        PowerMockito.doNothing().when(target, "putLowerFlows", anyString(), anyString());
        PowerMockito.doNothing().when(target, "putLayerizedlinks", anyString(), anyString());

        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);
        PowerMockito.doReturn("dummy").when(target, "getUniqueLinkId", any());

        Whitebox.invokeMethod(target, "addLinkOfLayerizerFromFlow", networkId, flow1);

        PowerMockito.verifyPrivate(networkInterfaces.get(NETWORK_ID_LAYERIZER),
                times(1)).invoke("postLink", anyObject());
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#addLinkOfLayerizerFromFlow()}
     * @throws Exception
     */
    @Test
    public void testAddLinkOfLayerizerFromFlowStatusEstablished() throws Exception {
        String networkId = NETWORK_ID_LOWER;
        flow1.setStatus(FlowStatus.ESTABLISHED.toString());
        ((BasicFlow) flow1).putAttribute(TableManager.TRANSACTION_ID, "1");

        Map<String, String> layerizedlinks = new HashMap<String, String>();
        layerizedlinks.put(flow1.getFlowId(), "linkid");
        Whitebox.setInternalState(target, "layerizedlinks", layerizedlinks);
        Whitebox.setInternalState(target, "lowerflows", new HashMap<String, String>());
        PowerMockito.doReturn(link1).when(networkInterfaces.get(NETWORK_ID_LAYERIZER),
                "getLink", anyObject());
        PowerMockito.doReturn(DEFAULT_RESPONSE).when(networkInterfaces.get(NETWORK_ID_LAYERIZER),
                "putLink", anyObject());

        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        Whitebox.invokeMethod(target, "addLinkOfLayerizerFromFlow", networkId, flow1);

        PowerMockito.verifyPrivate(networkInterfaces.get(NETWORK_ID_LAYERIZER),
                times(1)).invoke("putLink", anyObject());
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer
     * #updateLinkOfLayerizerFromFlow()}
     * @throws Exception
     */
    @Test
    public void testUpdateLinkOfLayerizerFromFlow() throws Exception {
        String networkId = NETWORK_ID_LOWER;
        flow1.setStatus(FlowStatus.ESTABLISHED.toString());
        ((BasicFlow) flow1).putAttribute(TableManager.TRANSACTION_ID, "1");

        Map<String, String> layerizedlinks = new HashMap<String, String>();
        layerizedlinks.put(flow1.getFlowId(), "linkid");
        Whitebox.setInternalState(target, "layerizedlinks", layerizedlinks);
        Whitebox.setInternalState(target, "lowerflows", new HashMap<String, String>());
        PowerMockito.doReturn(link1).when(networkInterfaces.get(NETWORK_ID_LAYERIZER),
                "getLink", anyObject());
        PowerMockito.doReturn(DEFAULT_RESPONSE).when(networkInterfaces.get(NETWORK_ID_LAYERIZER),
                "putLink", anyObject());

        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        Whitebox.invokeMethod(target, "updateLinkOfLayerizerFromFlow", networkId, flow1);

        PowerMockito.verifyPrivate(networkInterfaces.get(NETWORK_ID_LAYERIZER),
                times(1)).invoke("putLink", anyObject());
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer
     * #updateLinkOfLayerizerFromFlow()}
     * @throws Exception
     */
    @Test
    public void testUpdateLinkOfLayerizerFromFlowNotSetLink() throws Exception {
        String networkId = NETWORK_ID_LOWER;
        flow1.setStatus(FlowStatus.ESTABLISHED.toString());
        ((BasicFlow) flow1).putAttribute(TableManager.TRANSACTION_ID, "1");

        Map<String, String> layerizedlinks = new HashMap<String, String>();
        Whitebox.setInternalState(target, "layerizedlinks", layerizedlinks);
        Whitebox.setInternalState(target, "lowerflows", new HashMap<String, String>());
        PowerMockito.doReturn(link1).when(networkInterfaces.get(NETWORK_ID_LAYERIZER),
                "getLink", anyObject());
        PowerMockito.doReturn(DEFAULT_RESPONSE).when(networkInterfaces.get(NETWORK_ID_LAYERIZER),
                "postLink", anyObject());
        PowerMockito.doReturn(DEFAULT_RESPONSE).when(networkInterfaces.get(NETWORK_ID_LAYERIZER),
                "putLink", anyObject());

        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        Whitebox.invokeMethod(target, "updateLinkOfLayerizerFromFlow", networkId, flow1);

        PowerMockito.verifyPrivate(networkInterfaces.get(NETWORK_ID_LAYERIZER),
                never()).invoke("postLink", anyObject());
        PowerMockito.verifyPrivate(networkInterfaces.get(NETWORK_ID_LAYERIZER),
                never()).invoke("putLink", anyObject());
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#createLinkFromFlow()}
     * @throws Exception
     */
    @Test
    public void testCreateLinkFromFlow() throws Exception {
        PowerMockito.doReturn(null).when(networkInterfaces.get(NETWORK_ID_LAYERIZER),
                "getLink", anyObject());

        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        Whitebox.invokeMethod(target, "putBoundary", "boundary1", boundary1);
        Whitebox.invokeMethod(target, "putBoundary", "boundary2", boundary2);
        Whitebox.invokeMethod(target, "putBoundary", "boundary3", boundary3);
        Whitebox.invokeMethod(target, "putBoundary", "boundary4", boundary4);
        Whitebox.invokeMethod(target, "putBoundary", "boundary5", boundary5);
        Whitebox.invokeMethod(target, "putBoundary", "boundary6", boundary6);

        Link link = Whitebox.invokeMethod(target, "createLinkFromFlow", flow1);

        link.setId("dummy");

        assertTrue(link.validate());

    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#createLinkFromFlow()}
     * @throws Exception
     */
    @Test
    public void testCreateLinkFromFlowEdgeSizeOver() throws Exception {
        PowerMockito.doReturn(null).when(networkInterfaces.get(NETWORK_ID_LAYERIZER),
                "getLink", anyObject());

        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        ((BasicFlow) flow1).edgeActions.clear();
        ((BasicFlow) flow1).edgeActions.put("test", new ArrayList<FlowAction>() {
            {
                add(new FlowActionOutput("1"));
                add(new FlowActionOutput("2"));
            }
        });
        ((BasicFlow) flow1).edgeActions.put("test2", new ArrayList<FlowAction>() {
            {
                add(new FlowActionOutput("1"));
                add(new FlowActionOutput("2"));
            }
        });

        Whitebox.invokeMethod(target, "putBoundary", "boundary1", boundary1);
        Whitebox.invokeMethod(target, "putBoundary", "boundary2", boundary2);
        Whitebox.invokeMethod(target, "putBoundary", "boundary3", boundary3);
        Whitebox.invokeMethod(target, "putBoundary", "boundary4", boundary4);
        Whitebox.invokeMethod(target, "putBoundary", "boundary5", boundary5);
        Whitebox.invokeMethod(target, "putBoundary", "boundary6", boundary6);
        Link link = Whitebox.invokeMethod(target, "createLinkFromFlow", flow1);

        assertNull(link);

    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#createLinkFromFlow()}
     * @throws Exception
     */
    @Test
    public void testCreateLinkFromFlowEdgeUnMatch() throws Exception {
        PowerMockito.doReturn(null).when(networkInterfaces.get(NETWORK_ID_LAYERIZER),
                "getLink", anyObject());

        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        ((BasicFlow) flow1).edgeActions.clear();
        ((BasicFlow) flow1).edgeActions.put("test", new ArrayList<FlowAction>() {
            {
                add(new FlowActionOutput("1"));
            }
        });

        Whitebox.invokeMethod(target, "putBoundary", "boundary1", boundary1);
        Whitebox.invokeMethod(target, "putBoundary", "boundary2", boundary2);
        Whitebox.invokeMethod(target, "putBoundary", "boundary3", boundary3);
        Whitebox.invokeMethod(target, "putBoundary", "boundary4", boundary4);
        Whitebox.invokeMethod(target, "putBoundary", "boundary5", boundary5);
        Whitebox.invokeMethod(target, "putBoundary", "boundary6", boundary6);
        Link link = Whitebox.invokeMethod(target, "createLinkFromFlow", flow1);

        assertNull(link);

    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#createLinkFromFlow()}
     * @throws Exception
     */
    @Test
    public void testCreateLinkFromFlowMatchesUnMatch() throws Exception {
        PowerMockito.doReturn(null).when(networkInterfaces.get(NETWORK_ID_LAYERIZER),
                "getLink", anyObject());

        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        ((BasicFlow) flow1).matches.clear();
        ((BasicFlow) flow1).matches.add(new BasicFlowMatch("test", "test"));

        Whitebox.invokeMethod(target, "putBoundary", "boundary1", boundary1);
        Whitebox.invokeMethod(target, "putBoundary", "boundary2", boundary2);
        Whitebox.invokeMethod(target, "putBoundary", "boundary3", boundary3);
        Whitebox.invokeMethod(target, "putBoundary", "boundary4", boundary4);
        Whitebox.invokeMethod(target, "putBoundary", "boundary5", boundary5);
        Whitebox.invokeMethod(target, "putBoundary", "boundary6", boundary6);
        Link link = Whitebox.invokeMethod(target, "createLinkFromFlow", flow1);

        assertNull(link);

    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#getUniqueLinkId()}
     * @throws Exception
     */
    @Test
    public void testGetUniqueLinkId() throws Exception {

        NetworkInterface nwif = PowerMockito.mock(NetworkInterface.class);
        when(nwif.getLink(anyString())).thenReturn(link1).thenReturn(null);

        networkInterfaces.put(NETWORK_ID_LAYERIZER, nwif);
        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);

        Whitebox.invokeMethod(target, "getUniqueLinkId", NETWORK_ID_LAYERIZER);

        PowerMockito.verifyPrivate(nwif, times(2)).invoke("getLink", anyObject());

    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#addFlowToUpper()}
     * @throws Exception
     */
    @Test
    public void testAddFlowToUpper() throws Exception {
        PowerMockito.doNothing().when(target, "addFlowAndSync",
                anyString(), anyObject(), anyString());

        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);
        Whitebox.invokeMethod(target, "addFlowToUpper", NETWORK_ID_LAYERIZER, flow1);
        PowerMockito.verifyPrivate(target).invoke("addFlowAndSync", NETWORK_ID_LAYERIZER, flow1,
                NETWORK_ID_UPPER);
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#addFlowAndSync()}
     * @throws Exception
     */
    @Test
    public void testAddFlowAndSyncTargetLayerizer() throws Exception {
        String orgNetworkId = NETWORK_ID_UPPER;
        Flow flow = new BasicFlow("flowId");

        NetworkInterface nwIf = PowerMockito.mock(NetworkInterface.class);
        when(nwIf.getFlow(flow.getFlowId())).thenReturn(flow);

        networkInterfaces.put(orgNetworkId, nwIf);

        PowerMockito.doReturn(DEFAULT_RESPONSE).when(networkInterfaces.get(NETWORK_ID_LAYERIZER),
                "putFlow", anyObject());
        PowerMockito.doNothing().when(conversionTable, "addEntryFlow",
                anyString(), anyString(), anyString(), anyString());

        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        Whitebox.invokeMethod(target, "addFlowAndSync",
                orgNetworkId, flow, NETWORK_ID_LAYERIZER);

        PowerMockito.verifyPrivate(networkInterfaces.get(NETWORK_ID_LAYERIZER), times(1)).invoke(
                "putFlow", anyObject());
        PowerMockito.verifyPrivate(conversionTable).invoke("addEntryFlow",
                anyString(), anyString(), anyString(), anyString());
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#addFlowAndSync()}
     * @throws Exception
     */
    @Test
    public void testAddFlowAndSyncTargetUpper() throws Exception {
        String orgNetworkId = NETWORK_ID_LAYERIZER;
        Flow flow = new BasicFlow("flowId");

        NetworkInterface nwIf = PowerMockito.mock(NetworkInterface.class);
        when(nwIf.getFlow(flow.getFlowId())).thenReturn(flow);

        networkInterfaces.put(orgNetworkId, nwIf);

        PowerMockito.doReturn(DEFAULT_RESPONSE).when(networkInterfaces.get(NETWORK_ID_UPPER),
                "putFlow", anyObject());
        PowerMockito.doNothing().when(conversionTable, "addEntryFlow",
                anyString(), anyString(), anyString(), anyString());

        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);
        Whitebox.invokeMethod(target, "addFlowAndSync",
                NETWORK_ID_LAYERIZER, flow, NETWORK_ID_UPPER);

        PowerMockito.verifyPrivate(networkInterfaces.get(NETWORK_ID_UPPER), times(1)).invoke(
                "putFlow", anyObject());
        PowerMockito.verifyPrivate(conversionTable).invoke("addEntryFlow",
                anyString(), anyString(), anyString(), anyString());
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#addUpperLink()}
     * @throws Exception
     */
    @Test
    public void testAddUpperLink() throws Exception {

        PowerMockito.doNothing().when(target, "registerLinkAndSync",
                anyString(), anyString(), anyObject());
        PowerMockito.doNothing().when(target, "uppdateLowerFlowFromLayerizerLink",
                anyObject());

        link1.putAttribute(AttrElements.ESTABLISHMENT_STATUS, "establishing");
        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);
        Whitebox.invokeMethod(target, "addUpperLink", NETWORK_ID_LAYERIZER, link1);

        PowerMockito.verifyPrivate(target, times(1)).invoke("registerLinkAndSync",
                anyString(), anyString(), anyObject());

    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#addUpperLink()}
     * @throws Exception
     */
    @Test
    public void testAddUpperLinkNotSetStatus() throws Exception {

        PowerMockito.doNothing().when(target, "registerLinkAndSync",
                anyString(), anyString(), anyObject());
        PowerMockito.doNothing().when(target, "uppdateLowerFlowFromLayerizerLink",
                anyObject());

        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);
        link1.getAttributes().remove(AttrElements.ESTABLISHMENT_STATUS);
        Whitebox.invokeMethod(target, "addUpperLink", NETWORK_ID_LAYERIZER, link1);

        PowerMockito.verifyPrivate(target, never()).invoke("registerLinkAndSync",
                anyString(), anyString(), anyObject());

    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#
     * uppdateLowerFlowFromLayerizerLink()}
     * @throws Exception
     */
    @Test
    public void testUppdateLowerFlowFromLayerizerLink() throws Exception {
        PowerMockito.doReturn(DEFAULT_RESPONSE).when(networkInterfaces.get(NETWORK_ID_LOWER),
                "putFlow", anyObject());

        PowerMockito.doReturn((BasicFlow) flow1).when(target, "linkToFlow",
                anyObject(), anyObject());

        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);
        link1.putAttribute(AttrElements.ESTABLISHMENT_STATUS, "establishing");
        Whitebox.invokeMethod(target, "uppdateLowerFlowFromLayerizerLink", link1);

        PowerMockito.verifyPrivate(networkInterfaces.get(NETWORK_ID_LOWER), times(1)).invoke(
                "putFlow", anyObject());
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#
     * uppdateLowerFlowFromLayerizerLink()}
     * @throws Exception
     */
    @Test
    public void testUppdateLowerFlowFromLayerizerLinkStatusEstablished() throws Exception {
        PowerMockito.doReturn(DEFAULT_RESPONSE).when(networkInterfaces.get(NETWORK_ID_LOWER),
                "putFlow", anyObject());

        PowerMockito.doReturn((BasicFlow) flow1).when(target, "linkToFlow",
                anyObject(), anyObject());

        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);
        Whitebox.invokeMethod(target, "uppdateLowerFlowFromLayerizerLink", link1);

        PowerMockito.verifyPrivate(networkInterfaces.get(NETWORK_ID_LOWER), never()).invoke(
                "putFlow", anyObject());
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#registerLinkAndSync()}
     * @throws Exception
     */
    @Test
    public void testRegisterLinkAndSync() throws Exception {

        PowerMockito.doReturn(DEFAULT_RESPONSE).when(networkInterfaces.get(NETWORK_ID_UPPER),
                "putLink", anyObject());
        PowerMockito.doNothing().when(conversionTable, "addEntryFlow",
                anyString(), anyString(), anyString(), anyString());

        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);
        Whitebox.invokeMethod(target, "registerLinkAndSync",
                NETWORK_ID_LAYERIZER, NETWORK_ID_UPPER, link1);

        PowerMockito.verifyPrivate(networkInterfaces.get(NETWORK_ID_UPPER), times(1)).invoke(
                "putLink", anyObject());
        PowerMockito.verifyPrivate(conversionTable).invoke("addEntryLink",
                anyString(), anyString(), anyString(), anyString());
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#delLowerFlows()}
     * @throws Exception
     */
    @Test
    public void testDelLowerFlows() throws Exception {
        Map<String, String> layerizedlinks = new HashMap<String, String>();
        Map<String, ArrayList<String>> lowerflows = new HashMap<String, ArrayList<String>>();
        layerizedlinks.put("FlowId1", link1.getId());
        layerizedlinks.put("FlowId2", link1.getId());
        layerizedlinks.put("FlowId3", link1.getId());
        lowerflows.put(link1.getId(), new ArrayList<String>() {
            {
                add("FlowId1");
                add("FlowId2");
                add("FlowId3");
            }
        });

        PowerMockito.doReturn(DEFAULT_RESPONSE).when(networkInterfaces.get(NETWORK_ID_LOWER),
                "delFlow", anyObject());
        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        Whitebox.setInternalState(target, "layerizedlinks", layerizedlinks);
        Whitebox.setInternalState(target, "lowerflows", lowerflows);

        Whitebox.invokeMethod(target, "delLowerFlows", link1.getId());

        PowerMockito.verifyPrivate(networkInterfaces.get(NETWORK_ID_LOWER), times(3)).invoke(
                "delFlow", anyObject());

    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#delLowerFlows()}
     * @throws Exception
     */
    @Test
    public void testDelLowerFlowsNotSet() throws Exception {
        Map<String, String> layerizedlinks = new HashMap<String, String>();
        Map<String, ArrayList<String>> lowerflows = new HashMap<String, ArrayList<String>>();

        PowerMockito.doReturn(DEFAULT_RESPONSE).when(networkInterfaces.get(NETWORK_ID_LOWER),
                "delFlow", anyObject());
        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        Whitebox.setInternalState(target, "layerizedlinks", layerizedlinks);
        Whitebox.setInternalState(target, "lowerflows", lowerflows);

        Whitebox.invokeMethod(target, "delLowerFlows", link1.getId());

        PowerMockito.verifyPrivate(networkInterfaces.get(NETWORK_ID_LOWER), never()).invoke(
                "delFlow", anyObject());

    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#putLowerFlows()}
     * @throws Exception
     */
    @Test
    public void testPutLowerFlowsAdd() throws Exception {
        Whitebox.invokeMethod(target, "putLowerFlows", link1.getId(), flow1.getFlowId());
        Whitebox.invokeMethod(target, "putLowerFlows", link1.getId(), flow2.getFlowId());
        Map<String, ArrayList<String>> lowerflows = Whitebox.getInternalState(target, "lowerflows");
        Map<String, ArrayList<String>> checkLowerflows = new HashMap<String, ArrayList<String>>() {
            {
                put(link1.getId(), new ArrayList<String>() {
                    {
                        add(flow1.getFlowId());
                        add(flow2.getFlowId());
                    }
                });
            }
        };
        assertEquals(lowerflows, checkLowerflows);
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#putLowerFlows()}
     * @throws Exception
     */
    @Test
    public void testPutLowerFlowsNotSet() throws Exception {
        Whitebox.invokeMethod(target, "putLowerFlows", link1.getId(), flow1.getFlowId());
        Map<String, ArrayList<String>> lowerflows = Whitebox.getInternalState(target, "lowerflows");
        Map<String, ArrayList<String>> checkLowerflows = new HashMap<String, ArrayList<String>>() {
            {
                put(link1.getId(), new ArrayList<String>() {
                    {
                        add(flow1.getFlowId());
                    }
                });
            }
        };
        assertEquals(lowerflows, checkLowerflows);
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#putLayerizedlinks()}
     * @throws Exception
     */
    @Test
    public void testPutLayerizedlinksPut() throws Exception {
        Whitebox.invokeMethod(target, "putLayerizedlinks", link1.getId(), flow1.getFlowId());
        Whitebox.invokeMethod(target, "putLayerizedlinks", link1.getId(), flow2.getFlowId());
        Map<String, String> layerizedlinks = Whitebox.getInternalState(target, "layerizedlinks");
        Map<String, String> checkLayerizedlinks = new HashMap<String, String>() {
            {
                put(flow1.getFlowId(), link1.getId());
                put(flow2.getFlowId(), link1.getId());
            }
        };
        assertEquals(layerizedlinks, checkLayerizedlinks);
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#putLayerizedlinks()}
     * @throws Exception
     */
    @Test
    public void testPutLayerizedlinksNotSet() throws Exception {
        Whitebox.invokeMethod(target, "putLayerizedlinks", link1.getId(), flow1.getFlowId());
        Map<String, String> layerizedlinks = Whitebox.getInternalState(target, "layerizedlinks");
        Map<String, String> checkLayerizedlinks = new HashMap<String, String>() {
            {
                put(flow1.getFlowId(), link1.getId());
            }
        };
        assertEquals(layerizedlinks, checkLayerizedlinks);
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#linkToFlow()}
     * @throws Exception
     */
    @Test
    public void testLinkToFlow() throws Exception {

        PowerMockito.doReturn(null).when(networkInterfaces.get(NETWORK_ID_LOWER), "getFlowSet");

        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        Whitebox.invokeMethod(target, "putBoundary", "boundary5", boundary5);
        Whitebox.invokeMethod(target, "putBoundary", "boundary6", boundary6);
        Whitebox.invokeMethod(target, "putBoundary", "boundary7", boundary7);
        Whitebox.invokeMethod(target, "putBoundary", "boundary8", boundary8);

        BasicFlow flow = Whitebox.invokeMethod(target, "linkToFlow",
                networkInterfaces.get(NETWORK_ID_LOWER), link1);
        Map<String, List<FlowAction>> checkEdge = new HashMap<String, List<FlowAction>>() {
            {
                put("NW=SDN,NE=PT21", new ArrayList<FlowAction>() {
                    {
                        add(new FlowActionOutput("NW=SDN,NE=PT21,Layer=Ether,TTP=1"));
                    }
                });
            }
        };

        List<BasicFlowMatch> checkFlowMatch = new ArrayList<BasicFlowMatch>() {
            {
                add(new BasicFlowMatch("NW=SDN,NE=PT11",
                        "NW=SDN,NE=PT11,Layer=Ether,TTP=1"));
            }
        };
        assertEquals(flow.getEdgeActions(), checkEdge);
        assertEquals(flow.getMatches(), checkFlowMatch);
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#changePortBoundaryUppreToLow()}
     * @throws Exception
     */
    @Test
    public void testChangePortBoundaryUppreToLow() throws Exception {
        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        Whitebox.invokeMethod(target, "putBoundary", "boundary5", boundary5);
        Whitebox.invokeMethod(target, "putBoundary", "boundary6", boundary6);
        Whitebox.invokeMethod(target, "putBoundary", "boundary7", boundary7);
        Whitebox.invokeMethod(target, "putBoundary", "boundary8", boundary8);

        String[] result = Whitebox.invokeMethod(target, "changePortBoundaryUppreToLow",
                port1.getNode(), port1.getId());

        assertEquals(result[0], "NW=SDN,NE=PT11");
        assertEquals(result[1], "NW=SDN,NE=PT11,Layer=Ether,TTP=1");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#changePortBoundaryUppreToLow()}
     * @throws Exception
     */
    @Test
    public void testChangePortBoundaryUppreToLowNodeUnMatch() throws Exception {
        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        Whitebox.invokeMethod(target, "putBoundary", "boundary5", boundary5);
        Whitebox.invokeMethod(target, "putBoundary", "boundary6", boundary6);
        Whitebox.invokeMethod(target, "putBoundary", "boundary7", boundary7);
        Whitebox.invokeMethod(target, "putBoundary", "boundary8", boundary8);

        String[] result = Whitebox.invokeMethod(target, "changePortBoundaryUppreToLow",
                port1.getNode() + "a", port1.getId());

        assertEquals(result[0], "");
        assertEquals(result[1], "");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#changePortBoundaryUppreToLow()}
     * @throws Exception
     */
    @Test
    public void testChangePortBoundaryUppreToLowPortUnMatch() throws Exception {
        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        Whitebox.invokeMethod(target, "putBoundary", "boundary5", boundary5);
        Whitebox.invokeMethod(target, "putBoundary", "boundary6", boundary6);
        Whitebox.invokeMethod(target, "putBoundary", "boundary7", boundary7);
        Whitebox.invokeMethod(target, "putBoundary", "boundary8", boundary8);

        String[] result = Whitebox.invokeMethod(target, "changePortBoundaryUppreToLow",
                port1.getNode(), port1.getId() + "a");

        assertEquals(result[0], "");
        assertEquals(result[1], "");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#changePortBoundaryUppreToLow()}
     * @throws Exception
     */
    @Test
    public void testChangePortBoundaryUppreToLowNotSetBoundary() throws Exception {
        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        String[] result = Whitebox.invokeMethod(target, "changePortBoundaryUppreToLow",
                port1.getNode(), port1.getId());

        assertEquals(result[0], "");
        assertEquals(result[1], "");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#changePortBoundaryLowToUpper()}
     * @throws Exception
     */
    @Test
    public void testChangePortBoundaryLowToUpper() throws Exception {
        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        Whitebox.invokeMethod(target, "putBoundary", "boundary1", boundary1);
        Whitebox.invokeMethod(target, "putBoundary", "boundary2", boundary2);
        Whitebox.invokeMethod(target, "putBoundary", "boundary3", boundary3);
        Whitebox.invokeMethod(target, "putBoundary", "boundary4", boundary4);

        String[] result = Whitebox.invokeMethod(target, "changePortBoundaryLowToUpper",
                port1.getNode(), port1.getId());

        assertEquals(result[0], "NW=SDN,NE=PT11");
        assertEquals(result[1], "NW=SDN,NE=PT11,Layer=Ether,TTP=1");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#changePortBoundaryLowToUpper()}
     * @throws Exception
     */
    @Test
    public void testChangePortBoundaryLowToUpperNodeUnMatch() throws Exception {
        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        Whitebox.invokeMethod(target, "putBoundary", "boundary1", boundary1);
        Whitebox.invokeMethod(target, "putBoundary", "boundary2", boundary2);
        Whitebox.invokeMethod(target, "putBoundary", "boundary3", boundary3);
        Whitebox.invokeMethod(target, "putBoundary", "boundary4", boundary4);

        String[] result = Whitebox.invokeMethod(target, "changePortBoundaryLowToUpper",
                port1.getNode() + "a", port1.getId());

        assertEquals(result[0], "");
        assertEquals(result[1], "");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#changePortBoundaryLowToUpper()}
     * @throws Exception
     */
    @Test
    public void testChangePortBoundaryLowToUpperPortUnMatch() throws Exception {
        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        Whitebox.invokeMethod(target, "putBoundary", "boundary1", boundary1);
        Whitebox.invokeMethod(target, "putBoundary", "boundary2", boundary2);
        Whitebox.invokeMethod(target, "putBoundary", "boundary3", boundary3);
        Whitebox.invokeMethod(target, "putBoundary", "boundary4", boundary4);

        String[] result = Whitebox.invokeMethod(target, "changePortBoundaryLowToUpper",
                port1.getNode(), port1.getId() + "a");

        assertEquals(result[0], "");
        assertEquals(result[1], "");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#changePortBoundaryLowToUpper()}
     * @throws Exception
     */
    @Test
    public void testChangePortBoundaryLowToUpperNotSetBoundary() throws Exception {
        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        String[] result = Whitebox.invokeMethod(target, "changePortBoundaryLowToUpper",
                port1.getNode(), port1.getId());

        assertEquals(result[0], "");
        assertEquals(result[1], "");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#checkUpperEvent()}
     * @throws Exception
     */
    @Test
    public void testCheckUpperEventUpper() throws Exception {

        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        boolean result = Whitebox.invokeMethod(target, "checkUpperEvent", NETWORK_ID_UPPER);
        assertTrue(result);
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#checkUpperEvent()}
     * @throws Exception
     */
    @Test
    public void testCheckUpperEventUpperNotSetLayerizer() throws Exception {

        conversionTable.delEntryConnectionType(NETWORK_ID_LAYERIZER);

        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        boolean result = Whitebox.invokeMethod(target, "checkUpperEvent", NETWORK_ID_UPPER);
        assertFalse(result);
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#checkUpperEvent()}
     * @throws Exception
     */
    @Test
    public void testCheckUpperEventLower() throws Exception {

        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        boolean result = Whitebox.invokeMethod(target, "checkUpperEvent", NETWORK_ID_LOWER);
        assertFalse(result);
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#checkUpperEvent()}
     * @throws Exception
     */
    @Test
    public void testCheckUpperEventLayerizer() throws Exception {

        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        boolean result = Whitebox.invokeMethod(target, "checkUpperEvent", NETWORK_ID_LAYERIZER);
        assertFalse(result);
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#checkLayerizerEvent()}
     * @throws Exception
     */
    @Test
    public void testCheckLayerizerEventUpper() throws Exception {
        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        boolean result = Whitebox.invokeMethod(target, "checkLayerizerEvent", NETWORK_ID_UPPER,
                link1);
        assertFalse(result);
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#checkLayerizerEvent()}
     * @throws Exception
     */
    @Test
    public void testCheckLayerizerEventLower() throws Exception {
        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        boolean result = Whitebox.invokeMethod(target, "checkLayerizerEvent", NETWORK_ID_LOWER,
                link1);
        assertFalse(result);
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#checkLayerizerEvent()}
     * @throws Exception
     */
    @Test
    public void testCheckLayerizerEventLayerizer() throws Exception {
        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        boolean result = Whitebox.invokeMethod(target, "checkLayerizerEvent", NETWORK_ID_LAYERIZER,
                link1);
        assertTrue(result);
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#checkLayerizerEvent()}
     * @throws Exception
     */
    @Test
    public void testCheckLayerizerEventLayerizerNotSetUpper() throws Exception {

        conversionTable.delEntryConnectionType(NETWORK_ID_UPPER);

        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        boolean result = Whitebox.invokeMethod(target, "checkLayerizerEvent", NETWORK_ID_LAYERIZER,
                link1);
        assertFalse(result);
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#checkLayerizerEvent()}
     * @throws Exception
     */
    @Test
    public void testCheckLayerizerEventLayerizerLinkValidateFailure() throws Exception {

        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        link1.setPorts(null, null, null, null);
        boolean result = Whitebox.invokeMethod(target, "checkLayerizerEvent",
                NETWORK_ID_LAYERIZER, link1);
        assertFalse(result);
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#getUpperNetworkId()}
     * @throws Exception
     */
    @Test
    public void testGetUpperNetworkId() throws Exception {
        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);
        String networkId = Whitebox.invokeMethod(target, "getUpperNetworkId");

        assertEquals(networkId, NETWORK_ID_UPPER);

    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#getUpperNetworkId()}
     * @throws Exception
     */
    @Test
    public void testGetUpperNetworkIdNotSetNetworkIds() throws Exception {

        String networkId = Whitebox.invokeMethod(target, "getUpperNetworkId");
        assertEquals(networkId, "");

    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#getLowerNetworkId()}
     * @throws Exception
     */
    @Test
    public void testGetLowerNetworkId() throws Exception {
        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);
        String networkId = Whitebox.invokeMethod(target, "getLowerNetworkId");

        assertEquals(networkId, NETWORK_ID_LOWER);

    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#getLowerNetworkId()}
     * @throws Exception
     */
    @Test
    public void testGetLowerNetworkIdNotSetNetworkIds() throws Exception {

        String networkId = Whitebox.invokeMethod(target, "getLowerNetworkId");
        assertEquals(networkId, "");

    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#getLayerizerNetworkId()}
     * @throws Exception
     */
    @Test
    public void testGetLayerizerNetworkId() throws Exception {
        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);
        String networkId = Whitebox.invokeMethod(target, "getLayerizerNetworkId");

        assertEquals(networkId, NETWORK_ID_LAYERIZER);

    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#getLayerizerNetworkId()}
     * @throws Exception
     */
    @Test
    public void testGetLayerizerNetworkIdNotSetNetworkIds() throws Exception {

        String networkId = Whitebox.invokeMethod(target, "getLayerizerNetworkId");
        assertEquals(networkId, "");

    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#getNetworkIdSpecifyType()}
     * @throws Exception
     */
    @Test
    public void testGetNetworkIdSpecifyTypeUpper() throws Exception {
        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);
        String networkId = Whitebox.invokeMethod(target, "getNetworkIdSpecifyType", "upper");

        assertEquals(networkId, NETWORK_ID_UPPER);
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#getNetworkIdSpecifyType()}
     * @throws Exception
     */
    @Test
    public void testGetNetworkIdSpecifyTypeLower() throws Exception {
        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);
        String networkId = Whitebox.invokeMethod(target, "getNetworkIdSpecifyType", "lower");

        assertEquals(networkId, NETWORK_ID_LOWER);
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#getNetworkIdSpecifyType()}
     * @throws Exception
     */
    @Test
    public void testGetNetworkIdSpecifyTypeLayerizer() throws Exception {
        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);
        String networkId = Whitebox.invokeMethod(target, "getNetworkIdSpecifyType", "layerized");

        assertEquals(networkId, NETWORK_ID_LAYERIZER);
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onRequest()}
     */
    @Test
    public void testOnRequestSuccess() throws Exception {
        Method method = Request.Method.GET;
        ObjectProperty body = null;
        Request request = new Request("LinkLayerizer", method, "lower_flows", body);
        Response result = target.onRequest(request);
        assertThat(result.statusCode, is(Response.OK));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#onRequest()}
     */
    @Test
    public final void testOnRequestParseErr() {
        Method method = Request.Method.GET;
        Object body = new Object();
        Request request = new Request("LinkLayerizer", method, "settings/default_idle_timer", body);

        Response result = target.onRequest(request);

        assertThat(result.statusCode, is(Response.BAD_REQUEST));
        assertThat((String) WhiteboxImpl.getInternalState(result, "body"),
                is("Error unknown request "));

    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#postBoundary()}
     * @throws Exception
     */
    @Test
    public void testPostBoundary() throws Exception {
        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);
        String result = Whitebox.invokeMethod(target, "postBoundary", boundary1);

        String chkBoundary = "{\"type\": \"LinkLayerizerBoundary\","
                + "\"boundary_id\": \"boundary1\","
                + "\"lower_nw\": \"networkcomponent0\",\"lower_nw_node\": \"NW=SDN,NE=PT1\","
                + "\"lower_nw_port\": \"NW=SDN,NE=PT1,Layer=Ether,TTP=1\","
                + "\"upper_nw\": \"networkcomponent1\",\"upper_nw_node\": \"NW=SDN,NE=PT11\","
                + "\"upper_nw_port\": \"NW=SDN,NE=PT11,Layer=Ether,TTP=1\"}";

        ObjectMapper mapper = new ObjectMapper();

        assertEquals(mapper.readValue(result, HashMap.class),
                mapper.readValue(chkBoundary, HashMap.class));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#postBoundary()}
     * @throws Exception
     */
    @Test
    public void testPostBoundaryCheckBoundaryFailure() throws Exception {
        String result = Whitebox.invokeMethod(target, "postBoundary", boundary1);
        assertTrue(result.contains("Undefined Boundary."));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#putBoundary()}
     * @throws Exception
     */
    @Test
    public void testPutBoundary() throws Exception {
        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);
        String result = Whitebox.invokeMethod(target, "putBoundary",
                "boundary1", boundary1);

        String chkBoundary = "{\"type\": \"LinkLayerizerBoundary\","
                + "\"boundary_id\": \"boundary1\","
                + "\"lower_nw\": \"networkcomponent0\",\"lower_nw_node\": \"NW=SDN,NE=PT1\","
                + "\"lower_nw_port\": \"NW=SDN,NE=PT1,Layer=Ether,TTP=1\","
                + "\"upper_nw\": \"networkcomponent1\",\"upper_nw_node\": \"NW=SDN,NE=PT11\","
                + "\"upper_nw_port\": \"NW=SDN,NE=PT11,Layer=Ether,TTP=1\"}";

        ObjectMapper mapper = new ObjectMapper();

        assertEquals(mapper.readValue(result, HashMap.class),
                mapper.readValue(chkBoundary, HashMap.class));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#putBoundary()}
     * @throws Exception
     */
    @Test
    public void testPutBoundaryCheckBoundaryFailure() throws Exception {
        String result = Whitebox.invokeMethod(target, "putBoundary",
                "boundary1", boundary1);
        assertTrue(result.contains("Undefined Boundary."));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#delBoundary()}
     * @throws Exception
     */
    @Test
    public void testDelBoundary() throws Exception {
        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        LinkLayerizerBoundarySet boundaryset = new LinkLayerizerBoundarySet();
        Whitebox.setInternalState(target, "boundaryset", boundaryset);
        Whitebox.invokeMethod(target, "putBoundary", "boundary1", boundary1);

        assertThat(boundaryset.getBoundary("boundary1"),
                is(instanceOf(LinklayerizerBoundary.class)));

        Whitebox.invokeMethod(target, "delBoundary", "boundary1");
        assertNull(boundaryset.getBoundary("boundary1"));

    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#getFlows()}
     * @throws Exception
     */
    @Test
    public void testGetFlows() throws Exception {
        Map<String, ArrayList<String>> lowerflows = new HashMap<String, ArrayList<String>>();
        lowerflows.put(link1.getId(), new ArrayList<String>() {
            {
                add("FlowId1");
                add("FlowId2");
                add("FlowId3");
            }
        });
        Whitebox.setInternalState(target, "lowerflows", lowerflows);

        String result = Whitebox.invokeMethod(target, "getFlows");

        ObjectMapper mapper = new ObjectMapper();
        assertEquals(mapper.readValue(result, HashMap.class), lowerflows);

    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#getFlows()}
     * @throws Exception
     */
    @Test
    public void testGetFlowsNotSetLowerFlows() throws Exception {
        String result = Whitebox.invokeMethod(target, "getFlows");
        assertEquals(result, "{}");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#getFlows()}
     * @throws Exception
     */
    @Test
    public void testGetFlowsSetFlowId() throws Exception {
        Map<String, ArrayList<String>> lowerflows = new HashMap<String, ArrayList<String>>();
        lowerflows.put(link1.getId(), new ArrayList<String>() {
            {
                add("FlowId1");
                add("FlowId2");
                add("FlowId3");
            }
        });

        lowerflows.put(link2.getId(), new ArrayList<String>() {
            {
                add("FlowId4");
                add("FlowId5");
                add("FlowId6");
            }
        });

        Whitebox.setInternalState(target, "lowerflows", lowerflows);

        String result = Whitebox.invokeMethod(target, "getFlows", link2.getId());

        ArrayList<String> chkLowerflows = new ArrayList<String>() {
            {
                add("FlowId4");
                add("FlowId5");
                add("FlowId6");
            }
        };

        ObjectMapper mapper = new ObjectMapper();
        assertEquals(mapper.readValue(result, ArrayList.class), chkLowerflows);

    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#getLinks()}
     * @throws Exception
     */
    @Test
    public void testGetLinks() throws Exception {
        Map<String, String> layerizedlinks = new HashMap<String, String>();
        layerizedlinks.put(flow1.getFlowId(), link1.getId());
        layerizedlinks.put(flow2.getFlowId(), link1.getId());

        Whitebox.setInternalState(target, "layerizedlinks", layerizedlinks);

        String result = Whitebox.invokeMethod(target, "getLinks");

        ObjectMapper mapper = new ObjectMapper();
        assertEquals(mapper.readValue(result, HashMap.class), layerizedlinks);
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#getLinks()}
     * @throws Exception
     */
    @Test
    public void testGetLinksNotSetLayerizerLinks() throws Exception {

        String result = Whitebox.invokeMethod(target, "getLinks");

        assertEquals(result, "{}");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#getLinks()}
     * @throws Exception
     */
    @Test
    public void testGetLinksSetFlowId() throws Exception {
        Map<String, String> layerizedlinks = new HashMap<String, String>();
        layerizedlinks.put(flow1.getFlowId(), link1.getId());
        layerizedlinks.put(flow2.getFlowId(), link2.getId());

        Whitebox.setInternalState(target, "layerizedlinks", layerizedlinks);

        String result = Whitebox.invokeMethod(target, "getLinks", flow2.getFlowId());

        assertEquals(result, link2.getId());
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#getLinks()}
     * @throws Exception
     */
    @Test
    public void testGetLinksSetFlowId2() throws Exception {
        Map<String, String> layerizedlinks = new HashMap<String, String>();
        layerizedlinks.put(flow1.getFlowId(), link1.getId());
        layerizedlinks.put(flow2.getFlowId(), link2.getId());

        Whitebox.setInternalState(target, "layerizedlinks", layerizedlinks);

        String result = Whitebox.invokeMethod(target, "getLinks", "linkid");

        assertNull(result);
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#checkBoundary()}
     * @throws Exception
     */
    @Test
    public void testCheckBoundary() throws Exception {
        LinklayerizerBoundary boundary = new LinklayerizerBoundary();
        boundary.setLower_nw(NETWORK_ID_LOWER);
        boundary.setUpper_nw(NETWORK_ID_UPPER);

        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        boolean result = Whitebox.invokeMethod(target, "checkBoundary", boundary);

        assertTrue(result);
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#checkBoundary()}
     * @throws Exception
     */
    @Test
    public void testCheckBoundaryNotSetUpper() throws Exception {
        LinklayerizerBoundary boundary = new LinklayerizerBoundary();
        boundary.setLower_nw(NETWORK_ID_LOWER);
        boundary.setUpper_nw(NETWORK_ID_UPPER);

        conversionTable.delEntryConnectionType(NETWORK_ID_UPPER);

        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        boolean result = Whitebox.invokeMethod(target, "checkBoundary", boundary);

        assertFalse(result);
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#checkBoundary()}
     * @throws Exception
     */
    @Test
    public void testCheckBoundaryNotSetLower() throws Exception {
        LinklayerizerBoundary boundary = new LinklayerizerBoundary();
        boundary.setLower_nw(NETWORK_ID_LOWER);
        boundary.setUpper_nw(NETWORK_ID_UPPER);

        conversionTable.delEntryConnectionType(NETWORK_ID_LOWER);

        PowerMockito.doReturn(networkInterfaces).when(target, METHOD_NETWOEK_IF);
        PowerMockito.doReturn(conversionTable).when(target, METHOD_CONVERSITON_TABLE);

        boolean result = Whitebox.invokeMethod(target, "checkBoundary", boundary);

        assertFalse(result);
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.linklayerizer.LinkLayerizer#createBoundary()}
     * @throws Exception
     */
    @Test
    public void testCreateBoundary() throws Exception {
        LinklayerizerBoundary result = Whitebox.invokeMethod(target, "createBoundary",
                new JSONObject(boundary1));

        LinklayerizerBoundary boundary = new LinklayerizerBoundary();
        boundary.setBoundary_id("boundary1");
        boundary.setLower_nw("networkcomponent0");
        boundary.setLower_nw_node("NW=SDN,NE=PT1");
        boundary.setLower_nw_port("NW=SDN,NE=PT1,Layer=Ether,TTP=1");
        boundary.setUpper_nw("networkcomponent1");
        boundary.setUpper_nw_node("NW=SDN,NE=PT11");
        boundary.setUpper_nw_port("NW=SDN,NE=PT11,Layer=Ether,TTP=1");

        assertTrue(result.equals(boundary));

    }
}
