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

package com.exactprosystems.clearth.utils.tabledata.readers;

import com.exactprosystems.clearth.utils.tabledata.RowsListFactory;
import com.exactprosystems.clearth.utils.tabledata.StringTableData;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Set;

/**
 * Reader of table-like data from SQL query result set.
 * @author vladimir.panarin
 */
public class DbDataReader extends AbstractDbDataReader<StringTableData>
{
	public DbDataReader(PreparedStatement preparedStatement)
	{
		super(preparedStatement);
	}
	
	@Override
	protected StringTableData createTableData(Set<String> header, RowsListFactory<String, String> rowsListFactory)
	{
		return new StringTableData(header, rowsListFactory);
	}
	
	
	/**
	 * Executes SQL query and reads its result to the table data object.
	 * @param preparedStatement compiled and ready to execute SQL statement object.
	 * @return TableData object with header that corresponds to metadata of resultSet and rows that contain all data from query result.
	 * @throws SQLException if any error occurred while executing statement.
	 * @throws IOException if something went wrong while reading gotten data.
	 */
	public static StringTableData read(PreparedStatement preparedStatement) throws SQLException, IOException
	{
		DbDataReader reader = null;
		try
		{
			reader = new DbDataReader(preparedStatement);
			return reader.readAllData();
		}
		finally
		{
			if (reader != null)
				reader.close();
		}
	}
}
