/*******************************************************************************
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

package com.exactprosystems.clearth.automation.report.comparisonwriters;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.report.ResultDetail;
import com.exactprosystems.clearth.automation.report.results.ComparisonResult;
import com.exactprosystems.clearth.automation.report.results.CsvDetailedResult;
import com.exactprosystems.clearth.automation.report.results.DetailedResult;
import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.utils.csv.writers.ClearThCsvWriter;
import com.exactprosystems.clearth.utils.csv.writers.ClearThCsvWriterConfig;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringJoiner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.exactprosystems.clearth.automation.report.Result.DETAILS_DIR;

/**
 * Implementation of ComparisonWriter that uses CsvWriter to write details.<br>
 * Uses temporary path {@link ClearThCore#tempPath()} by default (deleted during {@link #finishReport})<br>
 * Has an initial buffer: report will be made only when number of details (a.k.a. number of given row results) to store
 * exceeds {@link #maxBufferSize}.<br>
 */
public class CsvComparisonWriter implements ComparisonWriter<CsvDetailedResult>
{
	public static final String TEMP_FILE_PREFIX = "csvcontainerresult_report_";
	
	public static final String EXPECTED = "EXPECTED", ACTUAL = "ACTUAL", ZIP_EXT = ".zip", CSV_EXT = ".csv";
	
	public static final String COLUMN_COMPARISON_NAME = "Comparison name",
			COLUMN_COMPARISON_RESULT = "Comparison result",
			COLUMN_ROW_KIND = "Row kind",
			COLUMN_FAILED_COLUMNS = "Failed columns";

	private final File tempPath;
	private File tempFile;
	private ClearThCsvWriter csvWriter;
	protected boolean headerWritten = false;
	protected List<DetailedResult> noWriteBuffer = null;
	protected List<String> writeBufferHeaders = null;
	protected final int maxBufferSize;
	protected final boolean listFailedColumns;

	/**
	 * @param maxBufferSize report will be created in {@link #finishReport(Path, String, String, boolean)}
	 *                      only if number of details added through {@link #addDetail(CsvDetailedResult)} exceeds this 
	 *                      number or last param of method finishReport (forceWrite) is true
	 * @param listFailedColumns if true, columns with unexpected values in actual row will be enumerated in separate report column
	 * @param reportPath path to directory where to store written file
	 */
	public CsvComparisonWriter(int maxBufferSize, boolean listFailedColumns, File reportPath)
	{
		this.maxBufferSize = maxBufferSize;
		this.listFailedColumns = listFailedColumns;
		this.tempPath = reportPath;
		if (maxBufferSize > 0)
		{
			noWriteBuffer = new ArrayList<>();
			writeBufferHeaders = new ArrayList<>();
		}
	}
	
	protected ClearThCsvWriter getCsvWriter() throws IOException
	{
		if (csvWriter == null)
			csvWriter = createCsvWriter();
		
		return csvWriter;
	}
	
	protected File getTempFile() throws IOException
	{
		if (tempFile == null || !tempFile.exists())
			tempFile = createTempFile();
		
		return tempFile;
	}
	
	protected File getTempPath()
	{
		return tempPath;
	}
	
	protected File createTempFile() throws IOException
	{
		return File.createTempFile(TEMP_FILE_PREFIX, ZIP_EXT, getTempPath());
	}

	@Override
	public void addDetail(CsvDetailedResult containerResult) throws IOException
	{
		List<DetailedResult> mainContainerDetails = containerResult.getDetails();
		for (DetailedResult result : mainContainerDetails)
			addDetail(result, result.getComment());
	}

	public void addDetail(DetailedResult detailedResult, String header) throws IOException
	{
		if (noWriteBuffer == null)
		{
			writeDetail(detailedResult, header);
			return;
		}

		noWriteBuffer.add(detailedResult);
		writeBufferHeaders.add(header);
		if (noWriteBuffer.size() > maxBufferSize)
		{
			writeBuffer();
			noWriteBuffer = null;
		}
	}
	
	public void writeBuffer() throws IOException
	{
		Iterator<String> iterator = writeBufferHeaders.iterator();
		for (DetailedResult result : noWriteBuffer)
			writeDetail(result, iterator.next());
	}

	public void writeDetail(DetailedResult detailedResult, String header) throws IOException
	{
		ClearThCsvWriter csvWriter = getCsvWriter();
		if (!headerWritten)
		{
			writeHeader(csvWriter, detailedResult);
			headerWritten = true;
		}
		writeDetailRow(csvWriter, true, header, detailedResult);
		writeDetailRow(csvWriter, false, header, detailedResult);
	}

	public void writeDetailRow(ClearThCsvWriter csvWriter, boolean forExpected, String header, DetailedResult detailedResult)
			throws IOException
	{
		csvWriter.write(header);
		csvWriter.write(ComparisonResult.from(detailedResult).name());
		csvWriter.write(forExpected ? EXPECTED : ACTUAL);
		if (listFailedColumns)
		{
			if (forExpected)
				csvWriter.write("");
			else
				csvWriter.write(collectFailedColumns(detailedResult));
		}
		
		for (ResultDetail detail : detailedResult.getResultDetails())
			csvWriter.write(forExpected ? detail.getExpected() : detail.getActual());
		csvWriter.endRecord();
	}

	public void writeHeader(ClearThCsvWriter csvWriter, DetailedResult detailedResult) throws IOException
	{
		csvWriter.write(COLUMN_COMPARISON_NAME);
		csvWriter.write(COLUMN_COMPARISON_RESULT);
		csvWriter.write(COLUMN_ROW_KIND);
		if (listFailedColumns)
			csvWriter.write(COLUMN_FAILED_COLUMNS);
		
		for (ResultDetail rd : detailedResult.getResultDetails())
			csvWriter.write(rd.getParam());
		csvWriter.endRecord();
	}
	
	@Override
	public void close()
	{
		Utils.closeResource(csvWriter);
	}

	@Override
	public Path finishReport(Path reportDir, String fileNamePrefix, String fileNameSuffix, boolean forceWrite) throws IOException
	{
		if (forceWrite)
		{
			if (noWriteBuffer != null)
				writeBuffer();
			if (tempFile == null)
				tempFile = createTempFile();
		}
		
		Utils.closeResource(csvWriter);
		csvWriter = null;
		
		if (tempFile == null)
			return null;

		Path targetFilePath = createTargetFilePath(reportDir, fileNamePrefix, fileNameSuffix);
		Files.move(tempFile.toPath(), targetFilePath, StandardCopyOption.REPLACE_EXISTING);
		return targetFilePath;
	}

	protected Path createTargetFilePath(Path reportDirectory, String fileNamePrefix, String fileNameSuffix)
			throws IOException
	{
		Path detailsDirectory = reportDirectory.resolve(DETAILS_DIR);
		Files.createDirectories(detailsDirectory);
		return Files.createTempFile(detailsDirectory, fileNamePrefix, fileNameSuffix + ZIP_EXT);
	}

	protected ClearThCsvWriter createCsvWriter() throws IOException
	{
		ZipEntry entry = new ZipEntry(FilenameUtils.removeExtension(getTempFile().getName()) + CSV_EXT);
		ZipOutputStream zipFile = new ZipOutputStream(Files.newOutputStream(getTempFile().toPath()));
		zipFile.putNextEntry(entry);
		return new ClearThCsvWriter(new OutputStreamWriter(zipFile), createCsvWriterConfig());
	}

	protected ClearThCsvWriterConfig createCsvWriterConfig()
	{
		return new ClearThCsvWriterConfig();
	}

	protected String collectFailedColumns(DetailedResult result)
	{
		if (result.isSuccess())
			return "";
		
		StringJoiner joiner = new StringJoiner(",");
		for (ResultDetail rd : result.getResultDetails())
		{
			if (!rd.isIdentical())
				joiner.add(rd.getParam());
		}
		return joiner.toString();
	}
}