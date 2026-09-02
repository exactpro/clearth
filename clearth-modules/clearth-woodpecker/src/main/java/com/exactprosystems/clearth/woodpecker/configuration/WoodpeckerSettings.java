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
package com.exactprosystems.clearth.woodpecker.configuration;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "WoodpeckerSettings")
public class WoodpeckerSettings
{
	@XmlElement(name = "schedulerName")
	private String schedulerName;

	public WoodpeckerSettings()
	{
	}

	public WoodpeckerSettings(String schedulerName)
	{
		this.schedulerName = schedulerName;
	}

	public String getSchedulerName()
	{
		return schedulerName;
	}

	public void setSchedulerName(String schedulerName)
	{
		this.schedulerName = schedulerName;
	}
}
