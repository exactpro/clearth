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

package com.exactprosystems.clearth.tools.matrixmaker.initialdata;

import java.util.List;

import com.exactprosystems.clearth.tools.matrixmaker.GenerateException;

public class MappingInitialData extends CsvInitialData
{

	String indexCol = null;
	
	public MappingInitialData(String formula, String name, List<CsvInitialRow> tableCsv, String fileName, String indexCol) throws GenerateException
	{
		super(formula, name, false, tableCsv, fileName);
		this.indexCol = indexCol;
	}


	@Override
	public boolean hasNext()
	{
		return false;
	}
	
	@Override
	public String getField(String[] params) throws GenerateException
	{
		if(params.length != 2)
			throw new GenerateException(name + " function must have 2 parameter");
		
		return findValue(params[0], params[1]);
	}


	protected String findValue(String initValue, String colName) throws GenerateException
	{
		String res = null;
		for (CsvInitialRow row : tableCsv)
		{
			if (row.get(indexCol).equals(initValue))
			{
				res = row.get(colName);
				break;
			}
		}
		if (res == null)
			throw new GenerateException("'"+colName + "' value for '"+indexCol+"'='"+initValue+"' is not found in mapping.");
		return res;
	}
	
}
