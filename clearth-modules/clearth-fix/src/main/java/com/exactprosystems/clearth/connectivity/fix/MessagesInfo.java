/******************************************************************************
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

package com.exactprosystems.clearth.connectivity.fix;

import java.util.Map;

/**
 * Storage for information about FIX messages and their components
 */
public class MessagesInfo
{
	private final Map<String, FieldsInfo> messageFieldsInfo,
			componentFieldsInfo;
	
	public MessagesInfo(Map<String, FieldsInfo> messageFieldsInfo, Map<String, FieldsInfo> componentFieldsInfo)
	{
		this.messageFieldsInfo = messageFieldsInfo;
		this.componentFieldsInfo = componentFieldsInfo;
	}

	
	public Map<String, FieldsInfo> getMessageFieldsInfo()
	{
		return messageFieldsInfo;
	}

	public Map<String, FieldsInfo> getComponentFieldsInfo()
	{
		return componentFieldsInfo;
	}
}
