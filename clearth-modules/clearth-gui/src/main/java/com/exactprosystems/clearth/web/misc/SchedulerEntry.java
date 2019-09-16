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

package com.exactprosystems.clearth.web.misc;

public class SchedulerEntry
{
	private boolean isCommon;
	private String forUser;
	private String name;
	
	public SchedulerEntry()
	{
		this.isCommon = false;
		this.forUser = "";
		this.name = "";
	}
	
	public SchedulerEntry(boolean isCommon, String forUser, String name)
	{
		this.isCommon = isCommon;
		this.forUser = forUser;
		this.name = name;
	}

	public boolean isCommon()
	{
		return this.isCommon;
	}

	public void setCommon(boolean isCommon)
	{
		this.isCommon = isCommon;
	}

	public String getForUser()
	{
		return this.forUser;
	}

	public void setForUser(String forUser)
	{
		this.forUser = forUser;
	}

	public String getName()
	{
		return this.name;
	}

	public void setName(String name)
	{
		this.name = name;
	}	
}
