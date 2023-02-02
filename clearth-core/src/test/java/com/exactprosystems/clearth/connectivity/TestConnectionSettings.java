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

import com.exactprosystems.clearth.connectivity.connections.ClearThConnectionSettings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TestConnectionSettings implements ClearThConnectionSettings
{
	private  BlockingQueue<String> source;
	private Collection<String> target;
	private boolean processReceived;
	
	public TestConnectionSettings()
	{
		source = new LinkedBlockingQueue<>();
		target = new ArrayList<>();
		processReceived = true;
	}
	
	public TestConnectionSettings(BlockingQueue<String> source, Collection<String> target, boolean processReceived)
	{
		this.source = source;
		this.target = target;
		this.processReceived = processReceived;
	}
	
	@Override
	public TestConnectionSettings copy()
	{
		return new TestConnectionSettings(source, target, processReceived);
	}

	@Override
	public void copyFrom(ClearThConnectionSettings settings1)
	{
		TestConnectionSettings settings = (TestConnectionSettings) settings1; 
		this.source = settings.source;
		this.target = settings.target;
		this.processReceived = settings.processReceived;
	}

	public BlockingQueue<String> getSource()
	{
		return source;
	}
	
	public Collection<String> getTarget()
	{
		return target;
	}
	
	
	public boolean isProcessReceived()
	{
		return processReceived;
	}
	
	public void setProcessReceived(boolean processReceived)
	{
		this.processReceived = processReceived;
	}
}