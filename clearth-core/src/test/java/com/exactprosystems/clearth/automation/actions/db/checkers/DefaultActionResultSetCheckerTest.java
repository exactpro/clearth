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
package com.exactprosystems.clearth.automation.actions.db.checkers;

import static com.exactprosystems.clearth.ResultTestUtils.*;
import static com.exactprosystems.clearth.automation.report.results.DefaultResult.failed;
import static com.exactprosystems.clearth.automation.report.results.DefaultResult.passed;
import static com.exactprosystems.clearth.utils.CollectionUtils.map;
import static com.exactprosystems.clearth.utils.CollectionUtils.setOf;
import static com.exactprosystems.clearth.utils.FileOperationUtils.resourceToAbsoluteFilePath;
import static java.lang.String.format;
import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.FileNotFoundException;
import java.sql.*;
import java.util.Map;
import java.util.Set;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.exactprosystems.clearth.BasicTestNgTest;
import com.exactprosystems.clearth.automation.report.results.DetailedResult;

public class DefaultActionResultSetCheckerTest extends BasicTestNgTest
{
	private static final String INIT_DB_SCRIPT_PATH = "DefaultActionResultSetCheckerTest/init.sql";
	private static final String URL_PATTERN = "jdbc:h2:mem:test;INIT=runscript from '%s'";
	
	// 'trades' table columns
	private static final String TRADE_ID = "TRADE_ID";
	private static final String BUY_FIRM = "BUY_FIRM";
	private static final String SELL_FIRM = "SELL_FIRM";
	private static final String INSTRUMENT_ID = "INSTRUMENT_ID";
	private static final String QUANTITY = "QUANTITY";
	private static final String PRICE = "PRICE";
	private static final String CURRENCY = "CURRENCY";
	
	private Connection connection;
	
	
	@BeforeClass
	void init() throws FileNotFoundException, SQLException
	{
		String initScriptPath = resourceToAbsoluteFilePath(INIT_DB_SCRIPT_PATH)
				.replace("\\", "/");
		String url = format(URL_PATTERN, initScriptPath);

		//noinspection CallToDriverManagerGetConnection
		connection = DriverManager.getConnection(url);
	}
	
	@AfterClass
	void dispose() throws SQLException
	{
		connection.close();
	}



	@DataProvider
	Object[][] createParameters()
	{
		return new Object[][]
				{
						// sqlQuery, inputParams, outputParamNames, expectedRecordsCount, expectedResult
						
						// passed comparison:
						{
								"select * from TRADES where TRADE_ID = '0000000001'",
								map(TRADE_ID, "0000000001", BUY_FIRM, "AAA", SELL_FIRM, "BBB",
										INSTRUMENT_ID, "ABC0000001", QUANTITY, "35", PRICE, "45000.35", 
										CURRENCY, "EUR"),
								setOf(QUANTITY, PRICE),
								1,
								new ActionSqlResult(
										containerResult(detailedResult(
												passedDetail(TRADE_ID, "0000000001"),
												passedDetail(BUY_FIRM, "AAA"),
												passedDetail(SELL_FIRM, "BBB"),
												passedDetail(INSTRUMENT_ID, "ABC0000001"),
												passedDetail(QUANTITY, "35"),
												passedDetail(PRICE, "45000.35"),
												passedDetail(CURRENCY, "EUR")
										)), 
										map(QUANTITY, "35", PRICE, "45000.35"))
						},
						{
								"select BUY_FIRM, SELL_FIRM, INSTRUMENT_ID from TRADES where TRADE_ID = '0000000001'",
								map(BUY_FIRM, "AAA", SELL_FIRM, "BBB"),
								singleton(INSTRUMENT_ID),
								1,
								new ActionSqlResult(
										containerResult(detailedResult(
												passedDetail(BUY_FIRM, "AAA"),
												passedDetail(SELL_FIRM, "BBB"),
												infoDetail(INSTRUMENT_ID, "ABC0000001")
										)), 
										singletonMap(INSTRUMENT_ID, "ABC0000001"))
						},
						{
								"select INSTRUMENT_ID from TRADES where TRADE_ID = '0000000001'",
								singletonMap(INSTRUMENT_ID, "ABC0000001"), null, 1,
								new ActionSqlResult(
										containerResult(detailedResult(
												passedDetail(INSTRUMENT_ID, "ABC0000001")
										)), 
										emptyMap())
						},
						{
								"select INSTRUMENT_ID from TRADES where TRADE_ID = '0000000001'",
								emptyMap(), null, 1,
								new ActionSqlResult(
										containerResult(detailedResult(
												infoDetail(INSTRUMENT_ID, "ABC0000001")
										)),
										emptyMap())
						},

						// failed comparison:
						{
								"select QUANTITY, PRICE, CURRENCY from TRADES where TRADE_ID = '0000000001'",
								map(QUANTITY, "45", PRICE, "25000.75", CURRENCY, "USD"),
								setOf(QUANTITY, PRICE, CURRENCY),
								1,
								new ActionSqlResult(
										containerResult(detailedResult(
												failedDetail(QUANTITY, "45", "35"),
												failedDetail(PRICE, "25000.75", "45000.35"),
												failedDetail(CURRENCY, "USD", "EUR")
										)), 
										map(QUANTITY, "35", PRICE, "45000.35", CURRENCY, "EUR"))
						},
						{
								"select QUANTITY, PRICE, CURRENCY from TRADES where TRADE_ID = '0000000001'",
								map(QUANTITY, "35", PRICE, "25000.75"),
								setOf(QUANTITY, PRICE, CURRENCY),
								1,
								new ActionSqlResult(
										containerResult(detailedResult(
												passedDetail(QUANTITY, "35"),
												failedDetail(PRICE, "25000.75", "45000.35"),
												infoDetail(CURRENCY, "EUR")
										)),
										map(QUANTITY, "35", PRICE, "45000.35", CURRENCY, "EUR"))
						},
						
						// empty ResultSet:
						{
								"select * from TRADES where QUANTITY = 0",
								singletonMap(CURRENCY, "EUR"), null, 1,
								new ActionSqlResult(failed("Query result is empty."), emptyMap())
						},
						{
								"select * from TRADES where TRADE_ID = '9999999999'",
								emptyMap(), null, 0,
								new ActionSqlResult(passed("Query result is empty"), emptyMap())
						},

						// passed multiple rows comparison:
						{
								"select CURRENCY, TRADE_ID from TRADES where BUY_FIRM = 'AAA'",
								singletonMap(CURRENCY, "EUR"), null, 2,
								new ActionSqlResult(containerResult(
										detailedResult(
												passedDetail(CURRENCY, "EUR"),
												infoDetail(TRADE_ID, "0000000001")),
										detailedResult(
												passedDetail(CURRENCY, "EUR"),
												infoDetail(TRADE_ID, "0000000002")
										)
								), emptyMap())
						},
						
						// failed multiple rows comparison
						{
								"select SELL_FIRM from TRADES where BUY_FIRM = 'AAA'",
								singletonMap(SELL_FIRM, "CCC"), null, 2,
								new ActionSqlResult(containerResult(
										detailedResult(failedDetail(SELL_FIRM, "CCC", "BBB")),
										detailedResult(passedDetail(SELL_FIRM, "CCC"))
								), emptyMap())
						},
						
						// less rows than expected
						{
								"select TRADE_ID from TRADES where SELL_FIRM = 'BBB'",
								emptyMap(), null, 2,
								new ActionSqlResult(containerResult(false, 
										"Query result contains less rows than expected. Expected: 2, actual: 1.",
										detailedResult(infoDetail(TRADE_ID, "0000000001"))										
								), emptyMap())
						},
						
						// more rows than expected
						{
								"select CURRENCY from TRADES where BUY_FIRM = 'DDD'",
								singletonMap(CURRENCY, "USD"), null, 2,
								new ActionSqlResult(containerResult(false,
										"Query result contains more rows than expected. Expected: 2, actual: 3.",
										detailedResult(passedDetail(CURRENCY, "USD")),
										detailedResult(passedDetail(CURRENCY, "USD")),
										detailedResult(failedDetail(CURRENCY, "USD", "CHF"))
								), emptyMap())
						},
						{
								"select TRADE_ID from TRADES where BUY_FIRM = 'DDD'",
								emptyMap(), null, 2,
								new ActionSqlResult(containerResult(false,
										"Query result contains more rows than expected. Expected: 2, actual: 3.",
										detailedResult(infoDetail(TRADE_ID, "0000000003")),
										detailedResult(infoDetail(TRADE_ID, "0000000004")),
										detailedResult(infoDetail(TRADE_ID, "0000000005"))
								), emptyMap())
						},
						{
								"select TRADE_ID from TRADES where CURRENCY = 'CHF'",
								emptyMap(), null, 0,
								new ActionSqlResult(containerResult(false,
										"Query result contains more rows than expected. Expected: 0, actual: 1.",
										detailedResult(infoDetail(TRADE_ID, "0000000005"))
								), emptyMap())
						},
						{
								"select TRADE_ID from TRADES",
								emptyMap(), null, 1,
								new ActionSqlResult(containerResult(false,
										"Query result contains more rows than expected. " +
												"Expected: 1, actual: more than 11.",
										detailedResult(infoDetail(TRADE_ID, "0000000001")),
										detailedResult(infoDetail(TRADE_ID, "0000000002")),
										detailedResult(infoDetail(TRADE_ID, "0000000003")),
										detailedResult(infoDetail(TRADE_ID, "0000000004")),
										detailedResult(infoDetail(TRADE_ID, "0000000005")),
										detailedResult(infoDetail(TRADE_ID, "0000000006")),
										detailedResult(infoDetail(TRADE_ID, "0000000007")),
										detailedResult(infoDetail(TRADE_ID, "0000000008")),
										detailedResult(infoDetail(TRADE_ID, "0000000009")),
										detailedResult(infoDetail(TRADE_ID, "0000000010")),
										detailedResult(infoDetail(TRADE_ID, "0000000011"))
								), emptyMap())
						},
						{
								"select BUY_FIRM, SELL_FIRM, CURRENCY from TRADES where TRADE_ID = '0000000001'",
								map(BUY_FIRM, "AAA", SELL_FIRM, "BBB"),
								setOf(CURRENCY, "TRADE_DATE", "ISD"),
								1,
								new ActionSqlResult(containerResult(false,
										"The following output parameters are missed in query result: TRADE_DATE, ISD",
										detailedResult(passedDetail(BUY_FIRM, "AAA"),
												passedDetail(SELL_FIRM, "BBB"),
												infoDetail(CURRENCY, "EUR"))
								), singletonMap(CURRENCY, "EUR"))
						}
				};
	}

	@Test(dataProvider = "createParameters")
	void executeTest(String sqlQuery, Map<String, String> inputParams, Set<String> outputParamNames, 
	                 int expectedRecordsCount, ActionSqlResult expectedResult) throws SQLException
	{
		ActionResultSetChecker checker = new DefaultActionResultSetChecker(inputParams, outputParamNames,
				expectedRecordsCount);

		ActionSqlResult actualResult;
		try (Statement statement = connection.createStatement();
		     ResultSet resultSet = statement.executeQuery(sqlQuery))
		{
			actualResult = checker.check(resultSet);
		}
		
		assertThat(actualResult)
				.usingRecursiveComparison()
				.ignoringOverriddenEqualsForTypes(DetailedResult.class)
				.ignoringFields("result.successManual", "result.failReasonManual")
				.isEqualTo(expectedResult);
	}
}
