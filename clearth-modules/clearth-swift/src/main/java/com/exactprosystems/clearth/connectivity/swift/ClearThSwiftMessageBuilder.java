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

package com.exactprosystems.clearth.connectivity.swift;

import java.util.Set;

import com.exactprosystems.clearth.messages.MessageBuilder;

public class ClearThSwiftMessageBuilder extends MessageBuilder<ClearThSwiftMessage>
{
	public ClearThSwiftMessageBuilder()
	{
		super();
	}
	
	public ClearThSwiftMessageBuilder(Set<String> serviceParameters, Set<String> metaFields)
	{
		super(serviceParameters, metaFields);
	}
	
	
	@Override
	protected ClearThSwiftMessage createMessage()
	{
		return new ClearThSwiftMessage();
	}
}
