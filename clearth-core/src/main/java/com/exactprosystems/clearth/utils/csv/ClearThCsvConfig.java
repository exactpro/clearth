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

package com.exactprosystems.clearth.utils.csv;

public class ClearThCsvConfig
{
	private char delimiter = ',';
	private boolean useTextQualifier = true;
	private char textQualifier = '"';
	private String lineSeparator = System.lineSeparator();
	private boolean withTrim = false;

	public char getDelimiter()
	{
		return delimiter;
	}

	public void setDelimiter(char delimiter)
	{
		this.delimiter = delimiter;
	}

	public boolean isUseTextQualifier()
	{
		return useTextQualifier;
	}

	public void setUseTextQualifier(boolean useTextQualifier)
	{
		this.useTextQualifier = useTextQualifier;
	}

	public boolean isWithTrim()
	{
		return withTrim;
	}

	public void setWithTrim(boolean withTrim)
	{
		this.withTrim = withTrim;
	}

	public char getTextQualifier()
	{
		return textQualifier;
	}

	public void setTextQualifier(char textQualifier)
	{
		this.textQualifier = textQualifier;
	}

	public String getLineSeparator()
	{
		return lineSeparator;
	}

	public void setLineSeparator(String lineSeparator)
	{
		this.lineSeparator = lineSeparator;
	}
}

