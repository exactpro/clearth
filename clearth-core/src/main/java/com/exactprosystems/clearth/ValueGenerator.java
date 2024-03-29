/******************************************************************************
 * Copyright 2009-2022 Exactpro Systems Limited
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

public abstract class ValueGenerator
{
	private volatile String lastGeneratedValue;
	
	public String generateValue(int length)
	{
		String value = newValue();
		value = adjustLength(value, length);
		lastGeneratedValue = value;
		return value;
	}
	
	
	protected abstract String newValue();
	
	public String getLastGeneratedValue()
	{
		return lastGeneratedValue;
	}
	
	protected void setLastGeneratedValue(String lastGeneratedValue)
	{
		this.lastGeneratedValue = lastGeneratedValue;
	}
	
	
	private String adjustLength(String value, int length)
	{
		int vl = value.length();
		if (vl == length)
			return value;
		
		if (vl > length)
			return value.substring(vl-length);
		
		StringBuilder sb = new StringBuilder();
		for (int i = vl; i < length; i++)
			sb.append("0");
		sb.append(value);
		return sb.toString();
	}
}
