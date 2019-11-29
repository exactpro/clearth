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

package com.exactprosystems.clearth.automation.report.results.complex;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.utils.LineBuilder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties({"highlighted"})
public class ComparisonRow
{
	private String param, expected, actual, errorMessage;
	private boolean identical, info, breaker, highlighted, forCompareValue;

	public ComparisonRow()
	{
		param = "";
		identical = false;
		info = false;
		breaker = false;
		highlighted = false;
	}

	public ComparisonRow(String param, String expected, String actual, boolean identical)
	{
		this.param = param;
		setExpected(expected);
		this.actual = actual;
		this.identical = identical;
		this.breaker = false;
		this.info = false;
		this.highlighted = false;
	}

	public ComparisonRow(boolean breaker)
	{
		this.breaker = breaker;
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
		forCompareValue = ClearThCore.getInstance().getComparisonUtils().isForCompareValues(expected);
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

	
	public boolean isBreaker()
	{
		return breaker;
	}

	public void setBreaker(boolean breaker)
	{
		this.breaker = breaker;
	}

	
	public boolean isInfo()
	{
		return info;
	}

	public void setInfo(boolean info)
	{
		this.info = info;
	}


	public boolean isHighlighted()
	{
		return highlighted;
	}

	public void setHighlighted(boolean highlighted)
	{
		this.highlighted = highlighted;
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
		builder.add(prefix).add(param).add(": ").add(identical).add(" / ").add(expected).add(" / ").add(actual).eol();
		builder.add(prefix).add("Error: ").add(errorMessage).eol();
		builder.add(prefix).add("Info: ").add(info).eol();
		builder.add(prefix).add("Breaker: ").add(breaker).eol();
		builder.add(prefix).add("Highlighted: ").add(highlighted).eol();
		return builder;
	}
}
