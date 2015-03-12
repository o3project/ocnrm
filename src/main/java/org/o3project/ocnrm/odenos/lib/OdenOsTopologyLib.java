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

import java.util.List;

import org.o3project.odenos.core.component.network.topology.Link;
import org.o3project.odenos.core.component.network.topology.Node;
import org.o3project.odenos.core.component.network.topology.Port;
import org.o3project.odenos.core.component.network.topology.Topology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OdenOsTopologyLib {
    private Logger logger = LoggerFactory.getLogger(OdenOsTopologyLib.class);
    private String seqNo = "";

    public static final String INITIAL_VERSION = "0";

    public Topology createTopology(List<org.o3project.ocnrm.model.Node> nodeList,
            List<org.o3project.ocnrm.model.Port> portList,
            List<org.o3project.ocnrm.model.Link> linkList)
            throws Exception {
        logger.info(seqNo + "\t" + "createTopology Start");
        Topology topo = new Topology();

        if (null == nodeList) {
            return topo;
        }

        for (org.o3project.ocnrm.model.Node node : nodeList) {
            Node odenonNode = new Node(node.getNodeId());
            topo.createNode(odenonNode);
        }

        if (null != portList) {
            for (org.o3project.ocnrm.model.Port port : portList) {
                String nodeId = OdenOsCommonLib.getNodeId(port.getPortId(), seqNo);

                logger.debug(seqNo + "\t" + "port id:" + port.getPortId());
                logger.debug(seqNo + "\t" + "node id:" + nodeId);

                Port odenosPort = new Port(port.getPortId());
                Node node = topo.getNode(nodeId);
                if (node != null) {
                    node.createPort(odenosPort);
                } else {
                    logger.error("node is not exist. nodeId: " + nodeId);
                }
            }
        }

        if (null != linkList) {
            for (org.o3project.ocnrm.model.Link link : linkList) {
                String srcNodeId = OdenOsCommonLib.getNodeId(link.getSrcTTP(), seqNo);
                String dstNodeId = OdenOsCommonLib.getNodeId(link.getDstTTP(), seqNo);

                Link odenosLink = new Link(link.getLinkId(), srcNodeId, link.getSrcTTP(),
                        dstNodeId, link.getDstTTP());
                if (topo.getNode(srcNodeId) != null && topo.getNode(dstNodeId) != null
                        && topo.getPort(srcNodeId, link.getSrcTTP()) != null
                        && topo.getPort(dstNodeId, link.getDstTTP()) != null) {
                    topo.createLink(odenosLink);
                } else {
                    logger.error("link: " + link.getLinkId() + " is not created.");
                    logger.error("src nodeId: " + srcNodeId + ", dst nodeId: " + dstNodeId);
                }
            }
        }

        logger.debug(seqNo + "\t" + "node:" + topo.getNodeMap().keySet().toString());
        logger.info(seqNo + "\t" + "createTopology End");
        return topo;
    }
}
