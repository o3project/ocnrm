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
package org.o3project.ocnrm.odenos.driver;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
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
import org.mockito.Mockito;
import org.o3project.ocnrm.lib.JSONParser;
import org.o3project.ocnrm.lib.table.Event;
import org.o3project.ocnrm.lib.table.ResourceInfoFlomMf;
import org.o3project.ocnrm.lib.table.TableManager;
import org.o3project.ocnrm.model.Constraint;
import org.o3project.ocnrm.model.TerminationPoints;
import org.o3project.ocnrm.model.odu.OduFlow;
import org.o3project.ocnrm.model.odu.OduFlowCreationResponse;
import org.o3project.ocnrm.odenos.driver.AbstractDriver.IActionCallback;
import org.o3project.ocnrm.odenos.driver.controller.ResourceSendController;
import org.o3project.odenos.core.component.Logic;
import org.o3project.odenos.core.component.Logic.AttrElements;
import org.o3project.odenos.core.component.NetworkInterface;
import org.o3project.odenos.core.component.network.flow.Flow;
import org.o3project.odenos.core.component.network.flow.FlowObject;
import org.o3project.odenos.core.component.network.flow.FlowObject.FlowStatus;
import org.o3project.odenos.core.component.network.flow.basic.BasicFlow;
import org.o3project.odenos.core.component.network.flow.basic.BasicFlowMatch;
import org.o3project.odenos.core.component.network.flow.ofpflow.OFPFlow;
import org.o3project.odenos.core.component.network.topology.Link;
import org.o3project.odenos.core.component.network.topology.Node;
import org.o3project.odenos.core.component.network.topology.Topology;
import org.o3project.odenos.core.manager.system.ComponentConnection;
import org.o3project.odenos.core.manager.system.ComponentConnectionLogicAndNetwork;
import org.o3project.odenos.core.manager.system.event.ComponentConnectionChanged;
import org.o3project.odenos.remoteobject.RequestParser;
import org.o3project.odenos.remoteobject.RequestParser.ParsedRequest;
import org.o3project.odenos.remoteobject.message.Request;
import org.o3project.odenos.remoteobject.message.Request.Method;
import org.o3project.odenos.remoteobject.message.Response;
import org.o3project.odenos.remoteobject.messagingclient.MessageDispatcher;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.slf4j.Logger;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ OptDriver.class, ComponentConnectionChanged.class, NetworkInterface.class,
        ComponentConnection.class, IActionCallback.class })
public class OptDriverTest {
    private OptDriver target;
    private MessageDispatcher dispatcher;
    private static final String TRANSACTION_ID = "20141016000001";
    private TableManager dummyManager;
    private static final String SEQUENCE_NO = "#1";
    private static final String NW_ID = "networkId";
    private static final String ODU_LAYER = "odu";
    private static final String OCH_LAYER = "och";

    @Before
    public void setUp() throws Exception {
        dispatcher = Mockito.mock(MessageDispatcher.class);
        target = PowerMockito.spy(new OptDriver("objectId", "baseUri", dispatcher));

        Event dummyEvent = mock(Event.class);
        when(dummyEvent.getTransactionId()).thenReturn(TRANSACTION_ID);
        dummyManager = spy(TableManager.getInstance());
        when(dummyManager.createEvent(any(String.class), any(String.class), any(String.class),
                any(String.class), any(String.class), any(String.class))).thenReturn(dummyEvent);
        Whitebox.setInternalState(target, "tableManager", dummyManager);
    }

    @After
    public void tearDown() throws Exception {
        target = null;
        dispatcher = null;
        Whitebox.invokeMethod(dummyManager, "clear");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#flowMessageManager()}
     */
    @Test
    public void testFlowMessageManager() throws Exception {
        String action = Logic.CONN_ADD;

        BasicFlow flow = spy(new BasicFlow("flowId"));

        PowerMockito.doReturn(mock(OduFlowCreationResponse.class))
                .when(target, "registerOdu", eq(SEQUENCE_NO), eq(flow), any(Event.class));

        Response dummyResponse = mock(Response.class, RETURNS_DEEP_STUBS);
        when(dummyResponse.getBodyValue().toString()).thenReturn("");

        NetworkInterface networkIf = PowerMockito.spy(new NetworkInterface(dispatcher, NW_ID));
        Map<String, Link> dummyLinkSet = new HashMap<>();
        doReturn(dummyLinkSet).when(networkIf).getLinks();
        doReturn(dummyResponse).when(networkIf).putFlow(any(Flow.class));

        PowerMockito.doNothing().when(target, "formFlowMessage", eq(flow),
                any(OduFlowCreationResponse.class), eq(dummyLinkSet), eq(TRANSACTION_ID));

        target.flowMessageManager(networkIf, NW_ID, action, flow, SEQUENCE_NO);

        verify(networkIf, times(1)).putFlow(any(Flow.class));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#flowMessageManager()}
     */
    @Test
    public void testFlowMessageManagerWithOfpFlow()
            throws Exception {
        String action = Logic.CONN_ADD;
        OFPFlow flow = spy(new OFPFlow("flowId"));

        NetworkInterface networkIf = PowerMockito.spy(new NetworkInterface(dispatcher, NW_ID));

        target.flowMessageManager(networkIf, NW_ID, action, flow, SEQUENCE_NO);

        verify(dummyManager, never()).createEvent(anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString());
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#deleteFlow()}
     */
    @Test
    public void testDeleteFlow() throws Exception {
        BasicFlow flow = spy(new BasicFlow("flowId"));
        flow.getAttributes().put(TableManager.TRANSACTION_ID, TRANSACTION_ID);

        ResourceInfoFlomMf resource = new ResourceInfoFlomMf(TRANSACTION_ID, NW_ID, "flowId",
                "mfId", new OduFlow(), "20141022", "optDriver");
        dummyManager.addResource(resource);
        when(dummyManager.delete(TRANSACTION_ID, NW_ID, "flowId")).thenReturn(true);

        NetworkInterface networkIf = PowerMockito.spy(new NetworkInterface(dispatcher, NW_ID));

        PowerMockito.doReturn(true).when(target, "deleteResource", eq(flow), eq(SEQUENCE_NO),
                eq(resource));

        target.deleteFlow(networkIf, NW_ID, flow, SEQUENCE_NO);

        verify(dummyManager, times(1)).delete(TRANSACTION_ID, NW_ID, "flowId");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#deleteFlow()}
     */
    @Test
    public void testDeleteFlowWithError() throws Exception {
        BasicFlow flow = spy(new BasicFlow("flowId"));
        flow.getAttributes().put(TableManager.TRANSACTION_ID, TRANSACTION_ID);

        ResourceInfoFlomMf resource = new ResourceInfoFlomMf(TRANSACTION_ID, NW_ID, "flowId",
                "mfId", new OduFlow(), "20141022", "optDriver");
        dummyManager.addResource(resource);

        NetworkInterface networkIf = PowerMockito.spy(new NetworkInterface(dispatcher, NW_ID));

        PowerMockito.doReturn(false).when(target, "deleteResource", eq(flow), eq(SEQUENCE_NO),
                eq(resource));

        target.deleteFlow(networkIf, NW_ID, flow, SEQUENCE_NO);

        verify(dummyManager, never()).delete(anyString(), anyString(), anyString());
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#getDescription()}
     */
    @Test
    public void testGetDescription() {
        String result = target.getDescription();
        assertThat(result, containsString("optDriver"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#onConnectionChangedAddedPre()}
     */
    @Test
    public void testOnConnectionChangedAddedPre() {
        ComponentConnection prev = Mockito.mock(ComponentConnection.class);
        ComponentConnection curr =
                new ComponentConnectionLogicAndNetwork("objectId", ODU_LAYER, "running",
                        "logic_id", "network1");
        curr.setProperty(ComponentConnectionLogicAndNetwork.LOGIC_ID,
                "objectId");

        ComponentConnectionChanged event =
                PowerMockito.spy(new ComponentConnectionChanged("action", prev, curr));

        assertThat(target.onConnectionChangedAddedPre(event), is(true));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#onConnectionChangedAddedPre()}
     */
    @Test
    public void testOnConnectionChangedAddedPreWithNullConnection()
            throws NoSuchFieldException, SecurityException, IllegalArgumentException,
            IllegalAccessException {
        Logger dummyLogger = mock(Logger.class);
        Field field = target.getClass().getSuperclass().getSuperclass().getDeclaredField("logger");
        field.setAccessible(true);
        field.set(target, dummyLogger);

        ComponentConnection prev = Mockito.mock(ComponentConnection.class);
        ComponentConnection curr = null;

        ComponentConnectionChanged event = new ComponentConnectionChanged("add", prev, curr);

        assertThat(target.onConnectionChangedAddedPre(event), is(false));
        verify(dummyLogger, times(1)).error(target.seqNo + "\t" + "-- [add] msg.curr() is null");
        verify(dummyLogger, never())
                .info(target.seqNo + "\t" + "-- onConnectionChangedAddedPre End");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#onConnectionChangedAddedPre()}
     */
    @Test
    public void testOnConnectionChangedAddedPreWithDifferentConnectionType()
            throws NoSuchFieldException, SecurityException, IllegalArgumentException,
            IllegalAccessException {
        Logger dummyLogger = mock(Logger.class);
        Field field = target.getClass().getSuperclass().getSuperclass().getDeclaredField("logger");
        field.setAccessible(true);
        field.set(target, dummyLogger);

        ComponentConnection prev = Mockito.mock(ComponentConnection.class);
        ComponentConnection curr =
                new ComponentConnectionLogicAndNetwork("objectId", "differentType", "running",
                        "logic_id", "network1");

        ComponentConnectionChanged event = new ComponentConnectionChanged("add", prev, curr);

        assertThat(target.onConnectionChangedAddedPre(event), is(false));
        verify(dummyLogger, times(1))
                .error(target.seqNo + "\t" + "-- [add] type: " + "differentType");
        verify(dummyLogger, never())
                .info(target.seqNo + "\t" + "-- onConnectionChangedAddedPre End");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#onConnectionChangedAddedPre()}
     */
    @Test
    public void testOnConnectionChangedAddedPreWithMismatchObjectId()
            throws NoSuchFieldException, SecurityException, IllegalArgumentException,
            IllegalAccessException {
        Logger dummyLogger = mock(Logger.class);
        Field field = target.getClass().getSuperclass().getSuperclass().getDeclaredField("logger");
        field.setAccessible(true);
        field.set(target, dummyLogger);

        ComponentConnection prev = Mockito.mock(ComponentConnection.class);
        ComponentConnection curr =
                new ComponentConnectionLogicAndNetwork("differentId", ODU_LAYER, "running",
                        "logic_id", "network1");
        curr.setProperty(ComponentConnectionLogicAndNetwork.LOGIC_ID,
                "differentId");

        ComponentConnectionChanged event = new ComponentConnectionChanged("add", prev, curr);

        assertThat(target.onConnectionChangedAddedPre(event), is(false));
        verify(dummyLogger, times(1))
                .error(target.seqNo + "\t" + "-- [add] mismatch logic id {} {}",
                        "objectId", "differentId");
        verify(dummyLogger, never())
                .info(target.seqNo + "\t" + "-- onConnectionChangedAddedPre End");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#onConnectionChangedAddedPre()}
     */
    @Test
    public void testOnConnectionChangedAddedPreWithNullComponentConnectionId()
            throws NoSuchFieldException, SecurityException, IllegalArgumentException,
            IllegalAccessException {
        Logger dummyLogger = mock(Logger.class);
        Field field = target.getClass().getSuperclass().getSuperclass().getDeclaredField("logger");
        field.setAccessible(true);
        field.set(target, dummyLogger);

        ComponentConnection prev = Mockito.mock(ComponentConnection.class);
        ComponentConnection curr =
                new ComponentConnectionLogicAndNetwork("objectId", ODU_LAYER, "running",
                        "logic_id", null);
        curr.setProperty(ComponentConnectionLogicAndNetwork.NETWORK_ID, null);
        curr.setProperty(ComponentConnectionLogicAndNetwork.LOGIC_ID,
                "objectId");

        ComponentConnectionChanged event = new ComponentConnectionChanged("add", prev, curr);

        assertThat(target.onConnectionChangedAddedPre(event), is(false));
        verify(dummyLogger, times(1))
                .error(target.seqNo + "\t" + "-- [add] networkComponentId is null");
        verify(dummyLogger, never())
                .info(target.seqNo + "\t" + "-- onConnectionChangedAddedPre End");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#onConnectionChangedAddedPre()}
     */
    @Test
    public void testOnConnectionChangedAddedPreWithMistatchObjectType()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException,
            SecurityException {
        Logger dummyLogger = mock(Logger.class);
        Field field = target.getClass().getSuperclass().getSuperclass().getDeclaredField("logger");
        field.setAccessible(true);
        field.set(target, dummyLogger);

        ComponentConnection prev = Mockito.mock(ComponentConnection.class);
        ComponentConnection curr = new ComponentConnection("objectId", ODU_LAYER, "running");
        curr.setProperty(ComponentConnectionLogicAndNetwork.LOGIC_ID, "objectId");
        curr.setProperty(ComponentConnectionLogicAndNetwork.NETWORK_ID, "networkId");
        //Whitebox.setInternalState(curr, "TYPE", "differentType");

        ComponentConnectionChanged event = new ComponentConnectionChanged("add", prev, curr);

        assertThat(target.onConnectionChangedAddedPre(event), is(false));
        verify(dummyLogger, times(1))
                .error(target.seqNo + "\t" + "-- [add] networkComponentId not eq");
        verify(dummyLogger, never())
                .info(target.seqNo + "\t" + "-- onConnectionChangedAddedPre End");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#onConnectionChangedAdded()}
     */
    @Test
    public void testOnConnectionChangedAdded() throws Exception {
        PowerMockito.doNothing().when(target, "subscribeNetworkComponent",
                anyString(), anyString());

        ComponentConnection prev = Mockito.mock(ComponentConnection.class);
        ComponentConnection curr = Mockito
                .spy(new ComponentConnection("objectId", "connectionType", "connectionState"));
        ComponentConnectionChanged event = new ComponentConnectionChanged("action", prev, curr);

        target.onConnectionChangedAdded(event);

        PowerMockito.verifyPrivate(target, times(1)).invoke("subscribeNetworkComponent",
                anyString(), anyString());
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#onConnectionChangedUpdatePre()}
     */
    @Test
    public void testOnConnectionChangedUpdatePre() {
        ComponentConnection prev = Mockito.mock(ComponentConnection.class);
        ComponentConnection curr =
                new ComponentConnectionLogicAndNetwork("objectId", ODU_LAYER, "running",
                        "logic_id", "network1");
        curr.setProperty(ComponentConnectionLogicAndNetwork.LOGIC_ID,
                "objectId");

        ComponentConnectionChanged event =
                PowerMockito.spy(new ComponentConnectionChanged("action", prev, curr));

        assertThat(target.onConnectionChangedUpdatePre(event), is(true));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#onConnectionChangedUpdatePre()}
     */
    @Test
    public void testOnConnectionChangedUpdatePreWithNullConnection()
            throws NoSuchFieldException, SecurityException, IllegalArgumentException,
            IllegalAccessException {
        Logger dummyLogger = mock(Logger.class);
        Field field = target.getClass().getSuperclass().getSuperclass().getDeclaredField("logger");
        field.setAccessible(true);
        field.set(target, dummyLogger);

        ComponentConnection prev = Mockito.mock(ComponentConnection.class);
        ComponentConnection curr = null;

        ComponentConnectionChanged event = new ComponentConnectionChanged("update", prev, curr);

        assertThat(target.onConnectionChangedUpdatePre(event), is(false));
        verify(dummyLogger, times(1)).error(target.seqNo + "\t" + "-- [update] msg.curr() is null");
        verify(dummyLogger, never())
                .info(target.seqNo + "\t" + "-- onConnectionChangedUpdatePre End");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#onConnectionChangedUpdatePre()}
     */
    @Test
    public void testOnConnectionChangedUpdatePreWithDifferentConnectionType()
            throws NoSuchFieldException, SecurityException, IllegalArgumentException,
            IllegalAccessException {
        Logger dummyLogger = mock(Logger.class);
        Field field = target.getClass().getSuperclass().getSuperclass().getDeclaredField("logger");
        field.setAccessible(true);
        field.set(target, dummyLogger);

        ComponentConnection prev = Mockito.mock(ComponentConnection.class);
        ComponentConnection curr = new ComponentConnection("objectId", "differentType", "running");

        ComponentConnectionChanged event = new ComponentConnectionChanged("update", prev, curr);

        assertThat(target.onConnectionChangedUpdatePre(event), is(false));
        verify(dummyLogger, times(1))
                .error(target.seqNo + "\t" + "-- [update] type: " + "differentType");
        verify(dummyLogger, never())
                .info(target.seqNo + "\t" + "-- onConnectionChangedUpdatePre End");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#onConnectionChangedUpdatePre()}
     */
    @Test
    public void testOnConnectionChangedUpdatePrewithMismatchObjectType()
            throws NoSuchFieldException, SecurityException, IllegalArgumentException,
            IllegalAccessException {
        Logger dummyLogger = mock(Logger.class);
        Field field = target.getClass().getSuperclass().getSuperclass().getDeclaredField("logger");
        field.setAccessible(true);
        field.set(target, dummyLogger);

        ComponentConnection prev = Mockito.mock(ComponentConnection.class);
        ComponentConnection curr = new ComponentConnection("objectId", ODU_LAYER, "running");

        ComponentConnectionChanged event = new ComponentConnectionChanged("update", prev, curr);

        assertThat(target.onConnectionChangedUpdatePre(event), is(false));
        verify(dummyLogger, times(1))
                .error(target.seqNo + "\t" + "-- [update] msg.curr() Object Type mismatch");
        verify(dummyLogger, never())
                .info(target.seqNo + "\t" + "-- onConnectionChangedUpdatePre End");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#onConnectionChangedUpdatePre()}
     */
    @Test
    public void testOnConnectionChangedUpdatePreWithMismatchObjectId()
            throws NoSuchFieldException, SecurityException, IllegalArgumentException,
            IllegalAccessException {
        Logger dummyLogger = mock(Logger.class);
        Field field = target.getClass().getSuperclass().getSuperclass().getDeclaredField("logger");
        field.setAccessible(true);
        field.set(target, dummyLogger);

        ComponentConnection prev = Mockito.mock(ComponentConnection.class);
        ComponentConnection curr =
                new ComponentConnectionLogicAndNetwork("differentId", ODU_LAYER, "running",
                        "logic_id", "network1");
        curr.setProperty(ComponentConnectionLogicAndNetwork.LOGIC_ID,
                "differentId");

        ComponentConnectionChanged event = new ComponentConnectionChanged("update", prev, curr);

        assertThat(target.onConnectionChangedUpdatePre(event), is(false));
        verify(dummyLogger, times(1))
                .error(target.seqNo + "\t" + "-- [update] mismatch logic id {} {}",
                        "objectId", "differentId");
        verify(dummyLogger, never())
                .info(target.seqNo + "\t" + "-- onConnectionChangedUpdatePre End");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#onConnectionChangedUpdate()}
     */
    @Test
    public void testOnConnectionChangedUpdate() throws Exception {
        PowerMockito.doNothing().when(target, "subscribeNetworkComponent",
                anyString(), anyString());

        ComponentConnection prev = Mockito.mock(ComponentConnection.class);
        ComponentConnection curr = Mockito
                .spy(new ComponentConnection("objectId", "connectionType", "connectionState"));
        ComponentConnectionChanged event = new ComponentConnectionChanged("action", prev, curr);

        target.onConnectionChangedUpdate(event);

        PowerMockito.verifyPrivate(target, times(1)).invoke("subscribeNetworkComponent",
                anyString(), anyString());
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#onConnectionChangedDeletePre()}
     */
    @Test
    public void testOnConnectionChangedDeletePre() {
        ComponentConnection prev =
                new ComponentConnectionLogicAndNetwork("objectId", "original", "running",
                        "logic_id", "network1");
        ComponentConnection curr = null;

        ComponentConnectionChanged event =
                PowerMockito.spy(new ComponentConnectionChanged("action", prev, curr));

        assertThat(target.onConnectionChangedDeletePre(event), is(true));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#onConnectionChangedDeletePre()}
     */
    @Test
    public void testOnConnectionChangedDeletePreWithNullConnection()
            throws NoSuchFieldException, SecurityException, IllegalArgumentException,
            IllegalAccessException {
        Logger dummyLogger = mock(Logger.class);
        Field field = target.getClass().getSuperclass().getSuperclass().getDeclaredField("logger");
        field.setAccessible(true);
        field.set(target, dummyLogger);

        ComponentConnection prev = null;
        ComponentConnection curr = null;

        ComponentConnectionChanged event =
                PowerMockito.spy(new ComponentConnectionChanged("action", prev, curr));

        assertThat(target.onConnectionChangedDeletePre(event), is(false));
        verify(dummyLogger, times(1)).error(target.seqNo + "\t" + "-- [delete] msg.prev is null");
        verify(dummyLogger, never())
                .info(target.seqNo + "\t" + "-- onConnectionChangedDeletePre End");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#onConnectionChangedDeletePre()}
     */
    @Test
    public void testOnConnectionChangedDeletePreWithMismatchObjectId()
            throws NoSuchFieldException, SecurityException, IllegalArgumentException,
            IllegalAccessException {
        Logger dummyLogger = mock(Logger.class);
        Field field = target.getClass().getSuperclass().getSuperclass().getDeclaredField("logger");
        field.setAccessible(true);
        field.set(target, dummyLogger);

        ComponentConnection prev =
                new ComponentConnectionLogicAndNetwork("differentId", "original", "running",
                        "logic_id", null);
        ComponentConnection curr = null;

        ComponentConnectionChanged event =
                PowerMockito.spy(new ComponentConnectionChanged("action", prev, curr));

        assertThat(target.onConnectionChangedDeletePre(event), is(false));
        verify(dummyLogger, times(1))
                .error(target.seqNo + "\t" + "-- [delete] msg.curr() Object ID mismatch");
        verify(dummyLogger, never())
                .info(target.seqNo + "\t" + "-- onConnectionChangedDeletePre End");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#onConnectionChangedDeletePre()}
     */
    @Test
    public void testOnConnectionChangedDeletePreWithNullComponentId()
            throws NoSuchFieldException, SecurityException, IllegalArgumentException,
            IllegalAccessException {
        Logger dummyLogger = mock(Logger.class);
        Field field = target.getClass().getSuperclass().getSuperclass().getDeclaredField("logger");
        field.setAccessible(true);
        field.set(target, dummyLogger);

        ComponentConnection prev =
                new ComponentConnectionLogicAndNetwork("objectId", "original", "running",
                        "logic_id", null);
        prev.setProperty(ComponentConnectionLogicAndNetwork.NETWORK_ID, null);
        ComponentConnection curr = null;

        ComponentConnectionChanged event =
                PowerMockito.spy(new ComponentConnectionChanged("action", prev, curr));

        assertThat(target.onConnectionChangedDeletePre(event), is(false));
        verify(dummyLogger, times(1))
                .error(target.seqNo + "\t" + "-- [delete] networkComponentId is not null");
        verify(dummyLogger, never())
                .info(target.seqNo + "\t" + "-- onConnectionChangedDeletePre End");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#onConnectionChangedDelete()}
     */
    @Test
    public void testOnConnectionChangedDelete() throws Exception {
        PowerMockito.doNothing().when(target, "unsubscribeNetworkComponent",
                anyString(), anyString(), anyString());

        ComponentConnection prev =
                new ComponentConnectionLogicAndNetwork("objectId", "original", "running",
                        "logic_id", NW_ID);
        prev.setProperty(ComponentConnectionLogicAndNetwork.NETWORK_ID, null);
        ComponentConnection curr = null;

        ComponentConnectionChanged event =
                PowerMockito.spy(new ComponentConnectionChanged("action", prev, curr));

        target.onConnectionChangedDelete(event);

        PowerMockito.verifyPrivate(target, times(1)).invoke("unsubscribeNetworkComponent",
                anyString(), anyString(), anyString());
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#subscribeNetworkComponent()}
     */
    @Test
    public void testSubscribeNetworkComponent() throws Exception {
        Whitebox.invokeMethod(target, "subscribeNetworkComponent", NW_ID, SEQUENCE_NO);

        PowerMockito.verifyPrivate(target, times(4)).invoke("addEntryEventSubscription",
                anyString(), anyString());
        PowerMockito.verifyPrivate(target, times(4)).invoke("updateEntryEventSubscription",
                anyString(), anyString(), any());

        PowerMockito.verifyPrivate(target, times(3)).invoke("applyEventSubscription");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#subscribeNetworkComponent()}
     */
    @Test
    public void testSubscribeNetworkComponentWithError() throws Exception {
        Exception exception = mock(Exception.class);
        PowerMockito.doThrow(exception).when(target, "applyEventSubscription");

        Whitebox.invokeMethod(target, "subscribeNetworkComponent", NW_ID, SEQUENCE_NO);

        PowerMockito.verifyPrivate(target, times(4)).invoke("addEntryEventSubscription",
                anyString(), anyString());
        PowerMockito.verifyPrivate(target, times(4)).invoke("updateEntryEventSubscription",
                anyString(), anyString(), any());

        PowerMockito.verifyPrivate(target).invoke("applyEventSubscription");
        verify(exception, times(1)).printStackTrace();
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#unsubscribeNetworkComponent()}
     */
    @Test
    public void testUnsubscribeNetworkComponent() throws Exception {
        PowerMockito.doNothing().when(target, "removeEntryEventSubscription", anyString(),
                anyString());

        Whitebox.invokeMethod(target, "unsubscribeNetworkComponent", "type", NW_ID, SEQUENCE_NO);

        PowerMockito.verifyPrivate(target, times(4)).invoke("removeEntryEventSubscription",
                anyString(), anyString());

        PowerMockito.verifyPrivate(target, times(3)).invoke("applyEventSubscription");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#unsubscribeNetworkComponent()}
     */
    @Test
    public void testUnsubscribeNetworkComponentWithError() throws Exception {
        Exception exception = mock(Exception.class);
        PowerMockito.doThrow(exception).when(target, "applyEventSubscription");

        Whitebox.invokeMethod(target, "unsubscribeNetworkComponent", "type", NW_ID, SEQUENCE_NO);

        PowerMockito.verifyPrivate(target, times(4)).invoke("removeEntryEventSubscription",
                anyString(), anyString());

        PowerMockito.verifyPrivate(target, times(1)).invoke("applyEventSubscription");
        verify(exception, times(1)).printStackTrace();
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#onFlowAddedPre()}
     */
    @Test
    public void testOnFlowAddedPre() {
        Flow flow = Mockito.spy(new Flow("networkId"));
        flow.setStatus(FlowStatus.ESTABLISHING.toString());
        BasicFlow dummyBasicFlow = spy(new BasicFlow("flowId"));
        dummyBasicFlow.addMatch(new BasicFlowMatch("inNode", "inPort"));
        when(dummyBasicFlow.getStatus()).thenReturn(FlowObject.FlowStatus.NONE.toString());

        NetworkInterface nwIf = PowerMockito.mock(NetworkInterface.class);
        when(nwIf.getFlow("networkId")).thenReturn(dummyBasicFlow);

        HashMap<String, NetworkInterface> networkIfs = new HashMap<String, NetworkInterface>();
        networkIfs.put("networkId", nwIf);

        Whitebox.setInternalState(target, "networkInterfaces", networkIfs);

        assertThat(target.onFlowAddedPre("networkId", flow), is(true));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#onFlowAddedPre()}
     */
    @Test
    public void testOnFlowAddedPreWithStatusEstablished() {
        Flow flow = Mockito.spy(new Flow("networkId"));
        flow.setStatus(FlowStatus.ESTABLISHED.toString());

        BasicFlow dummyBasicFlow = spy(new BasicFlow("flowId"));
        dummyBasicFlow.addMatch(new BasicFlowMatch("inNode", "inPort"));
        when(dummyBasicFlow.getStatus()).thenReturn(FlowObject.FlowStatus.ESTABLISHED.toString());

        NetworkInterface nwIf = PowerMockito.mock(NetworkInterface.class);
        when(nwIf.getFlow("networkId")).thenReturn(dummyBasicFlow);

        HashMap<String, NetworkInterface> networkIfs = new HashMap<String, NetworkInterface>();
        networkIfs.put("networkId", nwIf);

        Whitebox.setInternalState(target, "networkInterfaces", networkIfs);

        assertThat(target.onFlowAddedPre("networkId", flow), is(false));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#onFlowAdded()}
     */
    @Test
    public void testOnFlowAdded() throws Exception {
        Flow flow = spy(new Flow("flowId"));

        PowerMockito.doReturn(true).when(target, "onFlowAddedPre", eq(NW_ID), eq(flow));

        PowerMockito.doNothing().when(target, "onFlowProcess",
                NW_ID, flow, Logic.CONN_ADD);

        target.onFlowAdded(NW_ID, flow);

        PowerMockito.verifyPrivate(target, times(1)).invoke("onFlowProcess",
                NW_ID, flow, Logic.CONN_ADD);
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#onFlowUpdate()}
     */
    @Test
    public void testOnFlowUpdate() throws Exception {
        Flow prev = Mockito.mock(Flow.class);
        Flow curr = Mockito.mock(Flow.class);
        ArrayList<String> attributesList = new ArrayList<String>();

        PowerMockito.doReturn(true).when(target, "onFlowUpdatePre",
                eq(NW_ID), eq(prev), eq(curr), eq(attributesList));

        PowerMockito.doNothing().when(target, "onFlowProcess",
                NW_ID, curr, Logic.CONN_UPDATE);

        target.onFlowUpdate(NW_ID, prev, curr, attributesList);

        PowerMockito.verifyPrivate(target, times(1)).invoke("onFlowProcess",
                NW_ID, curr, Logic.CONN_UPDATE);
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#onFlowDelete()}
     */
    @Test
    public void testOnFlowDelete() throws Exception {
        Logger dummyLogger = mock(Logger.class);
        Field field = target.getClass().getSuperclass().getSuperclass().getDeclaredField("logger");
        field.setAccessible(true);
        field.set(target, dummyLogger);

        Flow flow = Mockito.spy(new Flow("flowId"));
        flow.setStatus(FlowStatus.ESTABLISHED.toString());
        flow.setEnabled(true);

        BasicFlow dummyBasicFlow = spy(new BasicFlow("flowId"));
        dummyBasicFlow.setStatus(FlowObject.FlowStatus.ESTABLISHED.toString());

        NetworkInterface nwIf = PowerMockito.mock(NetworkInterface.class);
        doReturn(dummyBasicFlow).when(nwIf).getFlow("flowId");

        HashMap<String, NetworkInterface> networkIfs = new HashMap<String, NetworkInterface>();
        networkIfs.put(NW_ID, nwIf);

        Whitebox.setInternalState(target, "networkInterfaces", networkIfs);

        PowerMockito.doNothing().when(target, "deleteFlow", eq(nwIf), eq(NW_ID), any(Flow.class),
                anyString());

        target.onFlowDelete(NW_ID, flow);

        PowerMockito.verifyPrivate(target, times(1)).invoke("deleteFlow",
                eq(nwIf), eq(NW_ID), any(Flow.class), anyString());

        verify(dummyLogger, never()).info(target.seqNo + "\t" + "get JSONException.");
        verify(dummyLogger, never()).info(target.seqNo + "\t" + "get IOException.");
        verify(dummyLogger, times(1)).info(target.seqNo + "\t" + "■onFlowDelete End");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#onFlowDelete()}
     */
    @Test
    public void testOnFlowDeleteWithNotStatusEstablished()
            throws Exception {
        Flow flow = Mockito.spy(new Flow("flowId"));
        flow.setStatus(FlowStatus.ESTABLISHING.toString());
        BasicFlow dummyBasicFlow = spy(new BasicFlow("flowId"));
        dummyBasicFlow.setEnabled(true);
        dummyBasicFlow.setStatus(FlowObject.FlowStatus.ESTABLISHING.toString());
        dummyBasicFlow.addMatch(new BasicFlowMatch("inNode", "inPort"));

        NetworkInterface nwIf = PowerMockito.mock(NetworkInterface.class);
        doReturn(dummyBasicFlow).when(nwIf).getFlow("flowId");

        HashMap<String, NetworkInterface> networkIfs = new HashMap<String, NetworkInterface>();
        networkIfs.put(NW_ID, nwIf);

        Whitebox.setInternalState(target, "networkInterfaces", networkIfs);

        PowerMockito.doNothing().when(target, "deleteFlow", eq(nwIf), eq(NW_ID), any(Flow.class),
                anyString());

        target.onFlowDelete(NW_ID, flow);

        PowerMockito.verifyPrivate(target, never()).invoke("deleteFlow",
                eq(nwIf), eq(NW_ID), any(Flow.class), anyString());

    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#onFlowDelete()}
     */
    @Test
    public void testOnFlowDeleteWithNotEnabled()
            throws Exception {
        Flow flow = Mockito.spy(new Flow("flowId"));
        flow.setEnabled(false);
        flow.setStatus(FlowStatus.ESTABLISHED.toString());

        BasicFlow dummyBasicFlow = spy(new BasicFlow("flowId"));
        dummyBasicFlow.setEnabled(false);
        dummyBasicFlow.setStatus(FlowObject.FlowStatus.ESTABLISHED.toString());
        dummyBasicFlow.addMatch(new BasicFlowMatch("inNode", "inPort"));

        NetworkInterface nwIf = PowerMockito.mock(NetworkInterface.class);
        doReturn(dummyBasicFlow).when(nwIf).getFlow("flowId");

        HashMap<String, NetworkInterface> networkIfs = new HashMap<String, NetworkInterface>();
        networkIfs.put(NW_ID, nwIf);

        Whitebox.setInternalState(target, "networkInterfaces", networkIfs);

        PowerMockito.doNothing().when(target, "deleteFlow", eq(nwIf), eq(NW_ID), any(Flow.class),
                anyString());

        target.onFlowDelete(NW_ID, flow);

        PowerMockito.verifyPrivate(target, never()).invoke("deleteFlow",
                eq(nwIf), eq(NW_ID), any(Flow.class), anyString());
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#onFlowDelete()}
     */
    @Test
    public void onFlowDeleteWithJsonException() throws Exception {
        Logger dummyLogger = mock(Logger.class);
        Field field = target.getClass().getSuperclass().getSuperclass().getDeclaredField("logger");
        field.setAccessible(true);
        field.set(target, dummyLogger);

        Flow flow = Mockito.spy(new Flow("flowId"));
        flow.setStatus(FlowStatus.ESTABLISHED.toString());
        flow.setEnabled(true);

        NetworkInterface nwIf = PowerMockito.mock(NetworkInterface.class);
        HashMap<String, NetworkInterface> networkIfs = new HashMap<>();
        networkIfs.put(NW_ID, nwIf);

        Whitebox.setInternalState(target, "networkInterfaces", networkIfs);

        Map<String, String> dummyMap = new HashMap<>();
        dummyMap.put(OCH_LAYER, "differentId");
        Whitebox.setInternalState(target, "connectionIdMap", dummyMap);

        PowerMockito.doThrow(mock(JSONException.class))
                .when(target, "deleteFlow", eq(nwIf), eq(NW_ID), any(Flow.class), anyString());

        target.onFlowDelete(NW_ID, flow);

        PowerMockito.verifyPrivate(target, times(1)).invoke("deleteFlow",
                eq(nwIf), eq(NW_ID), any(Flow.class), anyString());

        verify(dummyLogger, times(1)).error(target.seqNo + "\t" + "get JSONException.");
        verify(dummyLogger, never()).info(target.seqNo + "\t" + "■onFlowDelete End");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#onFlowDelete()}
     */
    @Test
    public void onFlowDeleteWithIOException() throws Exception {
        Logger dummyLogger = mock(Logger.class);
        Field field = target.getClass().getSuperclass().getSuperclass().getDeclaredField("logger");
        field.setAccessible(true);
        field.set(target, dummyLogger);

        Flow flow = Mockito.spy(new Flow("flowId"));
        flow.setStatus(FlowStatus.ESTABLISHED.toString());
        flow.setEnabled(true);

        NetworkInterface nwIf = PowerMockito.mock(NetworkInterface.class);

        HashMap<String, NetworkInterface> networkIfs = new HashMap<>();
        networkIfs.put(NW_ID, nwIf);

        Whitebox.setInternalState(target, "networkInterfaces", networkIfs);

        Map<String, String> dummyMap = new HashMap<>();
        dummyMap.put(OCH_LAYER, "differentId");
        Whitebox.setInternalState(target, "connectionIdMap", dummyMap);

        PowerMockito.doThrow(mock(IOException.class))
                .when(target, "deleteFlow", eq(nwIf), eq(NW_ID), any(Flow.class), anyString());

        target.onFlowDelete(NW_ID, flow);

        PowerMockito.verifyPrivate(target, times(1)).invoke("deleteFlow",
                eq(nwIf), eq(NW_ID), any(Flow.class), anyString());

        verify(dummyLogger, times(1)).error(target.seqNo + "\t" + "get IOException.");
        verify(dummyLogger, never()).info(target.seqNo + "\t" + "■onFlowDelete End");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#onFlowProcess()}
     */
    @Test
    public void testOnFlowProcess() throws Exception {
        Logger dummyLogger = mock(Logger.class);
        Field field = target.getClass().getSuperclass().getSuperclass().getDeclaredField("logger");
        field.setAccessible(true);
        field.set(target, dummyLogger);

        Flow flow = Mockito.spy(new Flow(NW_ID));
        flow.setStatus(FlowStatus.ESTABLISHING.toString());

        BasicFlow dummyBasicFlow = spy(new BasicFlow("flowId"));
        dummyBasicFlow.addMatch(new BasicFlowMatch("inNode", "inPort"));
        when(dummyBasicFlow.getStatus()).thenReturn("establishing");

        NetworkInterface nwIf = PowerMockito.spy(new NetworkInterface(dispatcher, NW_ID));
        doReturn(dummyBasicFlow).when(nwIf).getFlow(NW_ID);

        HashMap<String, NetworkInterface> networkIfs = new HashMap<String, NetworkInterface>();
        networkIfs.put(NW_ID, nwIf);

        Whitebox.setInternalState(target, "networkInterfaces", networkIfs);
        PowerMockito.doReturn(mock(Response.class)).when(target, "flowMessageManager", eq(nwIf),
                eq(NW_ID), eq(Logic.CONN_ADD), any(Flow.class), anyString());

        target.onFlowAdded(NW_ID, flow);

        verify(target, times(1)).flowMessageManager(nwIf, NW_ID, Logic.CONN_ADD, dummyBasicFlow,
                target.seqNo);

        verify(dummyLogger, times(1)).info(target.seqNo + "\t" + "■onFlowProcess End");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#onFlowProcess()}
     */
    @Test
    public void testOnFlowProcessWithJSONException() throws Exception {
        Logger dummyLogger = mock(Logger.class);
        Field field = target.getClass().getSuperclass().getSuperclass().getDeclaredField("logger");
        field.setAccessible(true);
        field.set(target, dummyLogger);

        Flow flow = Mockito.spy(new Flow(NW_ID));
        flow.setStatus(FlowStatus.ESTABLISHING.toString());

        BasicFlow dummyBasicFlow = spy(new BasicFlow("flowId"));
        dummyBasicFlow.addMatch(new BasicFlowMatch("inNode", "inPort"));
        when(dummyBasicFlow.getStatus()).thenReturn(FlowStatus.ESTABLISHING.toString());

        NetworkInterface nwIf = PowerMockito.spy(new NetworkInterface(dispatcher, NW_ID));
        doReturn(dummyBasicFlow).when(nwIf).getFlow(NW_ID);

        HashMap<String, NetworkInterface> networkIfs = new HashMap<String, NetworkInterface>();
        networkIfs.put(NW_ID, nwIf);

        Whitebox.setInternalState(target, "networkInterfaces", networkIfs);
        PowerMockito.doThrow(mock(JSONException.class)).when(target, "flowMessageManager", eq(nwIf),
                eq(NW_ID), eq(Logic.CONN_ADD), any(Flow.class), anyString());

        target.onFlowAdded(NW_ID, flow);

        verify(target, times(1)).flowMessageManager(nwIf, NW_ID, Logic.CONN_ADD, dummyBasicFlow,
                target.seqNo);
        verify(dummyLogger, times(1)).error(target.seqNo + "\t" + "get JSONException.");
        verify(dummyLogger, never()).info(target.seqNo + "\t" + "■onFlowProcess End");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#onFlowProcess()}
     */
    @Test
    public void testOnFlowProcessWithIOException() throws Exception {
        Logger dummyLogger = mock(Logger.class);
        Field field = target.getClass().getSuperclass().getSuperclass().getDeclaredField("logger");
        field.setAccessible(true);
        field.set(target, dummyLogger);

        Flow flow = Mockito.spy(new Flow(NW_ID));
        flow.setStatus(FlowStatus.ESTABLISHING.toString());

        BasicFlow dummyBasicFlow = spy(new BasicFlow("flowId"));
        dummyBasicFlow.addMatch(new BasicFlowMatch("inNode", "inPort"));
        when(dummyBasicFlow.getStatus()).thenReturn("establishing");

        NetworkInterface nwIf = PowerMockito.spy(new NetworkInterface(dispatcher, NW_ID));
        doReturn(dummyBasicFlow).when(nwIf).getFlow(NW_ID);

        HashMap<String, NetworkInterface> networkIfs = new HashMap<String, NetworkInterface>();
        networkIfs.put(NW_ID, nwIf);

        Whitebox.setInternalState(target, "networkInterfaces", networkIfs);
        PowerMockito.doThrow(mock(IOException.class)).when(target, "flowMessageManager", eq(nwIf),
                eq(NW_ID), eq(Logic.CONN_ADD), any(Flow.class), anyString());

        target.onFlowAdded(NW_ID, flow);

        verify(target, times(1)).flowMessageManager(nwIf, NW_ID, Logic.CONN_ADD, dummyBasicFlow,
                target.seqNo);
        verify(dummyLogger, times(1)).error(target.seqNo + "\t" + "get IOException.");
        verify(dummyLogger, never()).info(target.seqNo + "\t" + "■onFlowProcess End");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#onLinkAdded()}
     */
    @Test
    public void testOnLinkAdded() throws Exception {
        Link link = Mockito.spy(new Link(NW_ID));
        when(link.getAttribute(AttrElements.ESTABLISHMENT_STATUS))
                .thenReturn(FlowObject.FlowStatus.ESTABLISHED.toString());

        PowerMockito.doNothing().when(target, "onLinkProcess",
                NW_ID, link, Logic.CONN_ADD);

        target.onLinkAdded(NW_ID, link);

        PowerMockito.verifyPrivate(target, times(1)).invoke("onLinkProcess",
                NW_ID, link, Logic.CONN_ADD);
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#onLinkUpdate()}
     */
    @Test
    public void testOnLinkUpdate() throws Exception {
        Link prev = Mockito.mock(Link.class);
        Link curr = spy(new Link());
        curr.putAttribute(AttrElements.ESTABLISHMENT_STATUS, FlowStatus.ESTABLISHED.toString());

        ArrayList<String> attributesList = new ArrayList<String>();

        PowerMockito.doNothing().when(target, "onLinkProcess",
                NW_ID, curr, Logic.CONN_UPDATE);

        target.onLinkUpdate(NW_ID, prev, curr, attributesList);

        PowerMockito.verifyPrivate(target, times(1)).invoke("onLinkProcess",
                NW_ID, curr, Logic.CONN_UPDATE);
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#onLinkDelete()}
     */
    @Test
    public void testOnLinkDelete() {
        Link link = Mockito.spy(new Link(NW_ID));
        link.getAttributes().put(AttrElements.ESTABLISHMENT_STATUS,
                FlowObject.FlowStatus.ESTABLISHED.toString());

        NetworkInterface networkIf = spy(new NetworkInterface(dispatcher, NW_ID));

        HashMap<String, NetworkInterface> networkIfs = new HashMap<String, NetworkInterface>();
        networkIfs.put(NW_ID, networkIf);
        Whitebox.setInternalState(target, "networkInterfaces", networkIfs);

        PowerMockito.doNothing().when(target).deleteLink(any(NetworkInterface.class), eq(NW_ID),
                any(Link.class), anyString());

        target.onLinkDelete(NW_ID, link);

        verify(target, times(1)).deleteLink(networkIf, NW_ID, link, target.seqNo);
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#onLinkDelete()}
     */
    @Test
    public void testOnLinkDeleteWithNotStatusEstablished() {
        Link link = Mockito.spy(new Link(NW_ID));
        link.getAttributes().put(AttrElements.ESTABLISHMENT_STATUS,
                FlowObject.FlowStatus.ESTABLISHING.toString());

        NetworkInterface networkIf = spy(new NetworkInterface(dispatcher, NW_ID));

        HashMap<String, NetworkInterface> networkIfs = new HashMap<String, NetworkInterface>();
        networkIfs.put(NW_ID, networkIf);
        Whitebox.setInternalState(target, "networkInterfaces", networkIfs);

        PowerMockito.doNothing().when(target).deleteLink(eq(networkIf), eq(NW_ID), any(Link.class),
                eq(target.seqNo));

        target.onLinkDelete(NW_ID, link);

        verify(target, never()).deleteLink(networkIf, NW_ID, link, target.seqNo);
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#onLinkProcess()}
     */
    @Test
    public void testOnLinkProcess() throws Exception {
        Logger dummyLogger = mock(Logger.class);
        Field field = target.getClass().getSuperclass().getSuperclass().getDeclaredField("logger");
        field.setAccessible(true);
        field.set(target, dummyLogger);

        Link link = spy(new Link("linkId"));
        link.getAttributes()
                .put(AttrElements.ESTABLISHMENT_STATUS, FlowStatus.ESTABLISHED.toString());

        NetworkInterface nwIf = PowerMockito.spy(new NetworkInterface(dispatcher, NW_ID));

        HashMap<String, NetworkInterface> networkIfs = new HashMap<String, NetworkInterface>();
        networkIfs.put(NW_ID, nwIf);
        Whitebox.setInternalState(target, "networkInterfaces", networkIfs);

        PowerMockito.doNothing().when(target, "linkMessageManager",
                eq(nwIf), eq(NW_ID), eq(Logic.CONN_ADD), eq(link), anyString());

        target.onLinkAdded(NW_ID, link);

        verify(target, times(1))
                .linkMessageManager(eq(nwIf), eq(NW_ID), eq(Logic.CONN_ADD), eq(link),
                        eq(target.seqNo));
        verify(dummyLogger, times(1)).info(target.seqNo + "\t" + "■onLinkProcess End");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#onLinkProcess()}
     */
    @Test
    public void testOnLinkProcessWithNOTStatusEstablished()
            throws Exception {
        Link link = spy(new Link("linkId"));
        link.getAttributes().put(AttrElements.ESTABLISHMENT_STATUS,
                FlowObject.FlowStatus.ESTABLISHING.toString());

        NetworkInterface nwIf = PowerMockito.spy(new NetworkInterface(dispatcher, NW_ID));

        HashMap<String, NetworkInterface> networkIfs = new HashMap<String, NetworkInterface>();
        networkIfs.put(NW_ID, nwIf);
        Whitebox.setInternalState(target, "networkInterfaces", networkIfs);

        PowerMockito.doNothing().when(target, "linkMessageManager",
                eq(nwIf), eq(NW_ID), eq(Logic.CONN_ADD), eq(link), anyString());

        target.onLinkAdded(NW_ID, link);

        verify(target, never()).linkMessageManager(nwIf, NW_ID, Logic.CONN_ADD, link,
                target.seqNo);
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#onLinkProcess()}
     */
    @Test
    public void testOnLinkProcessWithJSONException() throws Exception {
        Logger dummyLogger = mock(Logger.class);
        Field field = target.getClass().getSuperclass().getSuperclass().getDeclaredField("logger");
        field.setAccessible(true);
        field.set(target, dummyLogger);

        Link link = spy(new Link("linkId"));
        link.getAttributes()
                .put(AttrElements.ESTABLISHMENT_STATUS, FlowStatus.ESTABLISHED.toString());

        NetworkInterface nwIf = PowerMockito.spy(new NetworkInterface(dispatcher, NW_ID));

        HashMap<String, NetworkInterface> networkIfs = new HashMap<String, NetworkInterface>();
        networkIfs.put(NW_ID, nwIf);
        Whitebox.setInternalState(target, "networkInterfaces", networkIfs);

        PowerMockito.doThrow(mock(JSONException.class)).when(target, "linkMessageManager",
                eq(nwIf), eq(NW_ID), eq(Logic.CONN_ADD), eq(link), anyString());

        Whitebox.invokeMethod(target, "onLinkProcess", NW_ID, link, Logic.CONN_ADD);

        verify(target, times(1))
                .linkMessageManager(nwIf, NW_ID, Logic.CONN_ADD, link, target.seqNo);
        verify(dummyLogger, times(1)).error(target.seqNo + "\t" + "get JSONException.");
        verify(dummyLogger, never()).info(target.seqNo + "\t" + "■onLinkProcess End");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#onLinkProcess()}
     */
    @Test
    public void testOnLinkProcessWithIOException() throws Exception {
        Logger dummyLogger = mock(Logger.class);
        Field field = target.getClass().getSuperclass().getSuperclass().getDeclaredField("logger");
        field.setAccessible(true);
        field.set(target, dummyLogger);

        Link link = spy(new Link("linkId"));
        link.getAttributes()
                .put(AttrElements.ESTABLISHMENT_STATUS, FlowStatus.ESTABLISHED.toString());

        NetworkInterface nwIf = PowerMockito.spy(new NetworkInterface(dispatcher, NW_ID));

        HashMap<String, NetworkInterface> networkIfs = new HashMap<String, NetworkInterface>();
        networkIfs.put(NW_ID, nwIf);
        Whitebox.setInternalState(target, "networkInterfaces", networkIfs);

        PowerMockito.doThrow(mock(IOException.class)).when(target, "linkMessageManager",
                eq(nwIf), eq(NW_ID), eq(Logic.CONN_ADD), eq(link), anyString());

        Whitebox.invokeMethod(target, "onLinkProcess", NW_ID, link, Logic.CONN_ADD);

        verify(target, times(1))
                .linkMessageManager(nwIf, NW_ID, Logic.CONN_ADD, link, target.seqNo);
        verify(dummyLogger, times(1)).error(target.seqNo + "\t" + "get IOException.");
        verify(dummyLogger, never()).info(target.seqNo + "\t" + "■onLinkProcess End");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#getFlow()}
     */
    @Test
    public void testGetFlow() {
        BasicFlow flow = spy(new BasicFlow("flowId"));
        flow.addMatch(new BasicFlowMatch("inNode", "inPort"));

        NetworkInterface nwIf = PowerMockito.spy(new NetworkInterface(dispatcher, NW_ID));
        PowerMockito.doReturn(flow).when(nwIf).getFlow(eq("flowId"));

        assertThat(target.getFlow(nwIf, flow.getFlowId()), is(flow));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#getFlow()}
     */
    @Test
    public void testGetFlowWithNullNwIf()
            throws NoSuchFieldException, SecurityException, IllegalArgumentException,
            IllegalAccessException {
        Logger dummyLogger = mock(Logger.class);
        Field field = target.getClass().getSuperclass().getSuperclass().getDeclaredField("logger");
        field.setAccessible(true);
        field.set(target, dummyLogger);

        BasicFlow flow = spy(new BasicFlow("flowId"));
        flow.addMatch(new BasicFlowMatch("inNode", "inPort"));

        NetworkInterface nwIf = null;

        assertThat(target.getFlow(nwIf, null), is(nullValue()));
        verify(dummyLogger, times(1)).error(target.seqNo + "\t" + "networkIF is null.");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#getFlow()}
     */
    @Test
    public void testGetFlowWithNullFlowId() {
        BasicFlow flow = spy(new BasicFlow());
        flow.addMatch(new BasicFlowMatch("inNode", "inPort"));

        NetworkInterface nwIf = PowerMockito.spy(new NetworkInterface(dispatcher, NW_ID));
        PowerMockito.doReturn(flow).when(nwIf).getFlow(eq("flowId"));

        assertThat(target.getFlow(nwIf, flow.getFlowId()), is(nullValue()));
        verify(nwIf, never()).getFlow(flow.getFlowId());
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#getFlow()}
     */
    @Test
    public void testGetFlowWithNotBasicFlow()
            throws NoSuchFieldException, SecurityException, IllegalArgumentException,
            IllegalAccessException {
        Logger dummyLogger = mock(Logger.class);
        Field field = target.getClass().getSuperclass().getSuperclass().getDeclaredField("logger");
        field.setAccessible(true);
        field.set(target, dummyLogger);

        String flowId = "flowId";
        OFPFlow ofpFlow = spy(new OFPFlow(flowId));

        NetworkInterface nwIf = PowerMockito.spy(new NetworkInterface(dispatcher, NW_ID));
        PowerMockito.doReturn(ofpFlow).when(nwIf).getFlow(eq(flowId));

        assertThat(target.getFlow(nwIf, ofpFlow.getFlowId()), is(nullValue()));

        verify(ofpFlow, never()).getMatches();
        verify(dummyLogger, times(1)).error(target.seqNo + "\t" + "flow type mismatch.");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#getFlow()}
     */
    @Test
    public void testGetFlowWithNullMatchInfo()
            throws NoSuchFieldException, SecurityException, IllegalArgumentException,
            IllegalAccessException {
        Logger dummyLogger = mock(Logger.class);
        Field field = target.getClass().getSuperclass().getSuperclass().getDeclaredField("logger");
        field.setAccessible(true);
        field.set(target, dummyLogger);

        BasicFlow flow = spy(new BasicFlow("flowId"));
        flow.matches = null;

        NetworkInterface nwIf = PowerMockito.spy(new NetworkInterface(dispatcher, NW_ID));
        PowerMockito.doReturn(flow).when(nwIf).getFlow(eq("flowId"));

        assertThat(target.getFlow(nwIf, flow.getFlowId()), is(nullValue()));
        verify(dummyLogger, times(1)).error(target.seqNo + "\t" + "flow matches is null.");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#getFlow()}
     */
    @Test
    public void testGetFlowWithEmptyMatchInfo()
            throws NoSuchFieldException, SecurityException, IllegalArgumentException,
            IllegalAccessException {
        Logger dummyLogger = mock(Logger.class);

        Field field = target.getClass().getSuperclass().getSuperclass().getDeclaredField("logger");
        field.setAccessible(true);
        field.set(target, dummyLogger);

        BasicFlow flow = spy(new BasicFlow("flowId"));

        NetworkInterface nwIf = PowerMockito.spy(new NetworkInterface(dispatcher, NW_ID));
        PowerMockito.doReturn(flow).when(nwIf).getFlow(eq("flowId"));

        assertThat(target.getFlow(nwIf, flow.getFlowId()), is(nullValue()));
        verify(dummyLogger, times(1)).error(target.seqNo + "\t" + "flow matches is empty.");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#formFlowMessage()}
     */
    @Test
    public void testFormFlowMessage() throws Exception {
        String linkId = "linkId";
        String bandwidth = "bandwidth";
        String latency = "latency";
        String srcPort = "NW=SDNNP-POC,NE=PE1,Layer=outerLSP,TTP=1";
        String dstPort = "NW=SDNNP-POC,NE=PE1,Layer=outerLSP,CTP=2";
        String srcPort2 = "NW=SDNNP-POC,NE=PE1,Layer=outerLSP,CTP=3";
        String dstPort2 = "NW=SDNNP-POC,NE=PE1,Layer=outerLSP,TTP=4";

        BasicFlow flow = spy(new BasicFlow("flowId"));

        Map<String, Link> links = new HashMap<>();
        Link link = new Link(linkId, "srcNode", dstPort, "dstNode", srcPort2);
        links.put(linkId, link);

        OduFlowCreationResponse response = new OduFlowCreationResponse();
        response.setFjFlowId("routeId");
        Constraint constraint = new Constraint();
        constraint.setBandwidth(bandwidth);
        constraint.setLatency(latency);
        response.setConstraint(constraint);

        TerminationPoints point = new TerminationPoints();
        point.setInPoint(srcPort);
        point.setOutPoint(dstPort);

        TerminationPoints pointPairs2 = new TerminationPoints();
        pointPairs2.setInPoint(srcPort2);
        pointPairs2.setOutPoint(dstPort2);

        List<TerminationPoints> terminationPointPairs = new ArrayList<>();
        terminationPointPairs.add(point);
        terminationPointPairs.add(pointPairs2);
        response.setTerminationPointPairs(terminationPointPairs);

        Whitebox.invokeMethod(target, "formFlowMessage", flow, response, links, TRANSACTION_ID);

        verify(flow, times(1)).addPath(linkId);
        verify(flow, times(1)).putAttribute(Logic.AttrElements.LATENCY, latency);
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#formFlowMessage()}
     */
    @Test
    public void testFormFlowMessageWithCutThrough() throws Exception {
        String linkId1 = "linkId1";
        String linkId2 = "linkId2";
        String bandwidth = "bandwidth";
        String latency = "latency";

        String srcPort = "NW=SDN,NE=FW9500SDN002,Layer=ODU,TTP=20";
        String dstPort = "NW=SDN,NE=FW9500SDN002,Layer=ODU,CTP=9";

        String srcPort2 = "NW=SDN,NE=FW9500SDN003,Layer=ODU,CTP=3";
        String dstPort2 = "NW=SDN,NE=FW9500SDN003,Layer=ODU,CTP=11";

        String srcPort3 = "NW=SDN,NE=FW9500SDN004,Layer=ODU,CTP=9";
        String dstPort3 = "NW=SDN,NE=FW9500SDN004,Layer=ODU,TTP=19";

        BasicFlow flow = spy(new BasicFlow("flowId"));

        Map<String, Link> links = new HashMap<>();
        Link link1 = new Link(linkId1, "srcNode1", dstPort, "dstNode1", srcPort2);
        Link link2 = new Link(linkId2, "srcNode2", dstPort2, "dstNode2", srcPort3);
        links.put(linkId1, link1);
        links.put(linkId2, link2);

        OduFlowCreationResponse response = new OduFlowCreationResponse();
        response.setFjFlowId("routeId");
        Constraint constraint = new Constraint();
        constraint.setBandwidth(bandwidth);
        constraint.setLatency(latency);
        response.setConstraint(constraint);

        TerminationPoints point = new TerminationPoints();
        point.setInPoint(srcPort);
        point.setOutPoint(dstPort);

        TerminationPoints pointPairs2 = new TerminationPoints();
        pointPairs2.setInPoint(srcPort2);
        pointPairs2.setOutPoint(dstPort2);

        TerminationPoints pointPairs3 = new TerminationPoints();
        pointPairs3.setInPoint(srcPort3);
        pointPairs3.setOutPoint(dstPort3);

        List<TerminationPoints> terminationPointPairs = new ArrayList<>();
        terminationPointPairs.add(point);
        terminationPointPairs.add(pointPairs2);
        terminationPointPairs.add(pointPairs3);
        response.setTerminationPointPairs(terminationPointPairs);

        Whitebox.invokeMethod(target, "formFlowMessage", flow, response, links, TRANSACTION_ID);

        verify(flow, times(1)).addPath(linkId1);
        verify(flow, times(1)).addPath(linkId2);
        verify(flow, times(1)).putAttribute(Logic.AttrElements.LATENCY, latency);
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#formFlowMessage()}
     */
    @Test
    public void testFormFlowMessageWithEmptyLinks() throws Exception {
        String linkId = "linkId";
        String bandwidth = "bandwidth";
        String latency = "latency";
        String srcPort = "NW=SDNNP-POC,NE=PE1,Layer=outerLSP,CTP=1";
        String dstPort = "NW=SDNNP-POC,NE=PE1,Layer=outerLSP,TTP=2";
        String srcPort2 = "NW=SDNNP-POC,NE=PE1,Layer=outerLSP,TTP=3";
        String dstPort2 = "NW=SDNNP-POC,NE=PE1,Layer=outerLSP,TTP=4";

        BasicFlow flow = spy(new BasicFlow("flowId"));

        Map<String, Link> links = new HashMap<>();

        OduFlowCreationResponse response = spy(new OduFlowCreationResponse());
        response.setFjFlowId("routeId");
        Constraint constraint = new Constraint();
        constraint.setBandwidth(bandwidth);
        constraint.setLatency(latency);
        response.setConstraint(constraint);

        TerminationPoints pointPairs = new TerminationPoints();
        pointPairs.setInPoint(srcPort);
        pointPairs.setOutPoint(dstPort);

        TerminationPoints pointPairs2 = new TerminationPoints();
        pointPairs2.setInPoint(srcPort2);
        pointPairs2.setOutPoint(dstPort2);

        ArrayList<TerminationPoints> terminationPointPairs = new ArrayList<>();
        terminationPointPairs.add(pointPairs);
        terminationPointPairs.add(pointPairs2);
        response.setTerminationPointPairs(terminationPointPairs);

        List<TerminationPoints> dummyCtppPoints = new ArrayList<>();
        TerminationPoints ctpps = new TerminationPoints();
        ctpps.setInPoint(srcPort);
        ctpps.setOutPoint(dstPort2);
        dummyCtppPoints.add(ctpps);
        PowerMockito.doReturn(dummyCtppPoints).when(target, "makeCtpPoints", eq(response));

        Whitebox.invokeMethod(target, "formFlowMessage", flow, response, links, TRANSACTION_ID);

        verify(response, never()).getTerminationPointPairs();
        verify(flow, never()).addPath(linkId);
        verify(flow, times(1)).putAttribute(Logic.AttrElements.LATENCY, latency);
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#formFlowMessage()}
     */
    @Test
    public void testFormFlowMessageWithEmptyTerminationPointPairs()
            throws Exception {
        String linkId = "linkId";
        String bandwidth = "bandwidth";
        String latency = "latency";
        String srcPort = "NW=SDNNP-POC,NE=PE1,Layer=outerLSP,TTP=1";
        String dstPort = "NW=SDNNP-POC,NE=PE1,Layer=outerLSP,CTP=2";

        BasicFlow flow = spy(new BasicFlow("flowId"));

        Map<String, Link> links = new HashMap<>();
        Link link = spy(new Link(linkId, "srcNode", srcPort, "dstNode", dstPort));
        links.put(linkId, link);

        OduFlowCreationResponse response = new OduFlowCreationResponse();
        response.setFjFlowId("routeId");
        Constraint constraint = new Constraint();
        constraint.setBandwidth(bandwidth);
        constraint.setLatency(latency);
        response.setConstraint(constraint);

        ArrayList<TerminationPoints> terminationPointPairs = new ArrayList<>();
        response.setTerminationPointPairs(terminationPointPairs);

        Whitebox.invokeMethod(target, "formFlowMessage", flow, response, links, TRANSACTION_ID);

        verify(link, never()).getSrcNode();
        verify(flow, never()).addPath(linkId);
        verify(flow, times(1)).putAttribute(Logic.AttrElements.LATENCY, latency);
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#formFlowMessage()}
     */
    @Test
    public void testFormFlowMessageWithMismatchSrcPorts() throws Exception {
        String linkId = "linkId";
        String bandwidth = "bandwidth";
        String latency = "latency";
        String srcPort = "NW=SDNNP-POC,NE=PE1,Layer=outerLSP,TTP=1";
        String dstPort = "NW=SDNNP-POC,NE=PE1,Layer=outerLSP,CTP=2";
        String srcPort2 = "NW=SDNNP-POC,NE=PE1,Layer=outerLSP,CTP=3";
        String dstPort2 = "NW=SDNNP-POC,NE=PE1,Layer=outerLSP,TTP=4";

        BasicFlow flow = spy(new BasicFlow("flowId"));

        Map<String, Link> links = new HashMap<>();
        Link link = new Link(linkId, "srcNode", dstPort, "dstNode", srcPort2);
        links.put(linkId, link);

        OduFlowCreationResponse response = new OduFlowCreationResponse();
        response.setFjFlowId("routeId");
        Constraint constraint = new Constraint();
        constraint.setBandwidth(bandwidth);
        constraint.setLatency(latency);
        response.setConstraint(constraint);

        TerminationPoints pointPairs = new TerminationPoints();
        pointPairs.setInPoint(srcPort);
        pointPairs.setOutPoint("NW=SDNNP-POC,NW=SDN,NE=PE1,Layer=outerLSP,CTP=different");

        TerminationPoints pointPairs2 = new TerminationPoints();
        pointPairs2.setInPoint(srcPort2);
        pointPairs2.setOutPoint(dstPort2);

        ArrayList<TerminationPoints> terminationPointPairs = new ArrayList<>();
        terminationPointPairs.add(pointPairs);
        terminationPointPairs.add(pointPairs2);
        response.setTerminationPointPairs(terminationPointPairs);

        Whitebox.invokeMethod(target, "formFlowMessage", flow, response, links, TRANSACTION_ID);

        verify(flow, never()).addPath(linkId);
        verify(flow, times(1)).putAttribute(Logic.AttrElements.LATENCY, latency);
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#formFlowMessage()}
     */
    @Test
    public void testFormFlowMessageWithMismatchDstPorts()
            throws Exception {
        String linkId = "linkId";
        String bandwidth = "bandwidth";
        String latency = "latency";
        String dstPort = "NW=SDNNP-POC,NE=PE1,Layer=outerLSP,CTP=2";
        String srcPort2 = "NW=SDNNP-POC,NE=PE1,Layer=outerLSP,CTP=3";
        String dstPort2 = "NW=SDNNP-POC,NE=PE1,Layer=outerLSP,TTP=4";

        BasicFlow flow = spy(new BasicFlow("flowId"));

        Map<String, Link> links = new HashMap<>();
        Link link = new Link(linkId, "srcNode", dstPort, "dstNode", srcPort2);
        links.put(linkId, link);

        OduFlowCreationResponse response = new OduFlowCreationResponse();
        response.setFjFlowId("routeId");
        Constraint constraint = new Constraint();
        constraint.setBandwidth(bandwidth);
        constraint.setLatency(latency);
        response.setConstraint(constraint);

        TerminationPoints pointPairs = new TerminationPoints();
        pointPairs.setInPoint("NW=SDNNP-POC,NE=PE1,Layer=outerLSP,CTP=differentPort");
        pointPairs.setOutPoint(dstPort);

        TerminationPoints pointPairs2 = new TerminationPoints();
        pointPairs2.setInPoint(srcPort2);
        pointPairs2.setOutPoint(dstPort2);

        ArrayList<TerminationPoints> terminationPointPairs = new ArrayList<>();
        terminationPointPairs.add(pointPairs);
        terminationPointPairs.add(pointPairs2);
        response.setTerminationPointPairs(terminationPointPairs);

        Whitebox.invokeMethod(target, "formFlowMessage", flow, response, links, TRANSACTION_ID);

        verify(flow, never()).addPath(linkId);
        verify(flow, times(1)).putAttribute(Logic.AttrElements.LATENCY, latency);
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#registerOdu()}
     */
    @Test
    public void testRegisterOdu() throws Exception {
        BasicFlow flow = mock(BasicFlow.class);
        Event event = mock(Event.class);

        ResourceSendController sender = PowerMockito.spy(new ResourceSendController());
        doReturn(mock(OduFlowCreationResponse.class)).when(sender)
                .registerNewOduFlow(flow, event, SEQUENCE_NO);
        PowerMockito.whenNew(ResourceSendController.class).withNoArguments().thenReturn(sender);

        assertThat(Whitebox.invokeMethod(target, "registerOdu", SEQUENCE_NO, flow, event),
                is(instanceOf(OduFlowCreationResponse.class)));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#deleteResource()}
     */
    @Test
    public void testDeleteResource() throws Exception {
        Flow flow = mock(BasicFlow.class);
        ResourceInfoFlomMf resource = mock(ResourceInfoFlomMf.class);

        ResourceSendController sender = PowerMockito.spy(new ResourceSendController());
        doReturn(true).when(sender).deleteOduFlow(resource, flow, SEQUENCE_NO);
        PowerMockito.whenNew(ResourceSendController.class).withNoArguments().thenReturn(sender);

        boolean result = Whitebox.invokeMethod(target, "deleteResource", flow, SEQUENCE_NO,
                resource);
        assertThat(result, is(true));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#sendToporogy()}
     */
    @Test
    public void testSendTopology() throws Exception {
        Topology topology = new Topology();

        Node node1 = new Node("nodeId1");
        topology.nodes.put(node1.getId(), node1);

        Node node2 = new Node("nodeId2");
        topology.nodes.put(node2.getId(), node2);

        Node node3 = new Node("nodeId3");
        topology.nodes.put(node3.getId(), node3);

        Link link1 = new Link("linkId1");
        topology.links.put(link1.getId(), link1);

        Link link2 = new Link("linkId2");
        topology.links.put(link2.getId(), link2);

        Link link3 = new Link("linkId3");
        topology.links.put(link3.getId(), link3);

        NetworkInterface nwIf = PowerMockito.spy(new NetworkInterface(dispatcher, NW_ID));
        PowerMockito.doReturn(mock(Response.class)).when(nwIf).putNode(any(Node.class));
        PowerMockito.doReturn(mock(Response.class)).when(nwIf).putLink(any(Link.class));
        PowerMockito.doReturn(spy(new Node())).when(nwIf).getNode(anyString());
        PowerMockito.doReturn(spy(new Link())).when(nwIf).getLink(anyString());

        HashMap<String, NetworkInterface> networkIfs = new HashMap<String, NetworkInterface>();
        networkIfs.put(NW_ID, nwIf);
        Whitebox.setInternalState(target, "networkInterfaces", networkIfs);

        target.sendToporogy(nwIf, NW_ID, topology);

        verify(nwIf, times(3)).putNode(any(Node.class));
        verify(nwIf, times(3)).putLink(any(Link.class));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#sendToporogy()}
     */
    @Test
    public void testSendTopologyWithEmptyNode() throws Exception {
        Topology topology = new Topology();

        Link link1 = new Link("linkId1");
        topology.links.put(link1.getId(), link1);

        Link link2 = new Link("linkId2");
        topology.links.put(link2.getId(), link2);

        Link link3 = new Link("linkId3");
        topology.links.put(link3.getId(), link3);

        NetworkInterface nwIf = PowerMockito.spy(new NetworkInterface(dispatcher, NW_ID));
        PowerMockito.doReturn(mock(Response.class)).when(nwIf).putNode(any(Node.class));
        PowerMockito.doReturn(mock(Response.class)).when(nwIf).putLink(any(Link.class));
        PowerMockito.doReturn(spy(new Node())).when(nwIf).getNode(anyString());
        PowerMockito.doReturn(spy(new Link())).when(nwIf).getLink(anyString());

        HashMap<String, NetworkInterface> networkIfs = new HashMap<String, NetworkInterface>();
        networkIfs.put(NW_ID, nwIf);
        Whitebox.setInternalState(target, "networkInterfaces", networkIfs);

        target.sendToporogy(nwIf, NW_ID, topology);

        verify(nwIf, never()).putNode(any(Node.class));
        verify(nwIf, times(3)).putLink(any(Link.class));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#sendToporogy()}
     */
    @Test
    public void testSendTopologyWithEmptyLink() throws Exception {
        Topology topology = new Topology();

        Node node1 = new Node("nodeId1");
        topology.nodes.put(node1.getId(), node1);

        Node node2 = new Node("nodeId2");
        topology.nodes.put(node2.getId(), node2);

        Node node3 = new Node("nodeId3");
        topology.nodes.put(node3.getId(), node3);

        NetworkInterface nwIf = PowerMockito.spy(new NetworkInterface(dispatcher, NW_ID));
        PowerMockito.doReturn(mock(Response.class)).when(nwIf).putNode(any(Node.class));
        PowerMockito.doReturn(mock(Response.class)).when(nwIf).putLink(any(Link.class));
        PowerMockito.doReturn(spy(new Node())).when(nwIf).getNode(anyString());
        PowerMockito.doReturn(spy(new Link())).when(nwIf).getLink(anyString());

        HashMap<String, NetworkInterface> networkIfs = new HashMap<String, NetworkInterface>();
        networkIfs.put(NW_ID, nwIf);
        Whitebox.setInternalState(target, "networkInterfaces", networkIfs);

        target.sendToporogy(nwIf, NW_ID, topology);

        verify(nwIf, times(3)).putNode(any(Node.class));
        verify(nwIf, never()).putLink(any(Link.class));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.PacketDriver#sendTopologyToOdenos()}
     */
    @Test
    public void testSendTopologyToOdenos() throws Exception {
        String body = "{\"upper\":"
                + "{\"node\":["
                + "{\"nodeId\":\"NW=SDN,NE=FW9500SDN001\"}],"
                + "\"port\":["
                + "{\"portId\":\"NW=SDN,NE=FW9500SDN001,Layer=ODU,TTP=1\"},"
                + "{\"portId\":\"NW=SDN,NE=FW9500SDN001,Layer=ODU,TTP=2\"}],"
                + "\"link\":["
                + "{\"linkId\":\"linkId1\",\"srcTTP\":\"NW=SDN,NE=FW9500SDN001,Layer=ODU,TTP=1\","
                + "\"dstTTP\":\"NW=SDN,NE=FW9500SDN001,Layer=ODU,TTP=2\"}]"
                + "}}";
        String nwId = "networkId";

        target.connectionIdMap.put("och", nwId);

        NetworkInterface nwIf = PowerMockito.mock(NetworkInterface.class);

        HashMap<String, NetworkInterface> networkIfs = new HashMap<String, NetworkInterface>();
        networkIfs.put(nwId, nwIf);
        Whitebox.setInternalState(target, "networkInterfaces", networkIfs);

        doNothing().when(target).sendToporogy(eq(nwIf), eq(nwId),
                (Topology) anyObject());

        Response result = target.sendTopologyToOdenos("och", body);

        assertThat(result.statusCode, is(Response.OK));
        assertThat(result.getBody(String.class), is(""));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.PacketDriver#sendTopologyToOdenos()}
     */
    @Test
    public void testSendTopologyToOdenosWithException() throws Exception {
        String body = "{\"upper\":"
                + "{\"node\":["
                + "{\"nodeId\":\"NW=SDN,NE=FW9500SDN001\"}],"
                + "\"port\":["
                + "{\"portId\":\"NW=SDN,NE=FW9500SDN001,Layer=ODU,TTP=1\"},"
                + "{\"portId\":\"NW=SDN,NE=FW9500SDN001,Layer=ODU,TTP=2\"}],"
                + "\"link\":["
                + "{\"linkId\":\"linkId1\",\"srcTTP\":\"NW=SDN,NE=FW9500SDN001,Layer=ODU,TTP=1\","
                + "\"dstTTP\":\"NW=SDN,NE=FW9500SDN001,Layer=ODU,TTP=2\"}]"
                + "}}";
        String nwId = "networkId";

        target.connectionIdMap.put("och", nwId);

        JSONParser parser = spy(new JSONParser());
        doThrow(mock(IOException.class)).when(parser)
                .upperNodeInfotoPOJO((JSONObject) anyObject(), anyString());
        PowerMockito.whenNew(JSONParser.class).withNoArguments().thenReturn(parser);

        Response result = target.sendTopologyToOdenos("och", body);

        assertThat(result.statusCode, is(Response.BAD_REQUEST));
        assertThat(result.getBody(String.class), is(""));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#createOdenosFlow()}
     */
    @Test
    public void testCreateOdenosFlow() throws Exception {
        Logger dummyLogger = mock(Logger.class);
        Field field = target.getClass().getSuperclass().getDeclaredField("logger");
        field.setAccessible(true);
        field.set(target, dummyLogger);

        List<String> linkIds = new ArrayList<>();
        linkIds.add("linkId1");
        linkIds.add("linkId2");

        org.o3project.ocnrm.model.Flow flow1 = new org.o3project.ocnrm.model.Flow();
        flow1.setFlowId("flowId1");
        flow1.setSrcTTP("NW=SDNNP-POC,NE=PE1,Layer=outerLSP,CTP=1");
        flow1.setDstTTP("NW=SDNNP-POC,NE=PE2,Layer=outerLSP,CTP=2");
        flow1.setLinkId(linkIds);

        org.o3project.ocnrm.model.Flow flow2 = new org.o3project.ocnrm.model.Flow();
        flow2.setFlowId("flowId2");
        flow2.setSrcTTP("NW=SDNNP-POC,NE=PE3,Layer=outerLSP,CTP=1");
        flow2.setDstTTP("NW=SDNNP-POC,NE=PE4,Layer=outerLSP,CTP=2");
        flow2.setLinkId(linkIds);

        List<org.o3project.ocnrm.model.Flow> flows = new ArrayList<>();
        flows.add(flow1);
        flows.add(flow2);

        NetworkInterface nwIf = PowerMockito.spy(new NetworkInterface(dispatcher, NW_ID));
        PowerMockito.doReturn(mock(Response.class)).when(nwIf).putFlow(any(Flow.class));

        HashMap<String, NetworkInterface> networkIfs = new HashMap<String, NetworkInterface>();
        networkIfs.put(NW_ID, nwIf);
        Whitebox.setInternalState(target, "networkInterfaces", networkIfs);

        Whitebox.invokeMethod(target, "createOdenosFlow", nwIf, flows, SEQUENCE_NO);

        verify(dummyLogger, times(2)).debug("link ID: " + "linkId1");
        verify(dummyLogger, times(2)).debug("link ID: " + "linkId2");
        verify(nwIf, times(2)).putFlow(any(Flow.class));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#createOdenosFlow()}
     */
    @Test
    public void testCreateOdenosFlowWithEmptyLink() throws Exception {
        Logger dummyLogger = mock(Logger.class);
        Field field = target.getClass().getSuperclass().getDeclaredField("logger");
        field.setAccessible(true);
        field.set(target, dummyLogger);

        org.o3project.ocnrm.model.Flow flow1 = new org.o3project.ocnrm.model.Flow();
        flow1.setFlowId("flowId1");
        flow1.setSrcTTP("NW=SDNNP-POC,NE=PE1,Layer=outerLSP,CTP=1");
        flow1.setDstTTP("NW=SDNNP-POC,NE=PE2,Layer=outerLSP,CTP=2");
        flow1.setLinkId(new ArrayList<String>());

        org.o3project.ocnrm.model.Flow flow2 = new org.o3project.ocnrm.model.Flow();
        flow2.setFlowId("flowId2");
        flow2.setSrcTTP("NW=SDNNP-POC,NE=PE3,Layer=outerLSP,CTP=1");
        flow2.setDstTTP("NW=SDNNP-POC,NE=PE4,Layer=outerLSP,CTP=2");
        flow2.setLinkId(new ArrayList<String>());

        List<org.o3project.ocnrm.model.Flow> flows = new ArrayList<>();
        flows.add(flow1);
        flows.add(flow2);

        NetworkInterface nwIf = PowerMockito.spy(new NetworkInterface(dispatcher, NW_ID));
        PowerMockito.doReturn(mock(Response.class)).when(nwIf).putFlow(any(Flow.class));

        HashMap<String, NetworkInterface> networkIfs = new HashMap<String, NetworkInterface>();
        networkIfs.put(NW_ID, nwIf);
        Whitebox.setInternalState(target, "networkInterfaces", networkIfs);

        BasicFlow dummyFlow = spy(new BasicFlow());
        PowerMockito.whenNew(BasicFlow.class).withArguments(
                anyString(), anyString(), eq(true), anyString()).thenReturn(dummyFlow);

        Whitebox.invokeMethod(target, "createOdenosFlow", nwIf, flows, SEQUENCE_NO);

        verify(nwIf, times(2)).putFlow(any(Flow.class));
        verify(dummyFlow, never()).addPath(anyString());
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#createOdenosFlow()}
     */
    @Test
    public void testCreateOdenosFlowWithNoFlows() throws Exception {
        Logger dummyLogger = mock(Logger.class);
        Field field = target.getClass().getSuperclass().getDeclaredField("logger");
        field.setAccessible(true);
        field.set(target, dummyLogger);

        List<Flow> flows = null;

        NetworkInterface nwIf = PowerMockito.spy(new NetworkInterface(dispatcher, NW_ID));
        PowerMockito.doReturn(mock(Response.class)).when(nwIf).putFlow(any(Flow.class));

        HashMap<String, NetworkInterface> networkIfs = new HashMap<String, NetworkInterface>();
        networkIfs.put(NW_ID, nwIf);
        Whitebox.setInternalState(target, "networkInterfaces", networkIfs);

        Whitebox.invokeMethod(target, "createOdenosFlow", nwIf, flows, SEQUENCE_NO);

        verify(dummyLogger, times(1)).debug(SEQUENCE_NO + "\t" + "flow is empty.");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#createOdenosFlow()}
     */
    @Test
    public void testCreateOdenosFlowWithEmptyFlow() throws Exception {
        List<Flow> flows = new ArrayList<>();

        NetworkInterface nwIf = PowerMockito.spy(new NetworkInterface(dispatcher, NW_ID));
        PowerMockito.doReturn(mock(Response.class)).when(nwIf).putFlow(any(Flow.class));

        HashMap<String, NetworkInterface> networkIfs = new HashMap<String, NetworkInterface>();
        networkIfs.put(NW_ID, nwIf);
        Whitebox.setInternalState(target, "networkInterfaces", networkIfs);

        Whitebox.invokeMethod(target, "createOdenosFlow", nwIf, flows, SEQUENCE_NO);

        verify(nwIf, never()).putFlow(any(Flow.class));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#onRequest()}
     */
    @Test
    public void testOnRequest() throws Exception {
        String param = "test";
        Request request = new Request("objId", Method.PUT, "path", param);

        @SuppressWarnings("unchecked")
        RequestParser<IActionCallback>.ParsedRequest parsed = mock(ParsedRequest.class);

        IActionCallback dummyCallback = mock(IActionCallback.class);
        doReturn(new Response(Response.OK, "")).when(dummyCallback).process(eq(parsed));

        doReturn(dummyCallback).when(parsed).getResult();

        @SuppressWarnings("unchecked")
        RequestParser<IActionCallback> dummyParser = mock(RequestParser.class);
        doReturn(parsed).when(dummyParser).parse(eq(request));
        Whitebox.setInternalState(target, "parser", dummyParser);

        Response result = target.onRequest(request);

        assertThat(result.statusCode, is(Response.OK));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#onRequest()}
     */
    @Test
    public void testOnRequestWithNullParsedRequest()
            throws NoSuchFieldException, SecurityException, IllegalArgumentException,
            IllegalAccessException {
        Logger dummyLogger = mock(Logger.class);
        Field field = target.getClass().getSuperclass().getSuperclass().getDeclaredField("logger");
        field.setAccessible(true);
        field.set(target, dummyLogger);

        String param = "test";
        Request request = new Request("objId", Method.PUT, "path", param);

        @SuppressWarnings("unchecked")
        RequestParser<IActionCallback> dummyParser = mock(RequestParser.class);
        doReturn(null).when(dummyParser).parse(eq(request));
        Whitebox.setInternalState(target, "parser", dummyParser);

        Response result = target.onRequest(request);

        assertThat(result.statusCode, is(Response.BAD_REQUEST));
        verify(dummyLogger, times(1)).error("ParsedRequest is null.");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.driver.OptDriver#onRequest()}
     */
    @Test
    public void testOnRequestWithNullCallback()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException,
            SecurityException {
        Logger dummyLogger = mock(Logger.class);
        Field field = target.getClass().getSuperclass().getSuperclass().getDeclaredField("logger");
        field.setAccessible(true);
        field.set(target, dummyLogger);

        String param = "test";
        Request request = new Request("objId", Method.PUT, "path", param);

        @SuppressWarnings("unchecked")
        RequestParser<IActionCallback>.ParsedRequest parsed = mock(ParsedRequest.class);

        doReturn(null).when(parsed).getResult();

        @SuppressWarnings("unchecked")
        RequestParser<IActionCallback> dummyParser = mock(RequestParser.class);
        doReturn(parsed).when(dummyParser).parse(eq(request));
        Whitebox.setInternalState(target, "parser", dummyParser);

        Response result = target.onRequest(request);

        assertThat(result.statusCode, is(Response.BAD_REQUEST));
        verify(dummyLogger, times(1)).error("IActionCallback is null.");
    }
}
