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

package com.exactprosystems.clearth.utils.tabledata.writers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;

import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.utils.tabledata.StringTableData;
import com.exactprosystems.clearth.utils.tabledata.TableDataWriter;
import com.exactprosystems.clearth.utils.tabledata.TableHeader;
import com.exactprosystems.clearth.utils.tabledata.TableRow;

/**
 * Writer of table-like data to database.
 * Data is written with INSERT query, generated for given header and table name
 * @author vladimir.panarin
 */
public class DbDataWriter extends TableDataWriter<String, String>
{
	protected final PreparedStatement stmt;
	
	public DbDataWriter(TableHeader<String> header, Connection con, String tableName) throws SQLException
	{
		super(header);
		stmt = createPrepatedStatement(con, tableName);  //Need to generate query to exactly match given header
	}
	
	
	/**
	 * Writes whole table to given database table, closing writer after that
	 * @param table to write data from
	 * @param con database connection to use
	 * @param tableName name of database table to write data to
	 * @throws SQLException if SQL-specific error occurs, for instance, while generating prepared statement to write data
	 * @throws IOException if error occurs while writing data
	 */
	public static void write(StringTableData table, Connection con, String tableName) throws SQLException, IOException
	{
		DbDataWriter writer = null;
		try
		{
			writer = new DbDataWriter(table.getHeader(), con, tableName);
			writer.write(table.getRows());
		}
		finally
		{
			Utils.closeResource(writer);
		}
	}
	

	@Override
	public void close() throws IOException
	{
		try
		{
			stmt.close();
		}
		catch (SQLException e)
		{
			throw new IOException(e);
		}
	}
	
	@Override
	protected int writeRow(TableRow<String, String> row) throws IOException
	{
		try
		{
			setQueryParameters(row, stmt);
			stmt.executeUpdate();
			return getGeneratedKey(stmt);
		}
		catch (SQLException e)
		{
			throw new IOException(e);
		}
	}
	
	@Override
	protected int writeRows(Collection<TableRow<String, String>> rows) throws IOException
	{
		try
		{
			for (TableRow<String, String> r : rows)
			{
				setQueryParameters(r, stmt);
				stmt.addBatch();
			}
			stmt.executeBatch();
			return getGeneratedKey(stmt);
		}
		catch (SQLException e)
		{
			throw new IOException(e);
		}
	}
	
	
	protected PreparedStatement createPrepatedStatement(Connection con, String tableName) throws SQLException
	{
		return con.prepareStatement(generateQuery(header, tableName), Statement.RETURN_GENERATED_KEYS);
	}
	
	protected String generateQuery(TableHeader<String> header, String tableName)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("INSERT INTO ").append(tableName).append(" (");
		
		boolean first = true;
		StringBuilder values = new StringBuilder();
		for (String key : header)
		{
			if (first)
				first = false;
			else
			{
				sb.append(", ");
				values.append(", ");
			}
			sb.append(key);
			values.append("?");
		}
		sb.append(") VALUES (").append(values.toString()).append(")");
		return sb.toString();
	}
	
	protected void setQueryParameters(TableRow<String, String> row, PreparedStatement ps) throws SQLException
	{
		int i = 0;
		for (String value : row)
		{
			i++;
			ps.setString(i, value);
		}
	}
	
	protected int getGeneratedKey(PreparedStatement ps) throws SQLException
	{
		ResultSet rs = null;
		try
		{
			rs = ps.getGeneratedKeys();
			if (rs.next())
				return rs.getInt(1);
			return -1;
		}
		finally
		{
			if (rs != null)
				rs.close();
		}
	}
}
