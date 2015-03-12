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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import javax.naming.NameNotFoundException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.o3project.ocnrm.lib.OcnMFSequenceLib;
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
@PrepareForTest({ OdenOsSender.class, Thread.class })
public class OdenOsSenderTest {
    private OdenOsSender target;
    private String seqNo = "1";

    @Before
    public void setUp() throws Exception {
        target = spy(OdenOsSender.getInstance());

        OcnMFSequenceLib lib = mock(OcnMFSequenceLib.class);
        when(lib.getNoToString()).thenReturn(seqNo);
        Whitebox.setInternalState(target, "mf", lib);
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.lib.OdenOsSender#getInstance()}
     */
    @Test
    public void testGetInstance() {
        assertThat(OdenOsSender.getInstance(), is(instanceOf(OdenOsSender.class)));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.lib.OdenOsSender#sendRequest()}
     */
    @Test
    public void testSendRequest() throws Exception {
        String objId = "objId";
        Method method = Method.POST;
        String path = "path";
        String body = "body";

        Response dummyResponse = spy(new Response(0, body));

        Request request = spy(new Request(objId, method, path, body));
        PowerMockito.whenNew(Request.class).withArguments(objId, method, path, body)
                .thenReturn(request);

        MessageDispatcher dummyDispatcher = mock(MessageDispatcher.class);
        when(dummyDispatcher.requestSync(request)).thenReturn(dummyResponse);
        Whitebox.setInternalState(target, "dispatcher", dummyDispatcher);

        Response result = target.sendRequest(objId, method, path, body);

        assertThat(result, is(dummyResponse));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.lib.OdenOsSender#sendRequest()}
     */
    @Test
    public void testSendRequestWithInterruptedException()
            throws InterruptedException, IllegalArgumentException, IllegalAccessException,
            NoSuchFieldException, SecurityException {
        Logger dummyLogger = mock(Logger.class);
        Field field = target.getClass().getSuperclass().getDeclaredField("logger");
        field.setAccessible(true);
        field.set(target, dummyLogger);

        String objId = "objId";
        Method method = Method.POST;
        String path = "path";
        String body = "body";

        PowerMockito.mockStatic(Thread.class);
        PowerMockito.doThrow(mock(InterruptedException.class)).when(Thread.class);
        Thread.sleep(anyLong());

        Response result = target.sendRequest(objId, method, path, body);

        assertThat(result, is(nullValue()));
        verify(dummyLogger, times(1)).error("#" + seqNo + "\t" + "InterruptedException occurred.");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.lib.OdenOsSender#sendRequest()}
     */
    @Test
    public void testSendRequestWithNullDispatcher()
            throws NoSuchFieldException, SecurityException, IllegalArgumentException,
            IllegalAccessException {
        Logger dummyLogger = mock(Logger.class);
        Field field = target.getClass().getSuperclass().getDeclaredField("logger");
        field.setAccessible(true);
        field.set(target, dummyLogger);

        String objId = "objId";
        Method method = Method.POST;
        String path = "path";
        String body = "body";

        MessageDispatcher dummyDispatcher = null;
        Whitebox.setInternalState(target, "dispatcher", dummyDispatcher);

        Response result = target.sendRequest(objId, method, path, body);

        assertThat(result, is(nullValue()));
        verify(dummyLogger, times(1)).error("#" + seqNo + "\t" + "dispatcher is null.");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.lib.OdenOsSender#sendRequest()}
     */
    @Test
    public void testSendRequestWithNameNotFoundException() throws Exception {
        Logger dummyLogger = mock(Logger.class);
        Field field = target.getClass().getSuperclass().getDeclaredField("logger");
        field.setAccessible(true);
        field.set(target, dummyLogger);

        String objId = "objId";
        Method method = Method.POST;
        String path = "path";
        String body = "body";

        Request request = spy(new Request(objId, method, path, body));
        PowerMockito.whenNew(Request.class).withArguments(objId, method, path, body)
                .thenReturn(request);

        MessageDispatcher dummyDispatcher = mock(MessageDispatcher.class);
        doThrow(mock(NameNotFoundException.class)).when(dummyDispatcher).requestSync(request);
        Whitebox.setInternalState(target, "dispatcher", dummyDispatcher);

        Response result = target.sendRequest(objId, method, path, body);

        assertThat(result, is(nullValue()));
        verify(dummyLogger, times(1))
                .error("#" + seqNo + "\t" + "[Error] Host not found: " + objId);
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.lib.OdenOsSender#sendRequest()}
     * @throws Exception
     */
    @Test
    public void testSendRequestWithRequestSyncException() throws Exception {
        Logger dummyLogger = mock(Logger.class);
        Field field = target.getClass().getSuperclass().getDeclaredField("logger");
        field.setAccessible(true);
        field.set(target, dummyLogger);

        String objId = "objId";
        Method method = Method.POST;
        String path = "path";
        String body = "body";

        Request request = spy(new Request(objId, method, path, body));
        PowerMockito.whenNew(Request.class).withArguments(objId, method, path, body)
                .thenReturn(request);

        Exception exception = mock(Exception.class);
        when(exception.toString()).thenReturn("exception");

        MessageDispatcher dummyDispatcher = mock(MessageDispatcher.class);
        doThrow(exception).when(dummyDispatcher).requestSync(request);
        Whitebox.setInternalState(target, "dispatcher", dummyDispatcher);

        Response result = target.sendRequest(objId, method, path, body);

        assertThat(result, is(nullValue()));
        verify(dummyLogger, times(1))
                .error("#" + seqNo + "\t" + "[Error] Sending error: exception");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.lib.OdenOsSender#sendRequest()}
     * @throws Exception
     */
    @Test
    public void testSendRequestWithNullResponse() throws Exception {
        Logger dummyLogger = mock(Logger.class);
        Field field = target.getClass().getSuperclass().getDeclaredField("logger");
        field.setAccessible(true);
        field.set(target, dummyLogger);

        String objId = "objId";
        Method method = Method.POST;
        String path = "path";
        String body = "body";

        Response dummyResponse = null;

        Request request = spy(new Request(objId, method, path, body));
        PowerMockito.whenNew(Request.class).withArguments(objId, method, path, body)
                .thenReturn(request);

        MessageDispatcher dummyDispatcher = mock(MessageDispatcher.class);
        when(dummyDispatcher.requestSync(request)).thenReturn(dummyResponse);
        Whitebox.setInternalState(target, "dispatcher", dummyDispatcher);

        Response result = target.sendRequest(objId, method, path, body);

        assertThat(result, is(dummyResponse));
        verify(dummyLogger, times(1)).error("Failed.");
    }
}
