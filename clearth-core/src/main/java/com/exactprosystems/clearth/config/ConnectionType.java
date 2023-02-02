/******************************************************************************
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

package com.exactprosystems.clearth.config;

import javax.xml.bind.annotation.*;

@XmlType(name = "type")
public class ConnectionType
{
	private String name,
			connectionClass,
			directory;
	private ValidationRulesConfig validationRules;
	
	public ConnectionType() {}
	
	@Override
	public String toString()
	{
		return "[name = " + getName() + 
				"; connectionClass = " + getConnectionClass() + 
				"; directory = " + getDirectory() + 
				"; validationRules = " + getValidationRules() + "]";
	}
	
	
	public String getName()
	{
		return name;
	}
	
	public void setName(String name)
	{
		this.name = name;
	}
	
	
	public String getConnectionClass()
	{
		return connectionClass;
	}
	
	public void setConnectionClass(String connectionClass)
	{
		this.connectionClass = connectionClass;
	}
	
	
	public String getDirectory()
	{
		return directory;
	}
	
	public void setDirectory(String directory)
	{
		this.directory = directory;
	}
	
	
	public ValidationRulesConfig getValidationRules()
	{
		if (validationRules == null)
			validationRules = new ValidationRulesConfig();
		return validationRules;
	}
	
	public void setValidationRules(ValidationRulesConfig validationRules)
	{
		this.validationRules = validationRules;
	}
}
