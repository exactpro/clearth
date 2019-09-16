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

package com.exactprosystems.clearth.converters;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.automation.ActionMetaData;
import com.exactprosystems.clearth.automation.DefaultActionGenerator;
import com.exactprosystems.clearth.automation.actions.MessageAction;

public abstract class Converter
{
	private static final Logger logger = LoggerFactory.getLogger(MessageConverter.class);
	
	protected List<ActionData> loadActions()
	{
		List<ActionData> actionsList = new LinkedList<ActionData>();
		
		for (Entry<String, ActionMetaData> action : getActions().entrySet())
		{
			try
			{
				Class<?> cls;
				try
				{
					cls = Class.forName(action.getValue().getClazz());
				}
				catch (Exception e)
				{
					// No need to write exception to log, because actionsmapping.cfg can
					// contain not only classes definition, but also packages like 'soap=%actions%.soap'
					continue;
				}
				boolean incomingAction = false;
				
				if (MessageAction.class.isAssignableFrom(cls))
				{
					MessageAction messageAction = (MessageAction)cls.newInstance();
					incomingAction = messageAction.isIncoming();
				}
				
				actionsList.add(new ActionData(action.getKey(), incomingAction, new HashMap<String, String>(action.getValue().getDefaultInputParams())));
			}
			catch (Exception e)
			{
				logger.warn("Error while solving msgType-actionName relation", e);
			}
		}
		
		return actionsList;
	}
	
	protected Map<String, ActionMetaData> getActions()
	{
		return DefaultActionGenerator.loadActionsMapping(false, logger);
	}
}
