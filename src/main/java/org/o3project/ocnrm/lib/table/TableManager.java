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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.o3project.ocnrm.lib.TransactionIdCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TableManager {
    private Logger logger = LoggerFactory.getLogger(TableManager.class);

    private static final TableManager tableManager = new TableManager();
    public static final SimpleDateFormat date = new SimpleDateFormat("yyyy/mm/dd HH:mm:ss");
    public static final String TRANSACTION_ID = "transactionId";

    private TableManager() {
    }

    private List<Event> eventTable = new ArrayList<>();
    private List<ResourceInfoFlomMf> responseTableFromMf = new ArrayList<>();

    public static TableManager getInstance() {
        return tableManager;
    }

    public void addResource(ResourceInfoFlomMf... resources) {
        for (ResourceInfoFlomMf resource: resources) {
            logger.debug("registered resource: " + resource.toString());
            responseTableFromMf.add(resource);
        }
    }

    public ResourceInfoFlomMf checkIncompleteResource(Event newEvent) {
        for (ResourceInfoFlomMf resource: responseTableFromMf) {
            if (resource.getTransactionId().equals(newEvent.getTransactionId())
                    && (resource.getNetworkComponent() == null)
                    && (resource.getDriverName() == null)) {
                resource.setNetworkComponent(newEvent.getSrcNetworkComponent());
                resource.setFlowId(newEvent.getEventId());
                resource.setDriverName(newEvent.getDriver());
                return resource;
            }
        }
        return null;
    }

    public ResourceInfoFlomMf checkExistingEvent(Event newEvent) {
        return checkExistingEvent(newEvent.getTransactionId(), newEvent.getSrcNetworkComponent(),
                newEvent.getEventId(), newEvent.getDriver());
    }

    public ResourceInfoFlomMf checkExistingEvent(String transactionId, String nwId, String flowId,
            String driverName) {
        for (ResourceInfoFlomMf resource: responseTableFromMf) {
            if (resource.getNetworkComponent() == null
                    || resource.getDriverName() == null) {
                continue;
            }

            if (resource.getTransactionId().equals(transactionId)
                    && resource.getNetworkComponent().equals(nwId)
                    && resource.getFlowId().equals(flowId)
                    && resource.getDriverName().equals(driverName)) {
                return resource;
            }
        }
        return null;
    }

    public Event createEvent(String nwId, String transactionId, String eventType,
            String flowId, String action, String driverName) {
        Event event = make(nwId, transactionId, eventType, flowId, action, driverName);
        eventTable.add(event);
        logger.debug("registered event: " + event.toString());
        return event;
    }

    private Event make(String nwId, String transactionId, String eventType, String flowId,
            String action, String driverName) {
        if (transactionId == null) {
            transactionId = TransactionIdCreator.getInstance().getCount();
        }

        Event event = new Event(transactionId, nwId, eventType, flowId, action,
                date.format(new Date()), driverName);

        return event;
    }

    public boolean delete(String targetTransactionId, String targetNetworkComponent,
            String targetFlowId) {
        Event deletedEvent = null;
        for (Event registeredEvent: eventTable) {
            if (registeredEvent.getTransactionId().equals(targetTransactionId)
                    && registeredEvent.getSrcNetworkComponent().equals(targetNetworkComponent)
                    && registeredEvent.getEventId().equals(targetFlowId)) {
                deletedEvent = registeredEvent;
                break;
            }
        }

        if (deletedEvent == null) {
            return false;
        }
        eventTable.remove(deletedEvent);
        logger.debug("deleted event: " + deletedEvent.toString());

        deleteResource(deletedEvent);
        return true;
    }

    private void deleteResource(Event deletedEvent) {
        ResourceInfoFlomMf deletedResource = null;
        for (ResourceInfoFlomMf resource: responseTableFromMf) {
            if (resource.getNetworkComponent() == null
                    || resource.getFlowId() == null) {
                continue;
            }

            if (resource.getTransactionId().equals(deletedEvent.getTransactionId())
                    && resource.getNetworkComponent().equals(deletedEvent.getSrcNetworkComponent())
                    && resource.getFlowId().equals(deletedEvent.getEventId())) {
                deletedResource = resource;
            }
        }

        if (deletedResource == null) {
            return;
        }
        responseTableFromMf.remove(deletedResource);
        logger.debug("deleted resource: " + deletedResource.toString());
    }

    //Clear tables for test
    void clear() {
        eventTable.clear();
        responseTableFromMf.clear();
    }

    public ResourceInfoFlomMf getResourceFromEventId(String targetTransactionId,
            String targetMfId) {
        for (ResourceInfoFlomMf resource: responseTableFromMf) {
            if (resource.getTransactionId().equals(targetTransactionId)
                    && resource.getMfId().equals(targetMfId)) {
                return resource;
            }
        }
        return null;
    }

    public List<ResourceInfoFlomMf> getResourcesFromFjFlowId(String fjFlowId) {
        List<ResourceInfoFlomMf> resources = new ArrayList<>();
        for (ResourceInfoFlomMf resource: responseTableFromMf) {
            if (resource.getMfId().equals(fjFlowId)) {
                resources.add(resource);
            }
        }
        return resources;
    }

    public List<Event> getEventsFromTransactionId(String transactionId) {
        List<Event> events = new ArrayList<>();
        for (Event event: eventTable) {
            if (event.getTransactionId().equals(transactionId)) {
                events.add(event);
            }
        }
        return events;
    }

    public boolean delete(String transactionId) {
        List<Event> deleteEvents = new ArrayList<>();
        for (Event event: eventTable) {
            if (event.getTransactionId().equals(transactionId)) {
                deleteEvents.add(event);
            }
        }
        if (deleteEvents.isEmpty()) {
            return false;
        }

        eventTable.removeAll(deleteEvents);

        return deleteResourcesFromTransactionId(transactionId);
    }

    private boolean deleteResourcesFromTransactionId(String transactionId) {
        List<ResourceInfoFlomMf> deleteResources = new ArrayList<>();

        for (ResourceInfoFlomMf resource: responseTableFromMf) {
            if (resource.getTransactionId().equals(transactionId)) {
                deleteResources.add(resource);
            }
        }
        if (deleteResources.isEmpty()) {
            return false;
        }
        responseTableFromMf.removeAll(deleteResources);

        return true;
    }

    public Event getEventFromFlowId(String flowId) {
        for (Event event: eventTable) {
            if (event.getEventId().equals(flowId)) {
                return event;
            }
        }
        return null;
    }
}
