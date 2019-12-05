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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.exactprosystems.clearth.automation.exceptions.ResultException;

public class StepContext
{
	private final String stepName;
	private final Date started;
	private Map<String, Object> context;
	
	public StepContext(String stepName, Date started)
	{
		this.stepName = stepName;
		this.started = started;
		this.context = new HashMap<String, Object>();
	}
	
	
	public String getStepName()
	{
		return stepName;
	}
	
	public Date getStarted()
	{
		return started;
	}
	
	
	public <T> T getContext(String key)
	{
		try
		{
			//noinspection unchecked
			return (T) context.get(key);
		}
		catch (ClassCastException e)
		{
			throw new ResultException(e);
		}
	}
	
	public void setContext(String key, Object obj)
	{
		context.put(key, obj);
	}
	
	public void removeContext(String key)
	{
		context.remove(key);
	}

	public void clearContext()
	{
		context.clear();
	}
}
