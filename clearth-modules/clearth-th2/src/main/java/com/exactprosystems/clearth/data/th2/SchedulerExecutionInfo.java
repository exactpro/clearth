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

package com.exactprosystems.clearth.data.th2;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.exactpro.th2.common.grpc.EventID;
import com.exactprosystems.clearth.automation.GlobalContext;

public class SchedulerExecutionInfo
{
	private final String name;
	private final Map<String, MatrixExecutionInfo> matrixInfos = new HashMap<>();
	private EventID eventId;
	private String startedByUser;
	private Instant startTimestamp,
			endTimestamp;
	
	public SchedulerExecutionInfo(String name)
	{
		this.name = name;
	}
	
	
	public void setFromGlobalContext(GlobalContext globalContext)
	{
		setStartedByUser(globalContext.getStartedByUser());
		setStartTimestamp(globalContext.getStarted().toInstant());
	}
	
	
	public String getName()
	{
		return name;
	}
	
	
	public Collection<MatrixExecutionInfo> getMatrixInfos()
	{
		return Collections.unmodifiableCollection(matrixInfos.values());
	}
	
	public MatrixExecutionInfo getMatrixInfo(String name)
	{
		return matrixInfos.get(name);
	}
	
	public void setMatrixInfo(String name, MatrixExecutionInfo info)
	{
		matrixInfos.put(name, info);
	}
	
	
	public EventID getEventId()
	{
		return eventId;
	}
	
	public void setEventId(EventID eventId)
	{
		this.eventId = eventId;
	}
	
	
	public String getStartedByUser()
	{
		return startedByUser;
	}
	
	public void setStartedByUser(String startedByUser)
	{
		this.startedByUser = startedByUser;
	}
	
	
	public Instant getStartTimestamp()
	{
		return startTimestamp;
	}
	
	public void setStartTimestamp(Instant startTimestamp)
	{
		this.startTimestamp = startTimestamp;
	}
	
	
	public Instant getEndTimestamp()
	{
		return endTimestamp;
	}
	
	public void setEndTimestamp(Instant endTimestamp)
	{
		this.endTimestamp = endTimestamp;
	}
}
