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
package org.o3project.ocnrm.model;

import java.util.List;

import org.o3project.ocnrm.lib.flow.model.ActionsData;
import org.o3project.ocnrm.lib.flow.model.MatchData;

public class FlowData {
    private String dpid;
    private String priority;
    private MatchData match;
    private List<ActionsData> actions;

    public String getDpid() {
        return dpid;
    }

    public void setDpid(String dpid) {
        this.dpid = dpid;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public MatchData getMatch() {
        return match;
    }

    public void setMatch(MatchData match) {
        this.match = match;
    }

    public List<ActionsData> getActions() {
        return actions;
    }

    public void setActions(List<ActionsData> actions) {
        this.actions = actions;
    }
}
