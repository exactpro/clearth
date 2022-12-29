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
import javax.xml.bind.annotation.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

@XmlRootElement(name = "ClearThConfiguration")
@XmlAccessorType(XmlAccessType.NONE)
public class ClearThConfiguration
{
	@XmlElement
	protected Automation automation =  new Automation();

	public ClearThConfiguration() {}

	public void setAutomation(Automation automation)
	{
		this.automation = automation;
	}

	public Automation getAutomation()
	{
		return this.automation;
	}

	protected static ClearThConfiguration unmarshal(File configFile) throws ConfigurationException
	{
		FileInputStream reader = null;
		try
		{
			reader = new FileInputStream(configFile);
			JAXBContext context = JAXBContext.newInstance(Automation.class, ClearThConfiguration.class);
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
		return "ClearThConfiguration {\n automation: " + automation.toString() + "\n}";
	}
}