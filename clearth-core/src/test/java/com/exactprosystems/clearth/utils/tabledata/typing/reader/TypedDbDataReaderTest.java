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
import com.exactprosystems.clearth.utils.tabledata.typing.converter.SqliteTypesConverter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.io.IOException;
import java.sql.*;

public class TypedDbDataReaderTest
{
	private static final String URL = "jdbc:sqlite::memory:";
	
	private static final String TABLE_NAME = "myTable";
	private static final String COLUMN_STRING = "column_string",
								COLUMN_BOOLEAN = "column_boolean",
								COLUMN_INT = "column_int",
								COLUMN_SHORT = "column_short",
								COLUMN_LONG = "column_long",
								COLUMN_BYTE = "column_byte",
								COLUMN_DOUBLE = "column_double",
								COLUMN_FLOAT = "column_float";
	private static final String CREATE_DB_QUERY = "CREATE TABLE " + TABLE_NAME + "(" +
														COLUMN_STRING + " TEXT, " +
														COLUMN_BOOLEAN + " BOOLEAN, " +
														COLUMN_INT + " INTEGER, " +
														COLUMN_SHORT + " INTEGER, " +
														COLUMN_LONG + " INTEGER, " +
														COLUMN_DOUBLE + " REAL, " +
														COLUMN_BYTE + " INTEGER, " +
														COLUMN_FLOAT + " REAL" + ");";
	
	private static final String INSERT_NORMAL_DATA_QUERY = "INSERT INTO " + TABLE_NAME
																	+ " values('text1',TRUE, " +
																		Integer.MAX_VALUE + "," +
																		Short.MAX_VALUE + "," +
																		Long.MAX_VALUE + "," +
																		Double.MAX_VALUE + "," +
																		Byte.MAX_VALUE + "," +
																		Float.MAX_VALUE + ");";
	private static final String INSERT_NULL_DATA_QUERY = "INSERT INTO " + TABLE_NAME
												+ " values(NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL);";
	private static final String SELECT_QUERY = "SELECT * FROM " + TABLE_NAME;
	
	private Connection connection;
	private TypedDbDataReader reader;
	
	@BeforeClass
	public void init() throws SQLException, IOException
	{
		initConnection();
		initReader();
	}
	
	@AfterClass
	public void closeAll()
	{
		Utils.closeResource(connection);
		Utils.closeResource(reader);
	}
	
	private void initReader() throws SQLException
	{
		reader = new TypedDbDataReader(connection.prepareStatement(SELECT_QUERY), new SqliteTypesConverter());
	}
	private void initConnection() throws SQLException 
	{
		connection = DriverManager.getConnection(URL);
		try (Statement statement = connection.createStatement())
		{
			statement.execute(CREATE_DB_QUERY);
			statement.execute(INSERT_NORMAL_DATA_QUERY);
			statement.execute(INSERT_NULL_DATA_QUERY);
		}
	}
	
	@DataProvider(name = "table-data")
	public Object[][] createTableData()
	{
		return new Object[][]
			{
				{
					"text1",
					Boolean.TRUE,
					Integer.MAX_VALUE,
					Short.MAX_VALUE,
					Long.MAX_VALUE,
					Float.MAX_VALUE,
					Double.MAX_VALUE,
					Byte.MAX_VALUE
				},
				{null, null, null, null, null, null, null , null}
			};
	}
	
	@Test(dataProvider = "table-data")
	public void checkDataRead(String stringValue,
	                          Boolean booleanValue,
	                          Integer integerValue,
	                          Short shortValue,
	                          Long longValue,
	                          Float floatValue,
	                          Double doubleValue,
	                          Byte byteValue) throws IOException, SQLException
	{
		if (!reader.hasMoreData())
			return;
		
		SoftAssert softAssert = new SoftAssert();
		
		softAssert.assertEquals(reader.getString(COLUMN_STRING), stringValue);
		softAssert.assertEquals(reader.getBoolean(COLUMN_BOOLEAN), booleanValue);
		softAssert.assertEquals(reader.getInteger(COLUMN_INT), integerValue);
		softAssert.assertEquals(reader.getShort(COLUMN_SHORT), shortValue);
		softAssert.assertEquals(reader.getLong(COLUMN_LONG), longValue);
		softAssert.assertEquals(reader.getDouble(COLUMN_DOUBLE), doubleValue);
		softAssert.assertEquals(reader.getFloat(COLUMN_FLOAT), floatValue);
		softAssert.assertEquals(reader.getByte(COLUMN_BYTE), byteValue);
		
		softAssert.assertAll();
	}
}