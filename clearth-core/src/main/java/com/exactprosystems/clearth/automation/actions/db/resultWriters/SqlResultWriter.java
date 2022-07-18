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

package com.exactprosystems.clearth.automation.actions.db.resultWriters;

import com.csvreader.CsvWriter;
import com.exactprosystems.clearth.utils.FileOperationUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class SqlResultWriter implements AutoCloseable
{
	private final boolean compressResult;
	private final CsvWriter csvWriter;
	private final File outputFile;
	private final File fileToWrite;

	public SqlResultWriter(File directory, String fileName, boolean compressResult, boolean append, char delimiter, boolean useQuotes) throws IOException
	{
		this.fileToWrite = createFileToWrite(directory, fileName);
		this.compressResult = compressResult;
		this.outputFile = compressResult ? new File(fileToWrite + ".zip") : fileToWrite;
		this.csvWriter = createCsvWriter(fileToWrite, delimiter, append, useQuotes);
	}

	protected CsvWriter createCsvWriter(File outputFile, char delimiter, boolean append, boolean useQuotes) throws IOException
	{
		CsvWriter writer = new CsvWriter(new FileWriter(outputFile, append), delimiter);
		writer.setForceQualifier(useQuotes);
		return writer;
	}

	public void writeHeader(List<String> columns) throws IOException
	{
		writeRecord(columns);
	}

	public void writeRecord(List<String> values) throws IOException
	{
		csvWriter.writeRecord(values.toArray(new String[0]));
	}

	protected File createFileToWrite(File directory, String fileName) throws IOException
	{
		Files.createDirectories(directory.toPath());
		return StringUtils.isEmpty(fileName) ?
				File.createTempFile(getClass().getSimpleName() + "_export_", ".csv", directory) : new File(directory, fileName);
	}

	protected void saveOutputFile(File fileToWrite) throws IOException
	{
		if (compressResult)
			compressResultFile(fileToWrite);
	}

	protected void compressResultFile(File resultFile) throws IOException
	{
		FileOperationUtils.zipFiles(outputFile, new File[]{resultFile});
		FileUtils.deleteQuietly(resultFile);
	}

	@Override
	public void close() throws IOException
	{
		csvWriter.close();
		saveOutputFile(fileToWrite);
	}

	public File getOutputFile()
	{
		return outputFile;
	}
}
