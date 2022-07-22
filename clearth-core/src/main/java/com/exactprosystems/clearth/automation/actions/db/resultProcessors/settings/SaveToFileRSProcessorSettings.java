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

package com.exactprosystems.clearth.automation.actions.db.resultProcessors.settings;

import java.io.File;

public class SaveToFileRSProcessorSettings extends ResultSetProcessorSettings
{
	protected boolean compressResult;
	protected File fileDir;
	protected String fileName;
	protected char delimiter;
	protected boolean useQuotes;
	protected boolean append;

	public SaveToFileRSProcessorSettings()
	{
	}

	public boolean isCompressResult()
	{
		return compressResult;
	}

	public void setCompressResult(boolean compressResult)
	{
		this.compressResult = compressResult;
	}

	public File getFileDir()
	{
		return fileDir;
	}

	public void setFileDir(File fileDir)
	{
		this.fileDir = fileDir;
	}

	public String getFileName()
	{
		return fileName;
	}

	public void setFileName(String fileName)
	{
		this.fileName = fileName;
	}

	public char getDelimiter()
	{
		return delimiter;
	}

	public void setDelimiter(char delimiter)
	{
		this.delimiter = delimiter;
	}

	public boolean isUseQuotes()
	{
		return useQuotes;
	}

	public void setUseQuotes(boolean useQuotes)
	{
		this.useQuotes = useQuotes;
	}

	public boolean isAppend()
	{
		return append;
	}

	public void setAppend(boolean append)
	{
		this.append = append;
	}
}
