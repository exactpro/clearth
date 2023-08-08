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

package com.exactprosystems.clearth.utils.csv.readers;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.*;

public class ClearThCsvReader implements AutoCloseable
{
	protected final CSVParser parser;
	protected CSVFormat format;
	protected Iterator<CSVRecord> records;
	protected CSVRecord record;

	public ClearThCsvReader(String fileName) throws IOException
	{
		this(new FileReader(fileName), new ClearThCsvReaderConfig());
	}

	public ClearThCsvReader(Reader reader) throws IOException
	{
		this(reader, new ClearThCsvReaderConfig());
	}

	public ClearThCsvReader(String fileName, ClearThCsvReaderConfig config) throws IOException
	{
		this(new FileReader(fileName), config);
	}

	public ClearThCsvReader(Reader reader, ClearThCsvReaderConfig config) throws IOException
	{
		format = createCsvFormat(config);
		this.parser = new CSVParser(reader, format);
		this.records = parser.iterator();
	}

	public boolean hasNext() throws IOException
	{
		boolean hasNext = records.hasNext();
		record = hasNext ? next() : null;
		return hasNext;
	}

	protected CSVRecord next()
	{
		return records.next();
	}

	public String[] getValues() throws IOException
	{
		String[] values = new String[record.size()];
		int i = 0;
		for (String s: record)
			values[i++] = s;

		return values;
	}

	public String getRawRecord()
	{
		StringJoiner joiner = new StringJoiner(",");
		for (String s: record)
			joiner.add(s);

		return joiner.toString();
	}

	/**
	 * @return current record as Map<String, String>
	 * @throws IllegalStateException if header is not available for this reader according to its configuration.
	 * See {@link ClearThCsvReaderConfig}
	 */
	public Map<String, String> getRecord() throws IllegalStateException
	{
		Map<String, String> map = record.toMap();
		if (map.isEmpty())
			throw new IllegalStateException("Header is not available for this reader according to its configuration");

		return map;
	}

	public String get(String s) throws IOException
	{
		return record.get(s);
	}

	public Set<String> getHeader() throws IOException
	{
		return (parser.getHeaderMap() != null) ? parser.getHeaderMap().keySet() : null;
	}

	public boolean readHeader() throws IOException
	{
		 return parser.getHeaderMap() != null && !parser.getHeaderMap().isEmpty();
	}

	@Override
	public void close() throws IOException
	{
		parser.close();
	}

	protected CSVFormat createCsvFormat(ClearThCsvReaderConfig config)
	{
		CSVFormat csvFormat = CSVFormat.newFormat(config.getDelimiter())
				.withRecordSeparator(config.getLineSeparator())
				.withIgnoreEmptyLines(config.isSkipEmptyRecords())
				.withTrim(config.isWithTrim())
				.withIgnoreSurroundingSpaces(config.isIgnoreSurroundingSpaces());

		if (config.isFirstLineAsHeader())
			csvFormat = csvFormat.withFirstRecordAsHeader();

		if (config.isUseTextQualifier())
			csvFormat = csvFormat.withQuote(config.getTextQualifier());

		return csvFormat;
	}

}
