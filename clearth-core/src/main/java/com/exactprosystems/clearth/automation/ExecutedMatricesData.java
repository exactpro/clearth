/******************************************************************************
 * Copyright 2009-2020 Exactpro Systems Limited
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

package com.exactprosystems.clearth.automation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.utils.DateTimeUtils;
import com.exactprosystems.clearth.utils.Utils;

public class ExecutedMatricesData
{
	private static final Logger logger = LoggerFactory.getLogger(ExecutedMatricesData.class);

	public static final String EXECUTED_MATRICES_DIR = "matrices", EXECUTED_MATRICES_FILENAME = "executed_matrices.csv";

	private List<MatrixData> executedMatrices;
	private final Path executedMatricesDirPath, executedMatricesFilePath;


	public ExecutedMatricesData(String lastExecutedDataDir) throws IOException
	{
		executedMatricesDirPath = Paths.get(lastExecutedDataDir, EXECUTED_MATRICES_DIR);
		executedMatricesFilePath = Paths.get(lastExecutedDataDir, EXECUTED_MATRICES_FILENAME);
		executedMatrices = loadExecutedMatrices(executedMatricesFilePath, executedMatricesDirPath);
	}


	public void loadExecutedMatrices() throws IOException
	{
		executedMatrices = loadExecutedMatrices(executedMatricesFilePath, executedMatricesDirPath);
	}

	public List<MatrixData> loadExecutedMatrices(Path executedMatricesListPath, Path executedMatricesPath) throws IOException
	{
		List<MatrixData> executedMatrices = new ArrayList<>();
		
		File f = executedMatricesListPath.toFile();
		if (!f.isFile())
			return null;
		
		CsvReader reader = null;
		MatrixDataFactory matrixDataFactory = ClearThCore.getInstance().getMatrixDataFactory();
		try
		{
			reader = new CsvReader(executedMatricesListPath.toString());
			reader.setSafetySwitch(false);
			reader.readHeaders();
			while (reader.readRecord())
			{
				String fileName = reader.get(SchedulerData.MATRIX);
				if (fileName == null || fileName.isEmpty())
					continue;
				
				File matrixFile = executedMatricesPath.resolve(fileName).toFile();
				if (!matrixFile.exists())
				{
					logger.error(MessageFormat.format("Executed matrix '{0}' doesn't exist", matrixFile.getName()));
					continue;
				}
				
				String uploadDateNum = reader.get(SchedulerData.UPLOADED);
				Date uploadDate = DateTimeUtils.getDateFromTimestampOrNull(uploadDateNum);
				
				boolean isExecute = Boolean.parseBoolean(reader.get(SchedulerData.EXECUTE));
				String trimSpaces = reader.get(SchedulerData.TRIM_SPACES);
				boolean isTrim = trimSpaces == null || trimSpaces.isEmpty()
						|| Boolean.parseBoolean(trimSpaces);
				
				MatrixData matrixData = matrixDataFactory.createMatrixData(matrixFile, uploadDate, isExecute, isTrim);
				executedMatrices.add(matrixData);
			}
		}
		finally
		{
			Utils.closeResource(reader);
		}
		
		return executedMatrices;
	}

	public Path getExecutedMatricesDirPath()
	{
		return executedMatricesDirPath;
	}

	public void setExecutedMatrices(List<MatrixData> executedMatrices)
	{
		this.executedMatrices = executedMatrices;
		try
		{
			saveExecutedMatricesList(executedMatricesFilePath, this.executedMatrices);
		}
		catch (IOException e)
		{
			logger.error("Executed matrices cannot be stored", e);
		}
	}

	public List<MatrixData> getExecutedMatrices()
	{
		return executedMatrices;
	}


	private void saveExecutedMatricesList(Path fileName, List<MatrixData> matrices) throws IOException
	{
		CsvWriter writer = null;
		try
		{
			writer = new CsvWriter(fileName.toString());
			writeExecutedMatricesTableHeaders(writer);
			for (MatrixData matrixData : matrices)
			{
				writeExecutedMatrixData(writer, matrixData);
			}
			writer.flush();
		}
		finally
		{
			Utils.closeResource(writer);
		}
	}

	private void writeExecutedMatricesTableHeaders(CsvWriter writer) throws IOException
	{
		writer.write(SchedulerData.MATRIX);
		writer.write(SchedulerData.UPLOADED);
		writer.write(SchedulerData.EXECUTE);
		writer.write(SchedulerData.TRIM_SPACES);
		writer.endRecord();
	}

	private void writeExecutedMatrixData(CsvWriter writer, MatrixData md) throws IOException
	{
		writer.write(md.getFile().getName());
		writer.write(md.getUploadDate() == null ? "" : Long.toString(md.getUploadDate().getTime()));
		writer.write(Boolean.toString(md.isExecute()));
		writer.write(Boolean.toString(md.isTrim()));
		writer.endRecord();
	}
}