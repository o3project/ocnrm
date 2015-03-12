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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.o3project.ocnrm.model.odu.OduFlow;
import org.o3project.odenos.core.component.Logic;

public class TableManagerTest {
    private String transactionId = "20141080000001";
    private String eventId = "0001";
    private String nwcId = "lsp";
    private String driverName = "pktDriver";
    private String mfId = "mf1";
    private String time = "20141009000000";

    private String transactionId2 = "20141080000002";
    private String eventId2 = "0002";
    private String nwcId2 = "odu";
    private String driverName2 = "optDriver";
    private String mfId2 = "mf2";
    private String time2 = "20141009000000";

    private Event dummyEvent;
    private Event dummyEvent2;
    private Event noTransactionIdEvent;
    private ResourceInfoFlomMf incompleteResource;
    private ResourceInfoFlomMf completeResource;
    private ResourceInfoFlomMf completeResource2;

    @Before
    public void setUp() {
        incompleteResource = new ResourceInfoFlomMf(transactionId,
                null, null, mfId, new OduFlow(), time, null);

        completeResource = new ResourceInfoFlomMf(transactionId, nwcId, eventId, mfId,
                new OduFlow(), time, driverName);

        completeResource2 = new ResourceInfoFlomMf(transactionId2, nwcId2, eventId2, mfId2,
                new OduFlow(), time2, driverName2);

        dummyEvent = mock(Event.class);
        when(dummyEvent.getTransactionId()).thenReturn(transactionId);
        when(dummyEvent.getEventId()).thenReturn(eventId);
        when(dummyEvent.getSrcNetworkComponent()).thenReturn(nwcId);
        when(dummyEvent.getDriver()).thenReturn(driverName);

        dummyEvent2 = mock(Event.class);
        when(dummyEvent2.getTransactionId()).thenReturn(transactionId2);
        when(dummyEvent2.getEventId()).thenReturn(eventId2);
        when(dummyEvent2.getSrcNetworkComponent()).thenReturn(nwcId2);
        when(dummyEvent2.getDriver()).thenReturn(driverName2);

        noTransactionIdEvent = mock(Event.class);
        when(noTransactionIdEvent.getTransactionId()).thenReturn("");
        when(noTransactionIdEvent.getEventId()).thenReturn(eventId);
        when(noTransactionIdEvent.getSrcNetworkComponent()).thenReturn(nwcId);
        when(noTransactionIdEvent.getDriver()).thenReturn(driverName);

        TableManager.getInstance().addResource(completeResource);
        TableManager.getInstance().addResource(incompleteResource);
        TableManager.getInstance().addResource(completeResource2);
    }

    @After
    public void tearDown() {
        TableManager.getInstance().clear();
    }

    @Test
    public void イベントに対応するリソースを完成させて返す() {
        ResourceInfoFlomMf result = TableManager.getInstance().checkIncompleteResource(dummyEvent);

        assertThat(result, not(completeResource));
        assertThat(result.getNetworkComponent(), is(nwcId));
        assertThat(result.getFlowId(), is(eventId));
        assertThat(result.getDriverName(), is(driverName));
    }

    @Test
    public void 渡されたイベントに対応するリソースを返す() {
        ResourceInfoFlomMf result = TableManager.getInstance().checkExistingEvent(dummyEvent2);

        assertThat(result, is(completeResource2));
        assertThat(result, not(incompleteResource));
        assertThat(result, not(completeResource));
    }

    @Test
    public void 渡されたイベントに対応するリソースが無い場合はnullを返す() {
        ResourceInfoFlomMf result = TableManager.getInstance()
                .checkExistingEvent(noTransactionIdEvent);

        assertThat(result, nullValue());
    }

    @Test
    public void 渡されたトランザクションIDとネットワークIDとドライバ名に対応するリソースを返す() {
        ResourceInfoFlomMf result = TableManager.getInstance()
                .checkExistingEvent(transactionId, nwcId, eventId, driverName);

        assertThat(result, is(completeResource));
        assertThat(result, not(completeResource2));
    }

    @Test
    public void 渡されたトランザクションIDとネットワークIDとドライバ名に対応するリソースが無い場合はnullを返す() {
        ResourceInfoFlomMf result = TableManager.getInstance()
                .checkExistingEvent(noTransactionIdEvent);

        assertThat(result, nullValue());
    }

    @Test
    public void 新しく生成したイベントをテーブルに登録して返す() {
        Event result = TableManager.getInstance().createEvent(nwcId, transactionId,
                Logic.FLOW_CHANGED, eventId, "add", driverName);

        assertThat(result.getTransactionId(), is(dummyEvent.getTransactionId()));
        assertThat(result.getEventId(), is(dummyEvent.getEventId()));
        assertThat(result.getSrcNetworkComponent(), is(dummyEvent.getSrcNetworkComponent()));
        assertThat(result.getDriver(), is(dummyEvent.getDriver()));
    }

    @Test
    public void イベントを削除してtrueを返す() {
        TableManager.getInstance().createEvent(nwcId, transactionId, Logic.FLOW_CHANGED,
                eventId, "add", driverName);

        boolean result = TableManager.getInstance().delete(dummyEvent.getTransactionId(),
                dummyEvent.getSrcNetworkComponent(), dummyEvent.getEventId());

        assertThat(result, is(true));
    }

    @Test
    public void 削除するイベントが無い場合はfalseを返す() {
        TableManager.getInstance().createEvent(nwcId, null, Logic.FLOW_CHANGED,
                eventId, "add", driverName);

        boolean result = TableManager.getInstance().delete(noTransactionIdEvent.getTransactionId(),
                noTransactionIdEvent.getSrcNetworkComponent(), noTransactionIdEvent.getDriver());

        assertThat(result, is(false));
    }
}
