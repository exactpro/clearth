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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.exactprosystems.clearth.automation.exceptions.ParametersException;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;

public class MatchesByMainAndRgsKeys<T extends ClearThMessage<T>> implements MessageMatcher<T>
{
	private final KeyFieldsData keys;
	private final List<KeyFieldsData> keysInRgs;
	
	public MatchesByMainAndRgsKeys(KeyFieldsData keys, List<KeyFieldsData> keysInRgs)
	{
		this.keys = keys;
		this.keysInRgs = keysInRgs;
	}
	
	@Override
	public boolean matches(T message) throws ParametersException
	{
		return checkKeys(message, keys, false) && matchesByRgsKeys(message);
	}
	
	
	private boolean checkKeys(T message, KeyFieldsData keys, boolean rg) throws ParametersException
	{
		MatchesByMainKeys<T> matchesByMainKeys = new MatchesByMainKeys<T>(keys, rg);
		return matchesByMainKeys.matches(message);
	}
	
	private boolean matchesByRgsKeys(T message) throws ParametersException
	{
		if (keysInRgs == null || keysInRgs.isEmpty())
			return true;
		
		Set<T> usedSubMessages = new HashSet<T>();
		for (KeyFieldsData keys: keysInRgs)
		{
			List<T> subMsgs = findSubMessages(keys, message, usedSubMessages);
			if (!matchesByRgsKeys(subMsgs, keys, usedSubMessages))
				return false;
		}
		return true;
	}
	
	
	protected List<T> findSubMessages(KeyFieldsData keys, T message, Set<T> usedSubMessages)
	{
		List<T> result = new ArrayList<>(),
				subMsgs = message.getSubMessages();
		String type = keys.getMsgType();
		for (T sm : subMsgs)
		{
			//If this RG is already "used", won't add it to result to avoid verifying it again.
			//However, its sub-RGs can be put into result
			if (type.equals(sm.getField(ClearThMessage.SUBMSGTYPE)) && !usedSubMessages.contains(sm))
				result.add(sm);
			
			if (sm.hasSubMessages())
				result.addAll(findSubMessages(keys, sm, usedSubMessages));
		}
		return result;
	}
	
	
	private boolean matchesByRgsKeys(List<T> subMsgs, KeyFieldsData keys, 
			Set<T> usedSubMessages) throws ParametersException
	{
		for (T subMsg : subMsgs)
		{
			if (!checkKeys(subMsg, keys, true))
				continue;
			
			usedSubMessages.add(subMsg);  //Marking this RG as "used" to avoid verifying it again
			return true;
		}
		return false;
	}
}
