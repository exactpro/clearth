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

package com.exactprosystems.clearth.woodpecker.message.generator.dictionary;

import com.exactprosystems.clearth.utils.KeyValueUtils;
import com.exactprosystems.clearth.utils.Pair;
import com.exactprosystems.clearth.utils.csv.readers.ClearThCsvReader;
import com.exactprosystems.clearth.utils.csv.readers.ClearThCsvReaderConfig;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerConfigException;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerException;
import com.exactprosystems.clearth.woodpecker.expressions.mvel.MvelExpressionCompiler;
import org.apache.commons.lang3.mutable.MutableInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.SourceVersion;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;

import static com.exactprosystems.clearth.connectivity.iface.ClearThMessage.MSGTYPE;
import static com.exactprosystems.clearth.connectivity.iface.ClearThMessage.SUBMSGTYPE;
import static com.exactprosystems.clearth.utils.StringOperationUtils.parseIntegerRange;
import static com.exactprosystems.clearth.utils.CollectionUtils.join;
import static com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerConfigException.whenLoadingFailed;
import static com.exactprosystems.clearth.woodpecker.expressions.mvel.MvelExpressionCompiler.containsExpression;
import static com.exactprosystems.clearth.woodpecker.utils.WoodpeckerFileUtils.newBufferedReaderIgnoresBom;
import static java.lang.String.format;
import static java.util.Collections.singleton;
import static org.apache.commons.lang3.StringUtils.*;

public class MessageDescriptionLoader
{
	private static final Logger log = LoggerFactory.getLogger(MessageDescriptionLoader.class);

	private static final String COMMENT_START = "//";
	
	private static final Pattern RG_START_PATTERN = Pattern.compile("^#+RgStart");
	private static final Pattern RG_END_PATTERN = Pattern.compile("^#+RgEnd");
	
	private static final String COUNT_RG_SETTING = "count";
	private static final Collection<String> RG_SETTINGS_NAMES = singleton(COUNT_RG_SETTING);
	
	private final MvelExpressionCompiler mvelExpressionCompiler = new MvelExpressionCompiler();
	
	
	public MessageDescription load(Path path) throws WoodpeckerException
	{
		log.info("Loading message template from '{}'...", path);
		
		try (BufferedReader br = newBufferedReaderIgnoresBom(path))
		{
			return parseMessageDescription(path, new ClearThCsvReader(br, createCsvReaderConfig()), new MutableInt());
		}
		catch (IOException | RuntimeException e)
		{
			throw whenLoadingFailed(path, "message template", e);
		}
	}

	private MessageDescription parseMessageDescription(Path sourceFilePath,
	                                                   ClearThCsvReader reader,
	                                                   MutableInt rowCounter) throws WoodpeckerException, IOException
	{
		return parseMessageDescription(sourceFilePath, reader, rowCounter, false, 0);
	}
	
	private MessageDescription parseMessageDescription(Path sourceFilePath,
	                                                   ClearThCsvReader reader,
	                                                   MutableInt rowCounter,
	                                                   boolean isRg,
	                                                   int rgStartRow) throws WoodpeckerException, IOException
	{
		MessageDescription md = isRg ? new RgDescription(sourceFilePath) : new MessageDescription(sourceFilePath);
		while (reader.hasNext())
		{
			String[] values = reader.getValues();
			rowCounter.increment();
			String name = values[0];
			
			if (RG_START_PATTERN.matcher(name).find())
			{
				int rowNumber = rowCounter.intValue();
				RgDescription rgDesc = (RgDescription) parseMessageDescription(sourceFilePath,
						reader,
						rowCounter, 
						true, 
						rowNumber);
				parseRgSettings(name, rgDesc, sourceFilePath, rowNumber);
				md.addRepeatingGroupDesc(rgDesc);
			}
			
			if (isRg && RG_END_PATTERN.matcher(name).find())
				break;
			
			parseMessageField(name, values[1], md, isRg, rowCounter);
		}
		
		if (md.getMessageType() == null)
			throw whenMessageTypeMissed(sourceFilePath, isRg, rgStartRow);
			
		return md;
	}
	
	private ClearThCsvReaderConfig createCsvReaderConfig() throws IOException
	{
		ClearThCsvReaderConfig config = new ClearThCsvReaderConfig();
		config.setDelimiter(',');
		config.setIgnoreSurroundingSpaces(true);
		return config;
	}
	
	private void parseMessageField(String name, 
	                               String value, 
	                               MessageDescription md, 
	                               boolean isRg,
	                               MutableInt rowCounter) throws WoodpeckerException
	{
		if (startsWith(name, COMMENT_START))
			return;
		else if (isBlank(name))
		{
			if (isNotBlank(value))
				log.warn("Missed parameter's name at line #{}.", rowCounter);
			return;
		}

		if (isBlank(value))
		{
			log.warn("Missed parameter's value at line #{}.", rowCounter);
			return;
		}

		if ((isRg && SUBMSGTYPE.equals(name)) || (!isRg && MSGTYPE.equals(name)))
			md.setMessageType(value);

		md.addField(createFieldDescription(name, value));
	}
	
	
	private void parseRgSettings(String rgLine, RgDescription rgDescription, 
	                             Path sourceFilePath, int rowNumber) throws WoodpeckerException
	{
		int lpIndex = rgLine.indexOf('(');
		int rpIndex = rgLine.indexOf(")", lpIndex + 1);
		if ((lpIndex == -1) || (rpIndex == -1))
			return;
		
		String settingsLine = rgLine.substring(lpIndex + 1, rpIndex);
		Map<String, String> settings = KeyValueUtils.parseKeyValueString(settingsLine, ",", true);
		
		String countLine = settings.remove(COUNT_RG_SETTING);
		if (countLine != null)
			parseRgCountSettings(countLine, rgDescription, sourceFilePath, rowNumber);
		
		if (!settings.isEmpty())
			throw whenExtraSettingsFound(sourceFilePath, settings.keySet(), rowNumber);
	}
	
	private void parseRgCountSettings(String countLine, RgDescription rgDescription, 
	                                  Path sourceFilePath, int rowNumber) throws WoodpeckerException
	{
		countLine = trim(countLine);
		
		if (isVariableName(countLine))
		{
			rgDescription.setCountVarName(countLine);
			return;
		}

		Pair<Integer, Integer> countRange = parseIntegerRange(countLine);
		if (countRange != null)
		{
			rgDescription.setMinCount(countRange.getFirst());
			rgDescription.setMaxCount(countRange.getSecond());
			return;
		}
		
		throw new WoodpeckerConfigException("Error in '%s' at line #%d: RG '%s' setting has invalid value '%s'. " +
				"It must be simple number, number range or variable name.",
				sourceFilePath, rowNumber, COUNT_RG_SETTING, countLine);
	}
	
	private boolean isVariableName(String string)
	{
		return SourceVersion.isName(string);
	}
	
	
	private FieldDescription createFieldDescription(String name, String value)
	{
		if (containsExpression(value))
			return new FieldDescription(name, value, mvelExpressionCompiler.compile(value));
		else 
			return new FieldDescription(name, value);
	}
	
	
	private WoodpeckerConfigException whenMessageTypeMissed(Path sourceFilePath, boolean isRg, int rgStartRow)
	{
		String message = isRg
				? format("Error in '%s': '%s' isn't specified for repeating group started at line #%d.", 
					sourceFilePath, SUBMSGTYPE, rgStartRow)
				: format("Error in '%s': '%s' isn't specified.", 
					sourceFilePath, MSGTYPE);
		
		return new WoodpeckerConfigException(message);
	}
	
	private WoodpeckerConfigException whenExtraSettingsFound(Path sourceFilePath, 
	                                                         Collection<String> extraSettingsNames,
	                                                         int rowNum)
	{
		return new WoodpeckerConfigException(format("Error in '%s'. " +
				"The following unknown setting(s) found for RG at line #%d: %s. " +
				"Possible settings: %s.", 
				sourceFilePath, rowNum, join(extraSettingsNames), RG_SETTINGS_NAMES));
	}
}
