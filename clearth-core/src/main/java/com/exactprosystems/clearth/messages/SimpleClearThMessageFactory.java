/******************************************************************************
 * Copyright 2009-2024 Exactpro Systems Limited
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

package com.exactprosystems.clearth.messages;

import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.MatrixContext;
import com.exactprosystems.clearth.automation.actions.metadata.MetaFieldsGetter;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.SimpleClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.SimpleClearThMessageBuilder;

import java.util.Map;
import java.util.Set;

public class SimpleClearThMessageFactory
{
	private final Set<String> serviceParams;
	private final MetaFieldsGetter metaGetter;
	
	public SimpleClearThMessageFactory(Set<String> serviceParams, MetaFieldsGetter metaGetter)
	{
		this.serviceParams = serviceParams;
		this.metaGetter = metaGetter;
	}
	
	public SimpleClearThMessage createMessage(Map<String, String> inputParams, MatrixContext matrixContext, Action action)
	{
		MessageBuilder<SimpleClearThMessage> builder = getMessageBuilder(serviceParams, getMetaFields(inputParams));
		return buildMessage(inputParams, matrixContext, action, builder)
				.type(inputParams.get(ClearThMessage.MSGTYPE))
				.build();
	}
	
	public SimpleClearThMessage createMessageWithoutType(Map<String, String> inputParams, MatrixContext matrixContext, Action action)
	{
		MessageBuilder<SimpleClearThMessage> builder = getMessageBuilder(serviceParams, getMetaFields(inputParams));
		return buildMessage(inputParams, matrixContext, action, builder)
				.build();
	}
	
	protected MessageBuilder<SimpleClearThMessage> buildMessage(Map<String, String> inputParams, MatrixContext matrixContext, Action action,
			MessageBuilder<SimpleClearThMessage> builder)
	{
		return builder.fields(inputParams)
				.metaFields(inputParams)
				.rgs(matrixContext, action);
	}
	
	protected MessageBuilder<SimpleClearThMessage> getMessageBuilder(Set<String> specialParams, Set<String> metaFields)
	{
		return new SimpleClearThMessageBuilder(specialParams, metaFields);
	}
	
	protected Set<String> getMetaFields(Map<String, String> inputParams)
	{
		Set<String> result = metaGetter.getFields(inputParams);
		metaGetter.checkFields(result, inputParams);
		return result;
	}
}
