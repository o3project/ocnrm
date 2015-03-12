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
package org.o3project.ocnrm.core;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.o3project.ocnrm.odenos.lib.OdenOsPropertyLoader;
import org.o3project.ocnrm.rest.RMrouterApplication;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.restlet.Component;
import org.restlet.Restlet;
import org.restlet.Server;
import org.restlet.data.Protocol;
import org.restlet.routing.TemplateRoute;
import org.restlet.routing.VirtualHost;
import org.restlet.util.ServerList;
import org.slf4j.Logger;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ OcnRMServ.class, Component.class, Restlet.class, ServerList.class })
public class OcnRMServTest {
    private String fs = File.separator;
    private String path = System.getProperty("user.dir") + fs + "src" + fs + "main" + fs
            + "java" + fs + "org" + fs + "o3project" + fs + "ocnrm" + fs + "core" + fs
            + "OCNRM.properties";

    private Component component;
    private Logger dummyLogger;

    @Before
    public void setUp() throws Exception {
        component = PowerMockito.mock(Component.class, RETURNS_DEEP_STUBS);

        ServerList list = PowerMockito.mock(ServerList.class);
        PowerMockito.doReturn(list).when(component).getServers();
        PowerMockito.doReturn(mock(Server.class)).when(list).add(any(Protocol.class), anyInt());

        VirtualHost host = PowerMockito.mock(VirtualHost.class);
        PowerMockito.doReturn(host).when(component).getDefaultHost();
        PowerMockito.doReturn(mock(TemplateRoute.class))
                .when(host).attach(anyString(), any(RMrouterApplication.class));

        PowerMockito.doNothing().when(component).start();

        PowerMockito.whenNew(Component.class).withNoArguments().thenReturn(component);

        OdenOsPropertyLoader loader = PowerMockito.spy(OdenOsPropertyLoader.getInstance());
        Whitebox.setInternalState(OcnRMServ.class, "loader", loader);

        dummyLogger = mock(Logger.class);
        Whitebox.setInternalState(OcnRMServ.class, "logger", dummyLogger);
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.core.OcnRMServ#main()}
     */
    @Test
    public void testMainWithEmptyArgument() throws Exception {
        String[] args = {};

        OcnRMServ.main(args);

        verify(dummyLogger).info("main() End");

        PowerMockito.verifyPrivate(OcnRMServ.class).invoke("loadProperties", eq(""));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.core.OcnRMServ#main()}
     */
    @Test
    public void testMainWithArguments() throws Exception {
        String[] args = { path, "" };

        OcnRMServ.main(args);

        verify(dummyLogger).info("main() End");

        PowerMockito.verifyPrivate(OcnRMServ.class).invoke("loadProperties", eq("test"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.core.OcnRMServ#loadProperties()}
     */
    @Test
    public void testLoadPropertiesWithEmptyArgument() throws Exception {
        assertThat(Whitebox.invokeMethod(OcnRMServ.class, "loadProperties", path),
                is(instanceOf(OdenOsPropertyLoader.class)));
        assertNotEquals(path, is(""));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.core.OcnRMServ#loadProperties()}
     */
    @Test
    public void testLoadPropertiesWithNullArgument() throws Exception {
        String param = null;

        assertThat(Whitebox.invokeMethod(OcnRMServ.class, "loadProperties", param),
                is(instanceOf(OdenOsPropertyLoader.class)));
        assertThat(param, is(nullValue()));
    }

}
