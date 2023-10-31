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

package com.exactprosystems.clearth.connectivity.listeners.factories;

import com.exactprosystems.clearth.connectivity.ListenerConfiguration;
import com.exactprosystems.clearth.connectivity.ListenerType;
import com.exactprosystems.clearth.connectivity.MessageListener;
import com.exactprosystems.clearth.connectivity.connections.ClearThConnection;
import com.exactprosystems.clearth.connectivity.connections.exceptions.ListenerException;
import com.exactprosystems.clearth.connectivity.listeners.DummyListener;
import com.exactprosystems.clearth.connectivity.listeners.FileListener;
import com.exactprosystems.clearth.utils.SettingsException;

import java.util.*;

public class CustomMessageListenerFactory extends BasicMessageListenerFactory
{
	private static final String DUMMY_TYPE = "Dummy";

	@Override
	public MessageListener createListenerEx(ClearThConnection connection, ListenerConfiguration configuration) throws ListenerException, SettingsException
	{
		String type = configuration.getType();

		if (DUMMY_TYPE.equals(type))
			return createDummyListener(configuration);

		return null;
	}

	protected DummyListener createDummyListener(ListenerConfiguration configuration)
	{
		return new DummyListener(createProperties(configuration));
	}

	@Override
	protected Map<String, Class<? extends MessageListener>> createTypesMap()
	{
		Map<String, Class<? extends MessageListener>> typesMap = new LinkedHashMap<>();
		typesMap.put(ListenerType.File.getLabel(), FileListener.class);
		typesMap.put(DUMMY_TYPE, DummyListener.class);
		return typesMap;
	}
}
