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
package org.o3project.ocnrm.model.bind;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OduMapping extends MappingData {
    private String odutype;
    private String ts;
    private String tpn;

    public String getOdutype() {
        return odutype;
    }
    public void setOdutype(String odutype) {
        this.odutype = odutype;
    }
    public String getTs() {
        return ts;
    }
    public void setTs(String ts) {
        this.ts = ts;
    }
    public String getTpn() {
        return tpn;
    }
    public void setTpn(String tpn) {
        int tpnInt = Integer.parseInt(tpn);
        if (0 > tpnInt) {
            this.tpn = String.valueOf(4096  +  tpnInt);
        } else {
            this.tpn = tpn;
        }
    }
}
