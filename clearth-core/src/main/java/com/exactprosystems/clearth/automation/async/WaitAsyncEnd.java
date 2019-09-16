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

package com.exactprosystems.clearth.automation.async;

public enum WaitAsyncEnd
{
	STEP("Step"),
	SCHEDULER("Scheduler"),
	NO("No");
	
	private final String label;
	
	WaitAsyncEnd(String label)
	{
		this.label = label;
	}
	
	
	public String getLabel()
	{
		return label;
	}


	public static WaitAsyncEnd byLabel(String label)
	{
		for (WaitAsyncEnd v : values())
		{
			if (v.getLabel().equals(label))
				return v;
		}
		return NO;
	}
}
