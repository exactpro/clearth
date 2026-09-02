/******************************************************************************
 * Copyright 2009-2020 Exactpro Systems Limited
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

package com.exactprosystems.clearth.woodpecker.configuration.start;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "StartScript")
public class MessageDaemonStartScript extends StartScript
{
    @XmlElement(name = "messageProviders", required = true)
    private MessageProvidersBlockDesc messageProvidersBlock;
    
    @XmlElement(name = "messageSenders", required = true)
    private MessageSendersBlockDesc messageSendersBlock;

    
    public MessageProvidersBlockDesc getMessageProvidersBlock()
    {
        return messageProvidersBlock;
    }

    public void setMessageProvidersBlock(MessageProvidersBlockDesc messageProvidersBlock)
    {
        this.messageProvidersBlock = messageProvidersBlock;
    }

    
    public MessageSendersBlockDesc getMessageSendersBlock()
    {
        return messageSendersBlock;
    }

    public void setMessageSendersBlock(MessageSendersBlockDesc messageSendersBlock)
    {
        this.messageSendersBlock = messageSendersBlock;
    }
}
