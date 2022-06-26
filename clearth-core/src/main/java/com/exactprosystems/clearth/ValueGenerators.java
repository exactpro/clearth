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

package com.exactprosystems.clearth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class ValueGenerators
{
	protected final String defaultId;     // defaultId can be used in implementations, including createCommonGenerator()
	protected final ValueGenerator commonGenerator;
	protected final Map<String, ValueGenerator> generators = new ConcurrentHashMap<>();
	
	public ValueGenerators(String defaultId)
	{
		this.defaultId = defaultId;
		this.commonGenerator = createCommonGenerator();
	}
	
	public ValueGenerators()
	{
		this(null);
	}
	
	
	protected abstract ValueGenerator createGenerator(String id);
	
	
	public ValueGenerator getCommonGenerator()
	{
		return commonGenerator;
	}
	
	public ValueGenerator getGenerator(String id)
	{
		ValueGenerator result = generators.get(id);
		if (result != null)
			return result;
		
		synchronized (generators)
		{
			result = generators.get(id);
			if (result != null)
				return result;
			
			result = createGenerator(id);
			generators.put(id, result);
			return result;
		}
	}
	
	
	protected ValueGenerator createCommonGenerator()
	{
		return createGenerator(null);
	}
}
