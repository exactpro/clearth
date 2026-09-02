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

package com.exactprosystems.clearth.woodpecker.initialdata.tables.internal;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.*;

import static com.exactprosystems.clearth.woodpecker.initialdata.tables.internal.TreeIndexTestUtils.*;
import static com.exactprosystems.clearth.woodpecker.initialdata.tables.internal.TreeIndexTestUtils.node;
import static com.exactprosystems.clearth.woodpecker.initialdata.tables.internal.TreeIndexTestUtils.rowNums;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.testng.Assert.*;

public class TreeIndexTest
{
	private static final TreeIndex INDEX =
			new TreeIndex(asList("Firm", "Type"),
					root(13,
							subNodes(
									"F1", node(
											rowNums(0, 1, 2, 3, 4, 5),
											subNodes(
													"Client", node(rowNums(0)),
													"House", node(rowNums(1, 3)),
													"Bank", node(rowNums(2, 5)),
													"CCP", node(rowNums(4))
											)),
									"F2", node(
											rowNums(6, 7, 8),
											subNodes(
													"Client", node(rowNums(7)),
													"House", node(rowNums(6)),
													"CCP", node(rowNums(8))
											)),
									"F3", node(
											rowNums(9, 10, 11, 12),
											subNodes(
													"Client", node(rowNums(9, 11)),
													"House", node(rowNums(12)),
													"Bank", node(rowNums(10))
											))
							)
					));
	
	
	@DataProvider(name = "forCheckSearch")
	public Object[][] dataForCheckSearch()
	{
		return new Object[][]
				{
						{
								query("Firm", eq("F3"), "Type", eq("Client")),
								rowNums(9, 11)
						},
						{
								query("Firm", notEq("F1"), "Type", notEq("House")),
								rowNums(7, 8, 9, 10, 11)
						},
						{
								query("Firm", in("F2", "F3"), "Type", in("Bank", "CCP")),
								rowNums(8, 10)
						},
						{
								query("Firm", notIn("F2", "F3"), "Type", notIn("Client", "CCP")),
								rowNums(1, 2, 3, 5)
						},
						{
								query("Firm", notEq("F2"), "Type", eq("CCP")),
								rowNums(4)
						},
						{
								query("Firm", notEq("F3"), "Type", notIn("Client", "House", "CCP")),
								rowNums(2, 5)
						},
						{
								query("Firm", in("F2", "F3"), "Type", eq("Bank")),
								rowNums(10)
						}
				};
	}
	
	@Test(dataProvider = "forCheckSearch", invocationCount = 10)
	public void checkSearch(Map<String, KeyCondition> query, List<Integer> possibleRowNums) throws Exception
	{
		NonRootIndexNode resultNode = (NonRootIndexNode)INDEX.find(query);
		assertNotNull(resultNode);
		assertTrue(possibleRowNums.containsAll(resultNode.getRowNums()),
				format("Actual rowNums: %s. Possible rowNums: %s.", 
						resultNode.getRowNums(), possibleRowNums));
	}
}
