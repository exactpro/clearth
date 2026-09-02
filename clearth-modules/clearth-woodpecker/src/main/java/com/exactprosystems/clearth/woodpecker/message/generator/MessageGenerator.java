/******************************************************************************
 * Copyright 2009-2025 Exactpro Systems Limited
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

package com.exactprosystems.clearth.woodpecker.message.generator;

import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.woodpecker.configuration.start.OperationUnit;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerNotEnoughInitialDataException;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerException;
import com.exactprosystems.clearth.woodpecker.expressions.WoodpeckerFunctions;
import com.exactprosystems.clearth.woodpecker.message.data.LinkedMessages;
import com.exactprosystems.clearth.woodpecker.message.data.Message;
import com.exactprosystems.clearth.woodpecker.message.generator.dictionary.FieldDescription;
import com.exactprosystems.clearth.woodpecker.message.generator.dictionary.LinkedMessagesDescription;
import com.exactprosystems.clearth.woodpecker.message.generator.dictionary.MessageDescription;
import com.exactprosystems.clearth.woodpecker.message.generator.dictionary.RgDescription;
import com.exactprosystems.clearth.woodpecker.misc.encoder.MessageEncoder;
import org.apache.commons.lang3.mutable.MutableInt;
import org.mvel2.templates.CompiledTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.exactprosystems.clearth.connectivity.iface.ClearThMessage.MSGTYPE;
import static com.exactprosystems.clearth.connectivity.iface.ClearThMessage.SUBMSGTYPE;
import static com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerFunctionsException.whenEvaluationFailed;

public class MessageGenerator
{
	private static final Logger log = LoggerFactory.getLogger(MessageGenerator.class);
	
	private final MessageEncoder encoder;
	private final ClearThMessageConstructor messageConstructor;
	

	public MessageGenerator(MessageEncoder encoder, ClearThMessageConstructor messageConstructor)
	{
		this.encoder = encoder;
		this.messageConstructor = messageConstructor;
	}


	public LinkedMessages generate(LinkedMessagesDescription linkedMessagesDesc, OperationUnit opUnit,
	                               WoodpeckerFunctions functions, Map<String, Object> externalParameters)
			throws WoodpeckerException
	{
		LinkedMessages linkedMessages = new LinkedMessages();
		Map<String, Object> context = createContext(externalParameters);
		
		for (MessageDescription messageDesc : linkedMessagesDesc.getMessageDescs())
		{
			Map<String, MutableInt> rgCountersByType = new HashMap<>();
			ClearThMessage cthMessage = generateMessage(messageDesc, linkedMessagesDesc.getCodecName(),
					false, opUnit, rgCountersByType, functions, context);

			String messageText = encoder.encode(cthMessage, linkedMessagesDesc.getCodecName());
			Message message = new Message(messageText, messageDesc.getMessageType(), cthMessage);
			
			if (!rgCountersByType.isEmpty())
				message.setRgsCountByType(rgCountersByType);
			
			linkedMessages.addMessage(message);
		}
		return linkedMessages;
	}
	
	
	protected Map<String, Object> createContext(Map<String, Object> externalParameters)
	{
		return new HashMap<>(externalParameters);
	}
	
	
	protected ClearThMessage generateMessage(MessageDescription description, String codecName, boolean isRg,
	                                       OperationUnit opUnit, Map<String, MutableInt> rgCountersByType,
	                                       WoodpeckerFunctions functions, Map<String, Object> context) 
			throws WoodpeckerException
	{
		ClearThMessage message = messageConstructor.createMessage(codecName);
		message.addField(isRg ? SUBMSGTYPE : MSGTYPE, description.getMessageType());
		
		if (description.containsFields())
			generateMessageFields(message, description, functions, context);
		
		if (description.containsRepeatingGroups())
			generateRepeatingGroups(message, description, codecName, opUnit, rgCountersByType, functions, context);
		
		return message;
	}
	
	
	protected final void generateMessageFields(ClearThMessage message, MessageDescription md, WoodpeckerFunctions functions,
	                                   Map<String, Object> context) throws WoodpeckerException
	{
		for (FieldDescription fd : md.getFields())
		{
			String name = fd.getName();
			
			Object value = fd.containsExpression() 
					? calculateExpression(functions, context, fd, md) 
					: fd.getValue();
			
			if (value != null)
			{
				message.addField(name, String.valueOf(value));
				context.put(name, value);
			}
			else 
				log.debug("Expression for '{}' [{}] from template evaluated to null.", name, fd.getValue());
		}
	}


	@SuppressWarnings("unchecked")
	protected final void generateRepeatingGroups(ClearThMessage message, MessageDescription messageDescription, 
	                                     String codecName, OperationUnit opUnit, 
	                                     Map<String, MutableInt> rgCountersByType, WoodpeckerFunctions functions,
	                                     Map<String, Object> parentContext) throws WoodpeckerException
	{
		for (RgDescription rgDescription : messageDescription.getRepeatingGroupsDescs())
		{
			String type = rgDescription.getMessageType();
			int countToGenerate = rgDescription.getCountToGenerate(parentContext);
			if (countToGenerate <= 0)
				continue;

			for (int i = 0; i < countToGenerate; i++)
			{
				message.addSubMessage(generateMessage(rgDescription, codecName, true, opUnit, rgCountersByType, 
						functions, new HashMap<>(parentContext)));
			}
			
			MutableInt counter = rgCountersByType.get(type);
			if (counter == null)
			{
				counter = new MutableInt(countToGenerate);
				rgCountersByType.put(type, counter);
			}
			else 
				counter.add(countToGenerate);
		}
	}
	
	
	protected final Object calculateExpression(WoodpeckerFunctions functions, Map<String, Object> context, FieldDescription fd, 
	                                   MessageDescription md) throws WoodpeckerException
	{
		String fieldName = fd.getName();
		String value = fd.getValue();
		CompiledTemplate expression = fd.getValueExpression();
		try
		{
			return functions.calculate(expression, context);
		}
		catch (Exception e)
		{
			if (e instanceof WoodpeckerNotEnoughInitialDataException)
				throw (WoodpeckerNotEnoughInitialDataException) e;
			else 
				throw whenEvaluationFailed(e, fieldName, value, md.getSourceFilePath());
		}
	}
}
