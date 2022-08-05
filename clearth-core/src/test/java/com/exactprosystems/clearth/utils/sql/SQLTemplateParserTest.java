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

import java.sql.SQLException;

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
						},
						{ // case with /* */
								"select * from trades\\# where/* 1=1 and */ venue = #venue",
								new ParametrizedQuery("select * from trades# where  venue = ?",
										singletonList("venue"))
						},
						{ // case with /* \n */
								"select * from trades\\# where/* 1=1 and\n asdf */ venue = #venue",
								new ParametrizedQuery("select * from trades# where  venue = ?",
										singletonList("venue"))
						},
						{ // case with /* \n '' \n */
								"select * from trades\\# where/* 1=1 and\n 'must not appear' \n */ venue = #venue",
								new ParametrizedQuery("select * from trades# where  venue = ?",
										singletonList("venue"))
						},
						{ // case with --
								"select * from trades where buy_firm = #Firm and buy_venue = #Venue" +
										"--and sell_firm = #Firm\n and sell_venue = #Venue",
								new ParametrizedQuery("select * from trades where buy_firm = ? " +
										"and buy_venue = ?\n and sell_venue = ?",
										asList("Firm", "Venue", "Venue"))
						},
						{ // case with -- \r \n (space matters)
								"select * from trades where buy_firm = #Firm and buy_venue = #Venue" +
										"--and sell_firm = #Firm\r \n and sell_venue = #Venue",
								new ParametrizedQuery("select * from trades where buy_firm = ? " +
										"and buy_venue = ?\r \n and sell_venue = ?",
										asList("Firm", "Venue", "Venue"))
						},
						{ // case with -- \r\n
								"select * from trades where buy_firm = #Firm and buy_venue = #Venue" +
										"--and sell_firm = #Firm\r\n and sell_venue = #Venue",
								new ParametrizedQuery("select * from trades where buy_firm = ? " +
										"and buy_venue = ?\r\n and sell_venue = ?",
										asList("Firm", "Venue", "Venue"))
						},
						{ // case with -- \r without \n
								"select * from trades where buy_firm = #Firm and buy_venue = #Venue" +
										"--and sell_firm = #Firm\r and sell_venue = #Venue",
								new ParametrizedQuery("select * from trades where buy_firm = ? " +
										"and buy_venue = ?\r and sell_venue = ?",
										asList("Firm", "Venue", "Venue"))
						},
						{ // case with -- at the end
								"select * from trades where buy_firm = #Firm and buy_venue = #Venue" +
										"--and sell_firm = #Firm and sell_venue = #Venue",
								new ParametrizedQuery("select * from trades where buy_firm = ? " +
										"and buy_venue = ?",
										asList("Firm", "Venue"))
						},
						{ // case with -- \r
								"select * from trades where buy_firm = #Firm and buy_venue = #Venue" +
										"--and sell_firm = #Firm\r and sell_venue = #Venue",
								new ParametrizedQuery("select * from trades where buy_firm = ? " +
										"and buy_venue = ?\r and sell_venue = ?",
										asList("Firm", "Venue", "Venue"))
						},
						{ // case with -- \r\r
								"select * from trades where buy_firm = #Firm and buy_venue = #Venue" +
										"--and sell_firm = #Firm\r\r and sell_venue = #Venue",
								new ParametrizedQuery("select * from trades where buy_firm = ? " +
										"and buy_venue = ?\r\r and sell_venue = ?",
										asList("Firm", "Venue", "Venue"))
						},
						{ // case with -- \u0085
								"select * from trades where buy_firm = #Firm and buy_venue = #Venue" +
										"--and sell_firm = #Firm\u0085 and sell_venue = #Venue",
								new ParametrizedQuery("select * from trades where buy_firm = ? " +
										"and buy_venue = ?\u0085 and sell_venue = ?",
										asList("Firm", "Venue", "Venue"))
						},
						{ // case with -- \u2028
								"select * from trades where buy_firm = #Firm and buy_venue = #Venue" +
										"--and sell_firm = #Firm\u2028 and sell_venue = #Venue",
								new ParametrizedQuery("select * from trades where buy_firm = ? " +
										"and buy_venue = ?\u2028 and sell_venue = ?",
										asList("Firm", "Venue", "Venue"))
						},
						{ // case with -- \u2029
								"select * from trades where buy_firm = #Firm and buy_venue = #Venue" +
										"--and sell_firm = #Firm\u2029 and sell_venue = #Venue",
								new ParametrizedQuery("select * from trades where buy_firm = ? " +
										"and buy_venue = ?\u2029 and sell_venue = ?",
										asList("Firm", "Venue", "Venue"))
						},
						{ // case with --/* and --*/
								"select * from trades where buy_firm = #Firm --/*\nand buy_venue = #Venue --*/\n" +
										"and sell_firm = #Firm and sell_venue = #Venue",
								new ParametrizedQuery("select * from trades where buy_firm = ? \n" +
										"and buy_venue = ? \nand sell_firm = ? and sell_venue = ?",
										asList("Firm", "Venue", "Firm", "Venue"))
						},
						{ // case with --/* without */
								"select * from trades where buy_firm = #Firm --/*\nand buy_venue = #Venue " +
										"and sell_firm = #Firm and sell_venue = #Venue",
								new ParametrizedQuery("select * from trades where buy_firm = ? " +
										"\nand buy_venue = ? and sell_firm = ? and sell_venue = ?",
										asList("Firm", "Venue", "Firm", "Venue"))
						},
						{ // case with --*/ without /*
								"select * from trades where buy_firm = #Firm and buy_venue = #Venue --*/\n" +
										"and sell_firm = #Firm and sell_venue = #Venue",
								new ParametrizedQuery("select * from trades where buy_firm = ? " +
										"and buy_venue = ? \nand sell_firm = ? and sell_venue = ?",
										asList("Firm", "Venue", "Firm", "Venue"))
						},
						{ // case with '/**/' 
								"select * from trades where venue='#SourceVenue' and side='/*must appear*/'",
								new ParametrizedQuery("select * from trades where venue=? and side='/*must appear*/'",
										asList("SourceVenue"))
						},
						{ // case with '/*\n*/' 
								"select * from trades where venue='#SourceVenue' and side='/*must\nappear*/'",
								new ParametrizedQuery("select * from trades where venue=? and side='/*must\nappear*/'",
										asList("SourceVenue"))
						},
						{ // case with ' \' ' 
								"select * from trades where venue='#SourceVenue' and side='/*must\\' appear*/'",
								new ParametrizedQuery("select * from trades where venue=? and side='/*must\\' appear*/'",
										asList("SourceVenue"))
						},
						{ // case with "/**/"
								"select * from trades where venue='#SourceVenue' and side=\"/*must appear*/\"",
								new ParametrizedQuery("select * from trades where venue=? and side=\"/*must appear*/\"",
										asList("SourceVenue"))
						},
						{ // case with " \" "
								"select * from trades where venue='#SourceVenue' and side=\"/*must\\\" appear*/\"",
								new ParametrizedQuery("select * from trades where venue=? and side=\"/*must\\\" appear*/\"",
										asList("SourceVenue"))
						},
						{
								"select 1",
								new ParametrizedQuery("select 1",
										null)
						},
						{ // must be valid SQL (while "select1" is not)
								"select/* asdf */1",
								new ParametrizedQuery("select 1",
										null)
						},
						{
								"--",
								new ParametrizedQuery("",
										null)
						},
						{
								"--asdf",
								new ParametrizedQuery("",
										null)
						},
						{
								"'",
								new ParametrizedQuery("'",
										null)
						},
						{
								"/**/",
								new ParametrizedQuery(" ",
										null)
						},
						{
								"",
								new ParametrizedQuery("",
										null)
						},
						{
								"/*",
								new ParametrizedQuery("/*",
										null)
						},
				};
	}

	@Test(dataProvider = "createParameters")
	public void testParseParametrizedQueryTemplate(String queryTemplate, ParametrizedQuery expectedQuery) throws SQLException
	{
		SQLTemplateParser parser = new SQLTemplateParser();
		ParametrizedQuery actualQuery = parser.parseParametrizedQueryTemplate(queryTemplate);
		assertEquals(actualQuery, expectedQuery);
	}
}