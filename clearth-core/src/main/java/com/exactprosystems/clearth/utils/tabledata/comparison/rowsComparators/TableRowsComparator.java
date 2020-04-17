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

package com.exactprosystems.clearth.utils.tabledata.comparison.rowsComparators;

import com.exactprosystems.clearth.utils.ExceptionUtils;
import com.exactprosystems.clearth.utils.tabledata.TableHeader;
import com.exactprosystems.clearth.utils.tabledata.TableRow;
import com.exactprosystems.clearth.utils.tabledata.comparison.RowComparisonData;

import java.util.Objects;

/**
 * Class which compares two table data rows.
 * @param <A> class of column member.
 * @param <B> class of table values.
 */
public class TableRowsComparator<A, B>
{
	/**
	 * Compares two specified {@link TableRow} objects.
	 * @param row1 first object of comparison, usually marked as 'expected'.
	 * @param row2 second object of comparison, usually marked as 'actual'.
	 * @param commonHeader set of all column names for {@code row1} and {@code row2}.
	 * @return {@link RowComparisonData} object containing all details about rows' comparison.
	 */
	public RowComparisonData<A, B> compareRows(TableRow<A, B> row1, TableRow<A, B> row2, TableHeader<A> commonHeader)
	{
		RowComparisonData<A, B> compData = new RowComparisonData<>();
		if (checkRows(row1, row2, commonHeader, compData))
		{
			for (A column : commonHeader)
			{
				if (checkCurrentColumn(column, row1, row2, compData))
					processValuesComparison(column, row1.getValue(column), row2.getValue(column), compData);
			}
		}
		return compData;
	}
	
	
	protected boolean checkRows(TableRow<A, B> row1, TableRow<A, B> row2, TableHeader<A> commonHeader, RowComparisonData<A, B> compData)
	{
		if (row1 == null && row2 == null)
			throw new IllegalArgumentException("Both table row objects are null. Could not make comparison.");
		
		if (row1 == null || row2 == null)
		{
			for (A column : commonHeader)
			{
				compData.addComparisonDetail(column, row1 != null ? row1.getValue(column) : null,
						row2 != null ? row2.getValue(column) : null, false);
			}
			return false;
		}
		return true;
	}
	
	protected boolean checkCurrentColumn(A column, TableRow<A, B> row1, TableRow<A, B> row2, RowComparisonData<A, B> compData)
	{
		// Check if it's "unexpected" column which should be marked as 'INFO' in comparison results
		if (!row1.getHeader().containsColumn(column))
		{
			compData.addInfoComparisonDetail(column, null, row2.getValue(column));
			return false;
		}
		return true;
	}
	
	protected void processValuesComparison(A column, B value1, B value2, RowComparisonData<A, B> compData)
	{
		try
		{
			compData.addComparisonDetail(column, value1, value2, compareValues(value1, value2, column));
		}
		catch (Exception e)
		{
			compData.addErrorMsg(ExceptionUtils.getDetailedMessage(e));
		}
	}
	
	protected boolean compareValues(B value1, B value2, A column) throws Exception
	{
		return Objects.equals(value1, value2);
	}
}
