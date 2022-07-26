/******************************************************************************
 * Copyright 2009-2021 Exactpro Systems Limited
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

import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.exactprosystems.clearth.utils.SettingsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.connectivity.connections.ClearThConnectionStorage;
import com.exactprosystems.clearth.connectivity.listeners.ClearThMessageCollector;

@XmlRootElement(name="defaultMQConnection")
@XmlAccessorType(XmlAccessType.NONE)
public class DefaultMQConnection extends MQConnection
{
	private static final Logger logger = LoggerFactory.getLogger(DefaultMQConnection.class);
	
	public DefaultMQConnection()
	{
		super();
	}
	
	@Override
	public Logger getLogger()
	{
		return logger;
	}
	
	
	@Override
	public MQConnection copy()
	{
		MQConnection con = new DefaultMQConnection();
		con.copy(this);
		return con;
	}
	
	@Override
	protected MQConnectionSettings createSettings()
	{
		return new DefaultMQConnectionSettings();
	}
	

	@Override
	protected BasicClearThClient createMqClient() throws ConnectionException, SettingsException
	{
		return new DefaultMQClient(this);
	}
	
	@Override
	protected MessageListener createListenerEx(ListenerProperties props, String settings) throws SettingsException, ConnectivityException
	{
		return null;
	}
	
	@Override
	protected MessageListener createMessageCollector(ListenerProperties props, Map<String, String> settings) 
			throws SettingsException, ConnectivityException
	{
		String messageEndIndicator = getMessageEndIndicator();
		String type = settings.get(ClearThMessageCollector.TYPE_SETTING);
		if (type == null)
			return new ClearThMessageCollector(props, name, settings, messageEndIndicator);
		return new ClearThMessageCollector(props, name, createCodec(type), settings, messageEndIndicator);
	}
	
	@Override
	public Class<?> getMessageCollectorClass()
	{
		return ClearThMessageCollector.class;
	}

	@Override
	protected Class<?> getListenerClassEx(String type)
	{
		return null;
	}
	
	@Override
	protected String initType()
	{
		return ClearThConnectionStorage.MQ;
	}
	
	
	protected String getMessageEndIndicator()
	{
		return ClearThMessageCollector.DEFAULT_MESSAGE_END_INDICATOR;
	}
}
