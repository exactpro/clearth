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

package com.exactprosystems.clearth.woodpecker.daemons;

import com.exactprosystems.clearth.woodpecker.configuration.daemonsDictionary.DaemonDesc;
import com.exactprosystems.clearth.woodpecker.configuration.settings.DaemonSettings;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerException;
import com.exactprosystems.clearth.woodpecker.statistics.LoadingStatistics;

import java.time.LocalDateTime;
import java.util.UUID;

public class TestingMockDaemon implements WoodpeckerDaemon {

	protected DaemonStatus status;
	protected WoodpeckerDaemonFactory factory;
	protected DaemonSettings settings;
	protected String runId;

	TestingMockDaemon(WoodpeckerDaemonFactory daemonFactory) 
	{
		factory = daemonFactory;
		settings = new DaemonSettings("type", "name");
		status = DaemonStatus.INACTIVE;
		runId = UUID.randomUUID().toString();
	}

	public String start() throws WoodpeckerException 
	{
		status = DaemonStatus.RUNNING;
		return runId;
	}
	
	public void stop() throws WoodpeckerException 
	{
		if (status != DaemonStatus.RUNNING) 
			throw new WoodpeckerException("Only running daemon can be stopped");
		status = DaemonStatus.INACTIVE;
	}
	
	public DaemonStatus getStatus()
	{
		return status;
	}
	
	public String getRunId()
	{
		return runId;
	}
	
	public LocalDateTime getStartTime() 
	{
		return null;
	}
	
	public LocalDateTime getEndTime()
	{
		return null;
	}
	
	public DaemonSettings getSettings()
	{
		return settings;
	}

	public DaemonDesc getDescription()
	{
		return null;
	}

	public LoadingStatistics getLoadingStatistics()
	{
		return null;
	}
	
	public WoodpeckerDaemonFactory getFactory() {
		return factory;
	}
}