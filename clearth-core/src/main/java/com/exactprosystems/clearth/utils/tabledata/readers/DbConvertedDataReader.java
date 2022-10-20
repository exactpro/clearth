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

package com.exactprosystems.clearth.utils.tabledata.readers;

import com.exactprosystems.clearth.utils.sql.SQLUtils;
import com.exactprosystems.clearth.utils.sql.conversion.ConversionSettings;
import com.exactprosystems.clearth.utils.tabledata.RowsListFactory;
import com.exactprosystems.clearth.utils.tabledata.StringTableData;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Set;

public class DbConvertedDataReader extends AbstractDbDataReader<StringTableData>
{
	private ConversionSettings conversionSettings;
	
	public DbConvertedDataReader(PreparedStatement preparedStatement, ConversionSettings conversionSettings)
	{
		super(preparedStatement);
		this.conversionSettings = conversionSettings;
	}
	
	@Override
	protected Set<String> readHeader() throws IOException
	{
		try
		{
			Set<String> convertedHeaderSet = new LinkedHashSet<>();
			for (String columnName : SQLUtils.getColumnNames(resultSet.getMetaData()))
				convertedHeaderSet.add(conversionSettings.getTableHeader(columnName));
			return convertedHeaderSet;
		}
		catch (SQLException e)
		{
			throw new IOException("Error while getting header from query result", e);
		}
	}
	
	@Override
	protected String getValueFromResultSet(String tableHeader, ResultSet resultSet) throws SQLException, IOException
	{
		// result set header might not be equal to table header so converted value should be gotten by corresponding DB header
		String dbHeader = conversionSettings.getDBHeader(tableHeader),
				valueFromResultSet = super.getValueFromResultSet(dbHeader, resultSet);
		return conversionSettings.getConvertedDBValue(dbHeader, valueFromResultSet);
	}
	
	@Override
	protected StringTableData createTableData(Set<String> header, RowsListFactory<String, String> rowsListFactory)
	{
		return new StringTableData(header);
	}
	
	
	public ConversionSettings getConversionSettings()
	{
		return conversionSettings;
	}
	
	public void setConversionSettings(ConversionSettings conversionSettings)
	{
		this.conversionSettings = conversionSettings;
	}
}
