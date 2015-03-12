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

public abstract class Boundary {
    private String boundaryId = "";
    protected String type = "";

    public String getType() {
        return type;
    }

    public String getBoundary_id() {
        return boundaryId;
    }

    public void setBoundary_id(String boundaryId) {
        this.boundaryId = boundaryId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj == this) {
            return true;
        }

        if (!(obj instanceof Boundary)) {
            return false;
        }

        final Boundary obj2 = (Boundary) obj;

        if (obj2.getBoundary_id() == null && this.getBoundary_id() != null
                || obj2.getBoundary_id() != null && this.getBoundary_id() == null) {
            return false;
        }

        if (obj2.getType().equals(this.getType())
                && (obj2.getBoundary_id() == null && this.getBoundary_id() == null
                || obj2.getBoundary_id().equals(this.getBoundary_id()))) {
            return true;
        }
        return false;
    }

}
