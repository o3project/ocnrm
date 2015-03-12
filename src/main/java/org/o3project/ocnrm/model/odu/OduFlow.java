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

import java.util.List;

import org.o3project.ocnrm.model.FlowResponse;
import org.o3project.ocnrm.model.TerminationPoints;

public class OduFlow extends FlowResponse {
    private List<TerminationPoints> terminationPointPairs;

    public List<TerminationPoints> getTerminationPointPairs() {
        return terminationPointPairs;
    }
    public void setTerminationPointPairs(List<TerminationPoints> terminationPointPairs) {
        this.terminationPointPairs = terminationPointPairs;
    }
}
