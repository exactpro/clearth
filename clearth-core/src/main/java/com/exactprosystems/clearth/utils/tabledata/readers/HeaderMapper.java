/*******************************************************************************
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

package com.exactprosystems.clearth.utils.tabledata.readers;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class HeaderMapper<A>
{
	protected final Map<A, A> conversionMap;
	
	public HeaderMapper(Map<A, A> conversionMap)
	{
		this.conversionMap = new HashMap<>(conversionMap);
	}
	
	public Set<A> convert(Set<A> header)
	{
		Set<A> result = new LinkedHashSet<>();
		header.forEach(h -> result.add(conversionMap.getOrDefault(h, h)));
		return result;
	}
}