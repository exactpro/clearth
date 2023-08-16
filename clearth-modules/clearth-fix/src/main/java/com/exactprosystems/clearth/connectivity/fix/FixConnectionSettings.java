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

package com.exactprosystems.clearth.connectivity.fix;

import com.exactprosystems.clearth.connectivity.connections.ClearThConnectionSettings;
import com.exactprosystems.clearth.connectivity.connections.settings.ConnectionSetting;
import com.exactprosystems.clearth.connectivity.connections.settings.ConnectionSettings;
import com.exactprosystems.clearth.connectivity.connections.settings.InputType;
import com.exactprosystems.clearth.utils.LineBuilder;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import static java.lang.String.format;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@ConnectionSettings(order = {"fixSettings", "waitForLogon", "username", "password", "seqNumSender", "seqNumTarget"},
		columns = {"username"})
public class FixConnectionSettings implements ClearThConnectionSettings
{
	@XmlElement
	@ConnectionSetting(name = "FIX settings", inputType = InputType.TEXTAREA)
	protected String fixSettings;
	@XmlElement
	@ConnectionSetting(name = "Wait for logon")
	protected int waitForLogon;
	@XmlElement
	@ConnectionSetting
	protected String username;
	@XmlElement
	@ConnectionSetting(inputType = InputType.PASSWORD)
	protected String password;
	@XmlElement
	@ConnectionSetting(name = "Sender sequence number")
	protected int seqNumSender;
	@XmlElement
	@ConnectionSetting(name = "Target sequence number")
	protected int seqNumTarget;
	
	public FixConnectionSettings()
	{
		fixSettings = "";
		waitForLogon = 5000;
		username = "";
		password = "";
		seqNumSender = -1;
		seqNumTarget = -1;
	}
	
	public FixConnectionSettings(FixConnectionSettings settings)
	{
		this.copyFrom(settings);
	}
	
	public String getFixSettings()
	{
		return fixSettings;
	}
	
	public void setFixSettings(String fixSettings)
	{
		this.fixSettings = fixSettings;
	}
	
	
	public int getWaitForLogon()
	{
		return waitForLogon;
	}
	
	public void setWaitForLogon(int waitForLogon)
	{
		this.waitForLogon = waitForLogon;
	}
	
	
	public String getUsername()
	{
		return username;
	}
	
	public void setUsername(String username)
	{
		this.username = username;
	}
	
	
	public String getPassword()
	{
		return password;
	}
	
	public void setPassword(String password)
	{
		this.password = password;
	}
	
	
	public int getSeqNumSender()
	{
		return seqNumSender;
	}
	
	public void setSeqNumSender(int seqNumSender)
	{
		this.seqNumSender = seqNumSender;
	}
	
	
	public int getSeqNumTarget()
	{
		return seqNumTarget;
	}
	
	public void setSeqNumTarget(int seqNumTarget)
	{
		this.seqNumTarget = seqNumTarget;
	}
	
	
	@Override
	public String toString()
	{
		LineBuilder lb = new LineBuilder();
		
		lb.add("FIX settings=").append(fixSettings);
		lb.add("Wait for logon=").append(waitForLogon);
		lb.add("Username=").append(username);
		//Password is not shown here to be not visible in logs
		lb.add("seqNumSender=").append(seqNumSender);
		lb.add("seqNumTarget=").append(seqNumTarget);
		
		return lb.toString();
	}
	
	@Override
	public void copyFrom(ClearThConnectionSettings settings1)
	{
		if (!this.getClass().isAssignableFrom(settings1.getClass()))
		{
			throw new IllegalArgumentException(format("Could not copy settings. " +
							"Expected settings of class '%s', got settings of class '%s'",
					this.getClass().getSimpleName(), settings1.getClass().getSimpleName()));
		}
		
		FixConnectionSettings settings = (FixConnectionSettings) settings1;
		
		this.fixSettings = settings.fixSettings;
		this.waitForLogon = settings.waitForLogon;
		this.username = settings.username;
		this.password = settings.password;
		this.seqNumSender = settings.seqNumSender;
		this.seqNumTarget = settings.seqNumTarget;
	}
}
