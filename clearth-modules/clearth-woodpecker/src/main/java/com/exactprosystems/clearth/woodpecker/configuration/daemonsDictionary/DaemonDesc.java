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

package com.exactprosystems.clearth.woodpecker.configuration.daemonsDictionary;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "daemon")
public abstract class DaemonDesc
{
	@XmlAttribute(required = true)
	private String type;

	@XmlAttribute(required = true)
	private String baseDirectory;

	@XmlAttribute
	private String unitName;

	@XmlAttribute
	private String pluralUnitName;
	
	
	public abstract String getFactoryName();
	
	protected abstract String getDefaultUnitName();


	public String getType()
	{
		return type;
	}

	public void setType(String type)
	{
		this.type = type;
	}
	

	public String getBaseDirectory()
	{
		return baseDirectory;
	}

	public void setBaseDirectory(String baseDirectory)
	{
		this.baseDirectory = baseDirectory;
	}


	public String getUnitName()
	{
		return (unitName != null) ? unitName : getDefaultUnitName();
	}

	public void setUnitName(String unitName)
	{
		this.unitName = unitName;
	}


	public String getPluralUnitName()
	{
		return (pluralUnitName != null) ? pluralUnitName : getUnitName() + "s";
	}

	public void setPluralUnitName(String pluralUnitName)
	{
		this.pluralUnitName = pluralUnitName;
	}
}
