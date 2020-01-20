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
package com.exactprosystems.clearth.utils.tabledata.typing.reader;

import com.exactprosystems.clearth.utils.sql.SQLUtils;
import com.exactprosystems.clearth.utils.tabledata.*;
import com.exactprosystems.clearth.utils.tabledata.readers.DbRowFilter;
import com.exactprosystems.clearth.utils.tabledata.typing.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static java.sql.Types.*;


public  class TypedDbDataReader extends BasicTableDataReader<TypedTableHeaderItem,Object, TypedTableData>
{
	private static final Logger logger = LoggerFactory.getLogger(TypedDbDataReader.class);

	protected final PreparedStatement statement;
	protected ResultSet resultSet;
	protected DbRowFilter dbRowFilter;
	protected String queryDescription;
	protected TypedTableHeader header;

	public TypedDbDataReader(PreparedStatement statement)
	{
		this.statement = statement;
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
		try
		{
			statement.close();
		}
		catch (SQLException e)
		{
			throw new IOException(e);
		}
	}

	@Override
	protected Set<TypedTableHeaderItem> readHeader() throws IOException
	{
		Set<TypedTableHeaderItem> headerSet = new LinkedHashSet<>();

		if (resultSet == null)
			executeStatement();
		try
		{
			ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
			List<String> columnNames = new LinkedList<>(SQLUtils.getColumnNames(resultSetMetaData));

			for (int i = 1; i <= columnNames.size(); i++)
			{
				int typeIndex = resultSetMetaData.getColumnType(i);
				headerSet.add(new TypedTableHeaderItem(columnNames.get(i - 1), getType(typeIndex)));
			}
			return headerSet;
		}
		catch (SQLException e)
		{
			throw new IOException("Error while reading header from query result", e);
		}
	}

	@Override
	protected void fillRow(TableRow row) throws IOException
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
						typedTableRow.setInteger(headerName, (Integer) getValueFromResultSet(head.getName(), resultSet));
						break;
					case BOOLEAN:
						typedTableRow.setBoolean(headerName, (Boolean) getValueFromResultSet(head.getName(), resultSet));
						break;
					case FLOAT:
					case LOCALDATETIME:
						typedTableRow.setFloat(headerName, (Float) getValueFromResultSet(head.getName(), resultSet));
						break;
					case DOUBLE:
						typedTableRow.setDouble(headerName, (Double) getValueFromResultSet(head.getName(), resultSet));
						break;
					case BYTE:
						typedTableRow.setByte(headerName, (Byte) getValueFromResultSet(head.getName(), resultSet));
						break;
					case SHORT:
						typedTableRow.setShort(headerName, (Short) getValueFromResultSet(head.getName(), resultSet));
						break;
					case LONG:
						typedTableRow.setLong(headerName, (Long) getValueFromResultSet(head.getName(), resultSet));
						break;
					case LOCALDATE:
						typedTableRow.setLocalDate(headerName, (Date) getValueFromResultSet(head.getName(), resultSet));
						break;
					case LOCALTIME:
						typedTableRow.setLocalTime(headerName, (LocalTime) getValueFromResultSet(head.getName(), resultSet));
						break;
					case BIGDECIMAL:
						typedTableRow.setBigDecimal(headerName,
								(BigDecimal) getValueFromResultSet(head.getName(), resultSet));
						break;
					default:
						typedTableRow.setString(headerName, (String) getValueFromResultSet(head.getName(), resultSet));
						break;
				}
			}
			catch (SQLException e)
			{
				throw new IOException("Error while getting value for column '" + head.getName() + "'", e);
			}
		}
	}


	protected Object getValueFromResultSet(String tableHeader, ResultSet resultSet) throws SQLException
	{
		return resultSet.getObject(tableHeader);
	}

	private TableDataType getType(int index)
	{
		switch (index)
		{
			case INTEGER:
				return TableDataType.INTEGER;
			case CHAR:
			case (VARCHAR):
			case (LONGVARCHAR):
			case (NCHAR):
			case (NVARCHAR):
				return TableDataType.STRING;
			case (BOOLEAN):
				return TableDataType.BOOLEAN;
			case (REAL):
				return TableDataType.FLOAT;
			case FLOAT:
			case DOUBLE:
				return TableDataType.DOUBLE;
			case (TINYINT):
				return TableDataType.BYTE;
			case (SMALLINT):
				return TableDataType.SHORT;
			case (BIGINT):
				return TableDataType.LONG;
			case (DATE):
				return TableDataType.LOCALDATE;
			case (TIME):
				return TableDataType.LOCALTIME;
			case (TIMESTAMP):
				return TableDataType.LOCALDATETIME;
			case (NUMERIC):
				return TableDataType.BIGDECIMAL;
		}
		return TableDataType.OBJECT;
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