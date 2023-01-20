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

package com.exactprosystems.clearth.connectivity.connections2.settings;

import java.util.Map;

import org.slf4j.Logger;

import com.exactprosystems.clearth.connectivity.ConnectionException;
import com.exactprosystems.clearth.connectivity.ConnectivityException;
import com.exactprosystems.clearth.connectivity.ListenerProperties;
import com.exactprosystems.clearth.connectivity.MessageListener;
import com.exactprosystems.clearth.connectivity.connections.ClearThMessageConnection;
import com.exactprosystems.clearth.utils.SettingsException;

public class DummyConnection extends ClearThMessageConnection<DummyConnection, CorrectSettings>
{

	@Override
	public Logger getLogger()
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	protected MessageListener createListenerEx(ListenerProperties props, String settings)
			throws SettingsException, ConnectivityException
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	protected MessageListener createMessageCollector(ListenerProperties props, Map settings)
			throws SettingsException, ConnectivityException
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public Class getMessageCollectorClass()
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	protected Class getListenerClassEx(String type)
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void start() throws Exception
	{
		// TODO Auto-generated method stub
	}
	
	@Override
	public void stop() throws Exception
	{
		// TODO Auto-generated method stub
	}
	
	@Override
	public boolean restart() throws ConnectionException
	{
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public DummyConnection copy()
	{
		DummyConnection result = new DummyConnection();
		result.copy(this);
		return result;
	}
	
	@Override
	public void copy(DummyConnection copyFrom)
	{
		setName(copyFrom.getName());
		setSettings(copyFrom.getSettings());
	}
	
	@Override
	protected String initType()
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public String connectionFilePath()
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	protected CorrectSettings createSettings()
	{
		return new CorrectSettings();
	}
}