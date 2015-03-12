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
package org.o3project.ocnrm.lib.flow.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OchMatchData extends MatchData {
    @JsonProperty("och_sigtype")
    private String ochSigtype;
    private String grid;
    private String cs;
    private String n;
    private String m;

    public String getOchSigtype() {
        return ochSigtype;
    }
    public void setOchSigtype(String ochSigtype) {
        this.ochSigtype = ochSigtype;
    }
    public String getGrid() {
        return grid;
    }
    public void setGrid(String grid) {
        this.grid = grid;
    }
    public String getCs() {
        return cs;
    }
    public void setCs(String cs) {
        this.cs = cs;
    }
    public String getN() {
        return n;
    }
    public void setN(String n) {
        this.n = n;
    }
    public String getM() {
        return m;
    }
    public void setM(String m) {
        this.m = m;
    }
}
