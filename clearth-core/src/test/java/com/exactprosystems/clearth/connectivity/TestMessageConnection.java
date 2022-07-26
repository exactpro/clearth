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

package com.exactprosystems.clearth.connectivity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.connectivity.connections.BasicClearThMessageConnection;
import com.exactprosystems.clearth.utils.SettingsException;

/**
 * Minimal {@link BasicClearThMessageConnection} implementation to check its behavior 
 */
public class TestMessageConnection extends BasicClearThMessageConnection<TestMessageConnection, TestConnectionSettings>
{
	private static final Logger logger = LoggerFactory.getLogger(TestMessageConnection.class);
	
	@Override
	protected ClearThClient createClient() throws ConnectivityException, SettingsException
	{
		return new TestClearThClient(this);
	}
	
	@Override
	public Logger getLogger()
	{
		return logger;
	}
	
	@Override
	protected MessageListener createListenerEx(ListenerProperties props, String settings)
			throws SettingsException, ConnectivityException
	{
		return null;
	}
	
	@Override
	protected Class<?> getListenerClassEx(String type)
	{
		return null;
	}
	
	@Override
	public TestMessageConnection copy()
	{
		TestMessageConnection result = new TestMessageConnection();
		result.copy(this);
		return result;
	}
	
	@Override
	protected String initType()
	{
		return "Test";
	}
	
	@Override
	public String connectionFilePath()
	{
		return "testConnections";
	}
	
	@Override
	protected TestConnectionSettings createSettings()
	{
		return new TestConnectionSettings();
	}
}
