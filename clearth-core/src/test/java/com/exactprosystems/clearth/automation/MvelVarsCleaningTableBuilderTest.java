package com.exactprosystems.clearth.automation;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.exactprosystems.clearth.utils.CollectionUtils.map;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.testng.Assert.*;
import static org.mockito.Mockito.*;

public class MvelVarsCleaningTableBuilderTest
{
	@DataProvider(name = "createParameters")
	public Object[][] createParameters()
	{
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
						}
				};
	}

	@Test(dataProvider = "createParameters")
	public void testBuildCleaningTable(Matrix matrix, Collection<String> stepNames,
	                                   MultiValuedMap<String, String> expectedCleaningTable)
	{
		MvelVarsCleaningTableBuilder builder = new MvelVarsCleaningTableBuilder();
		MultiValuedMap<String, String> actualCleaningTable = builder.build(matrix, stepNames);
		assertEquals(actualCleaningTable, expectedCleaningTable);
	}


	private Action mockAction(String id, String stepName, Map<String, String> inputParams)
	{
		Action action = mock(Action.class);

		when(action.getIdInMatrix()).thenReturn(id);
		when(action.getInputParams()).thenReturn(new LinkedHashMap<>(inputParams));

		Step step = mock(Step.class);
		when(step.getName()).thenReturn(stepName);
		when(action.getStep()).thenReturn(step);

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
