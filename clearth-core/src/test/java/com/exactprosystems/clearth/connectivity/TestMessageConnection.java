/******************************************************************************
 * Copyright 2009-2024 Exactpro Systems Limited
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

import com.exactprosystems.clearth.connectivity.connections.BasicClearThMessageConnection;
import com.exactprosystems.clearth.connectivity.connections.SettingsClass;
import com.exactprosystems.clearth.utils.SettingsException;
import com.exactprosystems.clearth.utils.javaFunction.FunctionWithException;

/**
 * Minimal {@link BasicClearThMessageConnection} implementation to check its behavior 
 */
@SettingsClass(TestConnectionSettings.class)
public class TestMessageConnection extends BasicClearThMessageConnection
{
	private final FunctionWithException<TestMessageConnection, ClearThClient, Exception> clientCreator;
	
	public TestMessageConnection(FunctionWithException<TestMessageConnection, ClearThClient, Exception> clientCreator)
	{
		this.clientCreator = clientCreator;
	}

	@Override
	protected ClearThClient createClient() throws ConnectivityException, SettingsException
	{
		try
		{
			return clientCreator.apply(this);
		}
		catch (ConnectivityException e)
		{
			throw e;
		}
		catch (SettingsException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new ConnectivityException(e);
		}
	}

	@Override
	public boolean isAutoConnect()
	{
		return false;
	}
}
