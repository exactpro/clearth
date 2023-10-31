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

package com.exactprosystems.clearth.connectivity.listeners;

import com.exactprosystems.clearth.connectivity.ConnectivityException;
import com.exactprosystems.clearth.connectivity.ListenerDescription;
import com.exactprosystems.clearth.connectivity.ListenerProperties;
import com.exactprosystems.clearth.connectivity.SettingsDetails;
import com.exactprosystems.clearth.connectivity.iface.AbstractMessageListener;
import com.exactprosystems.clearth.connectivity.iface.EncodedClearThMessage;

import java.io.IOException;

@ListenerDescription(description = "ClearTH dummy listener")
@SettingsDetails(details = "Details...")
public class DummyListener extends AbstractMessageListener
{
	public DummyListener(ListenerProperties properties)
	{
		super(properties);
	}

	@Override
	public void onMessage(EncodedClearThMessage message) throws IOException, IllegalArgumentException
	{

	}

	@Override
	public void start() throws ConnectivityException
	{

	}

	@Override
	public void dispose()
	{

	}
}
