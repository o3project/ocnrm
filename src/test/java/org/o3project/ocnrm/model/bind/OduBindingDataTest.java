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
package org.o3project.ocnrm.model.bind;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.spy;

import java.io.IOException;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class OduBindingDataTest {
    private OduBindingData target;

    @Before
    public void setUp() {
        target = spy(new OduBindingData());
    }

    @After
    public void tearDown() throws Exception {
        target = null;
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.model.bind.OduBindingData#bind()}
     */
    @Test
    public void testBind() throws JsonParseException, JsonMappingException, IOException {
        String name = "name";
        String testData = "{"
                + "\"dpid\": \"00000000001\","
                + "\"odutype\": \"ODU2e\","
                + "\"port\": \"11\","
                + "\"tpn\": \"1\","
                + "\"ts\": \"1,2,3,4,5,6,7,8\""
                + "}";

        target.bind(name, testData);
        OduMapping result = (OduMapping) target.getBindMap().get(name);

        assertThat(result.getDpid(), is("00000000001"));
        assertThat(result.getPort(), is("11"));
        assertThat(result.getOdutype(), is("ODU2e"));
        assertThat(result.getTpn(), is("1"));
        assertThat(result.getTs(), is("1,2,3,4,5,6,7,8"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.model.bind.OduBindingData#bind()}
     */
    @Test
    public void testBindWithEmptyJson()
            throws JsonParseException, JsonMappingException, IOException {
        String name = "name";
        String testData = "{}";

        target.bind(name, testData);

        Map<String, MappingData> result = target.getBindMap();

        assertThat(result.size(), is(0));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.model.bind.OduBindingData#bind()}
     */
    @Test
    public void testBindWithEmptyDpid()
            throws JsonParseException, JsonMappingException, IOException {
        String name = "name";
        String testData = "{"
                + "\"odutype\": \"ODU2e\","
                + "\"port\": \"11\","
                + "\"tpn\": \"1\","
                + "\"ts\": \"1,2,3,4,5,6,7,8\""
                + "}";

        target.bind(name, testData);
        OduMapping result = (OduMapping) target.getBindMap().get(name);

        assertThat(result.getDpid(), is(nullValue()));
        assertThat(result.getPort(), is("11"));
        assertThat(result.getOdutype(), is("ODU2e"));
        assertThat(result.getTpn(), is("1"));
        assertThat(result.getTs(), is("1,2,3,4,5,6,7,8"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.model.bind.OduBindingData#bind()}
     */
    @Test
    public void testBindWithEmptyPort()
            throws JsonParseException, JsonMappingException, IOException {
        String name = "name";
        String testData = "{"
                + "\"dpid\": \"00000000001\","
                + "\"odutype\": \"ODU2e\","
                + "\"tpn\": \"1\","
                + "\"ts\": \"1,2,3,4,5,6,7,8\""
                + "}";

        target.bind(name, testData);
        OduMapping result = (OduMapping) target.getBindMap().get(name);

        assertThat(result.getDpid(), is("00000000001"));
        assertThat(result.getPort(), is(nullValue()));
        assertThat(result.getOdutype(), is("ODU2e"));
        assertThat(result.getTpn(), is("1"));
        assertThat(result.getTs(), is("1,2,3,4,5,6,7,8"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.model.bind.OduBindingData#bind()}
     */
    @Test
    public void testBindWithEmptyOduType()
            throws JsonParseException, JsonMappingException, IOException {
        String name = "name";
        String testData = "{"
                + "\"dpid\": \"00000000001\","
                + "\"port\": \"11\","
                + "\"tpn\": \"1\","
                + "\"ts\": \"1,2,3,4,5,6,7,8\""
                + "}";

        target.bind(name, testData);
        OduMapping result = (OduMapping) target.getBindMap().get(name);

        assertThat(result.getDpid(), is("00000000001"));
        assertThat(result.getPort(), is("11"));
        assertThat(result.getOdutype(), is(nullValue()));
        assertThat(result.getTpn(), is("1"));
        assertThat(result.getTs(), is("1,2,3,4,5,6,7,8"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.model.bind.OduBindingData#bind()}
     */
    @Test
    public void testBindWithEmptyTs()
            throws JsonParseException, JsonMappingException, IOException {
        String name = "name";
        String testData = "{"
                + "\"dpid\": \"00000000001\","
                + "\"odutype\": \"ODU2e\","
                + "\"port\": \"11\","
                + "\"tpn\": \"1\""
                + "}";

        target.bind(name, testData);
        OduMapping result = (OduMapping) target.getBindMap().get(name);

        assertThat(result.getDpid(), is("00000000001"));
        assertThat(result.getPort(), is("11"));
        assertThat(result.getOdutype(), is("ODU2e"));
        assertThat(result.getTpn(), is("1"));
        assertThat(result.getTs(), is(nullValue()));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.model.bind.OduBindingData#bind()}
     */
    @Test
    public void testBindWithEmptyTpn()
            throws JsonParseException, JsonMappingException, IOException {
        String name = "name";
        String testData = "{"
                + "\"dpid\": \"00000000001\","
                + "\"odutype\": \"ODU2e\","
                + "\"port\": \"11\","
                + "\"ts\": \"1,2,3,4,5,6,7,8\""
                + "}";

        target.bind(name, testData);
        OduMapping result = (OduMapping) target.getBindMap().get(name);

        assertThat(result.getDpid(), is("00000000001"));
        assertThat(result.getPort(), is("11"));
        assertThat(result.getOdutype(), is("ODU2e"));
        assertThat(result.getTpn(), is(nullValue()));
        assertThat(result.getTs(), is("1,2,3,4,5,6,7,8"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.model.bind.OduBindingData#bind()}
     */
    @Test
    public void testBindWithEmptyUnrecognizedField()
            throws JsonParseException, JsonMappingException, IOException {
        String name = "name";
        String testData = "{\"testField\": \"test\"}";
        try {
            target.bind(name, testData);

            fail();
        } catch (Exception e) {
            assertThat(e, is(instanceOf(IllegalStateException.class)));
        }
    }
}
