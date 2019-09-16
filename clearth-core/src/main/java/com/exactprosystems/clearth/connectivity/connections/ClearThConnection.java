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

package com.exactprosystems.clearth.connectivity.connections;

import com.exactprosystems.clearth.connectivity.ConnectivityException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.notExists;

@XmlRootElement(name="AbstractConnection")
@XmlAccessorType(XmlAccessType.NONE)
public abstract class ClearThConnection<C extends ClearThConnection<C,S>, 
										S extends ClearThConnectionSettings<S>>
{
	@XmlElement
	protected String name;
	
	@XmlElement
	protected S settings;
	
	protected String type;
	
	protected volatile boolean running = false;
	
	protected Date started;
	protected Date stopped;
	
	public  void setSettings(S connectionSettings)
	{
		this.settings = connectionSettings;
	}

	public S getSettings()
	{
		return settings;
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
		return this.type;
	}
	

	public void setType(String type)
	{
		this.type = type;
	}

	public Date getStarted()
	{
		return started;
	}

	public Date getStopped()
	{
		return stopped;
	}

	public abstract void start() throws Exception;
	public abstract void stop() throws Exception;
	public abstract C copy();
	public abstract void copy(C copyFrom);
	
	public boolean isAutoConnect()
	{
		return false;
	}

	public ClearThConnection()
	{
		name = "";
		settings = createSettings();
		this.type = initType();
	}
	
	protected abstract String initType();

	public String connectionFileName()
	{
		return connectionFilePath() + name + ".xml";
	}
	
	public abstract String connectionFilePath();
	protected abstract S createSettings();
	
	
	protected Marshaller createMarshaller() throws JAXBException
	{
		JAXBContext jc = JAXBContext.newInstance(this.getClass(), this.settings.getClass());
		return jc.createMarshaller();
	}
	
	public void save(File file) throws ConnectivityException
	{
		createParentDirForSettings(file);
		try
		{
			Marshaller m = createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			m.marshal(this, file);
		}
		catch (JAXBException e)
		{
			throw new ConnectivityException(e, "Cannot save settings of connection '%s' to file '%s'.",
					name, file.getAbsolutePath());
		}
	}
	
	public void save() throws ConnectivityException
	{
		save(new File(connectionFileName()));
	}
	
	public boolean isRunning()
	{
		return running;
	}
	
	
	private void createParentDirForSettings(File settingsFile) throws ConnectivityException
	{
		Path parentDir = settingsFile.getParentFile().toPath();
		if (notExists(parentDir))
		{
			try
			{
				createDirectories(parentDir);
			}
			catch (IOException e)
			{
				throw new ConnectivityException(e, "Unable to create directory '%s' to save connection settings file '%s'.",
						parentDir.toAbsolutePath(), settingsFile.getName());
			}
		}
	}
}
