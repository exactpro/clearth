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

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class QueryParameterSetter
{
	private final PreparedStatement ps;
	private int index = 1;
	
	public QueryParameterSetter(PreparedStatement ps) throws SQLException
	{
		this.ps = ps;
		ps.clearParameters();
	}
	
	public static QueryParameterSetter newInstance(PreparedStatement ps) throws SQLException
	{
		return new QueryParameterSetter(ps);
	}
	
	
	public QueryParameterSetter setInt(int value) throws SQLException
	{
		ps.setInt(index++, value);
		return this;
	}
	
	public QueryParameterSetter setLong(long value) throws SQLException
	{
		ps.setLong(index++, value);
		return this;
	}
	
	public QueryParameterSetter setBoolean(boolean value) throws SQLException
	{
		ps.setBoolean(index++, value);
		return this;
	}
	
	public QueryParameterSetter setString(String value) throws SQLException
	{
		ps.setString(index++, value);
		return this;
	}
	
	public QueryParameterSetter setBytes(byte[] value) throws SQLException
	{
		ps.setBytes(index++, value);
		return this;
	}
	
	public PreparedStatement getStatement()
	{
		return ps;
	}
}