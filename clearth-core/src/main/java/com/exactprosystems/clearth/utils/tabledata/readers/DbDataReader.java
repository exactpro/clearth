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

import com.exactprosystems.clearth.utils.ObjectToStringTransformer;
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

	public DbDataReader(PreparedStatement preparedStatement, boolean needCloseDbConnection)
	{
		super(preparedStatement, needCloseDbConnection);
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
		DbDataReader reader = new DbDataReader(preparedStatement);
		return readAll(reader);
	}

	/**
	 * Executes SQL query and reads its result to the table data object.
	 * @param preparedStatement compiled and ready to execute SQL statement object.
	 * @param transformer object for custom 'object-to-string' parsing.
	 * @return TableData object with header that corresponds to metadata of resultSet and rows that contain all data from query result.
	 * @throws SQLException if any error occurred while executing statement.
	 * @throws IOException if something went wrong while reading gotten data.
	 */
	public static StringTableData read(PreparedStatement preparedStatement, ObjectToStringTransformer transformer)
			throws SQLException, IOException
	{
		DbDataReader reader = new DbDataReader(preparedStatement);
		reader.setObjectToStringTransformer(transformer);
		return readAll(reader);
	}

	private static StringTableData readAll(DbDataReader reader) throws IOException
	{
		try
		{
			return reader.readAllData();
		}
		finally
		{
			reader.close();
		}
	}
}
