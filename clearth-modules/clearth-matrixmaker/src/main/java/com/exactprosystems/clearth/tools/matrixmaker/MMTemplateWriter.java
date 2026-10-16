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

package com.exactprosystems.clearth.tools.matrixmaker;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import com.exactprosystems.clearth.utils.StringOperationUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.utils.FileOperationUtils;
import com.exactprosystems.clearth.utils.Utils;

public class MMTemplateWriter
{
	public static final Logger logger = LoggerFactory.getLogger(MMTemplateWriter.class);

	protected static final String CSV_EXTENSION = ".csv";
	protected static final String ADD_NAME_DELIMITER = "_";
	protected static final String LN_SEPARATOR = System.lineSeparator();
	protected static final String WRITER_DELIMITER = ",";
	protected File resultDir = null;
	protected Writer writer = null;
	protected File currentFile = null;
	protected String currentMatrixName = null;
	protected boolean emptyResultFile = true;
	protected boolean renameMatrices = false;
	protected String currentAltName = null;
	
	//protected int fileIndex = 0;
	protected Map<String,String> fileMapping = null;
	
	public MMTemplateWriter(String matrixName, boolean renameMatrices, File resultDir, 
			Map<String, String> fileMapping) throws IOException
	{
		this.renameMatrices = renameMatrices;
		this.currentMatrixName = matrixName;
		this.resultDir = resultDir;
		this.fileMapping = fileMapping;
		//this.fileIndex = fileMapping.size() + 1;
		init(matrixName);
	}
	
	public void init(String matrixName) throws IOException
	{
		startNewFile(matrixName, "");
	}
	
	protected File createNewFile(String matrixName, File resultDir, String matrixPatternName, String addName)
	{
		logger.debug("matrix name="+matrixName+" resultDir="+resultDir+" matrixPatternName="+matrixPatternName+" addName="+addName);
		File resultFile = null;
		if (!StringUtils.isEmpty(addName))
			addName = ADD_NAME_DELIMITER +addName;
		else 
			addName = "";
		String name = !StringUtils.isEmpty(matrixName) ? matrixName : matrixPatternName;
		name = name.replace(CSV_EXTENSION, "") + addName;
		resultFile = FileOperationUtils.uniqueFileName(resultDir, name,CSV_EXTENSION);
		
		
		if (renameMatrices)
		{
			//if (!emptyResultFile)
			//	fileIndex.increment();;
			//else
			//	fileMapping.remove(key)
			currentAltName = ((getCurrentIndex() + 1) + CSV_EXTENSION);
			fileMapping.put(currentAltName,resultFile.getName());
			resultFile = new File(resultDir,currentAltName);
			
		}
		
			currentFile = resultFile;
		
		
		return resultFile;
	}
	
	protected int getCurrentIndex()
	{
		return fileMapping.size();
	}
	
	protected Writer startNewFile(String fileName, String addName) throws IOException
	{
		close();
		File resultFile =  createNewFile( fileName,  resultDir,  currentMatrixName,  addName);
		
		writer = new BufferedWriter(new FileWriter(resultFile.getAbsolutePath()));
		return writer;
	}
	
	public void close()
	{
		if (writer == null)
			return;
		if (!emptyResultFile)
		{
			flush();
		}
		else
			removeEmptyFile();
		
		Utils.closeResource(writer);
		
		writer = null;
		currentFile = null;
		emptyResultFile = true;
	}
	
	protected void removeEmptyFile()
	{
		if (renameMatrices && currentAltName != null)
		{
			fileMapping.remove(currentAltName);
			currentAltName = null;
		}
		if (currentFile.exists())
			FileUtils.deleteQuietly(currentFile);
		
	}
	
	public void flush()
	{
		try
		{
			writer.flush();
		}
		catch(Exception e)
		{
			//ignore
		}
	}
	
	public void writeArrRow(String[] row) throws GenerateException
	{
		try
		{
			if (row == null || row.length == 0)
				writer.write(LN_SEPARATOR);
			else
			{
				writer.write(createRecordLine(row));
				writer.write(LN_SEPARATOR);
				writer.flush();
				emptyResultFile = false;
			}
		} catch (IOException e)
		{
			throw new GenerateException(String.format("Cannot write record: %s. ", row), e);
		}
	}
	
	public void writeRow(TemplateRow row) throws GenerateException
	{
		writeArrRow(row == null ? null : row.toArray());
	}

	public void writeRow(List<String> row) throws GenerateException
	{
		writeArrRow(row == null ? null : row.toArray(new String[0]));
	}
	
	protected String createRecordLine(String[] array)
	{
		StringJoiner joiner = new StringJoiner(WRITER_DELIMITER);
		for (String s : array)
			joiner.add(StringOperationUtils.quote(s));
		return joiner.toString();
	}

	public File getCurrentFile()
	{
		return currentFile;
	}
}
