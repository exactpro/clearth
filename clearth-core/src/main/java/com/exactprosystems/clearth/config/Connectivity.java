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

@XmlType(name = "connectivity")
@XmlAccessorType(XmlAccessType.NONE)
public class Connectivity
{
	@XmlElement(name = "types")
	private ConnectionTypesConfig typesConfig;
	@XmlElement(name = "jdbcDrivers")
	private JDBCDriverConfig jdbcDrivers;
	
	public Connectivity(){}
	
	@Override
	public String toString()
	{
		return "[typesConfig: " + getTypesConfig().toString() + "; jdbcDrivers: " + getJdbcDrivers().toString() + "]";
	}
	
	
	public ConnectionTypesConfig getTypesConfig()
	{
		if (typesConfig == null)
			typesConfig = new ConnectionTypesConfig();
		return typesConfig;
	}
	
	public void setTypesConfig(ConnectionTypesConfig typesConfig)
	{
		this.typesConfig = typesConfig;
	}

	public JDBCDriverConfig getJdbcDrivers()
	{
		if(jdbcDrivers == null)
			jdbcDrivers = new JDBCDriverConfig();
		return jdbcDrivers;
	}

	public void setJdbcDrivers(JDBCDriverConfig jdbcDrivers)
	{
		this.jdbcDrivers = jdbcDrivers;
	}
}
