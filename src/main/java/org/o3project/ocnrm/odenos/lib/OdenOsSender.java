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

import java.util.HashMap;
import java.util.Map;

import javax.naming.NameNotFoundException;

import org.o3project.ocnrm.lib.OcnMFSenderSequenceLib;
import org.o3project.ocnrm.lib.OcnMFSequenceLib;
import org.o3project.odenos.remoteobject.message.Request;
import org.o3project.odenos.remoteobject.message.Request.Method;
import org.o3project.odenos.remoteobject.message.Response;
import org.o3project.odenos.remoteobject.messagingclient.MessageDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OdenOsSender {
    private Logger logger = LoggerFactory.getLogger(OdenOsSender.class);
    private static OdenOsPropertyLoader loader = OdenOsPropertyLoader.getInstance();
    private static final String SLEEP = loader.getOdenOSSenderSleep();

    private Map<String, String> connections = new HashMap<>();

    private static OdenOsSender os = new OdenOsSender();

    private MessageDispatcher dispatcher;

    private static final String SEQNO_PREFIX = "#";
    private OcnMFSequenceLib mf = OcnMFSequenceLib.getInstance();
    private String seqNo = "";

    private OcnMFSenderSequenceLib sendSeqNum = OcnMFSenderSequenceLib.createInstance();

    private OdenOsSender() {
    }

    public static OdenOsSender getInstance() {
        return os;
    }

    /**
     * OdenOS transmission method.
     * Registration and data acquisition processing a request to OdenOS are done.
     *
     * <br>Example (When you acquire Flow information on networkcomponent1). <br>
     * sendRequest(networkcomponent1, Method.GET, "flows", null)
     *
     * @param objId   ID of object
     * @param method  HTTP method used
     * @param path    Path of object
     * @param body    Transmitted data
     * @return Response
     */
    public synchronized Response sendRequest(String objId, Method method, String path,
            Object body) {
        Response rsp = null;
        seqNo = SEQNO_PREFIX + mf.getNoToString();

        logger.info("$$$$$ SendNo.:" + sendSeqNum.requestNoToString());

        try {
            Thread.sleep(Long.valueOf(SLEEP));
        } catch (InterruptedException e1) {
            logger.error(seqNo + "\t" + "InterruptedException occurred.");
            e1.printStackTrace();
        }

        if (null == dispatcher) {
            logger.error(seqNo + "\t" + "dispatcher is null.");
            return rsp;
        }

        logger.debug(seqNo + "\t" + "  -Sending: " + method + " " + objId + "/" + path);
        Request req = new Request(objId, method, path, body);

        try {
            logger.debug(seqNo + "\t" + "   " + req.getBodyValue());
        } catch (Exception e) {
            logger.error(seqNo + "\t" + "Exception occurred.");
            e.printStackTrace();
        }

        try {
            rsp = dispatcher.requestSync(req);
        } catch (NameNotFoundException e) {
            logger.error(seqNo + "\t" + "[Error] Host not found: " + objId);
            return null;
        } catch (Exception e) {
            logger.error(seqNo + "\t" + "[Error] Sending error: " + e.toString());
            return null;
        }

        if (rsp == null) {
            logger.error("Failed.");
            return null;
        }
        try {
            logger.debug(seqNo + "\t" + "  -Received: " + rsp.statusCode);
        } catch (Exception e) {
            logger.error(seqNo + "\t" + "Exception occurred.");
            e.printStackTrace();
        }

        return rsp;
    }

    public MessageDispatcher getDispatcher() {
        return dispatcher;
    }

    public void setDispatcher(MessageDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public void addConnection(String componentId, String objectId) {
            connections.put(componentId, objectId);
    }

    public String getConnections(String componentId) {
        return connections.get(componentId);
    }

}
