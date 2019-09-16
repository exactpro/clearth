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

package com.exactprosystems.clearth.utils.tabledata.comparison;

import com.exactprosystems.clearth.utils.tabledata.BasicTableDataReader;
import com.exactprosystems.clearth.utils.tabledata.comparison.valuesComparators.StringValuesComparator;
import com.exactprosystems.clearth.utils.tabledata.comparison.valuesComparators.ValuesComparator;

import java.io.IOException;

/**
 * Line by line table data comparator which works with columns and row values represented as strings.
 */
public class StringTableDataComparator extends TableDataComparator<String, String>
{
	public StringTableDataComparator(BasicTableDataReader<String, String, ?> expectedReader, BasicTableDataReader<String, String, ?> actualReader,
			ValuesComparator<String, String> valuesComparator) throws IOException
	{
		super(expectedReader, actualReader, valuesComparator);
	}
	
	public StringTableDataComparator(BasicTableDataReader<String, String, ?> expectedReader, BasicTableDataReader<String, String, ?> actualReader) throws IOException
	{
		this(expectedReader, actualReader, new StringValuesComparator());
	}
}
