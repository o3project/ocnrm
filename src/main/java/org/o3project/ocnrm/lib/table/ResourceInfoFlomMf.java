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
package org.o3project.ocnrm.lib.table;

import org.o3project.ocnrm.model.FlowResponse;


public class ResourceInfoFlomMf {
    private String transactionId;
    private String networkComponent;
    private String flowId;
    private String mfId;
    private FlowResponse resource;
    private String time;
    private String driverName;

    public ResourceInfoFlomMf(String transactionId, String networkComponent, String flowId,
            String mfId, FlowResponse resource, String time, String driverName) {
        this.transactionId = transactionId;
        this.networkComponent = networkComponent;
        this.flowId = flowId;
        this.mfId = mfId;
        this.resource = resource;
        this.time = time;
        this.driverName = driverName;
    }

    public String getFlowId() {
        return flowId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public void setNetworkComponent(String networkComponent) {
        this.networkComponent = networkComponent;
    }

    public void setFlowId(String flowId) {
        this.flowId = flowId;
    }

    public void setMfId(String mfId) {
        this.mfId = mfId;
    }

    public void setResource(FlowResponse resource) {
        this.resource = resource;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getMfId() {
        return mfId;
    }

    public FlowResponse getResourse() {
        return resource;
    }

    public String getTime() {
        return time;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getNetworkComponent() {
        return networkComponent;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    @Override
    public String toString() {
        return "{transactionId: " + transactionId
                + ", networkComponent: " + networkComponent
                + ", flowId: " + flowId
                + ", mfId: " + mfId
                + ", time: " + time
                + ", driver: " + driverName
                + "}";

    }
}
