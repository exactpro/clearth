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

package com.exactprosystems.clearth.automation.functions;

public class SpecialData
{
	private String name;
	private SpecialDataType type;
	private String value;
	private String usage;
	private String description;
	
	public SpecialData(String name, SpecialDataType type, String value, String usage, String description)
	{
		this.name = name;
		this.type = type;
		this.value = value;
		this.usage = usage;
		this.description = description;
	}
	
	public String getName()
	{
		return name;
	}
	
	public void setName(String name)
	{
		this.name = name;
	}
	
	public String getValue()
	{
		return value;
	}
	
	public void setValue(String value)
	{
		this.value = value;
	}
	
	public String getDescription()
	{
		return description;
	}
	
	public String getUsage()
	{
		return usage;
	}
	
	public void setUsage(String usage)
	{
		this.usage = usage;
	}
	
	public void setDescription(String description)
	{
		this.description = description;
	}
	
	public SpecialDataType getType()
	{
		return type;
	}
	
	public void setType(SpecialDataType type)
	{
		this.type = type;
	}
}
