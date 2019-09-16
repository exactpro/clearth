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

package com.exactprosystems.clearth.utils.tabledata;

/**
 * Interface to match table-like data rows. 
 * Rows can be quickly matched by comparing their primary keys. After that secondary key comparison finds if rows match exactly.
 * Primary key created by this class is used for direct comparison.
 * Secondary key comparison needs logic to match rows
 * @author vladimir.panarin
 * @param <A> class of table header members
 * @param <B> class of values in table rows
 * @param <C> class of primary key
 */
public interface TableRowMatcher<A, B, C>
{
	/**
	 * Creates primary key to quickly match given row with another one by comparing primary keys
	 * @param row to create primary key for
	 * @return primary key to compare with another one
	 */
	public C createPrimaryKey(TableRow<A, B> row);
	/**
	 * Checks if rows match by secondary key.
	 * This method must be called only for rows whose primary keys are equal to accurately find if rows match
	 * @param row1
	 * @param row2
	 * @return true only if rows completely match by secondary key
	 */
	public boolean matchBySecondaryKey(TableRow<A, B> row1, TableRow<A, B> row2);
}
