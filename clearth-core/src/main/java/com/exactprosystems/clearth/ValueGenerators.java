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

package com.exactprosystems.clearth;

import java.util.HashMap;
import java.util.Map;

/**
 *         14 November 2017
 */
public class ValueGenerators
{
	protected final ValueGenerator commonGenerator;
	protected final Map<String, ValueGenerator> generators = new HashMap<String, ValueGenerator>();
	
	
	public ValueGenerators()
	{
		this.commonGenerator = createCommonGenerator();
	}


	public ValueGenerator getCommonGenerator()
	{
		return commonGenerator;
	}
	
	public ValueGenerator getGenerator(String fileNameForLastGen, String generatorPrefix)
	{
		ValueGenerator generator = generators.get(fileNameForLastGen);
		if (generator == null)
		{
			synchronized (generators)
			{
				generator = generators.get(fileNameForLastGen);
				if (generator == null)
				{
					generator = createGenerator(fileNameForLastGen, generatorPrefix);
					generators.put(fileNameForLastGen, generator);
				}
			}
		}
		return generator;
	}

	
	protected ValueGenerator createGenerator(String fileNameForLastGen, String generatorPrefix)
	{
		return new ValueGenerator(fileNameForLastGen, generatorPrefix);
	}
	
	protected ValueGenerator createCommonGenerator()
	{
		return createGenerator("lastgen.txt", "");
	}
}
