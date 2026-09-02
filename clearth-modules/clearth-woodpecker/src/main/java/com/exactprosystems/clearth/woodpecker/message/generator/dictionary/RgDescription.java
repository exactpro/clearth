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

package com.exactprosystems.clearth.woodpecker.message.generator.dictionary;

import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerConfigException;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class RgDescription extends MessageDescription
{
	private String countVarName;
	//or
	private int minCount = 1;
	private int maxCount = 1;
	

	RgDescription(Path sourceFilePath)
	{
		super(sourceFilePath);
	}
	
	
	public int getCountToGenerate(Map<String, Object> contextVars) throws WoodpeckerConfigException
	{
		if (countVarName != null)
			return getCountFromContext(contextVars, countVarName);
		else if (minCount == maxCount)
			return minCount;
		else 
			return ThreadLocalRandom.current().nextInt(minCount, maxCount + 1);
	}
	
	private int getCountFromContext(Map<String, Object> contextVars, String varName) throws WoodpeckerConfigException
	{
		Object var = contextVars.get(varName);
		if (var == null)
			throw new WoodpeckerConfigException("Variable '%s' specified as count of repeating groups '%s' " +
					"isn't found in context.", varName, getMessageType());
		
		if (var instanceof Number)
			return ((Number) var).intValue();
		else if (var instanceof String)
		{
			try
			{
				return Integer.parseInt((String) var);
			}
			catch (NumberFormatException e)
			{
				throw new WoodpeckerConfigException(e, "Variable '%s'='%s' specified as count of repeating groups '%s' " +
						"isn't valid number.", varName, var, getMessageType());
			}
		}
		else 
			throw new WoodpeckerConfigException("Cannot parse variable '%s' with type %s " +
					"specified as count of repeating groups '%s' as number.",
					varName, var.getClass().getName(), getMessageType());
	}


	public void setCountVarName(String countVarName)
	{
		this.countVarName = countVarName;
	}

	public void setMinCount(int minCount)
	{
		this.minCount = minCount;
	}

	public void setMaxCount(int maxCount)
	{
		this.maxCount = maxCount;
	}
}
