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

package com.exactprosystems.clearth;

import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.ResultDetail;
import com.exactprosystems.clearth.automation.report.results.ContainerResult;
import com.exactprosystems.clearth.automation.report.results.DetailedResult;

public class ResultTestUtils
{
	
	public static ResultDetail resultDetail(String param, String expected, String actual,
	                                        boolean identical, boolean info)
	{
		ResultDetail rd = new ResultDetail(param, expected, actual, identical);
		rd.setInfo(info);
		return rd;
	}

	public static ResultDetail resultDetail(String param, String expected, String actual, boolean identical)
	{
		return resultDetail(param, expected, actual, identical, false);
	}
	
	public static ResultDetail passedDetail(String param, String value)
	{
		return resultDetail(param, value, value, true);
	}
	
	public static ResultDetail failedDetail(String param, String expected, String actual)
	{
		return resultDetail(param, expected, actual, false);
	}
	
	public static ResultDetail infoDetail(String param, String actual)
	{
		return resultDetail(param, null, actual, true, true);
	}
	
	
	public static DetailedResult detailedResult(ResultDetail... details)
	{
		DetailedResult result = new DetailedResult();
		for (ResultDetail detail : details)
		{
			result.addResultDetail(detail);
		}
		return result;
	}

	
	public static ContainerResult containerResult(Result... results)
	{
		return containerResult(null, null, results);
	}
	
	public static ContainerResult containerResult(Boolean success, String comment, Result... results)
	{
		ContainerResult container = new ContainerResult();
		if (success != null)
			container.setSuccess(success);
		container.setComment(comment);
		for (Result result : results)
		{
			container.addDetail(result);
		}
		return container;
	}
}
