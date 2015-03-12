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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.spy;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.powermock.reflect.Whitebox;

public class TransactionIdCreatorTest {
    private TransactionIdCreator target;

    @Before
    public void setUp() {
        target = spy(TransactionIdCreator.getInstance());
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.lib.TransactionIdCreator#getCount()}
     */
    @Test
    public void testGetCount() throws Exception {
        Whitebox.setInternalState(target, "count", 0);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        Date date = new Date(System.currentTimeMillis());
        String day = sdf.format(date);

        String result = target.getCount();

        assertThat(result, is(day + "000001"));

        result = target.getCount();

        assertThat(result, is(day + "000002"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.lib.TransactionIdCreator#getCount()}
     */
    @Test
    public void testGetCountWithEmptyDay() throws ParseException {
        Whitebox.setInternalState(target, "count", 0);
        Whitebox.setInternalState(target, "day", "");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        Date date = new Date(System.currentTimeMillis());
        String day = sdf.format(date);

        String result = target.getCount();

        assertThat(result, is(day + "000001"));

        result = target.getCount();

        assertThat(result, is(day + "000002"));
    }

    /**
     * Test method for
     * {@link org.o3project.ocnrm.lib.TransactionIdCreator#getCount()}
     */
    @Test
    public void testGetCountWithDifferentCurrentTime() throws ParseException {
        Whitebox.setInternalState(target, "count", 0);
        Whitebox.setInternalState(target, "day", "differentTime");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        Date date = new Date(System.currentTimeMillis());
        String day = sdf.format(date);

        String result = target.getCount();

        assertThat(result, is(day + "000001"));

        result = target.getCount();

        assertThat(result, is(day + "000002"));
    }

}
