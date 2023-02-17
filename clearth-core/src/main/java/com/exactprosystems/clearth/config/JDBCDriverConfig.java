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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

@XmlType(name = "jdbcDrivers")
@XmlAccessorType(XmlAccessType.NONE)
public class JDBCDriverConfig
{
	@XmlElement(name = "driverClass")
	private List<String> driverClassNames;

	public List<String> getDriverClassNames()
	{
		if(driverClassNames == null)
			driverClassNames = new ArrayList<>();
		return driverClassNames;
	}

	public void setDriverClassNames(List<String> driverClass)
	{
		this.driverClassNames = driverClass;
	}

	@Override
	public String toString()
	{
		return "Driver classes: " + getDriverClassNames().toString();
	}
}
