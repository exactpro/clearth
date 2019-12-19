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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.exactprosystems.clearth.automation.exceptions.ResultException;
import org.apache.commons.collections4.map.UnmodifiableMap;

public class MatrixContext
{
	protected Map<String, Object> context;
	protected LinkedHashMap<String, SubActionData> subActionsData;
	
	public MatrixContext()
	{
		context = new HashMap<String, Object>();
		subActionsData = new LinkedHashMap<String, SubActionData>();
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
	
	public void setContext(String key, Object value)
	{
		context.put(key, value);
	}
	
	
	public SubActionData getSubActionData(String subActionId)
	{
		return subActionsData.get(subActionId);
	}
	
	public void setSubActionData(String id, SubActionData subActionData)
	{
		subActionsData.put(id, subActionData);
	}
	
	public Map<String, SubActionData> getSubActionsData() {
		return UnmodifiableMap.unmodifiableMap(this.subActionsData);
	}

	public void clearContext()
	{
		context.clear();
		subActionsData.clear();
	}
}
