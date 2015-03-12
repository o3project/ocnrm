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
package org.o3project.ocnrm.model.odu;

import org.o3project.ocnrm.model.Constraint;
import org.o3project.ocnrm.model.TerminationPoints;

public class OptLink {
    private TerminationPoints terminationPoints;
    private String direction;
    private Constraint constraint;

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public Constraint getConstraint() {
        return constraint;
    }

    public void setConstraint(Constraint constraint) {
        this.constraint = constraint;
    }

    public TerminationPoints getTerminationPoints() {
        return terminationPoints;
    }

    public void setTerminationPoints(TerminationPoints terminationPoints) {
        this.terminationPoints = terminationPoints;
    }
}
