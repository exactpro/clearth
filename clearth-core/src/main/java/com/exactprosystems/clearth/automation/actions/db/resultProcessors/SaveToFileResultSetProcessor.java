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

package com.exactprosystems.clearth.automation.actions.db.resultProcessors;

import com.exactprosystems.clearth.automation.actions.db.checkers.RecordChecker;
import com.exactprosystems.clearth.automation.actions.db.resultProcessors.settings.SaveToFileRSProcessorSettings;
import com.exactprosystems.clearth.utils.FileOperationUtils;
import com.exactprosystems.clearth.utils.csv.ClearThQuoteMode;
import com.exactprosystems.clearth.utils.csv.writers.ClearThCsvWriter;
import com.exactprosystems.clearth.utils.csv.writers.ClearThCsvWriterConfig;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Map;

import static com.exactprosystems.clearth.automation.actions.db.SQLAction.OUT_QUERY_RESULT_PATH;

public class SaveToFileResultSetProcessor extends ResultSetProcessor
{
	protected final boolean compressResult;
	protected final ClearThCsvWriter csvWriter;
	protected final File outputFile;
	protected final File fileToWrite;
	protected final RecordChecker recordChecker;

	public SaveToFileResultSetProcessor(SaveToFileRSProcessorSettings settings) throws IOException
	{
		super(settings);
		this.recordChecker = Validate.notNull(settings.getRecordChecker(), "Record checker can't be null");
		this.fileToWrite = createFileToWrite(Validate.notNull(settings.getFileDir(), "File dir can't be null"), settings.getFileName());
		this.compressResult = settings.isCompressResult();
		this.outputFile = compressResult ? new File(fileToWrite + ".zip") : fileToWrite;
		this.csvWriter = createCsvWriter(fileToWrite, settings);
		outputParams.put(OUT_QUERY_RESULT_PATH, outputFile.toString());
	}

	protected ClearThCsvWriter createCsvWriter(File outputFile, SaveToFileRSProcessorSettings settings) throws IOException
	{
		return new ClearThCsvWriter(new FileWriter(outputFile, settings.isAppend()), createCsvWriterConfig(settings));
	}

	protected ClearThCsvWriterConfig createCsvWriterConfig(SaveToFileRSProcessorSettings settings)
	{
		ClearThCsvWriterConfig config = new ClearThCsvWriterConfig();
		config.setDelimiter(settings.getDelimiter());
		config.setUseTextQualifier(settings.isUseQuotes());
		if (settings.isUseQuotes())
			config.setQuoteMode(ClearThQuoteMode.ALL);
		return config;
	}

	protected File createFileToWrite(File directory, String fileName) throws IOException
	{
		Files.createDirectories(directory.toPath());
		return StringUtils.isEmpty(fileName) ?
				File.createTempFile(getClass().getSimpleName() + "_export_", ".csv", directory) : new File(directory, fileName);
	}

	@Override
	protected void processRecord(Map<String, String> record) throws IOException
	{
		processValues(record.values());
	}

	@Override
	protected void processValues(Collection<String> values) throws IOException
	{
		csvWriter.writeRecord(values.toArray(new String[0]));
	}

	protected void compressResultFile(File resultFile) throws IOException
	{
		FileOperationUtils.zipFiles(outputFile, new File[]{resultFile});
		FileUtils.deleteQuietly(resultFile);
	}

	protected void saveOutputFile() throws IOException
	{
		if (compressResult)
			compressResultFile(fileToWrite);
	}

	@Override
	public void close() throws IOException
	{
		csvWriter.close();
		saveOutputFile();
		result.appendComment(String.format("Written file: '%s'. File size is %s", outputFile, FileUtils.byteCountToDisplaySize(outputFile.length())));
	}
}
