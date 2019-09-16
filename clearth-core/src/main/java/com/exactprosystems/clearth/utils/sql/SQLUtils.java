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

package com.exactprosystems.clearth.utils.sql;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.utils.*;
import com.exactprosystems.clearth.utils.sql.conversion.ConversionSettings;
import com.exactprosystems.clearth.utils.sql.conversion.DBFieldMapping;
import com.exactprosystems.clearth.utils.sql.conversion.DBFieldMappingReader;
import com.exactprosystems.clearth.utils.tabledata.StringTableData;
import com.exactprosystems.clearth.utils.tabledata.readers.DbConvertedDataReader;
import com.exactprosystems.clearth.utils.tabledata.readers.DbDataReader;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.ConnectException;
import java.sql.*;
import java.util.*;

/**
 * Created by alexey.karpukhin on 7/27/15.
 */
public class SQLUtils
{
	private static final Logger log = LoggerFactory.getLogger(SQLUtils.class);
	
	private static final ObjectToStringTransformer OBJECT_TRANSFORMER = new DefaultSqlObjectToStringTransformer();
	public static final char CONVERT_BEGINNER = '$', CUSTOM_BEGINNER = '#';
	private static final String COMMENT_BEGIN = "/*", COMMENT_END="*/";
	public static final CharSequence TABLE_NAME_WITH_DOLLAR = "\\$", TABLE_NAME_WITH_SHARP = "\\#";
	public static final char TABLE_NAME_MARKER = '\\';
	private static final char[] BEGINNER_VARIANT = new char[]{ CONVERT_BEGINNER,
			CUSTOM_BEGINNER};
	public static final String QUERY_STARTER = "----SQL START----",
								 QUERY_ENDER = "-----SQL END-----";

	/**
	 * Parses SQL template file to an instance of ParametrizedQuery which provides an ability to execute query with specified parameters
	 */
	public static ParametrizedQuery parseSQLTemplate(File templateFile) throws IOException, SQLException
	{
		return new SQLTemplateParser().parseParametrizedQueryTemplate(templateFile);
	}

	/**
	 * Parses SQL template text to an instance of ParametrizedQuery which provides an ability to execute query with specified parameters
	 */
	public static ParametrizedQuery parseSQLTemplate(String templateText) throws SQLException
	{
		return new SQLTemplateParser().parseParametrizedQueryTemplate(templateText);
	}

	/**
	 * Creates an instance of PreparedStatement based on specified templateFile, SQL connection and parameters
	 * @param templateFile SQL template file
	 * @param connection SQL connection
	 * @param params parameters which should be passed to the query
	 * @return an instance of PreparedStatement with passed parameters
	 * @throws IOException if SQL template file could not be read
	 * @throws SQLException if there are missed some required query parameters or connection errors occurred
	 */
	public static PreparedStatement createPreparedStatement(File templateFile,
															Connection connection,
															Map<String, String> params) throws IOException, SQLException
	{
		return parseSQLTemplate(templateFile).createPreparedStatement(connection, params);
	}

	/**
	 * Creates an instance of PreparedStatement based on specified templateFile, SQL connection and parameters
	 * @param templateFile SQL template file
	 * @param connection SQL connection
	 * @param params parameters which should be passed to the query
	 * @param conversionSettings conversion settings for query parameters
	 * @return an instance of PreparedStatement with passed parameters
	 * @throws IOException if SQL template file could not be read
	 * @throws SQLException if there are missed some required query parameters or connection errors occurred
	 */
	public static PreparedStatement createPreparedStatement(File templateFile,
															Connection connection,
															Map<String, String> params,
															ConversionSettings conversionSettings) throws IOException, SQLException
	{
		Map<String, String> convertedParams = conversionSettings.createConvertedParams(params);
		return createPreparedStatement(templateFile, connection, convertedParams);
	}

	/**
	 * Creates an instance of PreparedStatement based on specified templateFile, SQL connection, parameters and data base field mapping file <br/>
	 * For better performance while handling multiple SQL templates with the same DBFieldMapping use an instance of
	 * ConversionSettings for params conversion instead of this method,
	 * this makes to avoid multiple parsing of the same DBFieldMapping file
	 * @param templateFile SQL template file
	 * @param connection SQL connection
	 * @param params parameters which should be passed to the query
	 * @param dbFieldMappingFile mapping file, used for params conversion
	 * @return an instance of PreparedStatement with passed parameters
	 * @throws IOException if SQL template file could not be read
	 * @throws SQLException if there are missed some required query parameters or connection errors occurred
	 */
	public static PreparedStatement createPreparedStatement(File templateFile,
															Connection connection,
															Map<String, String> params,
															File dbFieldMappingFile) throws IOException, SQLException
	{
		return createPreparedStatement(templateFile, connection, params, ConversionSettings.loadFromCSVFile(dbFieldMappingFile));
	}

	/**
	 * Executes SQL query based on specified SQL template file and parameters, returns StringTableData instance containing query result
	 * @param templateFile SQL template file
	 * @param connection SQL connection
	 * @param params parameters which should be passed to the query
	 * @param conversionSettings conversion settings for result table
	 * @return StringTableData instance containing query result, null if executed update query or there is no result
	 * @throws IOException if SQL template file could not be read
	 * @throws SQLException if there are missed some required query parameters or connection errors occurred
	 */
	public static StringTableData selectToTableData(File templateFile,
													Connection connection,
													Map<String, String> params,
													ConversionSettings conversionSettings)
			throws IOException, SQLException
	{
		PreparedStatement statement = null;
		DbConvertedDataReader reader = null;
		try
		{
			statement = createPreparedStatement(templateFile, connection, params, conversionSettings);
			reader = new DbConvertedDataReader(statement, conversionSettings);
			return reader.readAllData();
		}
		finally
		{
			Utils.closeResource(statement);
			Utils.closeResource(reader);
		}
	}

	/**
	 * Executes SQL query based on specified SQL template file and parameters, returns StringTableData instance containing query result
	 * @param templateFile SQL template file
	 * @param connection SQL connection
	 * @param params parameters which should be passed to the query
	 * @param dbFieldMappingFile DB field mapping file, which contains conversion rules for columns and values
	 * @return StringTableData instance containing query result, null if executed update query or there is no result
	 * @throws IOException if SQL template file could not be read
	 * @throws SQLException if there are missed some required query parameters or connection errors occurred
	 */
	public static StringTableData selectToTableData(File templateFile,
													Connection connection,
													Map<String, String> params,
													File dbFieldMappingFile) throws IOException, SQLException
	{
		return selectToTableData(templateFile, connection, params, ConversionSettings.loadFromCSVFile(dbFieldMappingFile));
	}

	/**
	 * Executes SQL query based on specified SQL template file and parameters, returns StringTableData instance containing query result
	 * @param templateFile SQL template file
	 * @param connection SQL connection
	 * @param params parameters which should be passed to the query
	 * @param mappings a list of conversion rules for columns and values
	 * @return StringTableData instance containing query result, null if executed update query or there is no result
	 * @throws IOException if SQL template file could not be read
	 * @throws SQLException if there are missed some required query parameters or connection errors occurred
	 */
	public static StringTableData selectToTableData(File templateFile,
													Connection connection,
													Map<String, String> params,
													List<DBFieldMapping> mappings) throws IOException, SQLException
	{
		return selectToTableData(templateFile, connection, params, new ConversionSettings(mappings));
	}

	/**
	 * Executes SQL query to StringTableData
	 * @param query SQL query
	 * @param connection SQL connection
	 * @return an instance of StringTableData containing result table, null if there is no result table returned
	 * @throws SQLException if there is some error occurred while executing SQL query
	 * @throws IOException if there is some error occurred while creating StringTableData based on ResultSet values
	 */
	public static StringTableData selectToTableData(String query, Connection connection)
			throws SQLException, IOException
	{
		PreparedStatement statement = null;
		try
		{
			statement = connection.prepareStatement(query);
			return toTableData(statement);
		}
		finally
		{
			Utils.closeResource(statement);
		}
	}


	public static StringTableData toTableData(PreparedStatement preparedStatement) throws IOException
	{
		DbDataReader reader = null;
		try
		{
			reader = new DbDataReader(preparedStatement);
			return reader.readAllData();
		}
		finally
		{
			Utils.closeResource(reader);
		}
	}

	/**
	 * Modifies table data fields according to specified transformer
	 */
	public static void applyTransformer(StringTableData tableData, IValueTransformer transformer) {
		TransformerUtils.transformStringTableData(tableData, transformer);
	}

	public static String loadQuery(String fileName) throws IOException
	{
		BufferedReader reader = null;
		try
		{
			reader = new BufferedReader(new FileReader(fileName));
			String line;
			LineBuilder query = new LineBuilder();
			while ((line = reader.readLine()) != null)
				query.append(line);
			return query.toString();
		}
		finally
		{
			Utils.closeResource(reader);
		}
	}

	private static String getConvertedValue(String paramName, String param, List<DBFieldMapping> mapping)
	{
		return mapping != null ? new ConversionSettings(mapping).getConvertedTableValue(paramName, param) : param;
	}

	public static PreparedStatement createStatement(Connection con, Collection<String> parameters, String preparedResult) throws SQLException
	{
		PreparedStatement prep = con.prepareStatement(preparedResult);
		int i = 1;

		for (String statValue : parameters)
		{
			prep.setString(i, statValue);
			i++;
		}

		return prep;
	}

	private static String removeComment(String query)
	{
		int commentInd = -1;
		StringBuilder sb = new StringBuilder(query);
		while ((commentInd = sb.indexOf(COMMENT_BEGIN)) >= 0)
		{
			int commentEnd = sb.indexOf(COMMENT_END);
			if (commentEnd > 0)
				sb.replace(commentInd, commentEnd + COMMENT_END.length(), "");
		}

		return sb.toString();
	}

	public static boolean isConnectException(Throwable e)
	{
		if (e instanceof ConnectException)
			return true;

		if (e instanceof SQLException)
		{
			String msg = e.getMessage();
			if ((msg.equalsIgnoreCase("No more data to read from socket")) || (msg.equalsIgnoreCase("IO Error: Broken pipe")) || (msg.equalsIgnoreCase("Closed Connection")))
				return true;
		}

		if (e.getCause() != null)
			return isConnectException(e.getCause());

		return false;
	}

	public static String getStringFromNumber(Number number)
	{
		if ((number instanceof Byte)
				|| (number instanceof Integer)
				|| (number instanceof Long)
				|| (number instanceof Short))
			return number.longValue()+"";
		else if ((number instanceof Double)
				|| (number instanceof Float))
			return number.doubleValue()+"";
		else if (number instanceof BigDecimal)
			return ((BigDecimal) number).toPlainString();
		else
			return number.doubleValue()+"";
	}
	
	public static String getDbValue(ResultSet rs, String rsColumnName, ObjectToStringTransformer objectTransformer)
			throws SQLException
	{
		Object value = rs.getObject(rsColumnName);
		return objectTransformer.transform(value);
	}
	
	public static String getDbValue(ResultSet rs, String rsColumnName) throws SQLException
	{
		return getDbValue(rs, rsColumnName, OBJECT_TRANSFORMER);
	}

	/**
	 * @deprecated
	 * this method used only for parsing SQL templates, use SQLTemplateParser instead
	 */
	public static String[] getKeysFromQuery(String query)
	{
		List<String> result = new ArrayList<String>();

		String resultQuery = removeComment(query);


		int start = -1;

		for (int i = 0, stringCount = resultQuery.length(); i< stringCount; i++) {
			char curr = resultQuery.charAt(i);
			if (start == -1) {
				if (ArrayUtils.contains(BEGINNER_VARIANT, curr)) {
					start = i;
				}
			} else {
				if (!Character.isLetterOrDigit(curr) && curr != '_') {
					result.add(resultQuery.substring(start + 1, i));
					start = -1;
				}
			}
		}
		if (start != -1) {
			result.add(resultQuery.substring(start + 1));
		}
		return result.toArray(new String[result.size()]);
	}

	/**
	 * @deprecated
	 * this method is too complex, use SQLTemplateParser, ParametrizedQuery, ConversionSettings and TransformerUtils instead <br/>
	 * or use needed methods of SQLUtils: parseSQLTemplate(), createConvertedStringTableData(), applyTransformer()
	 */
	@Deprecated
	public static Pair<PreparedStatement, String> prepareQueryAndText(String query,
	                                                                  String[] keys,
	                                                                  Map<String, String> params,
	                                                                  Connection con,
	                                                                  IValueTransformer transformer,
	                                                                  List<DBFieldMapping> mapping) throws SQLException
	{
		SqlQuery sqlQuery = prepareQuery(query, keys, params, transformer, mapping);
		
		PreparedStatement prep = createStatement(con, sqlQuery.parameters, sqlQuery.stringStatement);
		String queryText = sqlQuery.logStatement;
		log.debug("Using query:" + (Utils.EOL + QUERY_STARTER + Utils.EOL) + queryText + (Utils.EOL + QUERY_ENDER + Utils.EOL));

		return new Pair<>(prep, queryText);
	}

	/**
	 * @deprecated
	 * this method is too complex, use SQLTemplateParser, ParametrizedQuery, ConversionSettings and TransformerUtils instead
	 */
	@Deprecated
	public static SqlQuery prepareQuery(String query,
	                                    String[] keys,
	                                    Map<String, String> params,
	                                    IValueTransformer transformer,
	                                    List<DBFieldMapping> mapping)
	{
		List<String> parameters = new ArrayList<String>();

		String result = removeComment(query);//sb.toString();

		List<String> keyList = Arrays.asList(keys);

		StringBuilder preparedResult = new StringBuilder(result.length());
		StringBuilder loggerResult = new StringBuilder(result.length());

		int start = -1;
		int copyInd = 0;
		String paramName = null;
		int quoteTmp = -1;
		int quoteInd = -1;
		StringBuilder tmpString = null;

		for (int i = 0, stringCount = result.length(); i< stringCount; i++) {
			char curr = result.charAt(i);
			char prev = result.charAt(0);
			if (start == -1) {
				if (ArrayUtils.contains(BEGINNER_VARIANT, curr)) {
					start = i;
				}
			} else {
				if (!Character.isLetterOrDigit(curr) && curr != '_') {
					paramName = result.substring(start + 1, i);
					if (keyList.contains(paramName))
					{
						String p = params.get(paramName);
						if(start>0)
							prev = result.charAt(start-1);
						if (result.charAt(start) == CONVERT_BEGINNER && prev != TABLE_NAME_MARKER) {
							p = getConvertedValue(paramName,p,mapping);
						}
						p = transformer.transform(p);


						if (quoteTmp != -1) {
							if (tmpString == null) {
								tmpString = new StringBuilder();
							}
							tmpString.append(result.substring(quoteTmp, start));
							tmpString.append(p);
							quoteTmp = i;
							preparedResult.append(result.substring(copyInd, quoteInd));
							loggerResult.append(result.substring(copyInd, quoteInd));
							copyInd = quoteInd;
						} else {
							parameters.add(p);
							preparedResult.append(result.substring(copyInd, start)).append("?");
							loggerResult.append(result.substring(copyInd, start)).append("'").append(p).append("'");
							copyInd = i;
						}

					}
					start = -1;
				}
			}

			if (curr == '\'') {
				if (quoteTmp == -1) {
					quoteTmp = i + 1;
					quoteInd = i;
				} else {
					if (tmpString != null) {
						tmpString.append(result.substring(quoteTmp, i));
						parameters.add(tmpString.toString());
						preparedResult.append("?");
						loggerResult.append("'").append(tmpString.toString()).append("'");
						tmpString = null;
						copyInd = i + 1;
					}
					quoteTmp = -1;
					quoteInd = -1;
				}
			}
		}

		if(tmpString != null)
		{
			replaceAll(tmpString, TABLE_NAME_WITH_DOLLAR.toString(), Character.toString(CONVERT_BEGINNER));
			replaceAll(tmpString,TABLE_NAME_WITH_SHARP.toString(),Character.toString(CUSTOM_BEGINNER));
		}

		if (start != -1 && keyList.contains(paramName = result.substring(start + 1))) {
			String p = params.get(paramName);
			if (result.charAt(start) == CONVERT_BEGINNER) {
				p = getConvertedValue(paramName,p,mapping);
			}
			p = transformer.transform(p);
			parameters.add(p);
			preparedResult.append(result, copyInd, start).append("?");
			loggerResult.append(result.substring(copyInd, start)).append("'").append(p).append("'");
		} else {
			preparedResult.append(result.substring(copyInd));
			loggerResult.append(result.substring(copyInd));
		}

		return new SqlQuery(preparedResult.toString(), loggerResult.toString(), parameters);
	}

	public static void replaceAll(StringBuilder builder, String from, String to)
	{
		int index = builder.indexOf(from);
		while (index != -1)
		{
			builder.replace(index, index + from.length(), to);
			index += to.length();
			index = builder.indexOf(from, index);
		}
	}

	/**
	 * @deprecated
	 * this method is too complex, use SQLTemplateParser, ParametrizedQuery, ConversionSettings and TransformerUtils instead
	 */
	public static PreparedStatement prepareQuery(String query, String[] keys,
			Map<String, String> params, Connection con, IValueTransformer transformer, List<DBFieldMapping> mapping) throws SQLException
	{
		return prepareQueryAndText(query, keys, params, con, transformer, mapping).getFirst();
	}



	/**
	 * @deprecated
	 * this method should be called from  {@link ConversionSettings}
	 */
	public static List<DBFieldMapping> loadVerificationMapping(String fileName) throws IOException, NumberFormatException
	{
		return new DBFieldMappingReader().readEntities(new File(ClearThCore.rootRelative(fileName)));
	}

	/**
	 * Method for extraction data from ResultSet
	 * If mapping isn't null, it is used. If mapping is null, data are stored using columns names from ResultSet.
	 * If column name is undefined in the mapping, it is stored by RS's column name too.
	 * @param rs
	 * @param mapping can be null
	 * @param transformer can be null
	 * @return
	 * @throws SQLException
	 * @deprecated
	 * this method is too complex, there is more assumable implementation of table structure. Use DbDataReader, StringTableData, ConversionSettings and TransformerUtils instead
	 */
	public static List<Map<String, String>> resultSetToTable(ResultSet rs, List<DBFieldMapping> mapping,
	                                                         IValueTransformer transformer) throws SQLException
	{
		List<Map<String, String>> table = new ArrayList<Map<String, String>>();
		List<String> rsColumnNames = getColumnNames(rs.getMetaData());
		while (rs.next())
		{
			Map<String, String> row = getResultSetRow(rs, mapping, transformer, rsColumnNames);

			table.add(row);
		}
		return table;
	}

	/**
	 * @deprecated
	 * this is a part of deprecated method
	 */
	public static Map<String, String> getResultSetRow(ResultSet rs, List<DBFieldMapping> mapping, IValueTransformer transformer, List<String> rsColumnNames) throws SQLException
	{
		Map<String, String> row = new LinkedHashMap<String, String>(rsColumnNames.size());

		for (String rsColumnName : rsColumnNames)
		{
			String fieldName = getResultFieldName(rsColumnName, mapping);
			String value = getValue(rs, rsColumnName, mapping, transformer);
			row.put(fieldName, value);
		}
		log.trace("Loaded from DB: {}", row);
		return row;
	}

	/**
	 * @deprecated use ConversionSettings instead
	 */
	private static String getResultFieldName(String resultSetColumnName, List<DBFieldMapping> mapping)
	{
		return mapping == null ? resultSetColumnName : getMatrixFieldName(resultSetColumnName, mapping);
	}

	/**
	 * @deprecated use ConversionSettings instead
	 */
	private static String getValue(ResultSet rs, String rsColumnName, List<DBFieldMapping> mapping,
	                               IValueTransformer transformer) throws SQLException
	{
		String value = getDbValue(rs, rsColumnName);
		if (mapping != null)
			value = convertDbValue(rsColumnName, value, mapping);
		if (transformer != null)
			value = transformer.transform(value);
		return value;
	}


	/**
	 * Method for extraction columns names from ResultSet
	 * @param metaData
	 * @return list of columns names
	 * @throws SQLException
	 */
	public static List<String> getColumnNames(ResultSetMetaData metaData) throws SQLException
	{
		int columnsCount = metaData.getColumnCount();
		List<String> columns = new ArrayList<String>(columnsCount);
		for (int i = 1; i <= columnsCount; i++)
		{
			columns.add(metaData.getColumnLabel(i));
		}
		return columns;
	}

	/**
	 * Method for conversion from ResultSet's DB column name to matrix parameter name for reports
	 * @param dbColumnName column name from ResultSet
	 * @param mapping
	 * @return "Matrix Filed" from mapping. If conversion doesn't exist, returns dbColumnName
	 * @deprecated use ConversionSettings instead
	 */
	public static String getMatrixFieldName(String dbColumnName, List<DBFieldMapping> mapping)
	{
		for (DBFieldMapping fieldMapping : mapping)
		{
			if (StringUtils.equals(dbColumnName, fieldMapping.getDestField()))
				return fieldMapping.getSrcField();
		}
		return dbColumnName;
	}

	/**
	 * Conversion from DB value to user-readable matrix value
	 * @param dbColumnName name of ResultSet column
	 * @param dbValue value from RS
	 * @param mapping
	 * @return
	 * @deprecated use ConversionSettings instead
	 */
	public static String convertDbValue(String dbColumnName, String dbValue, List<DBFieldMapping> mapping)
	{
		return mapping != null ? new ConversionSettings(mapping).getConvertedDBValue(dbColumnName, dbValue) : dbValue;
	}

	/**
	 * @deprecated substituted by ParametrizedQuery
	 */
	@Deprecated
	public static class SqlQuery
	{
		private final String stringStatement;
		private final String logStatement;
		private final Collection<String> parameters;

		private SqlQuery(String stringStatement, String logStatement, Collection<String> parameters)
		{
			this.stringStatement = stringStatement;
			this.logStatement = logStatement;
			this.parameters = parameters;
		}

		public String getStringStatement()
		{
			return stringStatement;
		}

		public String getLogStatement()
		{
			return logStatement;
		}

		public Collection<String> getParameters()
		{
			return parameters;
		}

		@Override
		public String toString()
		{
			return "SqlQuery{" +
					"stringStatement='" + stringStatement + '\'' +
					", logStatement='" + logStatement + '\'' +
					", parameters=" + parameters +
					'}';
		}
	}
}
