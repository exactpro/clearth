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

package com.exactprosystems.clearth.tools.sqlexecutor;

import com.exactprosystems.clearth.utils.NameValidator;
import com.exactprosystems.clearth.utils.SettingsException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class QueryOperator
{
	private static final String EXT = ".json";
	
	private final Path queriesPath;
	private final Class<QueryData> dataClass;
	private final ObjectReader reader;
	private final ObjectWriter writer;
	
	public QueryOperator(Path queriesPath, Class<QueryData> dataClass)
	{
		this.queriesPath = queriesPath;
		this.dataClass = dataClass;
		
		ObjectMapper mapper = new ObjectMapper();
		reader = createReader(mapper);
		writer = createWriter(mapper);
	}
	
	
	public List<String> getQueryNames(String connectionName) throws IOException
	{
		Path dir = getDirPath(connectionName);
		if (!Files.isDirectory(dir))
			return null;
		
		try (Stream<Path> files = Files.list(dir))
		{
			return files.filter(f -> Files.isRegularFile(f))
					.map(f -> FilenameUtils.getBaseName(f.getFileName().toString()))
					.collect(Collectors.toList());
		}
	}
	
	public QueryData loadQuery(String connectionName, String queryName) throws IOException
	{
		Path f = getFilePath(connectionName, queryName);
		return reader.readValue(f.toFile(), dataClass);
	}
	
	public void saveQuery(String connectionName, String queryName, QueryData data) throws IOException, SettingsException
	{
		NameValidator.validate(queryName);
		
		Path f = getFilePath(connectionName, queryName);
		Files.createDirectories(f.getParent());
		writer.writeValue(f.toFile(), data);
	}
	
	
	protected ObjectReader createReader(ObjectMapper mapper)
	{
		return mapper.reader();
	}
	
	protected ObjectWriter createWriter(ObjectMapper mapper)
	{
		return mapper.writer().withDefaultPrettyPrinter();
	}
	
	
	protected final Path getDirPath(String connectionName)
	{
		return queriesPath.resolve(connectionName);
	}
	
	protected final Path getFilePath(String connectionName, String queryName)
	{
		return getDirPath(connectionName).resolve(queryName+EXT);
	}
}
