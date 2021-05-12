/******************************************************************************
 * Copyright 2009-2021 Exactpro Systems Limited
 * https://www.exactpro.com
 * Build Software to Test Software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.exactprosystems.clearth.automation;

import com.exactprosystems.clearth.ApplicationManager;
import com.exactprosystems.clearth.automation.actions.MessageComparator;
import com.exactprosystems.clearth.automation.actions.ReceiveMessageAction;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.ResultDetail;
import com.exactprosystems.clearth.automation.report.results.ContainerResult;
import com.exactprosystems.clearth.automation.report.results.DetailedResult;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.SimpleClearThMessage;
import com.exactprosystems.clearth.messages.MessageBuilder;
import com.exactprosystems.clearth.utils.ClearThException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;


public class MessageComparatorTest extends ReceiveMessageAction<SimpleClearThMessage> {
    private static ApplicationManager clearThManager;

    @BeforeClass
    public static void startTestApplication() throws ClearThException {
        clearThManager = new ApplicationManager();
    }


    @Test
    public void testCompareMessages1() {
        MessageComparator<SimpleClearThMessage> comparator = getMessageComparator();
        Result result = compareMessages(comparator, createMessage("equal"),
                Collections.singletonList(createMessage("equal")), null);

        ContainerResult detail = (ContainerResult)result;
        List<ResultDetail> detailedCount = ((DetailedResult)detail.getDetails().get(0)).getResultDetails();

        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(detailedCount.size(), 0);
    }


    @Test
    public void testCompareMessages2() {
        MessageComparator<SimpleClearThMessage> comparator = getMessageComparator();
        Result result = compareMessages(comparator, createMessage("equal"),
            Collections.singletonList(createMessage("noEqual")), null);

        Assert.assertFalse(result.isSuccess());
    }


    @AfterClass
    public static void disposeTestApplication() throws IOException {
        if (clearThManager != null) clearThManager.dispose();
    }


    private static SimpleClearThMessage createMessage(String msgType) {
        SimpleClearThMessage message = new SimpleClearThMessage();
        message.addField(ClearThMessage.MSGTYPE, msgType);
        return message;
    }

    @Override
    public MessageBuilder<SimpleClearThMessage> getMessageBuilder(Set<String> serviceParameters) {
        return null;
    }

    @Override
    protected String getDefaultCodecName() {
        return null;
    }

    @Override
    public boolean isIncoming() {
        return false;
    }

    @Override
    protected void afterSearch(GlobalContext globalContext, List<SimpleClearThMessage> messages) throws ResultException {

    }

}