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

package com.exactprosystems.clearth.connectivity.remotehand;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.exactprosystems.clearth.connectivity.ConnectivityException;
import com.exactprosystems.clearth.connectivity.connections.BasicClearThConnection;
import com.exactprosystems.clearth.connectivity.connections.ClearThCheckableConnection;
import com.exactprosystems.clearth.connectivity.connections.SettingsClass;
import com.exactprosystems.clearth.utils.SettingsException;

@XmlRootElement(name="RhConnection")
@XmlAccessorType(XmlAccessType.NONE)
@SettingsClass(RhConnectionSettings.class)
public class RhConnection extends BasicClearThConnection implements ClearThCheckableConnection
{
	public static final String TYPE_RH = "RemoteHand";
	
	@Override
	public RhConnectionSettings getSettings()
	{
		return (RhConnectionSettings)super.getSettings();
	}
	
	@Override
	public void check() throws SettingsException, ConnectivityException
	{
		try (RhClient client = RhUtils.createRhConnection(getSettings().getUrl()))
		{
		}
		catch (Exception e)
		{
			throw new ConnectivityException("Could not connect to RemoteHand", e);
		}
	}
}
