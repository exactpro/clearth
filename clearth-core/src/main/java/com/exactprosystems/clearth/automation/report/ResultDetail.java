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

package com.exactprosystems.clearth.automation.report;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.report.results.ComparisonResult;
import com.exactprosystems.clearth.utils.LineBuilder;

import java.io.Serializable;
import java.util.Objects;

public class ResultDetail implements Serializable
{
	private String param, expected, actual, errorMessage;
	private boolean identical, info, forCompareValue;
	
	public ResultDetail()
	{
		param = null;
		expected = null;
		actual = null;
		identical = false;
		info = false;
	}
	
	public ResultDetail(String param, String expected, String actual, boolean identical)
	{
		this.param = param;
		setExpected(expected);
		this.actual = actual;
		this.identical = identical;
		this.info = false;
	}
	
	public ResultDetail(String param, String expected, String actual, boolean identical, boolean expectedIsForCompareValue)
	{
		this.param = param;
		this.expected = expected;
		this.forCompareValue = expectedIsForCompareValue;
		this.actual = actual;
		this.identical = identical;
		this.info = false;
	}
	
	public ResultDetail(String param, String expected, String actual, ComparisonResult compResult)
	{
		this.param = param;
		setExpected(expected);
		this.actual = actual;
		this.identical = compResult.isIdentical();
		this.info = compResult.isInfo();
	}
	
	public ResultDetail(String param, String expected, String actual, ComparisonResult compResult, boolean expectedIsForCompareValue)
	{
		this.param = param;
		this.expected = expected;
		this.forCompareValue = expectedIsForCompareValue;
		this.actual = actual;
		this.identical = compResult.isIdentical();
		this.info = compResult.isInfo();
	}

	public String getParam()
	{
		return param;
	}
	
	public void setParam(String param)
	{
		this.param = param;
	}
	
	
	public String getExpected()
	{
		return expected;
	}

	public void setExpected(String expected)
	{
		this.expected = expected;
		forCompareValue = ClearThCore.getInstance() != null ? ClearThCore.getInstance().getComparisonUtils().isForCompareValues(expected) : false;
	}

	public boolean isForCompareValue()
	{
		return forCompareValue;
	}

	public void setForCompareValue(boolean forCompareValue)
	{}

	public String getActual()
	{
		return actual;
	}
	
	public void setActual(String actual)
	{
		this.actual = actual;
	}
	
	
	public boolean isIdentical()
	{
		return identical;
	}
	
	public void setIdentical(boolean identical)
	{
		this.identical = identical;
	}

	public boolean isInfo()
	{
		return info || (expected == null && identical);
	}

	public void setInfo(boolean info)
	{
		this.info = info;
	}

	public String getErrorMessage()	{ return this.errorMessage; }

	public void setErrorMessage(String errorMessage)
	{
		this.errorMessage = errorMessage;
	}

	@Override
	public String toString()
	{
		return toLineBuilder(new LineBuilder(), "").toString();
	}

	public LineBuilder toLineBuilder(LineBuilder builder, String prefix)
	{
		builder.add(prefix).add(param).add(": ").add(identical);
		if (isInfo())
			builder.append("(info)");
		builder.add(" / ").add(expected).add(" / ").append(actual).eol();
		builder.add(prefix).add("Error: ").add(errorMessage).eol();
		return builder;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (!(o instanceof ResultDetail)) return false;
		ResultDetail that = (ResultDetail) o;
		return identical == that.identical &&
				info == that.info &&
				Objects.equals(param, that.param) &&
				Objects.equals(expected, that.expected) &&
				Objects.equals(actual, that.actual) &&
				Objects.equals(errorMessage, that.errorMessage);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(param, expected, actual, errorMessage, identical, info);
	}
}
