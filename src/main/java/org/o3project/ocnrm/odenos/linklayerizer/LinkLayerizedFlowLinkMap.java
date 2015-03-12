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
package org.o3project.ocnrm.odenos.linklayerizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinkLayerizedFlowLinkMap {
    private Logger logger = LoggerFactory.getLogger(LinkLayerizedFlowLinkMap.class);

    private Map<String, Map<String, ArrayList<String>>> lowerflowsMap = null;
    private Map<String, Map<String, String>> layerizedlinksMap = null;

    private static LinkLayerizedFlowLinkMap instance = null;

    private LinkLayerizedFlowLinkMap() {

    }

    public static LinkLayerizedFlowLinkMap createInstance() {
        if (null == instance) {
            instance = new LinkLayerizedFlowLinkMap();
        }
        return instance;
    }

    public synchronized void putLowerFlowsMap(String layerizedObjName, String layerLinkId,
            String lowFlowId) {
        logger.info("** putLowerFlowsMap Start");

        if (null == lowerflowsMap) {
            lowerflowsMap = new HashMap<String, Map<String, ArrayList<String>>>();
        }

        Map<String, ArrayList<String>> lowerflows = lowerflowsMap.get(layerizedObjName);
        if (null == lowerflows) {
            lowerflows = new HashMap<String, ArrayList<String>>();
        }
        ArrayList<String> list = lowerflows.get(layerLinkId);
        if (null == list) {
            list = new ArrayList<String>();
        }

        list.add(lowFlowId);
        lowerflows.put(layerLinkId, list);
        lowerflowsMap.put(layerizedObjName, lowerflows);

        logger.info("** putLowerFlowsMap Start");
    }

    public synchronized void putLayerizedlinksMap(String layerizedObjName, String layerLinkId,
            String lowFlowId) {
        logger.info("** putLayerizedlinksMap Start");
        if (null == layerizedlinksMap) {
            layerizedlinksMap = new HashMap<String, Map<String, String>>();
        }

        Map<String, String> layerizedlinks = layerizedlinksMap.get(layerizedObjName);

        if (null == layerizedlinks) {
            layerizedlinks = new HashMap<String, String>();
        }
        layerizedlinks.put(lowFlowId, layerLinkId);
        layerizedlinksMap.put(layerizedObjName, layerizedlinks);

        logger.info("** putLayerizedlinksMap End");
    }

    public synchronized Map<String, ArrayList<String>> getLowerFlows(String layerizedObjName) {

        if (null == lowerflowsMap) {
            lowerflowsMap = new HashMap<String, Map<String, ArrayList<String>>>();
        }

        return lowerflowsMap.get(layerizedObjName);
    }

    public synchronized Map<String, String> getLayerizedlinksMap(String layerizedObjName) {

        if (null == layerizedlinksMap) {
            layerizedlinksMap = new HashMap<String, Map<String, String>>();
        }

        return layerizedlinksMap.get(layerizedObjName);
    }

}
