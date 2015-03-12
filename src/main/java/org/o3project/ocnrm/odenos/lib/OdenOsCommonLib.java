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

import org.o3project.ocnrm.lib.table.TableManager;
import org.o3project.odenos.core.component.Logic.AttrElements;
import org.o3project.odenos.core.component.network.flow.Flow;
import org.o3project.odenos.core.component.network.topology.Link;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OdenOsCommonLib {
    private static Logger logger = LoggerFactory.getLogger(OdenOsCommonLib.class);

    public static String getNodeId(String param, String seqNo) {
        logger.info(seqNo + "\t" + "getNodeId Start");
        logger.info(seqNo + "\t" + "getParam : " + param);
        if (null == param || "" == param) {
            return "";
        }

        String[] paramList = param.split(",");

        logger.info(seqNo + "\t" + "getNodeId End");
        return paramList[0] + "," + paramList[1];
    }

    public static void statusLog(Flow flow) {
        if (null != flow) {
            logger.info("** Flow TransactionID:{}, FlowID:{}, Status:{}",
                    flow.getAttribute(TableManager.TRANSACTION_ID), flow.getFlowId(),
                    flow.getStatus());
        } else {
            logger.info("** Flow Not Found.");
        }
    }

    public static void statusLog(Flow flow, String postscript) {
        if (null != flow) {
            logger.info("** Flow TransactionID:{}, FlowID:{}, Status:{}, " + postscript,
                    flow.getAttribute(TableManager.TRANSACTION_ID), flow.getFlowId(),
                    flow.getStatus());
        } else {
            logger.info("** Flow Not Found.");
        }
    }

    public static void statusLog(Link link) {
        if (null != link) {
            logger.info("** Link TransactionID:{}, LinkID:{}, Status:{}",
                    link.getAttribute(TableManager.TRANSACTION_ID), link.getId(),
                    link.getAttribute(AttrElements.ESTABLISHMENT_STATUS));
        } else {
            logger.info("** Link Not Found.");
        }
    }

    public static void statusLog(Link link, String postscript) {
        if (null != link) {
            logger.info("** Link TransactionID:{}, LinkID:{}, Status:{}, " + postscript,
                    link.getAttribute(TableManager.TRANSACTION_ID), link.getId(),
                    link.getAttribute(AttrElements.ESTABLISHMENT_STATUS));
        } else {
            logger.info("** Link Not Found.");
        }
    }
}
