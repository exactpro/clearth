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

package com.exactprosystems.clearth.automation;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultActionGenerator extends ActionGenerator
{
	private static final Logger logger = LoggerFactory.getLogger(DefaultActionGenerator.class);
	
	public DefaultActionGenerator(Map<String, Step> steps, List<Matrix> matrices, Map<String, Preparable> preparableActions)
	{
		super(steps, matrices, preparableActions);
	}
	
	
	@Override
	protected Logger getLogger()
	{
		return logger;
	}
	
//	@Override
//	protected ActionSettings createActionSettings()
//	{
//		return new ActionSettings();
//	}
	
//	@Override
//	protected Action newAction(Class<?> cls, String actionName) throws IllegalArgumentException, SecurityException, InstantiationException, IllegalAccessException, InvocationTargetException,
//			NoSuchMethodException
//	{
//		return (Action)cls.getConstructor().newInstance();
//	}
	
	@Override
	protected boolean customSetting(String name, String value, ActionSettings settings, int headerLineNumber, int lineNumber)
	{
		return false;
	}
	
	@Override
	protected int initAction(Action action, ActionSettings settings, int headerLineNumber, int lineNumber)
	{
		action.init(settings);
		if (!checkAction(action, headerLineNumber, lineNumber))
			return CHECKING_ERROR;
		return NO_ERROR;
	}

	protected boolean checkAction(Action action, int headerLineNumber, int lineNumber)
	{
		return true;
	}
}
