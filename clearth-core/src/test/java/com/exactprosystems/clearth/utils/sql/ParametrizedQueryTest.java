package com.exactprosystems.clearth.utils.sql;

import com.exactprosystems.clearth.utils.Utils;
import org.apache.commons.lang.StringUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.sql.*;
import java.util.Arrays;
import java.util.Map;

import static com.exactprosystems.clearth.utils.CollectionUtils.mapOf;

public class ParametrizedQueryTest extends Assert
{
	static String[] paramValues = {"A", "B", "C" , "D" , "E", "F" , "G"};
	static String createDbTemplate = "CREATE TABLE T(PARAM TEXT);" +
			"INSERT INTO T values ('A'),('B'),('C'),('D'),('E'),('F'),('G'),('H');";
	
	static String singleParamName = "SingleParam", multiParamName = "MultiParam";
	
	static String
			queryTemplate = "SELECT * FROM T WHERE T.PARAM IN ('@"+multiParamName+"') AND T.PARAM != '#"+singleParamName+"'";
	static String expectedTemplate = "SELECT * FROM T WHERE T.PARAM IN (%s) AND T.PARAM != ?";
	
	SQLTemplateParser sqlTemplateParser;
	Connection connection;

	@DataProvider
	public Object[][] getTestData()
	{
		int expResultRows = paramValues.length;
		int expParamsNumber = expResultRows+1;
		//0 - ParametersMap
		//1 - Expected number of params 
		//2 - Expected number of result rows 
		return new Object[][]
				{
						{
							mapOf(multiParamName, paramValues, 
									singleParamName, "H"), 
								expParamsNumber, expResultRows
						},
						{
							mapOf(multiParamName, StringUtils.join(paramValues, ','),
									singleParamName, "H"),
								expParamsNumber, expResultRows
						},
						{
							mapOf(multiParamName, Arrays.asList(paramValues),
									singleParamName, "H"),
								expParamsNumber, expResultRows
						}
		};
	}

	@BeforeClass
	public void init() throws SQLException
	{
		sqlTemplateParser = new SQLTemplateParser();
		connection = prepareConnection();
	}

	@AfterClass
	public void afterClass()
	{
		Utils.closeResource(connection);
	}

	private Connection prepareConnection() throws SQLException
	{
		String url = "jdbc:sqlite::memory:";
		Connection connection = DriverManager.getConnection(url);
		try (Statement statement = connection.createStatement())
		{
			statement.executeUpdate(createDbTemplate);
		}

		return connection;
	}

	@Test(dataProvider = "getTestData")
	public void testParseParametrizedQueryTemplate(Map<String, Object> params, int expParamsNumber, int expResultRowsNumber) throws SQLException
	{
		ParametrizedQuery parametrizedQuery = sqlTemplateParser.parseParametrizedQueryTemplate(queryTemplate);
		try (PreparedStatement preparedStatement = parametrizedQuery.createPreparedStatement(connection, params);
			 ResultSet resultSet = preparedStatement.executeQuery();)
		{
			int resultCount = 0;
			while (resultSet.next())
				resultCount++;

			String expected = String.format(expectedTemplate, StringUtils.repeat("?", ",", expParamsNumber-1));
			//Checking the query text
			assertEquals(parametrizedQuery.getQuery(), expected);
			//Checking the number of parameters
			assertEquals(preparedStatement.getParameterMetaData().getParameterCount(), expParamsNumber);
			//Checking the number of rows in resultSet
			assertEquals(resultCount, expResultRowsNumber);
		}
	}
}