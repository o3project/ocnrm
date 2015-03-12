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
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.lang.reflect.Field;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.restlet.representation.Representation;
import org.slf4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ RMNodeRestApi.class })
public class RMNodeRestApiTest {
    private RMNodeRestApi target;

    @Before
    public void setUp() {
        target = PowerMockito.spy(new RMNodeRestApi());
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.rest.RMNodeRestApi#createNodeInfo()}
     */
    @Test
    public void testCreateNodeInfoWithLowerInfo() throws Exception {
        String lowertestData = "{\"lower\":"
                + "{\"flow\":["
                + "{\"dstTTP\":\"NW=SDN,NE=FW950073202,Layer=OCh,TTP=2\","
                + "\"flowId\":\"NW=SDN,Layer=OCh,TL=1\","
                + "\"linkId\":[\"NW=SDN,Layer=OMS,TL=2\",\"NW=SDN,Layer=OMS,TL=3\"],"
                + "\"srcTTP\":\"NW=SDN,NE=FW950073201,Layer=OCh,TTP=1\"},"
                + "{\"dstTTP\":\"NW=SDN,NE=FW950073202,Layer=OCh,TTP=1\","
                + "\"flowId\":\"NW=SDN,Layer=OCh,TL=2\","
                + "\"linkId\":[\"NW=SDN,Layer=OMS,TL=1\"],"
                + "\"srcTTP\":\"NW=SDN,NE=FW950073201,Layer=OCh,TTP=2\"}"
                + "],"
                + "\"link\":["
                + "{\"dstTTP\":\"NW=SDN,NE=FW950073202,Layer=OMS,TTP=2\","
                + "\"linkId\":\"NW=SDN,Layer=OMS,TL=1\","
                + "\"srcTTP\":\"NW=SDN,NE=FW950073201,Layer=OMS,TTP=1\"},"
                + "{\"dstTTP\":\"NW=SDN,NE=FW950073203,Layer=OMS,TTP=2\","
                + "\"linkId\":\"NW=SDN,Layer=OMS,TL=2\","
                + "\"srcTTP\":\"NW=SDN,NE=FW950073201,Layer=OMS,TTP=2\"},"
                + "{\"dstTTP\":\"NW=SDN,NE=FW950073203,Layer=OMS,TTP=1\","
                + "\"linkId\":\"NW=SDN,Layer=OMS,TL=3\","
                + "\"srcTTP\":\"NW=SDN,NE=FW950073202,Layer=OMS,TTP=1\"}"
                + "],"
                + "\"node\":["
                + "{\"nodeId\":\"NW=SDN,NE=FW950073201\"},"
                + "{\"nodeId\":\"NW=SDN,NE=FW950073202\"},"
                + "{\"nodeId\":\"NW=SDN,NE=FW950073203\"}"
                + "],"
                + "\"port\":["
                + "{\"portId\":\"NW=SDN,NE=FW950073201,Layer=OCh,TTP=1\"},"
                + "{\"portId\":\"NW=SDN,NE=FW950073201,Layer=OCh,TTP=2\"},"
                + "{\"portId\":\"NW=SDN,NE=FW950073202,Layer=OCh,TTP=1\"},"
                + "{\"portId\":\"NW=SDN,NE=FW950073202,Layer=OCh,TTP=2\"},"
                + "{\"portId\":\"NW=SDN,NE=FW950073203,Layer=OCh,TTP=1\"},"
                + "{\"portId\":\"NW=SDN,NE=FW950073203,Layer=OCh,TTP=2\"},"
                + "{\"portId\":\"NW=SDN,NE=FW950073201,Layer=OCh,CTP=1\"},"
                + "{\"portId\":\"NW=SDN,NE=FW950073201,Layer=OCh,CTP=2\"},"
                + "{\"portId\":\"NW=SDN,NE=FW950073202,Layer=OCh,CTP=1\"},"
                + "{\"portId\":\"NW=SDN,NE=FW950073202,Layer=OCh,CTP=2\"},"
                + "{\"portId\":\"NW=SDN,NE=FW950073203,Layer=OCh,CTP=1\"},"
                + "{\"portId\":\"NW=SDN,NE=FW950073203,Layer=OCh,CTP=2\"},"
                + "{\"portId\":\"NW=SDN,NE=FW950073203,Layer=OCh,CTP=3\"}"
                + "]"
                + "}}";
        Logger dummyLogger = mock(Logger.class);

        Field field = target.getClass().getSuperclass().getDeclaredField("logger");
        field.setAccessible(true);
        field.set(target, dummyLogger);

        Representation response = target.createNodeInfo(lowertestData);
        JSONObject result = new JSONObject(response.getText());

        verify(dummyLogger, times(1)).debug("make lower topology.");
        assertThat(result.getString("ResultLevel"), is("0"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.rest.RMNodeRestApi#createNodeInfo()}
     */
    @Test
    public void testCreateNodeInfoWithUpperInfo() throws Exception {
        String uppertestData = "{\"upper\":"
                + "{\"node\":"
                + "["
                + "{\"nodeId\":\"NE=FW1\"},"
                + "{\"nodeId\":\"NE=FW2\"},"
                + "{\"nodeId\":\"NE=FW3\"}"
                + "],"
                + "\"port\":"
                + "["
                + "{\"portId\":\"NE=FW1, Layer=OCh, TTP=1\"},"
                + "{\"portId\":\"NE=FW1, Layer=OCh, TTP=2\"},"
                + "{\"portId\":\"NE=FW1, Layer=OCh, CTP=1\"},"
                + "{\"portId\":\"NE=FW1, Layer=OCh, CTP=2\"},"
                + "{\"portId\":\"NE=FW2, Layer=OCh, TTP=1\"},"
                + "{\"portId\":\"NE=FW2, Layer=OCh, TTP=2\"},"
                + "{\"portId\":\"NE=FW2, Layer=OCh, CTP=1\"},"
                + "{\"portId\":\"NE=FW2, Layer=OCh, CTP=2\"},"
                + "{\"portId\":\"NE=FW2, Layer=OCh, CTP=3\"},"
                + "{\"portId\":\"NE=FW2, Layer=OCh, CTP=4\"},"
                + "{\"portId\":\"NE=FW3, Layer=OCh, TTP=1\"},"
                + "{\"portId\":\"NE=FW3, Layer=OCh, TTP=2\"},"
                + "{\"portId\":\"NE=FW3, Layer=OCh, CTP=1\"},"
                + "{\"portId\":\"NE=FW3, Layer=OCh, CTP=2\"}"
                + "],"
                + "\"link\":"
                + "["
                + "{\"linkId\":\"Layer=OCh, TL=1\",\"srcTTP\":\"NE=FW1, Layer=OCh,"
                + "TTP=1\",\"dstTTP\":\"NE=FW2, Layer=OCh, TTP=1\"},"
                + "{\"linkId\":\"Layer=OCh, TL=2\",\"srcTTP\":\"NE=FW2, Layer=OCh,"
                + "TTP=2\",\"dstTTP\":\"NE=FW3, Layer=OCh, TTP=1\"}"
                + "]"
                + "}}";

        Logger dummyLogger = mock(Logger.class);

        Field field = target.getClass().getSuperclass().getDeclaredField("logger");
        field.setAccessible(true);
        field.set(target, dummyLogger);

        Representation response = target.createNodeInfo(uppertestData);
        JSONObject result = new JSONObject(response.getText());

        verify(dummyLogger, times(1)).debug("make upper topology.");
        assertThat(result.getString("ResultLevel"), is("0"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.rest.RMNodeRestApi#createNodeInfo()}
     */
    @Test
    public void testCreateNodeInfoWithDifferentFormatParam()
            throws JsonParseException, JsonMappingException, JSONException, IOException {
        String data = "{\"test\":\"test\"}";
        Representation response = target.createNodeInfo(data);
        JSONObject result = new JSONObject(response.getText());
        assertThat(result.getString("ResultLevel"), is("1"));
        assertThat(result.getString("ErrorMessage"), is("100 An error has occurred."));
    }

}
