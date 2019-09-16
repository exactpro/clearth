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

package com.exactprosystems.clearth.connectivity;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import static java.lang.String.format;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class ListenerConfiguration
{
	@XmlElement(name = "Name")
	protected String name;
	@XmlElement(name = "Type")
	protected String type;
	@XmlElement(name = "Settings")
	protected String settings;
	
	protected ReceiveListener implementation;
	
	public ListenerConfiguration()
	{
		name = null;
		type = null;
		settings = null;
		implementation = null;
	}
	
	public ListenerConfiguration(String name, String type, String settings)
	{
		this.name = name;
		this.type = type;
		this.settings = settings;
		this.implementation = null;
	}
	
	public ListenerConfiguration(ListenerConfiguration cfg)
	{
		this.name = cfg.name;
		this.type = cfg.type;
		this.settings = cfg.settings;
		this.implementation = cfg.implementation;
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
	
	
	public ReceiveListener getImplementation()
	{
		return implementation;
	}
	
	public void setImplementation(ReceiveListener implementation)
	{
		this.implementation = implementation;
	}

	@Override
	public String toString()
	{
		return format("Listener name='%s', type='%s', settings='%s', implementation='%s'",
				name, type, settings, (implementation != null) ? implementation.getClass() : null);
	}
}
