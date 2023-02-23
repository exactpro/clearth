/******************************************************************************
 * Copyright 2009-2022 Exactpro Systems Limited
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

package com.exactprosystems.clearth.connectivity;

import com.ibm.mq.MQException;
import com.ibm.mq.constants.MQConstants;

public class MQExceptionUtils
{
	public static boolean isConnectionBroken(MQException e)
	{
		return (e.completionCode == MQConstants.MQCC_FAILED) && (e.reasonCode == MQConstants.MQRC_CONNECTION_BROKEN);
	}
	
	public static boolean isConnectionBroken(ConnectivityException e)
	{
		if (e.getCause() instanceof MQException)
			return isConnectionBroken((MQException) e.getCause());
		else 
			return false;
	}
}
