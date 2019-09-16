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

package com.exactprosystems.clearth.utils;


import java.util.Objects;

public class ComparisonPair<T>
{
	private String name;
	private T expectedValue;
	private T actualValue;

	public ComparisonPair(String name, T expectedValue, T actualValue)
	{
		this.name = name;
		this.expectedValue = expectedValue;
		this.actualValue = actualValue;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public T getExpectedValue()
	{
		return expectedValue;
	}

	public void setExpectedValue(T expectedValue)
	{
		this.expectedValue = expectedValue;
	}

	public T getActualValue()
	{
		return actualValue;
	}

	public void setActualValue(T actualValue)
	{
		this.actualValue = actualValue;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (!(o instanceof ComparisonPair)) return false;
		ComparisonPair<?> that = (ComparisonPair<?>) o;
		return Objects.equals(name, that.name) &&
				Objects.equals(expectedValue, that.expectedValue) &&
				Objects.equals(actualValue, that.actualValue);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(name, expectedValue, actualValue);
	}

	@Override
	public String toString()
	{
		return "ComparisonPair{" +
				"name='" + name + '\'' +
				", expectedValue=" + expectedValue +
				", actualValue=" + actualValue +
				'}';
	}
}
