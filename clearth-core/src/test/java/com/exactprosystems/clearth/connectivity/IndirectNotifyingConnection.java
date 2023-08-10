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

package com.exactprosystems.clearth.connectivity;

import com.exactprosystems.clearth.connectivity.connections.BasicClearThMessageConnection;
import com.exactprosystems.clearth.connectivity.connections.SettingsClass;
import com.exactprosystems.clearth.utils.SettingsException;

@SettingsClass(IndirectNotifyingConnectionSettings.class)
public class IndirectNotifyingConnection extends BasicClearThMessageConnection
{
	@Override
	public IndirectNotifyingConnectionSettings getSettings()
	{
		return (IndirectNotifyingConnectionSettings)settings;
	}
	
	@Override
	protected ClearThClient createClient() throws ConnectivityException, SettingsException
	{
		return new IndirectNotifyingClient(this);
	}
	
	@Override
	public boolean isAutoConnect()
	{
		return false;
	}
}
