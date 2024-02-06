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

package com.exactprosystems.clearth.data.th2.config;

public class EventsConfig
{
	private String scope;
	private int maxBatchSize = 100;
	
	public EventsConfig()
	{
	}
	
	public EventsConfig(String scope, int maxBatchSize)
	{
		this.scope = scope;
		this.maxBatchSize = maxBatchSize;
	}
	
	@Override
	public String toString()
	{
		return "[scope = " + scope + 
				"; maxBatchSize = " + maxBatchSize + "]";
	}
	
	
	public String getScope()
	{
		return scope;
	}
	
	public void setScope(String scope)
	{
		this.scope = scope;
	}
	
	
	public int getMaxBatchSize()
	{
		return maxBatchSize;
	}
	
	public void setMaxBatchSize(int maxBatchSize)
	{
		this.maxBatchSize = maxBatchSize;
	}
}
