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

package com.exactprosystems.clearth.automation.expressions;

import com.exactprosystems.clearth.utils.Pair;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.testng.Assert.*;

public class ActionReferenceFinderTest
{

	@DataProvider(name = "expressions")
	Object[][] createExpressions()
	{
		return new Object[][]
				{
						{"@{id_615.messageID}", set(ref("id_615", "messageID"))},
						{"SomeTextBefore@{id_615.messageID}SomeTextAfter", set(ref("id_615", "messageID"))},
						{"SomeText.Before@{id_615.messageID}SomeText.After", set(ref("id_615", "messageID"))},
						{"/tools/data/@{id1.TradeDate}/report_@{id2.BusinessDate}.csv",
								set(ref("id1", "TradeDate"),
										ref("id2", "BusinessDate"))},
						{"@{round(round(mul(id_3.TradePrice, id13.TradeQuantity),2),2)}",
								set(ref("id_3", "TradePrice"),
										ref("id13", "TradeQuantity"))},
						{":22H::@{id_9.Currency}/@{mul(id_3.TradePrice, id13.TradeQuantity)}",
								set(ref("id_9", "Currency"),
										ref("id_3", "TradePrice"),
										ref("id13", "TradeQuantity"))},
						{"@{12.Currency}", set(ref("12", "Currency"))},
						{"@{id1.Compare2Values}", set(ref("id1", "Compare2Values"))},
						{"@{id1.Value1}", set(ref("id1", "Value1"))},
						{"@{id1._Qty}", set(ref("id1", "_Qty"))},
						{"@{id1._123}", set(ref("id1", "_123"))},
						{"@{id9.$env}", set(ref("id9", "$env"))},
						{"@{id8.$1}", set(ref("id8", "$1"))},
						{"@{format(id1.TradeDate,'MM.yyyy')}", set(ref("id1", "TradeDate"))},
						{"@{getTradeTimeUTC(id89.TradeSource,time(3),'HH:mm:ss.SSS000')}",
								set(ref("id89", "TradeSource"))},
						{"@{mul(mul(id368.NetPosition,id_308.ValuationPrice),0.0004)}",
								set(ref("id368", "NetPosition"),
										ref("id_308", "ValuationPrice"))},
						{"@{mul(mul(id368.NetPosition,id_308.ValuationPrice),0.0004B)}",
								set(ref("id368", "NetPosition"),
										ref("id_308", "ValuationPrice"))},
						{"@{id_16.out.MessageIsFound}", set(ref("id_16", "MessageIsFound"))},
						{"@{id_16.in.MessageIsFound}", set(ref("id_16", "MessageIsFound"))},
						{"ACTU/USD@{addComma(id_2.TradePrice.toString())}", set(ref("id_2", "TradePrice"))},
						{"@{id3.Params.split(',')[0]}", set(ref("id3", "Params"))},
						{"@{id3.in.Params.split(',')[0]}", set(ref("id3", "Params"))},
						{"@{id4.action.failReason}", set(ref("id4", "failReason"))},
						{"@{('APKE'.equals('ECLR') || 'APKE'.equals('SCOM')) ? 'SPRI':s1_id122.ActualSettlementDate}",
								set(ref("s1_id122", "ActualSettlementDate"))},
						{"@{a.b}", set(ref("a", "b"))},
						{"_@{a.b}_", set(ref("a", "b"))},
						{"@{a.b}_@{c.d}_@{e.f}", set(ref("a", "b"),
								ref("c", "d"), ref("e", "f"))},
						{"@{'@{@{@{{}'  id1.tmp  '@}}@{}a.b'  }", set(ref("id1", "tmp"))},
						{"@{pattern('[A-Z]'+id1.Character+'[A-Z0-9]{3} '+id2.Code)}", set(ref("id1", "Character"), ref("id2", "Code"))},
						{"@{SELECT * FROM tablename where column=@{id1.Param1} and row='@{a.b}' and id=@{id2.Param1}}", 
							set(ref("id1", "Param1"), ref("id2", "Param1"))}
				};
	}

	@Test(dataProvider = "expressions", timeOut = 1000)
	void checkFindAll(String text, Set<Pair<String, String>> expResult)
	{
		ActionReferenceFinder finder = new ActionReferenceFinder(text);
		assertEquals(finder.findAll(), expResult);
	}

	@DataProvider(name = "empty")
	Object[][] createEmptyValues()
	{
		return new Object[][]
				{
						{""},
						{" \t"},
						{"@{pattern('CUR.EUR')}"},
						{"@{id_615.messageID"},
						{"@{id_615....messageID}"},
						{"id_615.messageID}"},
						{"@{123.09+12}"}
				};
	}

	@Test(dataProvider = "empty", timeOut = 1000)
	void checkEmptyText(String text)
	{
		ActionReferenceFinder finder = new ActionReferenceFinder(text);
		assertTrue(isEmpty(finder.findAll()));
	}


	@SafeVarargs
	private static Set<Pair<String, String>> set(Pair<String, String>... refs)
	{
		return new LinkedHashSet<>(Arrays.asList(refs));
	}

	private static Pair<String, String> ref(String actionId, String paramName)
	{
		return new Pair<>(actionId, paramName);
	}
}
