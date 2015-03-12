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

import org.json.JSONArray;
import org.json.JSONException;
import org.o3project.ocnrm.lib.JSONParser;
import org.o3project.ocnrm.lib.flow.model.ActionsData;
import org.o3project.ocnrm.lib.flow.model.MatchData;
import org.o3project.ocnrm.model.FlowData;
import org.o3project.ocnrm.model.TerminationPoints;
import org.o3project.ocnrm.model.bind.MappingData;
import org.o3project.ocnrm.model.bind.RmData;
import org.o3project.ocnrm.model.odu.OptFlow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;

public abstract class AbstractFlowCreator {
    private Logger logger = LoggerFactory.getLogger(AbstractFlowCreator.class);

    protected static final String WRITE_ACTIONS = "WRITE_ACTIONS";
    protected static final String SET_FIELD = "SET_FIELD";
    protected static final String OUTPUT = "OUTPUT";

    protected abstract MatchData makeMatch(RmData data, TerminationPoints point);
    protected abstract List<ActionsData> makeActions(RmData data, TerminationPoints point);
    protected abstract List<String> removeDoubleQuotes(List<String> list);

    public List<String> createFlow(OptFlow optFlow, RmData data, String seqNo) {
        logger.info(seqNo + "\t" + "createFlow() Start");

        JSONParser jsonParse = new JSONParser();
        JSONArray jsonArrayFlow = new JSONArray();
        try {
            List<FlowData> flowList = new ArrayList<>();
            for (TerminationPoints point : optFlow.getTerminationPointPairs()) {
                logger.debug(seqNo + "\t" + "target termination point : " + point.toString());

                FlowData flowData = new FlowData();

                MappingData srcMappingData = data.getBindMap().get(point.getInPoint());
                if (srcMappingData.getDpid() == null) {
                    logger.error("dpid is empty.");
                } else {
                    flowData.setDpid(srcMappingData.getDpid());
                }

                flowData.setMatch(makeMatch(data, point));

                flowData.setActions(makeActions(data, point));

                flowList.add(flowData);
            }
            jsonArrayFlow = new JSONArray(jsonParse.convertToJson(flowList, seqNo));

            logger.info(seqNo + "\t" + "createFlow() End");
            return removeDoubleQuotes(jsonParse.convertToList(jsonArrayFlow, seqNo));
        } catch (JsonProcessingException e) {
            logger.error(seqNo + "\t" + "JsonProcessingException occured.");
            e.printStackTrace();
            List<String> list = new ArrayList<>();
            return list;
        } catch (JSONException e) {
            logger.error(seqNo + "\t" + "JSONException occured.");
            e.printStackTrace();
            List<String> list = new ArrayList<>();
            return list;
        }
    }

    protected ActionsData makeOutput(String type, String output) {
        ActionsData action = new ActionsData();
        action.setType(type);
        action.setPort(output);
        return action;
    }

    protected ActionsData makeAction(String type, List<String> field, List<String> value) {
        ActionsData action = new ActionsData();
        action.setType(type);
        action.setField(field);
        action.setValue(value);
        return action;
    }
}
