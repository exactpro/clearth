/******************************************************************************
 * Copyright 2009-2019 Exactpro Systems Limited
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

package com.exactprosystems.clearth.connectivity.iface;

import java.util.ArrayList;
import java.util.List;

import com.exactprosystems.clearth.connectivity.DecodeException;

public class MessageValidator
{
	private final List<MessageValidatorCondition> conditions;
	
	public MessageValidator()
	{
		conditions = null;
	}
	
	public MessageValidator(List<MessageValidatorCondition> conditions)
	{
		this.conditions = conditions;
	}

	public MessageValidator(String containsRegex, String notContainsRegex)
	{
		if ((containsRegex != null) || (notContainsRegex != null))
		{
			conditions = new ArrayList<MessageValidatorCondition>();
			if (containsRegex != null)
				conditions.add(new RegexCondition(containsRegex, false));
			if (notContainsRegex != null)
				conditions.add(new RegexCondition(notContainsRegex, true));
		}
		else
			conditions = null;
	}
	
	
	public void validate(String messageText, List<MessageValidatorCondition> conditions) throws DecodeException
	{
		MessageValidatorCondition errorCondition = checkConditions(messageText, conditions);
		if (errorCondition != null)
			throw new DecodeException(errorCondition.buildErrorMessage());
	}
	
	public boolean isValid(String messageText, List<MessageValidatorCondition> conditions)
	{
		return checkConditions(messageText, conditions) == null;
	}
	
	
	public void validate(String messageText) throws DecodeException
	{
		validate(messageText, conditions);
	}
	
	public boolean isValid(String messageText)
	{
		return isValid(messageText, conditions);
	}
	
	
	protected MessageValidatorCondition checkConditions(String s, List<MessageValidatorCondition> conditions)
	{
		if (conditions == null)
			return null;
		
		for (MessageValidatorCondition c : conditions)
		{
			if (!c.check(s))
				return c;
		}
		return null;
	}
}
