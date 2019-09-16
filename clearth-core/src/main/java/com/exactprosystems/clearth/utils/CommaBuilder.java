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

public class CommaBuilder
{
	public static final String commaDelimiter = ", ";
	
	private final StringBuilder sb = new StringBuilder();
	private final String delimiter;
	
	public CommaBuilder()
	{
		delimiter = commaDelimiter;
	}
	
	public CommaBuilder(String delimiter)
	{
		this.delimiter = delimiter;
	}
	
	
	public void appendDelimiter()
	{
		if (sb.length() > 0)
			sb.append(delimiter);
	}
	
	
	public CommaBuilder append(String str)
	{
		appendDelimiter();
		sb.append(str);
		return this;
	}
	
	public CommaBuilder append(int i)
	{
		appendDelimiter();
		sb.append(i);
		return this;
	}
	
	public CommaBuilder append(char c)
	{
		appendDelimiter();
		sb.append(c);
		return this;
	}
	
	public CommaBuilder append(Object o)
	{
		appendDelimiter();
		sb.append(o);
		return this;
	}
	
	
	public CommaBuilder add(String str)
	{
		sb.append(str);
		return this;
	}
	
	public CommaBuilder add(int i)
	{
		sb.append(i);
		return this;
	}
	
	public CommaBuilder add(char c)
	{
		sb.append(c);
		return this;
	}

	public CommaBuilder add(Object o)
	{
		sb.append(o);
		return this;
	}
	
	
	public int length()
	{
		return sb.length();
	}
	
	@Override
	public String toString()
	{
		return sb.toString();
	}
}
