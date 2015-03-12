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

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;

import org.o3project.ocnrm.lib.OcnMFSequenceLib;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OdenOsPropertyLoader {
    private Logger logger = LoggerFactory.getLogger(OdenOsPropertyLoader.class);

    private static OdenOsPropertyLoader loader = null;
    private static Properties conf = null;
    private static final String DEFAULT_PATH = "OCNRM.properties";
    private static HashMap<String, String> prop;

    private static OcnMFSequenceLib mf = OcnMFSequenceLib.getInstance();

    private enum propertyKey {
        SYSTEM_MGR_ID,
        DISPATCHER_HOST,
        DISPATCHER_PORT,
        COMPONENT_MGR_ID,
        SERVER_PORT,
        SERVER_PATH,
        RESTAPI_RESULT_MSG,
        RESTAPI_ERROR_MSG,
        RESTAPI_SUCCESS,
        RESTAPI_ERROR,
        ROUTER_PATH,
        LAYERIZER_PATH,
        CMST_PATH,
        NWCST_PATH,
        LGCST_PATH,
        CONNECTION_PATH,
        REQUEST_ODU_FLOW_URL,
        OFCTL_SEND_URL,
        GUI_PATH,
        ODENOS_SENDER_SLEEP,
        DELETE_ODU_FLOW_URL,
        REQUEST_ODU_REPLACEMENT_PIECE_URL,
        REQUEST_OCH_REPLACEMENT_PIECE_URL,
        ODU_SEND_TIME;
    }

    private OdenOsPropertyLoader() {

    }

    public static OdenOsPropertyLoader getInstance() {
        return getInstance("");
    }

    public static OdenOsPropertyLoader getInstance(String path) {
        if (null != loader) {
            return loader;
        }
        loader = new OdenOsPropertyLoader();
        loader.loadProperty(path);
        return loader;
    }

    private OdenOsPropertyLoader loadProperty(String path) {
        conf = new Properties();
        try {
            if (null == path || path.equals("")) {
                ClassLoader cl = OdenOsPropertyLoader.class.getClassLoader();
                conf.load(cl.getResourceAsStream(DEFAULT_PATH));
            } else {
                InputStream in = new BufferedInputStream(new FileInputStream(path));
                conf.load(in);
            }

            prop = new HashMap<String, String>();
            for (int index = 0; index < propertyKey.values().length; index++) {
                logger.debug("#" + mf.getNoToString() + "\t" + propertyKey.values()[index]
                        .toString());
                prop.put(propertyKey.values()[index].toString(), getValue(propertyKey
                        .values()[index].toString()));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return loader;
    }

    private String getValue(String key) {
        return conf.getProperty(key);
    }

    public String getSystemMgrId() {
        return prop.get(propertyKey.SYSTEM_MGR_ID.toString());
    }

    public String getDispatcherHost() {
        return prop.get(propertyKey.DISPATCHER_HOST.toString());
    }

    public int getDispatcherPort() {
        return Integer.valueOf(prop.get(propertyKey.DISPATCHER_PORT.toString())).intValue();
    }

    public String getComponentMgrId() {
        return prop.get(propertyKey.COMPONENT_MGR_ID.toString());
    }

    public int getServerPort() {
        return Integer.valueOf(prop.get(propertyKey.SERVER_PORT.toString())).intValue();
    }

    public String getServerPath() {
        return prop.get(propertyKey.SERVER_PATH.toString());
    }

    public String getRestAPIResultMsg() {
        return prop.get(propertyKey.RESTAPI_RESULT_MSG.toString());
    }

    public String getRestAPIErrorMsg() {
        return prop.get(propertyKey.RESTAPI_ERROR_MSG.toString());
    }

    public String getRestAPISuccess() {
        return prop.get(propertyKey.RESTAPI_SUCCESS.toString());
    }

    public String getRestAPIError() {
        return prop.get(propertyKey.RESTAPI_ERROR.toString());
    }

    public String getLayerizerPath() {
        return prop.get(propertyKey.LAYERIZER_PATH.toString());
    }

    public String getRouterPath() {
        return prop.get(propertyKey.ROUTER_PATH.toString());
    }

    public String getCMStarterPath() {
        return prop.get(propertyKey.CMST_PATH.toString());
    }

    public String getNWCStarterPath() {
        return prop.get(propertyKey.NWCST_PATH.toString());
    }

    public String getLGCStarterPath() {
        return prop.get(propertyKey.LGCST_PATH.toString());
    }

    public String getConectionPath() {
        return prop.get(propertyKey.CONNECTION_PATH.toString());
    }

    public String getRequestOduFlowURL() {
        return prop.get(propertyKey.REQUEST_ODU_FLOW_URL.toString());
    }

    public String getOfcTlSendURL() {
        return prop.get(propertyKey.OFCTL_SEND_URL.toString());
    }

    public String getGUIPath() {
        return prop.get(propertyKey.GUI_PATH.toString());
    }

    public String getOdenOSSenderSleep() {
        return prop.get(propertyKey.ODENOS_SENDER_SLEEP.toString());
    }

    public String getRequestOduReplacementPieceUrl() {
        return prop.get(propertyKey.REQUEST_ODU_REPLACEMENT_PIECE_URL.toString());
    }

    public String getDeleteOduFlowUrl() {
        return prop.get(propertyKey.DELETE_ODU_FLOW_URL.toString());
    }

    public String getRequestOchReplacementPieceUrl() {
        return prop.get(propertyKey.REQUEST_OCH_REPLACEMENT_PIECE_URL.toString());
    }

    public String getOduSendTime() {
        return prop.get(propertyKey.ODU_SEND_TIME.toString());
    }
}
