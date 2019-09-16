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

package com.exactprosystems.clearth.automation.actions.xls;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;

import com.exactprosystems.clearth.automation.MatrixContext;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.utils.multidata.MultiRowStringData;
import com.exactprosystems.clearth.utils.multidata.MultiStringData;

public class XlsUtils
{
	public static final String CONTEXT_NAME = "LoadedXlsData",
			CONTEXT_VERIFIEDROWS = "VerifiedXlsRows";
	
	private static final int CODE_A = 65, 
			CODE_Z = 90,
			ALPHABET_SIZE = 26;
	
	public static MultiRowStringData getStoredRowsData(MatrixContext matrixContext) throws ResultException
	{
		MultiRowStringData data = (MultiRowStringData)matrixContext.getContext(CONTEXT_NAME);
		if (data == null)
			throw ResultException.failed("No data stored, check if previous actions executed correctly");
		return data;
	}
	
	public static void storeRowsData(MultiRowStringData data, MatrixContext matrixContext)
	{
		matrixContext.setContext(CONTEXT_NAME, data);
	}
	
	
	public static Set<MultiStringData> getVerifiedRowsData(MatrixContext matrixContext)
	{
		return (Set<MultiStringData>)matrixContext.getContext(CONTEXT_VERIFIEDROWS);
	}
	
	public static void storeVerifiedRowsData(Set<MultiStringData> verifiedRows, MatrixContext matrixContext)
	{
		matrixContext.setContext(CONTEXT_VERIFIEDROWS, verifiedRows);
	}
	
	public static void storeVerifiedRowData(MultiStringData row, MatrixContext matrixContext)
	{
		Set<MultiStringData> verifiedRows = (Set<MultiStringData>)matrixContext.getContext(CONTEXT_VERIFIEDROWS);
		if (verifiedRows == null)
		{
			verifiedRows = new HashSet<MultiStringData>();
			matrixContext.setContext(CONTEXT_VERIFIEDROWS, verifiedRows);
		}
		
		verifiedRows.add(row);
	}
	
	
	public static int nameToCellIndex(String name) throws ResultException
	{
		name = name.toUpperCase();
		char[] chars = name.toCharArray();
		ArrayUtils.reverse(chars);
		
		int i = 0,
				result = 0;
		for (char c : chars)
		{
			i++;
			
			int code = (int)c;
			if ((code < CODE_A) || (code > CODE_Z))
				throw ResultException.failed("Invalid column name '"+name+"'. Only Excel-like (A..Z, AA..ZZ, etc.) column names are supported");
			
			result += (code-CODE_A)*i;
		}
		return result;
	}
	
	public static String cellIndexToName(int index)
	{
		List<Integer> numbers = new ArrayList<Integer>();
		do
		{
			int div = index/ALPHABET_SIZE,
					mod = index%ALPHABET_SIZE;
			numbers.add(mod);
			index = div-1;
		}
		while (index >= 0);
		
		String result = "";
		for (int n : numbers)
			result = (char)(n+CODE_A)+result;
		
		return result;
	}
}
