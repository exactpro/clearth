/******************************************************************************
 * Copyright 2009-2019 Exactpro Systems Limited
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

package com.exactprosystems.clearth.tools.matrixmaker.initialdata;

import com.exactprosystems.clearth.tools.matrixmaker.GenerateException;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by victor.akimov on 2/1/17.
 */
public class CsvInitialData implements InitialData
{
	
	protected String formula;
	protected String name;
	protected String fileName;
	protected Iterator<CsvInitialRow> iterator = null;
	protected List<CsvInitialRow> tableCsv;
	protected CsvInitialRow currentValues = null;
	

	public CsvInitialData(String formula, String name, boolean randomScenarios, List<CsvInitialRow> tableCsv, String fileName) throws GenerateException
	{
		this.formula = formula;
		this.name = name;
		this.fileName = fileName;
		this.tableCsv = tableCsv;
		iterator = tableCsv.iterator();
		
	}
	
	public List<CsvInitialRow> getCsvTable()
	{
		return tableCsv;
	}

	@Override
	public String getField(String[] params) throws GenerateException
	{
		checkCurrentLine();
		if(params.length != 1)
			throw new GenerateException(name + " function must have 1 parameter");
		
		String val = currentValues.get(params[0]);
		if (val == null)
			throw new GenerateException("CsvFile "+name+" doesn't have a parameter: "+params[0]);
		return val;
	}
	
	public Map<String, String> getCurrentRow() throws GenerateException
	{
		checkCurrentLine();
		return currentValues.getRow();
	}

	@Override
	public void next()
	{
		currentValues = iterator.next();
	}

	@Override
	public boolean hasNext()
	{
		return iterator.hasNext();
	}
	
	protected void checkCurrentLine() throws GenerateException
	{
		if (currentValues == null)
		{
			if (hasNext())
				next();
			else
				throw new GenerateException(name + " does not have lines");
		}
	}

	@Override
	public String getFormula()
	{
		return formula;
	}
	
	
	public String getFileName()
	{
		return fileName;
	}


	public static class CsvInitialRow
	{
		int rowNumber;
		Map<String,String> row;
		
		public CsvInitialRow(int rowNumber, Map<String, String> row)
		{
			super();
			this.rowNumber = rowNumber;
			this.row = row;
		}
		public int getRowNumber()
		{
			return rowNumber;
		}
		public Map<String, String> getRow()
		{
			return row;
		}
		
		public String get(String key)
		{
			return row.get(key);
		}
		
		public void put(String key, String value)
		{
			row.put(key, value);
		}
		
		public boolean containsKey(String key)
		{
			return row.containsKey(key);
		}

		
	}
}
