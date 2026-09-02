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
package com.exactprosystems.clearth.woodpecker.configuration.settings;

import com.exactprosystems.clearth.woodpecker.configuration.settings.loading.LoadingSchedule;
import com.exactprosystems.clearth.woodpecker.configuration.settings.loading.LoadingScheduleAdapter;
import com.exactprosystems.clearth.woodpecker.configuration.settings.loading.PriorityMode;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerSettingsException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static com.exactprosystems.clearth.ClearThCore.rootRelative;
import static com.exactprosystems.clearth.woodpecker.configuration.WoodpeckerPaths.WOODPECKER_DAEMONS_SETTINGS_DIR;
import static java.lang.String.format;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.notExists;

@XmlAccessorType(XmlAccessType.NONE)
public class DaemonSettings
{
	private static final LoadingSchedule EMPTY_SCHEDULE = new LoadingSchedule();
	
	@XmlElement
	private String daemonType;
	@XmlElement
	private String daemonName;
	
	@XmlElement
	private ExecutionMode executionMode = ExecutionMode.Simple;

	////////// Loading settings //////////
	
	@XmlElement
	private TimeUnit rateUnit = TimeUnit.SECONDS;
	
	//// Simple mode
	
	@XmlElement
	private long rate = 60;
	
	//// Scheduled mode
	
	@XmlElement
	@XmlJavaTypeAdapter(LoadingScheduleAdapter.class)
	private LoadingSchedule schedule = EMPTY_SCHEDULE;
	
	@XmlElement
	private PriorityMode priorityMode = PriorityMode.Time;
	
	
	public DaemonSettings(String daemonType, String daemonName)
	{
		this.daemonType = daemonType;
		this.daemonName = daemonName;
	}
	
	public DaemonSettings() {}


	public String getFullName()
	{
		return format("%s - %s", getDaemonType(), getDaemonName());
	}
	
	public String getDaemonType()
	{
		return daemonType;
	}

	public void setDaemonType(String daemonType)
	{
		this.daemonType = daemonType;
	}

	public String getDaemonName()
	{
		return daemonName;
	}

	public void setDaemonName(String daemonName)
	{
		this.daemonName = daemonName;
	}


	public ExecutionMode getExecutionMode()
	{
		return executionMode;
	}

	public void setExecutionMode(ExecutionMode executionMode)
	{
		this.executionMode = executionMode;
	}


	public TimeUnit getRateUnit()
	{
		return rateUnit;
	}

	public void setRateUnit(TimeUnit rateUnit)
	{
		this.rateUnit = rateUnit;
	}
	

	public long getRate()
	{
		return rate;
	}

	public void setRate(long rate)
	{
		this.rate = rate;
	}


	public LoadingSchedule getSchedule()
	{
		return schedule;
	}

	public void setSchedule(LoadingSchedule schedule)
	{
		this.schedule = schedule;
	}
	
	public String getTextSchedule()
	{
		return schedule.toString();
	}
	
	public void setTextSchedule(String text) throws WoodpeckerSettingsException
	{
		this.schedule = LoadingSchedule.parse(text);
	}

	public PriorityMode getPriorityMode()
	{
		return priorityMode;
	}

	public void setPriorityMode(PriorityMode priorityMode)
	{
		this.priorityMode = priorityMode;
	}


	public void save() throws WoodpeckerSettingsException
	{
		Path path = getSettingsPath(getDaemonType(), getDaemonName());
		try
		{
			Path parentDir = path.getParent();
			if (notExists(parentDir))
				createDirectories(parentDir);

			JAXBContext ctx = JAXBContext.newInstance(getClass());
			Marshaller m = ctx.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			
			m.marshal(this, path.toFile());
		}
		catch (IOException | JAXBException e)
		{
			throw new WoodpeckerSettingsException(e, "Error while saving daemon settings to file '%s'.", path);
		}
	}
	
	public static Path getSettingsPath(String type, String name)
	{
		return Paths.get(rootRelative(WOODPECKER_DAEMONS_SETTINGS_DIR), type, name + ".xml");
	}
}
