/*******************************************************************************
 * Copyright 2009-2023 Exactpro Systems Limited
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

package com.exactprosystems.clearth.automation.report.results;

import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.ResultDetail;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

public enum ComparisonResult
{
	PASSED(true, false),
	FAILED(false, false),
	INFO(true, true);

	final boolean identical;
	final boolean info;
	
	ComparisonResult(boolean identical, boolean info)
	{
		this.identical = identical;
		this.info = info;
	}

	public static ComparisonResult from(Result result)
	{
		return result.isSuccess() ? PASSED : FAILED;
	}
	
	public static ComparisonResult from(ResultDetail resultDetail)
	{
		return resultDetail.isIdentical() ? PASSED : resultDetail.isInfo() ? INFO : FAILED;
	}
	
	public static ComparisonResult from(String stringResult)
	{
		for (ComparisonResult cr : values())
		{
			if (StringUtils.equalsIgnoreCase(cr.toString(), stringResult))
				return cr;
		}
		throw new IllegalArgumentException("'" + stringResult + "' cannot be parsed to comparison result; " +
				"possible options are: " + Arrays.toString(values()));
	}

	public static ComparisonResult from(boolean equals)
	{
		return equals ? PASSED : FAILED;
	}

	public boolean isIdentical()
	{
		return identical;
	}
	
	public boolean isInfo()
	{
		return info;
	}
}
