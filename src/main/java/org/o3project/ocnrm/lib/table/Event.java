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

public class Event {
    private String transactionId;
    private String srcNetworkComponent;
    private String event;
    private String eventId;
    private String action;
    private String time;
    private String driver;

    public Event(String transactionId, String srcNetworkComponent, String event, String eventId,
            String action, String time, String driver) {
        this.transactionId = transactionId;
        this.srcNetworkComponent = srcNetworkComponent;
        this.event = event;
        this.eventId = eventId;
        this.action = action;
        this.time = time;
        this.driver = driver;
    }

    public String getSrcNetworkComponent() {
        return srcNetworkComponent;
    }

    public String getEvent() {
        return event;
    }

    public String getEventId() {
        return eventId;
    }

    public String getAction() {
        return action;
    }

    public String getTime() {
        return time;
    }

    public String getDriver() {
        return driver;
    }

    public String getTransactionId() {
        return transactionId;
    }

    @Override
    public String toString() {
        return "{transactionId: " + transactionId
                + ", networkComponent: " + srcNetworkComponent
                + ", event: " + event
                + ", eventId: " + eventId
                + ", action: " + action
                + ", time: " + time
                + ", driver: " + driver
                + "}";

    }
}
