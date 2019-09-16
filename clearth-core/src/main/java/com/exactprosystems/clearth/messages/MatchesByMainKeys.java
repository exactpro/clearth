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

package com.exactprosystems.clearth.messages;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.exceptions.ParametersException;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.utils.ComparisonUtils;

public class MatchesByMainKeys<T extends ClearThMessage<T>> implements MessageMatcher<T>
{
	private static final Logger logger = LoggerFactory.getLogger(MatchesByMainKeys.class);

	private final KeyFieldsData keys;
	private final boolean keysForRg;

	public MatchesByMainKeys(KeyFieldsData keys, boolean keysForRg)
	{
		this.keys = keys;
		this.keysForRg = keysForRg;
	}

	@Override
	public boolean matches(T message) throws ParametersException
	{
		String messageType = message.getField(keysForRg ? ClearThMessage.SUBMSGTYPE : ClearThMessage.MSGTYPE);
		if (!StringUtils.equals(keys.getMsgType(), messageType))
		{
			logger.trace("Message doesn't suit by type: expected '{}', actual '{}'", keys.getMsgType(), messageType);
			return false;
		}
		
		ComparisonUtils cu = ClearThCore.comparisonUtils();
		for (MessageKeyField keyField : keys.getKeys())
		{
			String key = keyField.getName(),
					value = keyField.getValue();
			logger.debug("Checking key field {}", keyField);

			try
			{
				if (!compareValues(key, value, message, cu))
					return false;
			}
			catch (ParametersException e)
			{
				throw new ParametersException(String.format("Error while checking key field '%s': %s", key, e.getMessage()));
			}
		}
		logger.trace("Key fields have been checked successfully");
		return true;
	}
	
	
	protected boolean compareValues(String name, String expectedValue, T message, ComparisonUtils cu) throws ParametersException
	{
		String messageValue = message.getField(name);
		if (messageValue == null)
		{
			logger.trace("No key field '{}' in message", name);
			return false;
		}
		
		if (!cu.compareValues(expectedValue, messageValue))
		{
			logger.debug("Checking of key field '{}' failed: expected '{}', actual '{}'", name, expectedValue, messageValue);
			return false;
		}
		return true;
	}
}
