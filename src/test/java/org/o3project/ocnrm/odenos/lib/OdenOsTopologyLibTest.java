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
package org.o3project.ocnrm.odenos.lib;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.o3project.ocnrm.model.Link;
import org.o3project.ocnrm.model.Node;
import org.o3project.ocnrm.model.Port;
import org.o3project.odenos.core.component.network.topology.Topology;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ OdenOsTopologyLib.class })
public class OdenOsTopologyLibTest {
    private OdenOsTopologyLib target;

    @Before
    public void setUp() throws Exception {
        target = PowerMockito.spy(new OdenOsTopologyLib());
    }

    @After
    public void tearDown() throws Exception {
        target = null;
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.lib.OdenOsTopologyLib#createTopology()}
     */
    @Test
    public void testCreateTopology() throws Exception {
        String node1Id = "NW=nw,NE=ne1";
        String node2Id = "NW=nw,NE=ne2";
        String port1Id = "NW=nw,NE=ne1,Layer=layer,TTP=ttp";
        String port2Id = "NW=nw,NE=ne2,Layer=layer,TTP=ttp";

        Node node1 = new Node();
        node1.setNodeId(node1Id);
        Node node2 = new Node();
        node2.setNodeId(node2Id);
        List<org.o3project.ocnrm.model.Node> nodes = new ArrayList<>();
        nodes.add(node1);
        nodes.add(node2);

        Port port1 = new Port();
        port1.setPortId(port1Id);
        Port port2 = new Port();
        port2.setPortId(port2Id);
        List<org.o3project.ocnrm.model.Port> ports = new ArrayList<>();
        ports.add(port1);
        ports.add(port2);

        Link link = new Link();
        link.setLinkId("linkId");
        link.setSrcTTP(port1Id);
        link.setDstTTP(port2Id);
        List<org.o3project.ocnrm.model.Link> links = new ArrayList<>();
        links.add(link);

        Topology result = target.createTopology(nodes, ports, links);

        assertThat(result.getNodeMap().size(), is(2));
        assertThat(result.getNodeMap().get(node1Id).getId(), is(node1Id));
        assertThat(result.getNodeMap().get(node2Id).getId(), is(node2Id));

        assertThat(result.getPort(node1Id, port1Id).getId(), is(port1Id));
        assertThat(result.getPort(node2Id, port2Id).getId(), is(port2Id));

        assertThat(result.getLinkMap().size(), is(1));
        assertThat(result.getLinkMap().get("linkId").getId(), is("linkId"));
        assertThat(result.getLinkMap().get("linkId").getSrcNode(), is(node1Id));
        assertThat(result.getLinkMap().get("linkId").getSrcPort(), is(port1Id));
        assertThat(result.getLinkMap().get("linkId").getDstNode(), is(node2Id));
        assertThat(result.getLinkMap().get("linkId").getDstPort(), is(port2Id));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.lib.OdenOsTopologyLib#createTopology()}
     */
    @Test
    public void testCreateTopologyWithNullNodes() throws Exception {
        List<org.o3project.ocnrm.model.Port> ports = new ArrayList<>();
        List<org.o3project.ocnrm.model.Link> links = new ArrayList<>();

        Topology result = target.createTopology(null, ports, links);

        assertThat(result.getNodeMap().size(), is(0));
        assertThat(result.getLinkMap().size(), is(0));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.lib.OdenOsTopologyLib#createTopology()}
     */
    @Test
    public void testCreateTopologyWithEmptyNodeList() throws Exception {
        String node1Id = "NW=nw,NE=ne1";
        String node2Id = "NW=nw,NE=ne2";
        String port1Id = "NW=nw,NE=ne1,Layer=layer,TTP=ttp";
        String port2Id = "NW=nw,NE=ne2,Layer=layer,TTP=ttp";

        List<org.o3project.ocnrm.model.Node> nodes = new ArrayList<>();

        Port port1 = new Port();
        port1.setPortId(port1Id);
        Port port2 = new Port();
        port2.setPortId(port2Id);
        List<org.o3project.ocnrm.model.Port> ports = new ArrayList<>();
        ports.add(port1);
        ports.add(port2);

        Link link = new Link();
        link.setLinkId("linkId");
        link.setSrcTTP(port1Id);
        link.setDstTTP(port2Id);
        List<org.o3project.ocnrm.model.Link> links = new ArrayList<>();
        links.add(link);

        Topology result = target.createTopology(nodes, ports, links);

        assertThat(result.getNodeMap().size(), is(0));

        assertThat(result.getPort(node1Id, port1Id), is(nullValue()));
        assertThat(result.getPort(node2Id, port2Id), is(nullValue()));

        assertThat(result.getLinkMap().size(), is(0));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.lib.OdenOsTopologyLib#createTopology()}
     */
    @Test
    public void testCreateTopologyWithNullPorts() throws Exception {
        Logger dummyLogger = mock(Logger.class);
        Field field = target.getClass().getSuperclass().getDeclaredField("logger");
        field.setAccessible(true);
        field.set(target, dummyLogger);

        String node1Id = "NW=nw,NE=ne1";
        String node2Id = "NW=nw,NE=ne2";
        String port1Id = "NW=nw,NE=ne1,Layer=layer,TTP=ttp";
        String port2Id = "NW=nw,NE=ne2,Layer=layer,TTP=ttp";

        Node node1 = new Node();
        node1.setNodeId(node1Id);
        Node node2 = new Node();
        node2.setNodeId(node2Id);
        List<org.o3project.ocnrm.model.Node> nodes = new ArrayList<>();
        nodes.add(node1);
        nodes.add(node2);

        Link link = new Link();
        link.setLinkId("linkId");
        link.setSrcTTP(port1Id);
        link.setDstTTP(port2Id);
        List<org.o3project.ocnrm.model.Link> links = new ArrayList<>();
        links.add(link);

        Topology result = target.createTopology(nodes, null, links);

        assertThat(result.getNodeMap().size(), is(2));
        assertThat(result.getNodeMap().get(node1Id).getId(), is(node1Id));
        assertThat(result.getNodeMap().get(node2Id).getId(), is(node2Id));

        assertThat(result.getNode(node1Id).getPortMap().size(), is(0));
        assertThat(result.getNode(node2Id).getPortMap().size(), is(0));

        assertThat(result.getLinkMap().size(), is(0));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.lib.OdenOsTopologyLib#createTopology()}
     */
    @Test
    public void testCreateTopologyWithEmptyPort() throws Exception {
        String node1Id = "NW=nw,NE=ne1";
        String node2Id = "NW=nw,NE=ne2";
        String port1Id = "NW=nw,NE=ne1,Layer=layer,TTP=ttp";
        String port2Id = "NW=nw,NE=ne2,Layer=layer,TTP=ttp";

        Node node1 = new Node();
        node1.setNodeId(node1Id);
        Node node2 = new Node();
        node2.setNodeId(node2Id);
        List<org.o3project.ocnrm.model.Node> nodes = new ArrayList<>();
        nodes.add(node1);
        nodes.add(node2);

        List<org.o3project.ocnrm.model.Port> ports = new ArrayList<>();

        Link link = new Link();
        link.setLinkId("linkId");
        link.setSrcTTP(port1Id);
        link.setDstTTP(port2Id);
        List<org.o3project.ocnrm.model.Link> links = new ArrayList<>();
        links.add(link);

        Topology result = target.createTopology(nodes, ports, links);

        assertThat(result.getNodeMap().size(), is(2));
        assertThat(result.getNodeMap().get(node1Id).getId(), is(node1Id));
        assertThat(result.getNodeMap().get(node2Id).getId(), is(node2Id));

        assertThat(result.getNode(node1Id).getPortMap().size(), is(0));
        assertThat(result.getNode(node2Id).getPortMap().size(), is(0));

        assertThat(result.getLinkMap().size(), is(0));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.lib.OdenOsTopologyLib#createTopology()}
     */
    @Test
    public void testCreateTopologyWithNullLinks() throws Exception {
        String node1Id = "NW=nw,NE=ne1";
        String node2Id = "NW=nw,NE=ne2";
        String port1Id = "NW=nw,NE=ne1,Layer=layer,TTP=ttp";
        String port2Id = "NW=nw,NE=ne2,Layer=layer,TTP=ttp";

        Node node1 = new Node();
        node1.setNodeId(node1Id);
        Node node2 = new Node();
        node2.setNodeId(node2Id);
        List<org.o3project.ocnrm.model.Node> nodes = new ArrayList<>();
        nodes.add(node1);
        nodes.add(node2);

        Port port1 = new Port();
        port1.setPortId(port1Id);
        Port port2 = new Port();
        port2.setPortId(port2Id);
        List<org.o3project.ocnrm.model.Port> ports = new ArrayList<>();
        ports.add(port1);
        ports.add(port2);

        Topology result = target.createTopology(nodes, ports, null);

        assertThat(result.getNodeMap().size(), is(2));
        assertThat(result.getNodeMap().get(node1Id).getId(), is(node1Id));
        assertThat(result.getNodeMap().get(node2Id).getId(), is(node2Id));

        assertThat(result.getPort(node1Id, port1Id).getId(), is(port1Id));
        assertThat(result.getPort(node2Id, port2Id).getId(), is(port2Id));

        assertThat(result.getLinkMap().size(), is(0));

    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.lib.OdenOsTopologyLib#createTopology()}
     */
    @Test
    public void testCreateTopologyWithEmptyLink() throws Exception {
        String node1Id = "NW=nw,NE=ne1";
        String node2Id = "NW=nw,NE=ne2";
        String port1Id = "NW=nw,NE=ne1,Layer=layer,TTP=ttp";
        String port2Id = "NW=nw,NE=ne2,Layer=layer,TTP=ttp";

        Node node1 = new Node();
        node1.setNodeId(node1Id);
        Node node2 = new Node();
        node2.setNodeId(node2Id);
        List<org.o3project.ocnrm.model.Node> nodes = new ArrayList<>();
        nodes.add(node1);
        nodes.add(node2);

        Port port1 = new Port();
        port1.setPortId(port1Id);
        Port port2 = new Port();
        port2.setPortId(port2Id);
        List<org.o3project.ocnrm.model.Port> ports = new ArrayList<>();
        ports.add(port1);
        ports.add(port2);

        List<org.o3project.ocnrm.model.Link> links = new ArrayList<>();

        Topology result = target.createTopology(nodes, ports, links);

        assertThat(result.getNodeMap().size(), is(2));
        assertThat(result.getNodeMap().get(node1Id).getId(), is(node1Id));
        assertThat(result.getNodeMap().get(node2Id).getId(), is(node2Id));

        assertThat(result.getPort(node1Id, port1Id).getId(), is(port1Id));
        assertThat(result.getPort(node2Id, port2Id).getId(), is(port2Id));

        assertThat(result.getLinkMap().size(), is(0));
    }
}
