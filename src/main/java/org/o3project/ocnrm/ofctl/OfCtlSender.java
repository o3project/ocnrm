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
package org.o3project.ocnrm.ofctl;

import java.util.List;

import org.json.JSONException;
import org.o3project.ocnrm.odenos.lib.OdenOsPropertyLoader;
import org.restlet.Client;
import org.restlet.Context;
import org.restlet.data.Protocol;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ClientResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OfCtlSender {
    public static final String FLOW_ADD_METHOD = "add";
    public static final String FLOW_DELETE_METHOD = "delete";

    private Logger logger = LoggerFactory.getLogger(OfCtlSender.class);
    private static OdenOsPropertyLoader loader = OdenOsPropertyLoader.getInstance();
    private static final String OFCTL_SEND_URL = loader.getOfcTlSendURL();
    private String seqNo = "";

    /**
     * Flow information is transmitted to OpenFlow Controller.
     * The array of Flow information passed by the argument is divided into one flow,
     * and it transmits.
     * @param jarray    Array of Flow information
     * @param seqNo     Sequence number
     * @throws JSONException
     */
    public void ofCtlSend(List<String> flows, String method, String seqNo) throws JSONException {
        this.seqNo = seqNo;
        logger.info(seqNo + "\t" + "ofCtlSend Start");
        logger.debug(seqNo + "\t" + "getFlow : " + flows.toString());
        for (int index = 0; index < flows.size(); index++) {
            ofCtlCall(flows.get(index).toString(), method);
        }
        logger.info(seqNo + "\t" + "ofCtlSend End");
    }

    private void ofCtlCall(String param, String method) {
        logger.info(seqNo + "\t" + "ofCtlCall Start");
        logger.debug(seqNo + "\t" + "Post Connect Route");
        logger.debug(seqNo + "\t" + "OFCTL_SEND_URL :" + OFCTL_SEND_URL);
        logger.debug(seqNo + "\t" + "method :" + method);
        logger.debug(seqNo + "\t" + "strParam :" + param);

        ClientResource client = new ClientResource(OFCTL_SEND_URL + "/" + method);
        Client ct = new Client(new Context(), Protocol.HTTP);
        client.setNext(ct);

        StringRepresentation srp = new StringRepresentation(param.toCharArray());
        Representation representation = null;
        try {
            representation = client.post(srp);
            String tmp = representation.toString();
            logger.debug(seqNo + "\t" + "Representation:" + tmp);
            if (tmp == null) {
                logger.error(seqNo + "\t" + "Representation is null.");
            }
        } catch (Exception e) {
            logger.error(seqNo + "\t" + "catch exception: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (representation != null) {
                representation.release();
            }
            srp.release();
            client.release();

            representation = null;
            srp = null;
            client = null;
            try {
                ct.stop();
            } catch (Exception e) {
                logger.debug(seqNo + "\t" + "catch exception: " + e.getMessage());
            }
            ct = null;
        }
        logger.info(seqNo + "\t" + "ofCtlCall End");
    }

}
