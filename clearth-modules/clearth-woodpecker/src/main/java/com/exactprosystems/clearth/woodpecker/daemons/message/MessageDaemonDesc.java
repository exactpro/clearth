/******************************************************************************
 * Copyright 2009-2025 Exactpro Systems Limited
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

package com.exactprosystems.clearth.woodpecker.daemons.message;

import com.exactprosystems.clearth.woodpecker.configuration.daemonsDictionary.DaemonDesc;
import com.exactprosystems.clearth.woodpecker.configuration.settings.loading.LoadingUnit;

import javax.xml.bind.annotation.*;

import static org.apache.commons.lang3.StringUtils.lowerCase;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "messageDaemon")
public class MessageDaemonDesc extends DaemonDesc
{
	private static final MessageStatisticsSettings DEF_STATISTICS_SETTINGS = new MessageStatisticsSettings();

	private LoadingUnit unit = LoadingUnit.Message;
	
	@XmlElement(name = "statistics")
	private MessageStatisticsSettings statisticsSettings = DEF_STATISTICS_SETTINGS;


	@Override
	public String getFactoryName()
	{
		return MessageDaemonFactory.FACTORY_NAME;
	}

	@Override
	protected String getDefaultUnitName()
	{
		return lowerCase(unit.name());
	}
	
	
	@XmlAttribute
	public String getUnit()
	{
		return unit.toString();
	}
	
	public LoadingUnit getLoadingUnit()
	{
		return unit;
	}

	public void setUnit(String unit)
	{
		this.unit = LoadingUnit.fromString(unit);
	}

	public MessageStatisticsSettings getStatisticsSettings()
	{
		return statisticsSettings;
	}

	public void setStatisticsSettings(MessageStatisticsSettings statisticsSettings)
	{
		this.statisticsSettings = statisticsSettings;
	}
}
