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

package com.exactprosystems.clearth.automation;

import java.util.HashSet;
import java.util.Set;

public class SpecialActionParams
{
	protected final Set<String> lowCaseNames;

	public SpecialActionParams(String... names)
	{
		this.lowCaseNames = new HashSet<>(names.length);
		for (String n : names)
			this.lowCaseNames.add(n.toLowerCase());
	}
	
	public SpecialActionParams(Set<String> names)
	{
		this.lowCaseNames = new HashSet<>(names.size());
		for (String n : names)
			this.lowCaseNames.add(n.toLowerCase());
	}
	
	
	public boolean isSpecialParam(String name)
	{
		return isSpecialParamLowCase(name.toLowerCase());
	}
	
	public boolean isSpecialParamLowCase(String lowCaseName)
	{
		return lowCaseNames.contains(lowCaseName);
	}
}
