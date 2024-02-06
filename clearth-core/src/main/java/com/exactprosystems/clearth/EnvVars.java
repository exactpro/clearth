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

package com.exactprosystems.clearth;

import org.apache.commons.lang.StringUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class EnvVars
{
	private final Map<String, String> envVars;

	public EnvVars()
	{
		envVars = Collections.emptyMap();
	}

	public EnvVars(Path cfgFile) throws IOException
	{
		envVars = Collections.unmodifiableMap(fillEnvVars(cfgFile));
	}
	
	private Map<String, String> fillEnvVars(Path cfgFile) throws IOException
	{
		Map<String, String> map = new LinkedHashMap<>();
		try (BufferedReader reader = new BufferedReader(new FileReader(cfgFile.toFile())))
		{
			Iterator<String> lines = reader.lines().iterator();
			while (lines.hasNext())
			{
				String var = lines.next(),
						value = System.getenv(var);

				if (StringUtils.isEmpty(value))
					continue;
				map.put(var, value);
			}
		}
		return map;
	}

	public Set<String> getNames()
	{
		return envVars.keySet();
	}

	public Map<String, String> getMap()
	{
		return envVars;
	}
}
