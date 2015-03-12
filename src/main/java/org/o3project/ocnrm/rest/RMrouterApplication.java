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

import org.o3project.ocnrm.core.starter.ComponentManagerStarter;
import org.o3project.ocnrm.core.starter.ConnectionCreator;
import org.o3project.ocnrm.core.starter.LGCStarter;
import org.o3project.ocnrm.core.starter.NWCStarter;
import org.o3project.ocnrm.lib.OcnMFSequenceLib;
import org.o3project.ocnrm.odenos.lib.OdenOsPropertyLoader;
import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.routing.Router;
import org.restlet.routing.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RMrouterApplication extends Application {
    private Logger logger = LoggerFactory.getLogger(RMrouterApplication.class);
    private static OdenOsPropertyLoader loader = OdenOsPropertyLoader.getInstance();
    private static final String ROUTER_PATH = loader.getRouterPath();
    private static final String LAYERIZER_PATH = loader.getLayerizerPath();
    private static final String CST_PATH = loader.getCMStarterPath();
    private static final String NWCST_PATH = loader.getNWCStarterPath();
    private static final String LGCST_PATH = loader.getLGCStarterPath();
    private static final String CONNECTION_PATH = loader.getConectionPath();
    private static final String GUI_PATH = loader.getGUIPath();

    private static final String SEQNO_PREFIX = "#";
    private OcnMFSequenceLib mf = OcnMFSequenceLib.getInstance();
    private String seqNo = "";

    @Override
    public synchronized Restlet createInboundRoot() {
        seqNo = SEQNO_PREFIX + mf.getNoToString();
        logger.info(seqNo + "\t" + "createInboundRoot() Start");
        Router router = new Router(getContext());

        router.attach(CST_PATH, ComponentManagerStarter.class);
        router.attach(NWCST_PATH, NWCStarter.class);
        router.attach(LGCST_PATH, LGCStarter.class);
        router.attach(CONNECTION_PATH, ConnectionCreator.class);
        router.attach(GUI_PATH, GUIRestApi.class);
        router.attach(ROUTER_PATH, RMNodeRestApi.class);
        router.attach(LAYERIZER_PATH, RMLinkLayerizerRestApi.class, Template.MODE_STARTS_WITH);

        logger.info(seqNo + "\t" + "createInboundRoot() End");
        return router;
    }
}