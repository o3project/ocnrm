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
package org.o3project.ocnrm.ofctl;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;

import org.json.JSONArray;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.o3project.ocnrm.lib.JSONParser;
import org.o3project.odenos.core.component.Logic;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.restlet.Client;
import org.restlet.Context;
import org.restlet.data.Protocol;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;
import org.slf4j.Logger;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ OfCtlSender.class })
public class OfCtlSenderTest {
    private OfCtlSender target;
    private String seqNo = "#1";

    @Before
    public void setUp() throws Exception {
        target = PowerMockito.spy(new OfCtlSender());
    }

    @After
    public void tearDown() throws Exception {
        target = null;
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.ofctl.OfCtlSender#ofCtlSend()}
     */
    @Test
    public void testOfCtlSend() throws Exception {
        JSONParser parser = new JSONParser();
        List<String> jarray = parser.convertToList(new JSONArray("[{1:test1},{2:test2}]"), seqNo);

        PowerMockito.doNothing().when(target, "ofCtlCall", anyString(), eq(Logic.CONN_ADD));

        target.ofCtlSend(jarray, Logic.CONN_ADD, seqNo);

        PowerMockito.verifyPrivate(target, times(1))
                .invoke("ofCtlCall", eq("{\"1\":\"test1\"}"), eq(Logic.CONN_ADD));
        PowerMockito.verifyPrivate(target, times(1))
                .invoke("ofCtlCall", eq("{\"2\":\"test2\"}"), eq(Logic.CONN_ADD));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.ofctl.OfCtlSender#ofCtlSend()}
     */
    @Test
    public void testOfCtlSendWithEmptyJsonArray() throws Exception {
        JSONParser parser = new JSONParser();
        List<String> jarray = parser.convertToList(new JSONArray("[]"), seqNo);

        PowerMockito.doNothing().when(target, "ofCtlCall", anyString(), eq(Logic.CONN_ADD));

        target.ofCtlSend(jarray, Logic.CONN_ADD, seqNo);

        PowerMockito.verifyPrivate(target, never())
                .invoke("ofCtlCall", anyString(), anyString());
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.ofctl.OfCtlSender#ofCtlCall()}
     */
    @Test
    public void testOfCtlCall() throws Exception {
        Logger dummyLogger = mock(Logger.class);
        Field field = target.getClass().getSuperclass().getDeclaredField("logger");
        field.setAccessible(true);
        field.set(target, dummyLogger);

        String param = "param";
        String method = "method";

        Representation dummyRepresantation = mock(Representation.class);
        when(dummyRepresantation.toString()).thenReturn("dummy");

        ClientResource dummyClient = mock(ClientResource.class);
        doReturn(dummyRepresantation).when(dummyClient).post((StringRepresentation) anyObject());
        PowerMockito.whenNew(ClientResource.class).withAnyArguments().thenReturn(dummyClient);

        Whitebox.setInternalState(target, "seqNo", seqNo);

        Whitebox.invokeMethod(target, "ofCtlCall", param, method);

        verify(dummyLogger, times(1)).info(seqNo + "\t" + "ofCtlCall End");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.ofctl.OfCtlSender#ofCtlCall()}
     */
    @Test
    public void testOfCtlCallWithNullRepresentation() throws Exception {
        Logger dummyLogger = mock(Logger.class);
        Field field = target.getClass().getSuperclass().getDeclaredField("logger");
        field.setAccessible(true);
        field.set(target, dummyLogger);

        String param = "param";
        String method = "method";

        Representation dummyRepresantation = mock(Representation.class);
        when(dummyRepresantation.toString()).thenReturn(null);

        ClientResource dummyClient = mock(ClientResource.class);
        doReturn(dummyRepresantation).when(dummyClient).post((StringRepresentation) anyObject());
        PowerMockito.whenNew(ClientResource.class).withAnyArguments().thenReturn(dummyClient);

        Whitebox.setInternalState(target, "seqNo", seqNo);

        Whitebox.invokeMethod(target, "ofCtlCall", param, method);

        verify(dummyLogger, times(1)).info(seqNo + "\t" + "ofCtlCall End");
        verify(dummyLogger, times(1)).error(seqNo + "\t" + "Representation is null.");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.ofctl.OfCtlSender#ofCtlCall()}
     */
    @Test
    public void testOfCtlCallWithException() throws Exception {
        Logger dummyLogger = mock(Logger.class);
        Field field = target.getClass().getSuperclass().getDeclaredField("logger");
        field.setAccessible(true);
        field.set(target, dummyLogger);

        String param = "param";
        String method = "method";

        Representation dummyRepresantation = mock(Representation.class);
        when(dummyRepresantation.toString()).thenReturn("dummy");

        ClientResource dummyClient = mock(ClientResource.class);
        ResourceException exception = mock(ResourceException.class);
        when(exception.getMessage()).thenReturn("message");
        doThrow(exception).when(dummyClient).post((StringRepresentation) anyObject());
        PowerMockito.whenNew(ClientResource.class).withAnyArguments().thenReturn(dummyClient);

        Whitebox.setInternalState(target, "seqNo", seqNo);

        Whitebox.invokeMethod(target, "ofCtlCall", param, method);

        verify(dummyLogger, times(1)).info(seqNo + "\t" + "ofCtlCall End");
        verify(dummyLogger, times(1)).error(seqNo + "\t" + "catch exception: " + "message");
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.ofctl.OfCtlSender#ofCtlCall()}
     */
    @Test
    public void testOfCtlCallWithClientStopException() throws Exception {
        Logger dummyLogger = mock(Logger.class);
        Field field = target.getClass().getSuperclass().getDeclaredField("logger");
        field.setAccessible(true);
        field.set(target, dummyLogger);

        String param = "param";
        String method = "method";

        Representation dummyRepresantation = mock(Representation.class);
        when(dummyRepresantation.toString()).thenReturn("dummy");

        Context context = new Context();
        PowerMockito.whenNew(Context.class).withNoArguments().thenReturn(context);

        Client client = PowerMockito.spy(new Client(context, Protocol.HTTP));
        doThrow(mock(Exception.class)).when(client).stop();
        PowerMockito.whenNew(Client.class).withParameterTypes(Context.class, Protocol.class)
                .withArguments(eq(context), eq(Protocol.HTTP)).thenReturn(client);

        ClientResource dummyClient = mock(ClientResource.class);
        doReturn(dummyRepresantation).when(dummyClient).post((StringRepresentation) anyObject());
        PowerMockito.whenNew(ClientResource.class).withAnyArguments().thenReturn(dummyClient);

        Whitebox.setInternalState(target, "seqNo", seqNo);

        Whitebox.invokeMethod(target, "ofCtlCall", param, method);

        verify(dummyLogger, times(1)).info(seqNo + "\t" + "ofCtlCall End");
    }

}
