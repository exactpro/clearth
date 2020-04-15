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

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.testng.Assert.*;

public class SQLTemplateParserTest
{
	@DataProvider
	public Object[][] createParameters()
	{
		return new Object[][]
				{
						{
								"select * from trades " +
										"where venue = '#SourceVenue' and side = '$side' and trade_date in ('@dates')",
								new ParametrizedQuery("select * from trades " +
										"where venue = ? and side = ? and trade_date in (?)",
										asList("SourceVenue", "side", "dates"))
						},
						{
								"select * from trades " +
										"where venue = #SourceVenue and side = $side and trade_date in (@dates)",
								new ParametrizedQuery("select * from trades " +
										"where venue = ? and side = ? and trade_date in (?)",
										asList("SourceVenue", "side", "dates"))
						},
						{
								"select * from trades where venue='#SourceVenue' and side=$side",
								new ParametrizedQuery("select * from trades where venue=? and side=?",
										asList("SourceVenue", "side"))
						},
						{
								"select * from trades where buy_firm = #Firm and buy_venue = #Venue " +
										"and sell_firm = #Firm and sell_venue = #Venue",
								new ParametrizedQuery("select * from trades where buy_firm = ? " +
										"and buy_venue = ? and sell_firm = ? and sell_venue = ?",
										asList("Firm", "Venue", "Firm", "Venue"))
						},
						{
								"select * from \\#trades where venue = #venue",
								new ParametrizedQuery("select * from #trades where venue = ?", 
										singletonList("venue"))
						},
						{
								"select * from \\$trades where venue = $venue",
								new ParametrizedQuery("select * from $trades where venue = ?", 
										singletonList("venue"))
						},
						{
								"select * from \\@trades where venue = #venue",
								new ParametrizedQuery("select * from @trades where venue = ?",
										singletonList("venue"))
						},
						{
								"select * from v\\#session where username = #User",
								new ParametrizedQuery("select * from v#session where username = ?",
										singletonList("User"))
						},
						{
								"select * from v\\$session where username = $User",
								new ParametrizedQuery("select * from v$session where username = ?",
										singletonList("User"))
						},
						{
								"select * from v\\@session where username = #User",
								new ParametrizedQuery("select * from v@session where username = ?",
										singletonList("User"))
						},
						{
								"select * from trades\\# where venue = #venue",
								new ParametrizedQuery("select * from trades# where venue = ?",
										singletonList("venue"))
						}
				};
	}
	
	@Test(dataProvider = "createParameters")
	public void testParseParametrizedQueryTemplate(String queryTemplate, ParametrizedQuery expectedQuery) 
	{
		SQLTemplateParser parser = new SQLTemplateParser();
		ParametrizedQuery actualQuery = parser.parseParametrizedQueryTemplate(queryTemplate);
		assertEquals(actualQuery, expectedQuery);
	}
}