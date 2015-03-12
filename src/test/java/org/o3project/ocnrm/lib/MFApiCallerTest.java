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
package org.o3project.ocnrm.lib;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.o3project.ocnrm.model.IdExchangeRequest;
import org.o3project.ocnrm.model.ResourceRequest;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.restlet.Client;
import org.restlet.Request;
import org.restlet.Uniform;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ClientResource;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ MFApiCaller.class, ClientResource.class, Representation.class,
    Client.class, Request.class })
public class MFApiCallerTest {
    private MFApiCaller target;
    private String seqNo = "#1";

    @Before
    public void setUp() throws Exception {
        target = PowerMockito.spy(new MFApiCaller());
    }

    @After
    public void tearDown() throws Exception {
        target = null;
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.lib.MFApiCaller#createFlow()}
     */
    @Test
    public void testCreateFlow() throws Exception {
        JSONObject dummy = new JSONObject();
        PowerMockito.doReturn(dummy)
                .when(target, "createFlow", anyString(), anyObject(), eq(seqNo));

        assertThat(target.createFlow(mock(ResourceRequest.class), seqNo), is(dummy));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.lib.MFApiCaller#createLspFlow()}
     */
    @Test
    public void testCreateLspFlow() throws Exception {
        JSONObject dummy = new JSONObject();
        PowerMockito.doReturn(dummy)
                .when(target, "createFlow", anyString(), anyObject(), eq(seqNo));

        assertThat(target.createFlow(mock(ResourceRequest.class), seqNo), is(dummy));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.lib.MFApiCaller#createFlow()}
     */
    @Test
    public void testPrivateCreateFlow() throws Exception {
        String path = "path";
        String result = "{\"test\":\"test\"}";
        ResourceRequest param = new ResourceRequest();

        PowerMockito.doReturn(result).when(target, "postToMF", eq(path), anyString(), eq(seqNo));
        JSONObject json = Whitebox.invokeMethod(target, "createFlow", path, param, seqNo);

        assertThat(json.toString(), is(result));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.lib.MFApiCaller#postToMF()}
     */
    @Test
    public void testPrivatePostToMf() throws Exception {
        String path = "path";
        String param = "param";

        Representation dummyResponse = mock(Representation.class);
        PowerMockito.doReturn("result").when(dummyResponse).getText();

        ClientResource dummyClient = mock(ClientResource.class);
        doNothing().when(dummyClient).setNext((Uniform) anyObject());

        StringRepresentation rep = spy(new StringRepresentation(param.toCharArray()));
        PowerMockito.whenNew(StringRepresentation.class).withAnyArguments().thenReturn(rep);

        doReturn(dummyResponse).when(dummyClient).post(eq(rep));

        PowerMockito.whenNew(ClientResource.class).withAnyArguments().thenReturn(dummyClient);

        String result = Whitebox.invokeMethod(target, "postToMF", path, param, seqNo);

        assertThat(result, is("result"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.lib.MFApiCaller#requestOchCorrespondingTable()}
     */
    @Test
    public void testRequestOchCorrespondingTable() throws Exception {
        IdExchangeRequest request = mock(IdExchangeRequest.class);
        JSONObject result = mock(JSONObject.class);

        PowerMockito.doReturn(result)
                .when(target, "getTable", anyString(), eq(request), eq(seqNo));
        assertThat(target.requestOchCorrespondingTable(request, seqNo), is(result));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.lib.MFApiCaller#requestOduCorrespondingTable()}
     */
    @Test
    public void testRequestOduCorrespondingTable() throws Exception {
        IdExchangeRequest request = mock(IdExchangeRequest.class);
        JSONObject result = mock(JSONObject.class);

        PowerMockito.doReturn(result)
                .when(target, "getTable", anyString(), eq(request), eq(seqNo));
        assertThat(target.requestOduCorrespondingTable(request, seqNo), is(result));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.lib.MFApiCaller#getTable()}
     */
    @Test
    public void testGetTable() throws Exception {
        String path = "path";
        IdExchangeRequest request = new IdExchangeRequest("flowId", "terminationPoint");

        PowerMockito.doReturn("{\"test\":\"test\"}")
                .when(target, "postToMF",
                        eq(path), anyString(), eq(seqNo));

        JSONObject result = Whitebox.invokeMethod(target, "getTable", path, request, seqNo);
        assertThat(result.toString(), is("{\"test\":\"test\"}"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.lib.MFApiCaller#deleteOduFlow()}
     */
    @Test
    public void testDeleteOduFlow() throws Exception {
        String param = "param";

        PowerMockito.doReturn("{\"test\":\"test\"}")
                .when(target, "postToMF", anyString(), eq(param), eq(seqNo));

        assertThat(target.deleteOduFlow(param, seqNo), is(true));
        PowerMockito.verifyPrivate(target, times(1))
                .invoke("postToMF", anyString(), eq(param), eq(seqNo));
    }

}
