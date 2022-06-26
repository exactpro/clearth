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

package com.exactprosystems.clearth.generators;

import org.apache.commons.io.FilenameUtils;

import com.exactprosystems.clearth.ValueGenerator;
import com.exactprosystems.clearth.ValueGenerators;


@Deprecated
public class LegacyValueGenerators extends ValueGenerators
{
	
	public LegacyValueGenerators(String defaultId)
	{
		super(defaultId);
	}
	
	public LegacyValueGenerators()
	{
		super("lastgen.txt");
	}
	
	@Override
	protected ValueGenerator createGenerator(String id)
	{
		String newId = createNewId(id);
		return new LegacyValueGenerator(newId, "");
	}
	
	@Override
	protected ValueGenerator createCommonGenerator()
	{
		return new LegacyValueGenerator(defaultId, "");
	}
	
	private String createNewId(String id)
	{
		LegacyValueGenerator cg = (LegacyValueGenerator) getCommonGenerator();
		String oldFilename = cg.getLastGenFileName();
		
		String baseName = FilenameUtils.getBaseName(oldFilename);
		String extension = FilenameUtils.getExtension(oldFilename);
		
		return String.format("%s_%s.%s", baseName, id, extension);
	}
}
