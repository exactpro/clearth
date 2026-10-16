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

package com.exactprosystems.clearth.web.api.helpers;

import com.exactprosystems.clearth.connectivity.ListenerConfiguration;
import com.exactprosystems.clearth.connectivity.connections.ClearThConnection;
import com.exactprosystems.clearth.connectivity.connections.ClearThConnectionSettings;
import com.exactprosystems.clearth.connectivity.connections.ClearThMessageConnection;
import com.thoughtworks.xstream.annotations.XStreamAlias;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@XmlRootElement(name="cthConnection")
@XStreamAlias("cthConnection")
public class ClearTHConnectionSettingsDataResource
{
	@XmlElement
	private String type;
	@XmlElement
	private String name;

	@XmlElement
	private final ClearThConnectionSettings connectionSettings;

	@XmlElement
	private final boolean autoConnect;
	@XmlElement
	private final LocalDateTime started;
	@XmlElement
	private final LocalDateTime stopped;
	@XmlElement
	private final boolean running;

	@XmlElementWrapper
	protected List<ClearTHListenerDataResource> listeners;
	
	public ClearTHConnectionSettingsDataResource(ClearThConnection connection)
	{
		this.name = connection.getName();
		this.type = connection.getTypeInfo().getName();
		
		connectionSettings = connection.getSettings();
		
		if (connection instanceof ClearThMessageConnection)
		{
			ClearThMessageConnection msgCon = (ClearThMessageConnection)connection;
			this.autoConnect = msgCon.isAutoConnect();
			this.started = msgCon.getStarted();
			this.stopped = msgCon.getStopped();
			this.running = msgCon.isRunning();
			
			List<ClearTHListenerDataResource> listeners = new ArrayList<ClearTHListenerDataResource>();
			for (ListenerConfiguration configuration : msgCon.getListeners())
				listeners.add(new ClearTHListenerDataResource(configuration));
			
			this.listeners = listeners;
		}
		else
		{
			this.autoConnect = false;
			this.started = null;
			this.stopped = null;
			this.running = false;
			this.listeners = Collections.emptyList();
		}
	}

	public boolean isAutoConnect() {
		return autoConnect;
	}

	public LocalDateTime getStarted() {
		return started;
	}

	public LocalDateTime getStopped() {
		return stopped;
	}

	public boolean isRunning() {
		return running;
	}

	public ClearThConnectionSettings getConnectionSettings() {
		return connectionSettings;
	}

	public String getType()
	{
		return type;
	}

	public void setType(String type)
	{
		this.type = type;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public List<ClearTHListenerDataResource> getListeners()
	{
		return listeners;
	}

	public void setListeners(List<ClearTHListenerDataResource> listeners)
	{
		this.listeners = listeners;
	}
}
