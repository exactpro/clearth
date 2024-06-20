/******************************************************************************
 * Copyright 2009-2024 Exactpro Systems Limited
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

package com.exactprosystems.clearth.automation.persistence.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.automation.persistence.ExecutorStateException;
import com.exactprosystems.clearth.utils.CommaBuilder;
import com.exactprosystems.clearth.utils.Pair;

public class QueryHelper implements AutoCloseable
{
	private static final Logger logger = LoggerFactory.getLogger(QueryHelper.class);
	private static final String INDEX_POSTFIX = "_index";
	
	private final Connection con;
	private final Statement stmt;
	
	public QueryHelper(Connection con) throws SQLException
	{
		this.con = con;
		this.stmt = con.createStatement();
	}
	
	@Override
	public void close() throws SQLException
	{
		stmt.close();
	}
	
	
	public void dropTable(String tableName) throws SQLException
	{
		stmt.execute("drop table if exists "+tableName);
	}
	
	public void dropIndex(String tableName) throws SQLException
	{
		stmt.execute("drop index if exists "+tableName+INDEX_POSTFIX);
	}
	
	public void createTable(String tableName, Pair<String, String> keyColumn, Map<String, String> otherColumns) throws SQLException
	{
		CommaBuilder cb = new CommaBuilder()
				.add(keyColumn.getFirst())
				.add(" ")
				.add(keyColumn.getSecond())
				.add(" primary key");
		
		for (Entry<String, String> c : otherColumns.entrySet())
			cb.append(c.getKey()).add(" ").add(c.getValue());
		
		StringBuilder sb = new StringBuilder("create table ").append(tableName)
				.append("(").append(cb.toString()).append(")");
		
		stmt.execute(sb.toString());
	}
	
	public void createIndex(String tableName, String indexColumn) throws SQLException
	{
		StringBuilder sb = new StringBuilder("create index ")
				.append(tableName).append(INDEX_POSTFIX)
				.append(" on ").append(tableName)
				.append("(").append(indexColumn).append(")");
		
		stmt.execute(sb.toString());
	}
	
	public PreparedStatement prepareInsert(String tableName, Collection<String> columnNames) throws SQLException
	{
		CommaBuilder names = new CommaBuilder(),
				placeholders = new CommaBuilder();
		for (String c : columnNames)
		{
			names.append(c);
			placeholders.append("?");
		}
		
		StringBuilder sb = new StringBuilder("insert into ").append(tableName)
				.append("(").append(names.toString()).append(") values(")
				.append(placeholders.toString()).append(")");
		
		return con.prepareStatement(sb.toString());
	}
	
	public PreparedStatement prepareUpdate(String tableName, String keyColumn, Collection<String> columnNames) throws SQLException
	{
		CommaBuilder pairs = new CommaBuilder();
		for (String c : columnNames)
			pairs.append(c).add(" = ?");
		
		StringBuilder sb = new StringBuilder("update ").append(tableName)
				.append(" set ").append(pairs.toString())
				.append(" where ").append(keyColumn).append(" = ?");
		
		return con.prepareStatement(sb.toString());
	}
	
	public PreparedStatement prepareSelect(String tableName, String keyColumn) throws SQLException
	{
		StringBuilder sb = new StringBuilder("select * from ").append(tableName)
				.append(" where ").append(keyColumn).append(" = ?");
		
		return con.prepareStatement(sb.toString());
	}
	
	public PreparedStatement prepare(String sql) throws SQLException
	{
		return con.prepareStatement(sql);
	}
	
	
	public ResultSet select(String sql) throws SQLException
	{
		logger.trace("Selecting by '{}'", sql);
		return stmt.executeQuery(sql);
	}
	
	public ResultSet selectNonEmpty(String sql, String entityName) throws SQLException, ExecutorStateException
	{
		ResultSet rs = select(sql);
		checkNotEmpty(rs, entityName);
		return rs;
	}
	
	public ResultSet select(PreparedStatement query) throws SQLException
	{
		logger.trace("Selecting by '{}'", query);
		return query.executeQuery();
	}
	
	public ResultSet selectNonEmpty(PreparedStatement query, String entityName) throws SQLException, ExecutorStateException
	{
		ResultSet rs = select(query);
		checkNotEmpty(rs, entityName);
		return rs;
	}
	
	public int insert(PreparedStatement query) throws SQLException
	{
		logger.trace("Inserting by '{}'", query);
		query.execute();
		return getLastId();
	}
	
	public int insertBatch(PreparedStatement query) throws SQLException
	{
		logger.trace("Inserting batch by '{}'", query);
		query.executeBatch();
		return getLastId();
	}
	
	public void update(PreparedStatement query) throws SQLException
	{
		logger.trace("Updating by '{}'", query);
		query.execute();
	}
	
	public void startTransaction() throws SQLException
	{
		logger.trace("Starting transaction");
		con.setAutoCommit(false);
	}
	
	public void commitTransaction() throws SQLException
	{
		logger.trace("Committing transaction");
		try
		{
			con.commit();
		}
		finally
		{
			con.setAutoCommit(true);
		}
	}
	
	public void rollbackTransaction() throws SQLException
	{
		logger.trace("Rolling transaction back");
		try
		{
			con.rollback();
		}
		finally
		{
			con.setAutoCommit(true);
		}
	}
	
	public int getLastId() throws SQLException
	{
		try (ResultSet rs = select("select last_insert_rowid()"))
		{
			rs.next();
			return rs.getInt(1);
		}
	}
	
	
	private void checkNotEmpty(ResultSet rs, String entityName) throws SQLException, ExecutorStateException
	{
		if (!rs.next())
			throw new ExecutorStateException("No "+entityName+" available");
	}
}