/*******************************************************************************
 * Copyright 2009-2023 Exactpro Systems Limited
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

package com.exactprosystems.clearth.connectivity.jms;

import com.exactprosystems.clearth.connectivity.connections.settings.ConnectionSettings;
import com.exactprosystems.clearth.connectivity.mq.ClearThBasicMqConnectionSettings;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@ConnectionSettings(order = {"hostname", "port", "receiveQueue", "useReceiveQueue", "sendQueue", "readDelay"},
		columns = {"hostname", "sendQueue", "receiveQueue"})
public class JmsConnectionSettings extends ClearThBasicMqConnectionSettings
{
}
