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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.spy;

import java.io.File;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ OdenOsPropertyLoader.class, Properties.class })
public class OdenOsPropertyLoaderTest {
    private String fs = File.separator;
    private String path = System.getProperty("user.dir") + fs + "src" + fs + "main" + fs
            + "resources" + fs + "OCNRM.properties";
    private OdenOsPropertyLoader target = PowerMockito.spy(OdenOsPropertyLoader
            .getInstance(path));

    private Properties conf = spy(new Properties());

    @Before
    public void setUp() throws Exception {
        PowerMockito.whenNew(Properties.class).withNoArguments().thenReturn(conf);
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.lib.OdenOsPropertyLoader#getInstance()}
     */
    @Test
    public void testGetInstance() {
        assertThat(OdenOsPropertyLoader.getInstance(),
                is(instanceOf(OdenOsPropertyLoader.class)));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.lib.OdenOsPropertyLoader#getInstance(String)}
     */
    @Test
    public void testGetInstanceString() {
        assertThat(OdenOsPropertyLoader.getInstance(path),
                is(instanceOf(OdenOsPropertyLoader.class)));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.lib.OdenOsPropertyLoader#loadProperty()}
     * @throws Exception
     */
    @Test
    public void testLoadProperty() throws Exception {
        OdenOsPropertyLoader result = Whitebox.invokeMethod(target, "loadProperty", path);

        assertThat(result, is(instanceOf(OdenOsPropertyLoader.class)));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.lib.OdenOsPropertyLoader#getSystemMgrId()}
     */
    @Test
    public void testGetSystemMgrId() {
        assertThat(target.getSystemMgrId(), is("systemmanager"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.lib.OdenOsPropertyLoader#getDispatcherHost()}
     */
    @Test
    public void testGetDispatcherHost() {
        assertThat(target.getDispatcherHost(), is("127.0.0.1"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.lib.OdenOsPropertyLoader#getDispatcherPort()}
     */
    @Test
    public void testGetDispatcherPort() {
        assertThat(target.getDispatcherPort(), is(6379));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.lib.OdenOsPropertyLoader#getComponentMgrId()}
     */
    @Test
    public void testGetComponentMgrId() {
        assertThat(target.getComponentMgrId(), is("componentmanager2"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.lib.OdenOsPropertyLoader#getServerPort()}
     */
    @Test
    public void testGetServerPort() {
        assertThat(target.getServerPort(), is(44444));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.lib.OdenOsPropertyLoader#getServerPath()}
     */
    @Test
    public void testGetServerPath() {
        assertThat(target.getServerPath(), is("/demo"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.lib.OdenOsPropertyLoader#getRestAPIResultMsg()}
     */
    @Test
    public void testGetRestAPIResultMsg() {
        assertThat(target.getRestAPIResultMsg(), is("ResultLevel"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.lib.OdenOsPropertyLoader#getRestAPIErrorMsg()}
     */
    @Test
    public void testGetRestAPIErrorMsg() {
        assertThat(target.getRestAPIErrorMsg(), is("ErrorMessage"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.lib.OdenOsPropertyLoader#getRestAPISuccess()}
     */
    @Test
    public void testGetRestAPISuccess() {
        assertThat(target.getRestAPISuccess(), is("0"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.lib.OdenOsPropertyLoader#getRestAPIError()}
     */
    @Test
    public void testGetRestAPIError() {
        assertThat(target.getRestAPIError(), is("1"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.lib.OdenOsPropertyLoader#getLayerizerPath()}
     */
    @Test
    public void testGetLayerizerPath() {
        assertThat(target.getLayerizerPath(), is("/layerizer/{linklayerizer_id}"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.lib.OdenOsPropertyLoader#getRouterPath()}
     */
    @Test
    public void testGetRouterPath() {
        assertThat(target.getRouterPath(), is("/node"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.lib.OdenOsPropertyLoader#getCMStarterPath()}
     */
    @Test
    public void testGetCMStarterPath() {
        assertThat(target.getCMStarterPath(), is("/connectionmanager"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.lib.OdenOsPropertyLoader#getNWCStarterPath()}
     */
    @Test
    public void testGetNWCStarterPath() {
        assertThat(target.getNWCStarterPath(), is("/nwcomponent"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.lib.OdenOsPropertyLoader#getLGCStarterPath()}
     */
    @Test
    public void testGetLGCStarterPath() {
        assertThat(target.getLGCStarterPath(), is("/lgcomponent"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.lib.OdenOsPropertyLoader#getConectionPath()}
     */
    @Test
    public void testGetConectionPath() {
        assertThat(target.getConectionPath(), is("/connections"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.lib.OdenOsPropertyLoader#getRequestOduFlowURL()}
     */
    @Test
    public void testGetRequestOduFlowURL() {
        assertThat(target.getRequestOduFlowURL(), is("http://127.0.0.1/DEMO/Generate/L1Path"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.lib.OdenOsPropertyLoader#getOfcTlSendURL()}
     */
    @Test
    public void testGetOfcTlSendURL() {
        assertThat(target.getOfcTlSendURL(), is("http://127.0.0.1:8080/stats/flowentry"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.lib.OdenOsPropertyLoader#getGUIPath()}
     */
    @Test
    public void testGetGUIPath() {
        assertThat(target.getGUIPath(), is("/info"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.lib.OdenOsPropertyLoader#getOdenOSSenderSleep()}
     */
    @Test
    public void testGetOdenOSSenderSleep() {
        assertThat(target.getOdenOSSenderSleep(), is("0"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.lib.OdenOsPropertyLoader
     * #getRequestOduReplacementPieceUrl()}
     */
    @Test
    public void testGetRequestOduReplacementPieceUrl() {
        assertThat(target.getRequestOduReplacementPieceUrl(), is("http://127.0.0.1/DEMO/ID/L1Request"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.lib.OdenOsPropertyLoader#getDeleteOduFlowUrl()}
     */
    @Test
    public void testGetDeleteOduFlowUrl() {
        assertThat(target.getDeleteOduFlowUrl(), is("http://127.0.0.1/DEMO/Delete/L1Path"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.lib.OdenOsPropertyLoader
     * #getRequestOchReplacementPieceUrl()}
     */
    @Test
    public void testGetRequestOchReplacementPieceUrl() {
        assertThat(target.getRequestOchReplacementPieceUrl(), is("http://127.0.0.1/DEMO/ID/L0Request"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.odenos.lib.OdenOsPropertyLoader#getOduSendTime()}
     */
    @Test
    public void testGetOduSendTime() {
        assertThat(target.getOduSendTime(), is("2000"));
    }
}
