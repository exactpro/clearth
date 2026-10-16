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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TemplateRow
{
	protected List<String> row;
	protected int rowNumber;
	protected String templateName;
	protected TemplateRow headerRow = null;
	
	public TemplateRow(String[] rowArray, String templateName, int rowNumber)
	{
		super();
		this.row = rowArray != null ? new ArrayList<String>(Arrays.asList(rowArray)) : new ArrayList<String>(0);
		this.rowNumber = rowNumber;
		this.templateName = templateName;
	}

	public TemplateRow(List<String> rowArray, String templateName, int rowNumber)
	{
		super();
		this.row = rowArray != null ? new ArrayList<String>(rowArray) : new ArrayList<String>(0);
		this.rowNumber = rowNumber;
		this.templateName = templateName;
	}

	public TemplateRow(TemplateRow src)
	{
		super();
		this.row = src.row == null ? new ArrayList<String>(0) : new ArrayList<String>(row);
		this.rowNumber = src.rowNumber;
		this.templateName = src.templateName;
	}
	
	public void setHeader(TemplateRow headerRow)
	{
		this.headerRow = headerRow;
	}
	
	public TemplateRow getHeader()
	{
		return headerRow;
	}

	public List<String> getRow()
	{
		return row;
	}
	public String[] toArray()
	{
		return row.toArray(new String[row.size()]);
	}
	public void setRow(String[] row)
	{
		this.row = new ArrayList<String>(Arrays.asList(row));
	}
	public void setRow(List<String> row)
	{
		this.row = row;
	}
	public int getRowNumber()
	{
		return rowNumber;
	}
	public void setRowNumber(int rowNumber)
	{
		this.rowNumber = rowNumber;
	}
	
	public String toString()
	{
		return Arrays.asList(row).toString();
	}
	
	public String get(int index)
	{
		return row.get(index);
	}
	
	public String get(String headerName)
	{
		if (!headerRow.getRow().contains(headerName))
			return null;
		int index = headerRow.getRow().indexOf(headerName);
		if (row.size() < index - 1)
			return null;
		return row.get(index);
	}
	
	public String getTemplateName()
	{
		return templateName;
	}
	public void setTemplateName(String templateName)
	{
		this.templateName = templateName;
	}
	
	public int size()
	{
		return row.size();
	}
	
	public void set(int index, String val)
	{
		row.set(index, val);
	}
	
	public boolean isEmpty()
	{
		return row.size() == 0;
	}
	
	public boolean isActionRow()
	{
		return (headerRow != null);
	}
}
