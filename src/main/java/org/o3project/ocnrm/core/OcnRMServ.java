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

import org.o3project.ocnrm.odenos.lib.OdenOsPropertyLoader;
import org.o3project.ocnrm.rest.RMrouterApplication;
import org.restlet.Component;
import org.restlet.data.Protocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OcnRMServ {
    private static Logger logger = LoggerFactory.getLogger(OcnRMServ.class);
    private static OdenOsPropertyLoader loader;

    private static int SERVER_PORT;
    private static String SERVER_PATH;

    public static void main(String[] args) throws Exception {
        logger.info("main() Start");

        if (args.length == 0) {
            loadProperties("");
        } else {
            loadProperties(args[0]);
        }

        Component component = new Component();
        component.getServers().add(Protocol.HTTP, SERVER_PORT);
        component.getDefaultHost().attach(SERVER_PATH, new RMrouterApplication());
        component.start();

        logger.info("main() End");
    }

    private static OdenOsPropertyLoader loadProperties(String param) {
        logger.info("setProperties() Start");
        String filename = "";

        if (param != null) {
            filename = param;
        }

        loader = OdenOsPropertyLoader.getInstance(filename);

        SERVER_PORT = loader.getServerPort();
        SERVER_PATH = loader.getServerPath();

        logger.info("setProperties() End");
        return loader;
    }

}
