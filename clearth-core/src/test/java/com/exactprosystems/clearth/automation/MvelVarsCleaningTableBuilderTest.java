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

package com.exactprosystems.clearth.automation;

import com.exactprosystems.clearth.BasicTestNgTest;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.exactprosystems.clearth.utils.CollectionUtils.map;
import static com.exactprosystems.clearth.utils.FileOperationUtils.resourceToAbsoluteFilePath;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static org.testng.Assert.*;
import static org.mockito.Mockito.*;

public class MvelVarsCleaningTableBuilderTest extends BasicTestNgTest
{
	private static final String QUERY_WITH_EXPRESSIONS_PATH = "MvelVarsCleaningTableBuilderTest/queryWithExpressions.sql";
	
	
	@DataProvider(name = "createParameters")
	public Object[][] createParameters() throws FileNotFoundException
	{
		String queryPath = resourceToAbsoluteFilePath(QUERY_WITH_EXPRESSIONS_PATH);
		
		return new Object[][]
				{
						{
								mockMatrix(
										mockAction("id1", "Step1", map("Qty", "40", "Consideration", "56.98")),
										mockAction("id2", "Step1", map("TradeSource", "AIX", "Currency", "KZT")),

										mockAction("id3", "Step2", map("Qty", "@{id1.Qty}", "TS", "@{id2.TradeSource}")),
										mockAction("id4", "Step2", map("messageId", "msg-12345",
												"msgId", "@{thisAction.messageID}")),

										mockAction("id5", "Step3", map("messageId", "@{prevAction.messageId}")),
										mockAction("id6", "Step3", map("Value", "@{id2.Currency}/@{id1.Consideration}"))),

								asList("Step1", "Step2", "Step3"),

								cleaningTable("id3", singleton("id3"),
										"id4", singleton("id4"),
										"id5", singleton("id5"),
										"id6", asList("id1", "id2", "id6"))
						},
						{
								mockMatrix(
										mockAction("id1", "Step1", map("Qty", "40", "Consideration", "56.98")),
										mockAction("id2", "Step1", map("TradeSource", "AIX", "Currency", "KZT")),

										mockAction("id5", "Step3", map("messageId", "@{prevAction.messageId}")),
										mockAction("id6", "Step3", map("Value", "@{id2.Currency}/@{id1.Consideration}")),

										mockAction("id3", "Step2", map("Qty", "@{id1.Qty}", "TS", "@{id2.TradeSource}")),
										mockAction("id4", "Step2", map("messageId", "msg-12345"))),

								asList("Step1", "Step2", "Step3"),

								cleaningTable("id3", singleton("id3"),
										"id4", singleton("id4"),
										"id5", singleton("id5"),
										"id6", asList("id1", "id2", "id6"))
						},
						{
								mockMatrix(
										mockAction("id1", "Step1", map("Currency", "KZT")),
										mockAction("id2", "Step1", map("QueryFile",  queryPath), "QueryFile")),
								
								singleton("Step1"),
								
								cleaningTable("id2", asList("id1", "id2"))
						},
						{
								mockMatrix(
										mockAction("id1", "Step1", map("Currency", "KZT")),
										mockAction("id2", "Step1", map("QueryFile", queryPath)),
										mockAction("id3", "Step1", map("QueryFile",  "@{id2.QueryFile}"), "QueryFile")),

								singleton("Step1"),

								cleaningTable("id3", asList("id1", "id2", "id3"))
						},
						{
								mockMatrix(
										mockAction("id1", "Step1"), mockAction("id2", "Step1"), 
										mockAction("id3", "Step1"), mockAction("id4", "Step1"),
										mockAction("id5", "Step1"), mockAction("id6", "Step1"),
										mockAction("id7", "Step1"),
										mockAction("id0", "Step1", 
												"@{id1.execute}", "@{id2.inverted}",
												"@{id3.comment}", "@{id4.timeout}",
												"@{id5.async}", "@{id6.asyncGroup}", 
												"@{id7.waitAsyncEnd}")),

								singleton("Step1"),

								cleaningTable("id0", asList("id0", "id1", "id2", "id3", "id4", "id5", "id6", "id7"))
						}
				};
	}

	@Test(dataProvider = "createParameters")
	public void testBuildCleaningTable(Matrix matrix, Collection<String> stepNames,
	                                   MultiValuedMap<String, String> expectedCleaningTable)
	{
		MatrixFunctions mf = new MatrixFunctions(emptyMap(), null, null, true, null);
		MvelVarsCleaningTableBuilder builder = new MvelVarsCleaningTableBuilder(mf);
		
		MultiValuedMap<String, String> actualCleaningTable = builder.build(matrix, stepNames);
		assertEquals(actualCleaningTable, expectedCleaningTable);
	}


	private Action mockAction(String id, String stepName)
	{
		return mockAction(id, stepName, emptyMap());
	}
	
	private Action mockAction(String id, String stepName, Map<String, String> inputParams)
	{
		return mockAction(id, stepName, inputParams, null);
	}
	
	private Action mockAction(String id, String stepName, Map<String, String> inputParams, String inputFileParamName)
	{
		Action action = mock(Action.class);

		when(action.getIdInMatrix()).thenReturn(id);
		when(action.getInputParams()).thenReturn(new LinkedHashMap<>(inputParams));
		if (inputFileParamName != null)
			when(action.getInputFileParamNames()).thenReturn(singleton(inputFileParamName));

		Step step = mock(Step.class);
		when(step.getName()).thenReturn(stepName);
		when(action.getStep()).thenReturn(step);

		return action;
	}
	
	@SuppressWarnings("MethodWithTooManyParameters")
	private Action mockAction(String id, String stepName, String executable, String inverted, String comment,
	                          String timeout, String async, String asyncGroup, String waitAsyncEnd)
	{
		Action action = mockAction(id, stepName);
		when(action.getFormulaExecutable()).thenReturn(executable);
		when(action.getFormulaInverted()).thenReturn(inverted);
		when(action.getFormulaComment()).thenReturn(comment);
		when(action.getFormulaTimeout()).thenReturn(timeout);
		when(action.getFormulaAsync()).thenReturn(async);
		when(action.getFormulaAsyncGroup()).thenReturn(asyncGroup);
		when(action.getFormulaWaitAsyncEnd()).thenReturn(waitAsyncEnd);
		return action;
	}

	private Matrix mockMatrix(Action... actions)
	{
		Matrix matrix = mock(Matrix.class);
		when(matrix.getActions()).thenReturn(asList(actions));
		return matrix;
	}

	private MultiValuedMap<String, String> cleaningTable(Object... params)
	{
		MultiValuedMap<String, String> table = new HashSetValuedHashMap<>();
		for (int i = 1; i < params.length; i += 2)
		{
			String actionId = (String) params[i - 1];
			@SuppressWarnings("unchecked")
			Collection<String> idsToClean = (Collection<String>) params[i];

			table.putAll(actionId, idsToClean);
		}
		return table;
	}
}
