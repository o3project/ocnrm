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

public class LinklayerizerBoundary extends Boundary {

    protected String type = "LinkLayerizerBoundary";

    private String lowerNw;
    private String upperNw;
    private String lowerNwNode;
    private String upperNwNode;
    private String lowerNwPort;
    private String upperNwPort;

    public String getLower_nw() {
        return lowerNw;
    }

    public void setLower_nw(String lowerNw) {
        this.lowerNw = lowerNw;
    }

    public String getUpper_nw() {
        return upperNw;
    }

    public void setUpper_nw(String upperNw) {
        this.upperNw = upperNw;
    }

    public String getLower_nw_node() {
        return lowerNwNode;
    }

    public void setLower_nw_node(String lowerNwNode) {
        this.lowerNwNode = lowerNwNode;
    }

    public String getUpper_nw_node() {
        return upperNwNode;
    }

    public void setUpper_nw_node(String upperNwNode) {
        this.upperNwNode = upperNwNode;
    }

    public String getLower_nw_port() {
        return lowerNwPort;
    }

    public void setLower_nw_port(String lowerNwPort) {
        this.lowerNwPort = lowerNwPort;
    }

    public String getUpper_nw_port() {
        return upperNwPort;
    }

    public void setUpper_nw_port(String upperNwPort) {
        this.upperNwPort = upperNwPort;
    }

    public String getType() {
        return type;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj == this) {
            return true;
        }

        if (!(obj instanceof LinklayerizerBoundary)) {
            return false;
        }

        if (!(super.equals(obj))) {
            return false;
        }

        final LinklayerizerBoundary obj2 = (LinklayerizerBoundary) obj;

        if (!obj2.getLower_nw().equals(this.lowerNw)
                || !obj2.getLower_nw_node().equals(this.lowerNwNode)
                || !obj2.getLower_nw_port().equals(this.lowerNwPort)
                || !obj2.getUpper_nw().equals(this.upperNw)
                || !obj2.getUpper_nw_node().equals(this.upperNwNode)
                || !obj2.getUpper_nw_port().equals(this.upperNwPort)
                ) {
            return false;
        }
        return true;
    }

}
