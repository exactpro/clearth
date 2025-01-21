/*******************************************************************************
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

package com.exactprosystems.clearth.utils.tabledata.comparison.rowsComparators;

import com.exactprosystems.clearth.utils.tabledata.TableRow;
import com.exactprosystems.clearth.utils.tabledata.comparison.mappings.DataMapping;
import com.exactprosystems.clearth.utils.tabledata.comparison.result.RowComparisonData;
import com.exactprosystems.clearth.utils.tabledata.comparison.valuesComparators.ValuesComparator;

public class MappedTableRowsComparator<A, B> extends TableRowsComparator<A, B>
{
	protected DataMapping<A> dataMapping;
	
	public MappedTableRowsComparator(ValuesComparator<A, B> valuesComparator, DataMapping<A> dataMapping)
	{
		super(valuesComparator);
		this.dataMapping = dataMapping;
	}
	
	public MappedTableRowsComparator(ValuesComparator<A, B> valuesComparator, boolean failUnexpectedColumns, DataMapping<A> dataMapping)
	{
		super(valuesComparator, failUnexpectedColumns);
		this.dataMapping = dataMapping;
	}
	
	@Override
	protected boolean checkCurrentColumn(A column, TableRow<A, B> row1, TableRow<A, B> row2,
			RowComparisonData<A, B> compData, boolean failUnexpectedColumns)
	{
		if (dataMapping.isIgnore(column))
			return false;
		
		return super.checkCurrentColumn(column, row1, row2, compData, !dataMapping.isInfo(column) && failUnexpectedColumns);
	}
}