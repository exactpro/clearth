/******************************************************************************
 * Copyright 2009-2025 Exactpro Systems Limited
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

package com.exactprosystems.clearth.woodpecker.configuration.blacklist;

import com.exactprosystems.clearth.utils.csv.readers.ClearThCsvReader;
import com.exactprosystems.clearth.utils.csv.readers.ClearThCsvReaderConfig;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerBlackListException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static com.exactprosystems.clearth.utils.CollectionUtils.join;
import static com.exactprosystems.clearth.utils.inputparams.InputParamsUtils.YES;
import static com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerBlackListException.fromIOException;
import static com.exactprosystems.clearth.woodpecker.utils.WoodpeckerFileUtils.newBufferedReaderIgnoresBom;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.*;

public class BlackListLoader
{
	private static final String ENTITY_TYPE = "EntityType";
	private static final String ENTITY_ID = "EntityID";
	private static final String IS_REG_EX = "IsRegEx";
	
	private static final Set<String> HEADER = new LinkedHashSet<String>() 
	{{
		add(ENTITY_TYPE);
		add(ENTITY_ID);
		add(IS_REG_EX);
	}};
	
	
	public BlackList load(String name, Path path) throws WoodpeckerBlackListException
	{
		try (ClearThCsvReader reader = new ClearThCsvReader(newBufferedReaderIgnoresBom(path), createCsvReaderConfig()))
		{
			BlackList blackList = new BlackList(name);
			if (reader.hasHeader())
			{
				checkHeader(path, reader.getHeader());
				
				while (reader.hasNext())
				{
					processRecord(reader, blackList);
				}
			}
			return blackList;
		}
		catch (IOException e)
		{
			throw fromIOException(e, "Error while loading black list from file '%s'.", path);
		}
	}
	
	private void checkHeader(Path path, Set<String> headerSet) throws WoodpeckerBlackListException
	{
		if (!headerSet.containsAll(HEADER))
			throw new WoodpeckerBlackListException(format("Invalid header in black list '%s'. " +
							"Header should contain columns %s.", path, join(HEADER)));
	}
	
	private void processRecord(ClearThCsvReader reader, BlackList blackList) throws IOException, WoodpeckerBlackListException
	{
		String entityType = reader.get(ENTITY_TYPE);
		String entityId = reader.get(ENTITY_ID);
		
		if (isNotBlank(entityType) && isNotBlank(entityId))
		{
			if (isRegEx(reader.get(IS_REG_EX)))
				blackList.addIdPredicate(entityType, parseRegExPredicate(entityId, entityType));
			else 
				blackList.addId(entityType, entityId);
		}
	}
	
	private ClearThCsvReaderConfig createCsvReaderConfig()
	{
		ClearThCsvReaderConfig config = ClearThCsvReaderConfig.withFirstLineAsHeader();
		config.setIgnoreSurroundingSpaces(true);
		return config;
	}
	
	private boolean isRegEx(String flag)
	{
		return (flag != null) && YES.contains(trim(flag));
	}
	
	private Predicate<String> parseRegExPredicate(String s, String entityType) throws WoodpeckerBlackListException
	{
		try
		{
			return new Matches(Pattern.compile(s));
		}
		catch (PatternSyntaxException e)
		{
			throw new WoodpeckerBlackListException(e, "%s %s '%s' isn't valid regular expression.", 
					entityType, ENTITY_ID, s);
		}
	}
}
