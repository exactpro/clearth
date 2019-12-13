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

package com.exactprosystems.clearth.utils.tabledata.comparison.result;

/**
 * Comparison detail for the one table data column.
 * @param <A> class of column object.
 * @param <B> class of expected and actual values.
 */
public class ColumnComparisonDetail<A, B>
{
	private A column;
	private B expectedValue, actualValue;
	private boolean identical, info;
	
	public ColumnComparisonDetail() { }
	
	public ColumnComparisonDetail(A column, B expectedValue, B actualValue, boolean identical, boolean info)
	{
		this.column = column;
		this.expectedValue = expectedValue;
		this.actualValue = actualValue;
		this.identical = identical;
		this.info = info;
	}
	
	public void setColumn(A column)
	{
		this.column = column;
	}
	
	public A getColumn()
	{
		return column;
	}
	
	public void setExpectedValue(B expectedValue)
	{
		this.expectedValue = expectedValue;
	}
	
	public B getExpectedValue()
	{
		return expectedValue;
	}
	
	public void setActualValue(B actualValue)
	{
		this.actualValue = actualValue;
	}
	
	public B getActualValue()
	{
		return actualValue;
	}
	
	public void setIdentical(boolean identical)
	{
		this.identical = identical;
	}
	
	public boolean isIdentical()
	{
		return identical;
	}
	
	public void setInfo(boolean info)
	{
		this.info = info;
	}
	
	public boolean isInfo()
	{
		return info;
	}
}
