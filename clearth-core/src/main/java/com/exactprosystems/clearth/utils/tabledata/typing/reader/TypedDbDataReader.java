/******************************************************************************
 * Copyright 2009-2022 Exactpro Systems Limited
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
package com.exactprosystems.clearth.utils.tabledata.typing.reader;

import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.utils.javaFunction.BiFunctionWithException;
import com.exactprosystems.clearth.utils.sql.SQLUtils;
import com.exactprosystems.clearth.utils.tabledata.readers.BasicTableDataReader;
import com.exactprosystems.clearth.utils.tabledata.RowsListFactory;
import com.exactprosystems.clearth.utils.tabledata.TableRow;
import com.exactprosystems.clearth.utils.tabledata.readers.DbRowFilter;
import com.exactprosystems.clearth.utils.tabledata.typing.*;
import com.exactprosystems.clearth.utils.tabledata.typing.converter.DbTypesConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.*;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;


public class TypedDbDataReader extends BasicTableDataReader<TypedTableHeaderItem,Object, TypedTableData>
{
	private static final Logger logger = LoggerFactory.getLogger(TypedDbDataReader.class);

	protected final PreparedStatement statement;
	protected final DbTypesConverter converter;
	protected ResultSet resultSet;
	protected DbRowFilter dbRowFilter;
	protected String queryDescription;
	protected TypedTableHeader header;

	public TypedDbDataReader(PreparedStatement statement, DbTypesConverter converter)
	{
		this.statement = statement;
		this.converter = converter;
	}

	@Override
	public boolean hasMoreData() throws IOException
	{
		if (resultSet == null)
			executeStatement();

		try
		{
			return resultSet.next();
		}
		catch (SQLException e)
		{
			throw new IOException("Error while getting next query result row", e);
		}
	}

	@Override
	public boolean filter() throws IOException
	{
		return dbRowFilter == null || dbRowFilter.filter(resultSet);
	}

	@Override
	protected TypedTableData createTableData(Set<TypedTableHeaderItem> header,
	                                         RowsListFactory<TypedTableHeaderItem, Object> rowsListFactory)
	{
		return new TypedTableData(header);
	}

	@Override
	public void close() throws IOException
	{
		Utils.closeResource(resultSet);
		Utils.closeResource(statement);
	}

	@Override
	protected Set<TypedTableHeaderItem> readHeader() throws IOException
	{
		Set<TypedTableHeaderItem> headerSet = new LinkedHashSet<>();

		if (resultSet == null)
			executeStatement();
		try
		{
			ResultSetMetaData rsMetadata = resultSet.getMetaData();
			List<String> columnNames = new LinkedList<>(SQLUtils.getColumnNames(rsMetadata));

			for (int i = 1; i <= columnNames.size(); i++)
			{
				int typeIndex = rsMetadata.getColumnType(i);
				headerSet.add(new TypedTableHeaderItem(columnNames.get(i - 1), converter.getType(typeIndex)));
			}
			return headerSet;
		}
		catch (SQLException e)
		{
			throw new IOException("Error while reading header from query result", e);
		}
	}

	@Override
	protected void fillRow(TableRow<TypedTableHeaderItem, Object> row) throws IOException
	{
		TypedTableRow typedTableRow = (TypedTableRow) row;
		header = (TypedTableHeader) row.getHeader();
		for (TypedTableHeaderItem head : header)
		{
			try
			{
				TableDataType type = head.getType();
				String headerName = head.getName();
				switch (type)
				{
					case INTEGER:
						typedTableRow.setInteger(headerName, getValue(headerName, ResultSet::getInt));
						break;
					case BOOLEAN:
						typedTableRow.setBoolean(headerName, getValue(headerName, ResultSet::getBoolean));
						break;
					case FLOAT:
						typedTableRow.setFloat(headerName, getValue(headerName, ResultSet::getFloat));
						break;
					case DOUBLE:
						typedTableRow.setDouble(headerName, getValue(headerName, ResultSet::getDouble));
						break;
					case BYTE:
						typedTableRow.setByte(headerName, getValue(headerName, ResultSet::getByte));
						break;
					case SHORT:
						typedTableRow.setShort(headerName, getValue(headerName, ResultSet::getShort));
						break;
					case LONG:
						typedTableRow.setLong(headerName, getValue(headerName, ResultSet::getLong));
						break;
					case LOCALDATE:
						typedTableRow.setLocalDate(headerName, getValue(headerName, ResultSet::getDate));
						break;
					case LOCALTIME:
						typedTableRow.setLocalTime(headerName, getValue(headerName, ResultSet::getTime, Time::toLocalTime));
						break;
					case BIGDECIMAL:
						typedTableRow.setBigDecimal(headerName, getValue(headerName, ResultSet::getBigDecimal));
						break;
					case STRING:
						typedTableRow.setString(headerName, getValue(headerName, ResultSet::getString));
						break;
					case LOCALDATETIME:
						typedTableRow.setDateTime(headerName, getValue(headerName, ResultSet::getTimestamp, Timestamp::toLocalDateTime));
						break;
					default:
						typedTableRow.setObject(headerName, getValue(headerName, ResultSet::getObject));
				}
			}
			catch (SQLException e)
			{
				throw new IOException("Error while getting value for column '" + head.getName() + "'", e);
			}
		}
	}

	protected <R> R getValue(String tableHeader, BiFunctionWithException<ResultSet, String, R, SQLException> function) throws SQLException
	{
		return getValue(tableHeader, function, Function.identity());
	}

	protected <T, R> R getValue(String tableHeader, BiFunctionWithException<ResultSet, String, T, SQLException> function,
							 Function<T, R> transformFunction) throws SQLException
	{
		T result = function.apply(resultSet, tableHeader);
		return resultSet.wasNull() ? null : transformFunction.apply(result);
	}

	protected void executeStatement() throws IOException
	{
		try
		{
			long startTime = System.currentTimeMillis();
			if (!statement.execute())
				throw new IOException("No data in DB result set. Probably an update query has been used or there is no result at all");
			logger.debug("Query {}has been executed in {} sec.", queryDescription != null ? queryDescription + " "
							: "",
					TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime));
			resultSet = statement.getResultSet();
		}
		catch (SQLException e)
		{
			throw new IOException("Error occurred while executing SQL query", e);
		}
	}
}