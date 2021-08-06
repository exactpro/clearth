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

import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.utils.sql.SQLUtils;
import com.exactprosystems.clearth.utils.tabledata.BasicTableDataReader;
import com.exactprosystems.clearth.utils.tabledata.RowsListFactory;
import com.exactprosystems.clearth.utils.tabledata.TableRow;
import com.exactprosystems.clearth.utils.tabledata.readers.DbRowFilter;
import com.exactprosystems.clearth.utils.tabledata.typing.*;
import com.exactprosystems.clearth.utils.tabledata.typing.converter.DbTypesConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;


public  class TypedDbDataReader extends BasicTableDataReader<TypedTableHeaderItem,Object, TypedTableData>
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
						typedTableRow.setInteger(headerName, getInteger(headerName, resultSet));
						break;
					case BOOLEAN:
						typedTableRow.setBoolean(headerName, getBoolean(headerName, resultSet));
						break;
					case FLOAT:
						typedTableRow.setFloat(headerName, getFloat(headerName, resultSet));
						break;
					case DOUBLE:
						typedTableRow.setDouble(headerName, getDouble(headerName, resultSet));
						break;
					case BYTE:
						typedTableRow.setByte(headerName, getByte(headerName, resultSet));
						break;
					case SHORT:
						typedTableRow.setShort(headerName, getShort(headerName, resultSet));
						break;
					case LONG:
						typedTableRow.setLong(headerName, getLong(headerName, resultSet));
						break;
					case LOCALDATE:
						typedTableRow.setLocalDate(headerName, getLocalDate(headerName, resultSet));
						break;
					case LOCALTIME:
						typedTableRow.setLocalTime(headerName, getLocalTime(headerName, resultSet));
						break;
					case BIGDECIMAL:
						typedTableRow.setBigDecimal(headerName, getBigDecimal(headerName, resultSet));
						break;
					case STRING:
						typedTableRow.setString(headerName, getString(headerName, resultSet));
						break;
					case LOCALDATETIME:
						typedTableRow.setDateTime(headerName, getLocalDateTime(headerName, resultSet));
						break;
					default:
						typedTableRow.setObject(headerName, getObject(headerName, resultSet));
				}
			}
			catch (SQLException e)
			{
				throw new IOException("Error while getting value for column '" + head.getName() + "'", e);
			}
		}
	}

	protected Integer getInteger(String tableHeader, ResultSet resultSet) throws SQLException
	{
		return resultSet.getInt(tableHeader);
	}

	protected Boolean getBoolean(String tableHeader, ResultSet resultSet) throws SQLException
	{
		return resultSet.getBoolean(tableHeader);
	}

	protected Float getFloat(String tableHeader, ResultSet resultSet) throws SQLException
	{
		return resultSet.getFloat(tableHeader);
	}

	protected Double getDouble(String tableHeader, ResultSet resultSet) throws SQLException
	{
		return resultSet.getDouble(tableHeader);
	}

	protected Byte getByte(String tableHeader, ResultSet resultSet) throws SQLException
	{
		return resultSet.getByte(tableHeader);
	}

	protected Short getShort(String tableHeader, ResultSet resultSet) throws SQLException
	{
		return resultSet.getShort(tableHeader);
	}

	protected Long getLong(String tableHeader, ResultSet resultSet) throws SQLException
	{
		return resultSet.getLong(tableHeader);
	}

	protected Date getLocalDate(String tableHeader, ResultSet resultSet) throws SQLException
	{
		return resultSet.getDate(tableHeader);
	}

	protected LocalTime getLocalTime(String tableHeader, ResultSet resultSet) throws SQLException
	{
		Time time = resultSet.getTime(tableHeader);
		return time == null ? null : time.toLocalTime();
	}

	protected BigDecimal getBigDecimal(String tableHeader, ResultSet resultSet) throws SQLException
	{
		return resultSet.getBigDecimal(tableHeader);
	}

	protected String getString(String tableHeader, ResultSet resultSet) throws SQLException
	{
		return resultSet.getString(tableHeader);
	}

	protected LocalDateTime getLocalDateTime(String tableHeader, ResultSet resultSet) throws SQLException
	{
		Timestamp timestamp = resultSet.getTimestamp(tableHeader);
		return timestamp == null ? null : timestamp.toLocalDateTime();
	}

	protected Object getObject(String tableHeader, ResultSet resultSet) throws SQLException
	{
		return resultSet.getObject(tableHeader);
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