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
import com.google.api.client.util.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TypedDbDataWriter extends TableDataWriter<TypedTableHeaderItem, Object>
{

	private static final Logger logger = LoggerFactory.getLogger(TypedDbDataWriter.class);

	protected final PreparedStatement preparedStatement;

	public TypedDbDataWriter(TableHeader header, Connection con, String tableName) throws SQLException
	{
		super(header);
		this.preparedStatement = createPreparedStatement(con, tableName);
	}

	public static void write(TypedTableData table, Connection con, String tableName) throws SQLException, IOException
	{
		try(TypedDbDataWriter writer = new TypedDbDataWriter(table.getHeader(), con, tableName))
		{
			List<TableRow<TypedTableHeaderItem, Object>> rows = new ArrayList<>(table.getTableRows());
			writer.write(rows);
		}
	}

	@Override
	protected int writeRow(TableRow row) throws IOException
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
			for (TableRow r : rows)
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

	protected void setQueryParameters(TableRow row, PreparedStatement ps) throws SQLException
	{
		int i = 0;
		for (Object value : row)
		{
			i++;
			if (value instanceof String)
			{
				ps.setString(i, (String) value);
				continue;
			}
			if (value instanceof Integer)
			{
				ps.setInt(i, (Integer) value);
				continue;
			}
			if (value instanceof Boolean)
			{
				ps.setBoolean(i, (Boolean) value);
				continue;
			}
			if (value instanceof Byte)
			{
				ps.setByte(i, (Byte) value);
				continue;
			}
			if (value instanceof Short)
			{
				ps.setShort(i, (Short) value);
				continue;
			}
			if (value instanceof Long)
			{
				ps.setLong(i, (Long) value);
				continue;
			}
			if (value instanceof Float)
			{
				ps.setFloat(i, (Float) value);
				continue;
			}
			if (value instanceof Double)
			{
				ps.setDouble(i, (Double) value);
				continue;
			}
			if (value instanceof BigDecimal)
			{
				ps.setBigDecimal(i, (BigDecimal) value);
				continue;
			}
			if (value instanceof Date)
			{
				ps.setDate(i, (Date) value);
				continue;
			}
			if (value instanceof Time)
			{
				ps.setTime(i, (Time) value);
				continue;
			}
			if (value instanceof DateTime)
			{
				ps.setTimestamp(i, new Timestamp(((DateTime) value).getValue()));
				continue;
			}
			ps.setObject(i, value);
		}
	}

	protected int getGeneratedKey(PreparedStatement ps)
	{
		try (ResultSet rs = ps.getGeneratedKeys())
		{
			if (rs.next())
				return rs.getInt(1);
		}
		catch (Exception e)
		{
			logger.warn("Error while retrieving auto-generated keys", e);
		}

		return -1;
	}

	protected String generateQuery(TableHeader header, String tableName)
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