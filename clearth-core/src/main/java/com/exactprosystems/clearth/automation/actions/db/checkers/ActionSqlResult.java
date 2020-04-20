/******************************************************************************
 * Copyright 2009-2020 Exactpro Systems Limited
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
package com.exactprosystems.clearth.automation.actions.db.checkers;

import com.exactprosystems.clearth.automation.report.Result;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ActionSqlResult
{
	private Result result;
	private final Map<String, String> outputParams;


	public ActionSqlResult()
	{
		outputParams = new LinkedHashMap<>();
	}
	
	public ActionSqlResult(Result result, Map<String, String> outputParams)
	{
		this.result = result;
		this.outputParams = outputParams;
	}


	public void setResult(Result result)
	{
		this.result = result;
	}

	public void addOutputParam(String key, String value)
	{
		outputParams.put(key, value);
	}

	public void addOutputParams(Map<String, String> outputParams)
	{
		this.outputParams.putAll(outputParams);
	}

	public Map<String, String> getOutputParams()
	{
		return Collections.unmodifiableMap(outputParams);
	}

	public Result getResult()
	{
		return result;
	}
}
