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

public class LineBuilder
{
	private final StringBuilder sb = new StringBuilder();
	private final String delimiter;
	
	public LineBuilder()
	{
		delimiter = Utils.EOL;
	}
	
	public LineBuilder(String delimiter)
	{
		this.delimiter = delimiter;
	}
	
	public LineBuilder append(String str)
	{
		sb.append(str).append(delimiter);
		return this;
	}
	
	public LineBuilder append(int i)
	{
		sb.append(i).append(delimiter);
		return this;
	}
	
	public LineBuilder append(char c)
	{
		sb.append(c).append(delimiter);
		return this;
	}
	
	public LineBuilder append(Object o)
	{
		sb.append(o).append(delimiter);
		return this;
	}
	
	// Use  
	//   lineBuilder.add("A: ").add(a).add(", B:").add(b).eol()  
	// instead of  
	//   lineBuilder.append("A: " + a + ", B:" + b);
	// +35% Performance
	//
	//   lineBuilder.append("SimpleValue");
	//   lineBuilder.add("SimpleValue").eol();
	// Doesn't have a difference;
	
	public LineBuilder add(String str)
	{
		sb.append(str);
		return this;
	}
	
	public LineBuilder add(int i)
	{
		sb.append(i);
		return this;
	}
	
	public LineBuilder add(char c)
	{
		sb.append(c);
		return this;
	}

	public LineBuilder add(Object o)
	{
		sb.append(o);
		return this;
	}
	
	public LineBuilder eol() //Adds "End of line"
	{
		sb.append(delimiter);
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
