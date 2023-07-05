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

package com.exactprosystems.clearth.tools.matrixupdater.settings;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Update")
public class Update
{
	@XmlAttribute
	private String name;

	@XmlElement(name = "Process")
	private UpdateType process;

	@XmlElement(name = "Settings")
	private Settings settings;

	public Update()
	{
		this.settings = new Settings();
	}

	public Update(String name)
	{
		this.settings = new Settings();
		this.name = name;
	}

	public Update(String name, UpdateType process)
	{
		this(name);
		this.process = process;
	}

	public UpdateType getProcess()
	{
		return process;
	}

	public Settings getSettings()
	{
		return settings;
	}

	public String getName()
	{
		return name;
	}

	public void setProcess(String process)
	{
		this.process = UpdateType.valueOf(process.toUpperCase());
	}

	public void setProcess(UpdateType process)
	{
		this.process = process;
	}

	public void setSettings(Settings settings)
	{
		this.settings = settings;
	}

	public void setName(String name)
	{
		this.name = name;
	}

}
