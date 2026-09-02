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

import com.exactprosystems.clearth.utils.tabledata.StringTableData;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;

import static com.exactprosystems.clearth.woodpecker.initialdata.tables.internal.TreeIndexTestUtils.*;
import static java.util.Arrays.asList;
import static org.testng.Assert.*;

/**
 * 25 October 2018
 */
public class TreeIndexBuilderTest
{
	private static final StringTableData TABLE = table(new String[][]
			{
					{"Firm",    "Type",     "Account"   },
					
					{"F1",      "Client",   "a1"        },
					{"F1",      "House",    "a2"        },
					{"F1",      "Bank",     "a3"        },
					{"F1",      "House",    "a4"        },
					{"F1",      "CCP",      "a5"        },
					{"F1",      "Bank",     "a6"        },
					
					{"F2",      "House",    "a7"        },
					{"F2",      "Client",   "a8"        },
					{"F2",      "CCP",      "a9"        },
					
					{"F3",      "Client",   "a10"       },
					{"F3",      "Bank",     "a11"       },
					{"F3",      "Client",   "a12"       },
					{"F3",      "House",    "a13"       }
			});

	@DataProvider(name = "validData")
	public Object[][] createData()
	{
		return new Object[][]
				{
						{
								asList("Firm", "Type"),

								root(TABLE.size(),
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
								)
						},
						{
								asList("Type", "Firm"),

								root(TABLE.size(),
										subNodes(
												"Client", node(
														rowNums(0, 7, 9, 11),
														subNodes(
																"F1", node(rowNums(0)),
																"F2", node(rowNums(7)),
																"F3", node(rowNums(9, 11))
														)),
												"House", node(
														rowNums(1, 3, 6, 12),
														subNodes(
																"F1", node(rowNums(1, 3)),
																"F2", node(rowNums(6)),
																"F3", node(rowNums(12))
														)),
												"Bank", node(
														rowNums(2, 5, 10),
														subNodes(
																"F1", node(rowNums(2, 5)),
																"F3", node(rowNums(10))
														)),
												"CCP", node(
														rowNums(4, 8),
														subNodes(
																"F1", node(rowNums(4)),
																"F2", node(rowNums(8))
														))
										)
								)
						}
				};
	}
	
	@Test(dataProvider = "validData")
	public void checkBuild(List<String> keys, IndexNode rootNode) throws Exception
	{
		TreeIndexBuilder builder = new TreeIndexBuilder("MyTable", keys);
		TreeIndex actualIndex = builder.build(TABLE);
		
		TreeIndex expectedIndex = new TreeIndex(keys, rootNode);
		
		assertEquals(actualIndex, expectedIndex);
	}
}
