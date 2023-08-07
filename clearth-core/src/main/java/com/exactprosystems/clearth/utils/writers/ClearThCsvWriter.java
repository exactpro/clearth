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

package com.exactprosystems.clearth.utils.writers;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

public class ClearThCsvWriter implements AutoCloseable
{
	private final static String SEPARATOR = System.lineSeparator();
	private final CSVPrinter csvPrinter;
	private CSVFormat format;

	public ClearThCsvWriter(String fileName) throws IOException
	{
		this(new FileWriter(fileName));
	}

	public ClearThCsvWriter(Writer writer) throws IOException
	{
		format = CSVFormat.DEFAULT.withRecordSeparator(SEPARATOR).withNullString("");
		csvPrinter = new CSVPrinter(writer, format);
	}

	public ClearThCsvWriter(String fileName, ClearThCsvWriterConfig config) throws IOException
	{
		this(new FileWriter(fileName), config);
	}

	public ClearThCsvWriter(Writer writer, ClearThCsvWriterConfig config) throws IOException
	{
		this.csvPrinter = createCsvWriter(writer, config);
	}

	protected CSVPrinter createCsvWriter(Writer writer, ClearThCsvWriterConfig config) throws IOException
	{
		format = createCsvFormat(config);
		return new CSVPrinter(writer, format);
	}

	public void write(String value) throws IOException
	{
		if (value == null)
			value = format.getNullString();
		csvPrinter.print(value);
	}

	public void endRecord() throws IOException
	{
		csvPrinter.println();
	}

	public void writeRecord(String[] rowLine) throws IOException
	{
		csvPrinter.printRecord(rowLine);
	}

	@Override
	public void close() throws IOException
	{
		csvPrinter.close();
	}

	protected CSVFormat createCsvFormat(ClearThCsvWriterConfig config)
	{
		CSVFormat csvFormat = CSVFormat.newFormat(config.getDelimiter())
				.withRecordSeparator(config.getLineSeparator())
				.withTrim(config.isWithTrim())
				.withNullString(config.getNullString());

		if (config.isUseTextQualifier())
			csvFormat = csvFormat.withQuote(config.getTextQualifier());

		return csvFormat;
	}
}