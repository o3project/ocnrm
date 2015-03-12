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
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;

import org.junit.Test;
import org.o3project.ocnrm.lib.table.TableManager;
import org.o3project.odenos.core.component.Logic.AttrElements;
import org.o3project.odenos.core.component.network.flow.Flow;
import org.o3project.odenos.core.component.network.flow.FlowObject;
import org.o3project.odenos.core.component.network.topology.Link;
import org.slf4j.Logger;

public class OdenOsCommonLibTest {
    private String seqNo = "#1";

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.lib.OdenOsCommonLib#getNodeId()}
     */
    @Test
    public void testGetNodeId() {
        String port = "NW=nw,NE=ne,Layer=layer,TTP=ttp";

        String node = OdenOsCommonLib.getNodeId(port, seqNo);
        assertThat(node, is("NW=nw,NE=ne"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.lib.OdenOsCommonLib#getNodeId()}
     */
    @Test
    public void testGetNodeIdWithNullParam() {
        String port = null;

        String node = OdenOsCommonLib.getNodeId(port, seqNo);
        assertThat(node, is(""));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.lib.OdenOsCommonLib#getNodeId()}
     */
    @Test
    public void testGetNodeIdWithEmptyParam() {
        String port = "";

        String node = OdenOsCommonLib.getNodeId(port, seqNo);
        assertThat(node, is(""));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.lib.OdenOsCommonLib#statusLog(Flow)}
     */
    @Test
    public void testStatusLogFlow() throws Exception {
        Logger dummyLogger = mock(Logger.class);
        Field field = OdenOsCommonLib.class.getDeclaredField("logger");
        field.setAccessible(true);
        field.set(OdenOsCommonLib.class, dummyLogger);

        Flow flow = new Flow("flowId");
        flow.getAttributes().put(TableManager.TRANSACTION_ID, "transactionId");
        flow.setStatus(FlowObject.FlowStatus.ESTABLISHED.toString());

        OdenOsCommonLib.statusLog(flow);

        verify(dummyLogger, times(1))
                .info("** Flow TransactionID:{}, FlowID:{}, Status:{}",
                        "transactionId", "flowId", "established");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.lib.OdenOsCommonLib#statusLog(Flow)}
     */
    @Test
    public void testStatusLogFlowWithNullFlow()
            throws NoSuchFieldException, SecurityException, IllegalArgumentException,
            IllegalAccessException {
        Logger dummyLogger = mock(Logger.class);
        Field field = OdenOsCommonLib.class.getDeclaredField("logger");
        field.setAccessible(true);
        field.set(OdenOsCommonLib.class, dummyLogger);

        Flow flow = null;

        OdenOsCommonLib.statusLog(flow);

        verify(dummyLogger, times(1)).info("** Flow Not Found.");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.lib.OdenOsCommonLib#statusLog(Flow, String)}
     */
    @Test
    public void testStatusLogFlowString()
            throws NoSuchFieldException, SecurityException, IllegalArgumentException,
            IllegalAccessException {
        Logger dummyLogger = mock(Logger.class);
        Field field = OdenOsCommonLib.class.getDeclaredField("logger");
        field.setAccessible(true);
        field.set(OdenOsCommonLib.class, dummyLogger);

        Flow flow = new Flow("flowId");
        flow.getAttributes().put(TableManager.TRANSACTION_ID, "transactionId");
        flow.setStatus(FlowObject.FlowStatus.ESTABLISHED.toString());

        OdenOsCommonLib.statusLog(flow, "script");

        verify(dummyLogger, times(1))
                .info("** Flow TransactionID:{}, FlowID:{}, Status:{}, " + "script",
                        "transactionId", "flowId", "established");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.lib.OdenOsCommonLib#statusLog(Flow, String)}
     */
    @Test
    public void testStatusLogFlowStringWithNullFlowa()
            throws NoSuchFieldException, SecurityException, IllegalArgumentException,
            IllegalAccessException {
        Logger dummyLogger = mock(Logger.class);
        Field field = OdenOsCommonLib.class.getDeclaredField("logger");
        field.setAccessible(true);
        field.set(OdenOsCommonLib.class, dummyLogger);

        Flow flow = null;

        OdenOsCommonLib.statusLog(flow, "script");

        verify(dummyLogger, times(1)).info("** Flow Not Found.");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.lib.OdenOsCommonLib
     * #statusLog(org.o3project.odenos.component.network.topology.Link)}
     * @throws Exception
     */
    @Test
    public void testStatusLogLink() throws Exception {
        Logger dummyLogger = mock(Logger.class);
        Field field = OdenOsCommonLib.class.getDeclaredField("logger");
        field.setAccessible(true);
        field.set(OdenOsCommonLib.class, dummyLogger);

        Link link = new Link("linkId");
        link.getAttributes().put(TableManager.TRANSACTION_ID, "transactionId");
        link.getAttributes().put(AttrElements.ESTABLISHMENT_STATUS, "state");

        OdenOsCommonLib.statusLog(link);

        verify(dummyLogger, times(1))
                .info("** Link TransactionID:{}, LinkID:{}, Status:{}",
                        "transactionId", "linkId", "state");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.lib.OdenOsCommonLib
     * #statusLog(org.o3project.odenos.component.network.topology.Link)}
     */
    @Test
    public void testStatusLogLinkWithNullLink()
            throws NoSuchFieldException, SecurityException, IllegalArgumentException,
            IllegalAccessException {
        Logger dummyLogger = mock(Logger.class);
        Field field = OdenOsCommonLib.class.getDeclaredField("logger");
        field.setAccessible(true);
        field.set(OdenOsCommonLib.class, dummyLogger);

        Link link = null;

        OdenOsCommonLib.statusLog(link);

        verify(dummyLogger, times(1)).info("** Link Not Found.");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.lib.OdenOsCommonLib
     * #statusLog(org.o3project.odenos.component.network.topology.Link, String)}
     * @throws Exception
     */
    @Test
    public void testStatusLogLinkString() throws Exception {
        Logger dummyLogger = mock(Logger.class);
        Field field = OdenOsCommonLib.class.getDeclaredField("logger");
        field.setAccessible(true);
        field.set(OdenOsCommonLib.class, dummyLogger);

        Link link = new Link("linkId");
        link.getAttributes().put(TableManager.TRANSACTION_ID, "transactionId");
        link.getAttributes().put(AttrElements.ESTABLISHMENT_STATUS, "state");

        OdenOsCommonLib.statusLog(link, "script");

        verify(dummyLogger, times(1))
                .info("** Link TransactionID:{}, LinkID:{}, Status:{}, " + "script",
                        "transactionId", "linkId", "state");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.lib.OdenOsCommonLib
     * #statusLog(org.o3project.odenos.component.network.topology.Link, String)}
     */
    @Test
    public void testStatusLogLinkStringWithNullLink()
            throws NoSuchFieldException, SecurityException, IllegalArgumentException,
            IllegalAccessException {
        Logger dummyLogger = mock(Logger.class);
        Field field = OdenOsCommonLib.class.getDeclaredField("logger");
        field.setAccessible(true);
        field.set(OdenOsCommonLib.class, dummyLogger);

        Link link = null;

        OdenOsCommonLib.statusLog(link, "script");

        verify(dummyLogger, times(1)).info("** Link Not Found.");
    }

}
