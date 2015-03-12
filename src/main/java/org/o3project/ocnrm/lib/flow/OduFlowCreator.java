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
package org.o3project.ocnrm.lib.flow;

import java.util.ArrayList;
import java.util.List;

import org.o3project.ocnrm.lib.flow.model.ActionsData;
import org.o3project.ocnrm.lib.flow.model.MatchData;
import org.o3project.ocnrm.lib.flow.model.OduMatchData;
import org.o3project.ocnrm.lib.flow.model.TsElements;
import org.o3project.ocnrm.model.TerminationPoints;
import org.o3project.ocnrm.model.bind.OduMapping;
import org.o3project.ocnrm.model.bind.RmData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OduFlowCreator extends AbstractFlowCreator {
    private static final String TSLEN = "tslen";
    private static final String TSMAP = "tsmap";
    private static final String TPN = "tpn";
    private final String tslen = "8";
    private Logger logger = LoggerFactory.getLogger(OduFlowCreator.class);

    @Override
    protected MatchData makeMatch(RmData data, TerminationPoints point) {
        OduMapping srcMappingData = (OduMapping) data.getBindMap().get(point.getInPoint());
        OduMatchData match = new OduMatchData();

        if (srcMappingData.getPort() == null) {
            logger.error("match port is empty.");
        } else {
            match.setIn_port(srcMappingData.getPort());
        }

        TsElements elems = new TsElements();

        if (srcMappingData.getTs() == null) {
            logger.warn("match ts map is empty.");
            return match;
        }
        elems.setTsmap(srcMappingData.getTs());

        if (srcMappingData.getOdutype() == null) {
            logger.warn("match Odu type is empty.");
        } else {
            match.setOdu_sigtype(srcMappingData.getOdutype());
        }

        if (srcMappingData.getTpn() == null) {
            logger.warn("match tpn is empty.");
        } else {
            elems.setTpn(srcMappingData.getTpn());
        }

        elems.setTslen(tslen);

        match.setOdu_sigid(elems);

        return match;
    }

    @Override
    protected List<ActionsData> makeActions(RmData data, TerminationPoints point) {
        OduMapping dstMappingData = (OduMapping) data.getBindMap().get(point.getOutPoint());

        List<ActionsData> actions = new ArrayList<>();

        if (dstMappingData.getPort() == null) {
            logger.error("output port is empty.");
        } else {
            actions.add(makeOutput(OUTPUT, dstMappingData.getPort()));
        }

        List<String> field = new ArrayList<>();
        List<String> value = new ArrayList<>();

        if (dstMappingData.getTs() == null) {
            logger.warn("action ts map is empty.");
        } else {
            field.add(TSMAP);
            value.add(dstMappingData.getTs());

            if (dstMappingData.getTpn() == null) {
                logger.warn("action tpn is empty.");
            } else {
                field.add(TPN);
                value.add(dstMappingData.getTpn());
        }

        field.add(TSLEN);
        value.add(tslen);

            actions.add(makeAction(SET_FIELD, field, value));
        }

        return actions;
    }

    @Override
    protected List<String> removeDoubleQuotes(List<String> list) {
        return list;
    }
}
