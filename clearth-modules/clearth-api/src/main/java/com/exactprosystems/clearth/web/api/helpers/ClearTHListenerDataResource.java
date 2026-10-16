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

package com.exactprosystems.clearth.web.api.helpers;

import com.exactprosystems.clearth.connectivity.ListenerConfiguration;
import com.thoughtworks.xstream.annotations.XStreamAlias;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="cthListener")
@XStreamAlias("cthListener")
public class ClearTHListenerDataResource
{
	@XmlElement
	String name;

	@XmlElement
	String type;

	@XmlElement
	String settings;

	public ClearTHListenerDataResource(ListenerConfiguration configuration)
	{
		name = configuration.getName();
		type = configuration.getType();
		settings = configuration.getSettings();
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getType()
	{
		return type;
	}

	public void setType(String type)
	{
		this.type = type;
	}

	public String getSettings()
	{
		return settings;
	}

	public void setSettings(String settings)
	{
		this.settings = settings;
	}
}
