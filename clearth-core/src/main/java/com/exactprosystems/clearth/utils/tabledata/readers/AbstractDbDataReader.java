/******************************************************************************
 * Copyright 2009-2020 Exactpro Systems Limited
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

import com.exactprosystems.clearth.utils.IValueTransformer;
import com.exactprosystems.clearth.utils.ObjectToStringTransformer;
import com.exactprosystems.clearth.utils.sql.SQLUtils;
import com.exactprosystems.clearth.utils.tabledata.BasicTableData;
import com.exactprosystems.clearth.utils.tabledata.TableRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public abstract class AbstractDbDataReader<C extends BasicTableData<String, String>> extends BasicTableDataReader<String, String, C>
{
	private static final Logger logger = LoggerFactory.getLogger(AbstractDbDataReader.class);
	
	protected final PreparedStatement statement;
	protected ResultSet resultSet = null;
	
	protected DbRowFilter dbRowFilter;
	protected IValueTransformer valueTransformer;
	protected ObjectToStringTransformer objectTransformer;
	protected String queryDescription = null;
	
	public AbstractDbDataReader(PreparedStatement preparedStatement)
	{
		this.statement = preparedStatement;
	}
	
	@Override
	protected Set<String> readHeader() throws IOException
	{
		if (resultSet == null)
			executeStatement();
		
		try
		{
			return new LinkedHashSet<>(SQLUtils.getColumnNames(resultSet.getMetaData()));
		}
		catch (SQLException e)
		{
			throw new IOException("Error while getting header from query result", e);
		}
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
	protected void fillRow(TableRow<String, String> row) throws IOException
	{
		for (String column : row.getHeader())
		{
			try
			{
				row.setValue(column, getValueFromResultSet(column, resultSet));
			}
			catch (SQLException e)
			{
				throw new IOException("Error while getting value for column '" + column + "'", e);
			}
		}
	}
	
	@Override
	public void close() throws IOException
	{
		try
		{
			statement.close(); // This should close appropriate ResultSet object too
		}
		catch (SQLException e)
		{
			throw new IOException(e);
		}
	}
	
	
	protected void executeStatement() throws IOException
	{
		try
		{
			long startTime = System.currentTimeMillis();
			if (!statement.execute())
				throw new IOException("No data in DB result set. Probably an update query has been used or there is no result at all");
			getLogger().debug("Query {}has been executed in {} sec.", queryDescription != null ? queryDescription + " " : "",
					TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime));
			resultSet = statement.getResultSet();
		}
		catch (SQLException e)
		{
			throw new IOException("Error occurred while executing SQL query: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Extracts value from current ResultSet row by specified table header. Note: table header can differ from SQL result table header.
	 * @param tableHeader header of created table data instance.
	 * @return value from current ResultSet row by specified table header, specially transformed if needed.
	 * @throws SQLException if some SQL error occurred.
	 * @throws IOException if some IO error occurred.
	 */
	protected String getValueFromResultSet(String tableHeader, ResultSet resultSet) throws SQLException, IOException
	{
		String value = objectTransformer != null ?
				SQLUtils.getDbValue(resultSet, tableHeader, objectTransformer) : SQLUtils.getDbValue(resultSet, tableHeader);
		return valueTransformer != null ? valueTransformer.transform(value) : value;
	}
	
	protected Logger getLogger()
	{
		return logger;
	}
	
	
	public void setDbRowFilter(DbRowFilter dbRowFilter)
	{
		this.dbRowFilter = dbRowFilter;
	}
	
	public void setValueTransformer(IValueTransformer valueTransformer)
	{
		this.valueTransformer = valueTransformer;
	}
	
	public void setObjectToStringTransformer(ObjectToStringTransformer objectTransformer)
	{
		this.objectTransformer = objectTransformer;
	}
	
	public void setQueryDescription(String queryDescription)
	{
		this.queryDescription = queryDescription;
	}
	
	public String getQueryDescription()
	{
		return queryDescription;
	}
}
