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
package org.o3project.ocnrm.lib;

public class OcnRMStatusLib {
    private static OcnRMStatusLib rm = new OcnRMStatusLib();
    private boolean cmStatus = false;

    private OcnRMStatusLib() {

    }

    public static OcnRMStatusLib getInstance() {
        return rm;
    }

    public boolean getCMStatus() {
        return cmStatus;
    }

    public void setCMStatus(boolean status) {
        this.cmStatus = status;
    }
}
