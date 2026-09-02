/******************************************************************************
 * Copyright 2009-2019 Exactpro Systems Limited
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

package com.exactprosystems.clearth.woodpecker.configuration.start;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.NONE)
public abstract class MessageSenderDesc
{
	@XmlAttribute(required = true)
	private String operationName;
	
	@XmlAttribute
	private boolean enabled = true;

	@XmlAttribute()
	private int part = 1;

	@XmlAttribute()
	private int threadsCount = 1;
	
	@XmlAttribute
	private int maxSeqErrorsCount = 10;
	
	
	public abstract String getSenderType();


	public String getOperationName()
	{
		return operationName;
	}

	public void setOperationName(String operationName)
	{
		this.operationName = operationName;
	}

	
	public boolean isEnabled()
	{
		return enabled;
	}

	public void setEnabled(boolean enabled)
	{
		this.enabled = enabled;
	}

	
	public int getPart()
	{
		return part;
	}

	public void setPart(int part)
	{
		this.part = part;
	}

	
	public int getThreadsCount()
	{
		return threadsCount;
	}

	public void setThreadsCount(int threadsCount)
	{
		this.threadsCount = threadsCount;
	}


	public int getMaxSeqErrorsCount()
	{
		return maxSeqErrorsCount;
	}

	public void setMaxSeqErrorsCount(int maxSeqErrorsCount)
	{
		this.maxSeqErrorsCount = maxSeqErrorsCount;
	}
}
