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
package com.exactprosystems.clearth.utils.tabledata.typing.writer;

import com.exactprosystems.clearth.utils.CommaBuilder;
import com.exactprosystems.clearth.utils.tabledata.TableDataWriter;
import com.exactprosystems.clearth.utils.tabledata.TableHeader;
import com.exactprosystems.clearth.utils.tabledata.TableRow;
import com.exactprosystems.clearth.utils.tabledata.typing.TypedTableData;
import com.exactprosystems.clearth.utils.tabledata.typing.TypedTableHeader;
import com.exactprosystems.clearth.utils.tabledata.typing.TypedTableHeaderItem;
import com.exactprosystems.clearth.utils.tabledata.typing.TypedTableRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TypedDbDataWriter extends TableDataWriter<TypedTableHeaderItem, Object>
{

	private static final Logger logger = LoggerFactory.getLogger(TypedDbDataWriter.class);

	protected final PreparedStatement preparedStatement;
	private boolean isGeneratedKeyAvailable = true;

	public TypedDbDataWriter(TableHeader<TypedTableHeaderItem> header, Connection con, String tableName) throws SQLException
	{
		super(header);
		this.preparedStatement = createPreparedStatement(con, tableName);
	}

	public static void write(TypedTableData table, Connection con, String tableName) throws SQLException, IOException
	{
		try(TypedDbDataWriter writer = new TypedDbDataWriter(table.getHeader(), con, tableName))
		{
			List<TableRow<TypedTableHeaderItem, Object>> rows = new ArrayList<>(table.getRows());
			writer.write(rows);
		}
	}

	@Override
	protected int writeRow(TableRow<TypedTableHeaderItem, Object> row) throws IOException
	{
		try
		{
			setQueryParameters(row, preparedStatement);
			preparedStatement.executeUpdate();
			return getGeneratedKey(preparedStatement);
		}
		catch (SQLException e)
		{
			throw new IOException(e);
		}
	}

	@Override
	protected int writeRows(Collection<TableRow<TypedTableHeaderItem, Object>> rows) throws IOException
	{
		try
		{
			for (TableRow<TypedTableHeaderItem, Object> r : rows)
			{
				TypedTableRow typedTableRow = (TypedTableRow) r;
				setQueryParameters(typedTableRow, preparedStatement);
				preparedStatement.addBatch();
			}
			preparedStatement.executeBatch();
			return getGeneratedKey(preparedStatement);
		}
		catch (SQLException e)
		{
			throw new IOException(e);
		}
	}

	@Override
	public void close() throws IOException
	{
		try
		{
			preparedStatement.close();
		}
		catch (SQLException e)
		{
			throw new IOException(e);
		}
	}

	protected PreparedStatement createPreparedStatement(Connection con, String tableName) throws SQLException
	{
		return con.prepareStatement(generateQuery(header, tableName), Statement.RETURN_GENERATED_KEYS);
	}

	protected void setQueryParameters(TableRow<TypedTableHeaderItem, Object> row, PreparedStatement ps) throws SQLException
	{
		int i = 0;
		for (Object value : row)
		{
			i++;
			if (value instanceof String)
				ps.setString(i, (String) value);
			else if (value instanceof Integer)
				ps.setInt(i, (Integer) value);
			else if (value instanceof Boolean)
				ps.setBoolean(i, (Boolean) value);
			else if (value instanceof Byte)
				ps.setByte(i, (Byte) value);
			else if (value instanceof Short)
				ps.setShort(i, (Short) value);
			else if (value instanceof Long)
				ps.setLong(i, (Long) value);
			else if (value instanceof Float)
				ps.setFloat(i, (Float) value);
			else if (value instanceof Double)
				ps.setDouble(i, (Double) value);
			else if (value instanceof BigDecimal)
				ps.setBigDecimal(i, (BigDecimal) value);
			else if (value instanceof Date)
				ps.setDate(i, (Date) value);
			else if (value instanceof Time)
				ps.setTime(i, (Time) value);
			else if (value instanceof LocalDateTime)
				ps.setTimestamp(i, Timestamp.valueOf((LocalDateTime) value));
			else if (value instanceof Timestamp)
				ps.setTimestamp(i, (Timestamp) value);
			else if (!checkAdditionalTypes(value, i, ps))
				ps.setObject(i, value);
		}
	}

	protected boolean checkAdditionalTypes(Object value, int i, PreparedStatement ps)
	{
		//Do nothing here, this method is needed for the possibility to add more types
		//If any new type fits then return true
		return false;
	}

	protected int getGeneratedKey(PreparedStatement ps)
	{
		if (!isGeneratedKeyAvailable)
			return -1;
		
		try (ResultSet rs = ps.getGeneratedKeys())
		{
			if (rs.next())
			{
				Object key = rs.getObject(1);
				String stringKey = String.valueOf(key);
				//From some database value came in string format, so it's better to check number  with parse method
				try
				{
					return Integer.parseInt(stringKey);
				}
				catch (NumberFormatException e)
				{
					logger.warn("Bad value type while retrieving auto-generated key, expected int, but " +
							"value is {}",	stringKey);
					isGeneratedKeyAvailable = false;
				}
			}
		}
		catch (Exception e)
		{
			logger.warn("Error while retrieving auto-generated keys", e);
		}

		return -1;
	}

	protected String generateQuery(TableHeader<TypedTableHeaderItem> header, String tableName)
	{
		TypedTableHeader typedTableHeader = (TypedTableHeader) header;
		StringBuilder sb = new StringBuilder();
		sb.append("INSERT INTO ").append(tableName).append(" (");

		CommaBuilder params = new CommaBuilder(),
				values = new CommaBuilder();
		for (String key : typedTableHeader.getColumnNames())
		{
			params.append(key);
			values.append("?");
		}
		sb.append(params).append(") VALUES (").append(values).append(")");
		return sb.toString();
	}
}