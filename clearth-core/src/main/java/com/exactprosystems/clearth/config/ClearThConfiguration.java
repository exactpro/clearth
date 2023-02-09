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

import com.exactprosystems.clearth.utils.Utils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Map;

@XmlRootElement(name = "ClearThConfiguration")
@XmlAccessorType(XmlAccessType.NONE)
public class ClearThConfiguration
{
	@XmlElement
	protected Automation automation;
	@XmlElement
	protected Connectivity connectivity;
	@XmlElement
	protected Memory memory;
	@XmlElement
	protected LocationConfig locations;
	@XmlElement
	protected Data data;

	public ClearThConfiguration() {}

	public void setAutomation(Automation automation)
	{
		this.automation = automation;
	}

	public Automation getAutomation()
	{
		if(automation == null)
			automation = new Automation();
		return automation;
	}
	
	
	public void setConnectivity(Connectivity connectivity)
	{
		this.connectivity = connectivity;
	}
	
	public Connectivity getConnectivity()
	{
		if (connectivity == null)
			connectivity = new Connectivity();
		return connectivity;
	}
	

	public void setData(Data data)
	{
		this.data = data;
	}

	public Data getData()
	{
		if (data == null)
			data = new Data();
		return data;
	}


	public void setMemory(Memory memory)
	{
		this.memory = memory;
	}

	public Memory getMemory()
	{
		if(memory == null)
			memory = new Memory();
		return memory;
	}

	public void setLocations(LocationConfig locations)
	{
		this.locations = locations;
	}

	public LocationConfig getLocations()
	{
		if(locations == null)
			locations = new LocationConfig();
		return this.locations;
	}

	public Map<String, String> getLocationsMapping()
	{
		if(locations == null)
			return null;
		return locations.getLocationsMapping();
	}

	protected static ClearThConfiguration unmarshal(File configFile) throws ConfigurationException
	{
		FileInputStream reader = null;
		try
		{
			reader = new FileInputStream(configFile);
			JAXBContext context = JAXBContext.newInstance(ClearThConfiguration.class, Automation.class, Connectivity.class, Data.class,
						ConnectionTypesConfig.class, ConnectionType.class, ValidationRulesConfig.class, 
						Memory.class, MemoryMonitorCfg.class, MatrixFatalErrors.class, LocationConfig.class, ReplacedPath.class,
						SpecialActionParameters.class);
			Unmarshaller unmarshal = context.createUnmarshaller();

			return (ClearThConfiguration) unmarshal.unmarshal(reader);
		}
		catch (FileNotFoundException e)
		{
			throw new ConfigurationException(e, "Could not find file '%s'.", configFile.getName());
		}
		catch (JAXBException e)
		{
			throw new ConfigurationException(e, "Configuration file '%s' cannot be deserialized.", configFile.getName());
		}
		finally
		{
			Utils.closeResource(reader);
		}
	}

	public static ClearThConfiguration create(File configFile) throws ConfigurationException
	{
		return unmarshal(configFile);
	}

	@Override
	public String toString()
	{
		return "ClearThConfiguration {\n automation: " + this.getAutomation().toString() +
				"\n connectivity: " + this.getConnectivity().toString() +
				"\n data: " + this.getData().toString() +
				"\n locations: " + this.getLocations().toString() +
				"\n memory: " + this.getMemory().toString() + "\n}";
	}
}