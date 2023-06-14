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

package com.exactprosystems.clearth.connectivity.remotehand;

import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class DefaultRhScriptProcessor implements RhScriptProcessor
{
	private static final Logger logger = LoggerFactory.getLogger(DefaultRhScriptProcessor.class);
	
	private static final String ACTION_COLUMN_NAME = "#action";
	
	private final CSVFormat csvFormat = CSVFormat.DEFAULT.withIgnoreEmptyLines(false);
	
	@Override
	public String process(String script) throws IOException, RhException
	{
		return process(script, Collections.emptyMap());
	}
	
	@Override
	public String process(String script, Path templatesDir) throws IOException, RhException
	{
		return process(script, getTemplates(templatesDir));
	}
	
	@Override
	public String process(String script, Map<String, Path> templates) throws IOException, RhException
	{
		String[] scriptLines = script.split(RhUtils.LINE_SEPARATOR);
		List<String> newScriptLines = new ArrayList<>();
		String cachedHeader = null;
		for (int i = 0; i < scriptLines.length; i++)
		{
			String line = scriptLines[i];
			if (line.isEmpty() || line.startsWith("//") || line.startsWith("#include"))
			{
				newScriptLines.add(line);
			}
			else if (line.startsWith("#"))
			{
				newScriptLines.add(line);
				cachedHeader = line;
			}
			else
			{
				if (cachedHeader == null)
				{
					logger.warn("Line #{} contains action that does not have header, unable to process it", i + 1);
					newScriptLines.add(line);
				}
				else
				{
					processScriptAction(newScriptLines, line, cachedHeader, templates);
				}
			}
		}
		return String.join(RhUtils.LINE_SEPARATOR, newScriptLines);
	}
	
	private void processScriptAction(List<String> newScriptLines, String actionLine, String actionHeader,
			Map<String, Path> templates) throws IOException, RhException
	{
		CSVParser scriptParser = CSVParser.parse(actionHeader + "\n" + actionLine, csvFormat);
		List<CSVRecord> records = scriptParser.getRecords();
		if (records.size() != 2)
			throw new RhException("Error while parsing RemoteHand script: header-action pair must have only 2 lines, but it has "+records.size());
		
		Set<String> header = new LinkedHashSet<>(csvRecordToList(records.get(0)));
		List<String> values = csvRecordToList(records.get(1));
		
		String actionName = findActionName(header, values);
		if (actionName == null)
			throw new RhException("Line {} does not contain action name", actionLine);
		
		if (templates.containsKey(actionName))
		{
			removeHeaderIfLast(newScriptLines, actionHeader);
			newScriptLines.add(templateToScript(actionName, createArguments(header,  values), templates));
		}
		else
		{
			newScriptLines.add(actionLine);
		}
	}
	
	private String templateToScript(String templateName, Map<String, String> templateArguments,
			Map<String, Path> templates) throws IOException, RhException
	{
		StringBuilder templateScriptBuilder = new StringBuilder();
		RhUtils.loadScriptFromFile(templateScriptBuilder, templates.get(templateName), templateArguments);
		return process(templateScriptBuilder.toString(), templates);
	}
	
	private Map<String, Path> getFilesMap(Path dir, String... extensions) throws IllegalArgumentException, IOException
	{
		if (!Files.isDirectory(dir))
			throw new FileNotFoundException("Directory does not exist: " + dir);
		
		Map<String, Path> filesMap = new HashMap<>();
		try (Stream<Path> files = Files.list(dir).filter(f -> Files.isRegularFile(f)))
		{
			Iterator<Path> it = files.iterator();
			while (it.hasNext())
			{
				Path file = it.next();
				String name = file.getFileName().toString();
				for (String extension : extensions)
				{
					if (name.endsWith(extension))
					{
						filesMap.put(name.substring(0, name.length() - extension.length()), file);
						break;
					}
				}
			}
		}
		return filesMap;
	}

	private Map<String, Path> getTemplates(Path templatesDir) throws IllegalArgumentException, IOException
	{
		if (templatesDir == null)
			return Collections.emptyMap();
		
		Map<String, Path> templates = getFilesMap(templatesDir, ".csv");
		if (logger.isTraceEnabled())
			logger.trace("List of templates: {}", String.join(", ", templates.keySet()));
		return templates;
	}

	private void removeHeaderIfLast(List<String> newScriptLines, String cachedHeader)
	{
		int lastNewLineIndex = newScriptLines.size() - 1;
		if (newScriptLines.get(lastNewLineIndex).equals(cachedHeader))
			newScriptLines.remove(lastNewLineIndex);
	}
	
	private List<String> csvRecordToList(CSVRecord record)
	{
		return IteratorUtils.toList(record.iterator(), record.size());
	}
	
	private String findActionName(Collection<String> header, List<String> values)
	{
		int column = -1;
		for (String h : header)
		{
			column++;
			if (column >= values.size())
				return null;
			
			if (ACTION_COLUMN_NAME.equals(h))
				return values.get(column).trim();
		}
		return null;
	}
	
	protected Map<String, String> createArguments(Collection<String> header, List<String> values)
	{
		Map<String, String> arguments = new HashMap<>();
		int column = -1;
		for (String h : header)
		{
			column++;
			String value = column >= values.size() ? "" : values.get(column).trim();
			//Header starts with #, removing it from argument name
			arguments.put(h.trim().substring(1), value);
		}
		return arguments;
	}
}
