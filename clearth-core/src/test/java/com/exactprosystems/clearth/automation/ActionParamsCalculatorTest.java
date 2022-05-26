/******************************************************************************
 * Copyright 2009-2022 Exactpro Systems Limited
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
import com.exactprosystems.clearth.ValueGenerator;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.exactprosystems.clearth.utils.CollectionUtils.join;
import static com.exactprosystems.clearth.utils.CollectionUtils.map;
import static java.util.Collections.emptyMap;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.testng.Assert.*;
import static org.mockito.Mockito.*;

public class ActionParamsCalculatorTest extends BasicTestNgTest
{
	private Matrix matrix;
	private MatrixFunctions functions;


	@Override
	protected void mockOtherBeforeClass()
	{
		MvelVariables mvelVars = new MvelVariables();
		mvelVars.put("id1", map("Qty", "40", "Consideration", "84.46"));
		mvelVars.put("id2", map("messageId", "msg-12345"));
		mvelVars.put("id3", map("TradingVenue", "AIX", "TradeCurrency", "KZT"));

		matrix = mock(Matrix.class);
		when(matrix.getMvelVars()).thenReturn(mvelVars);

		functions = new TestMatrixFunctions(emptyMap(), new Date(), null, true, null);
	}


	@DataProvider(name = "createValidParameters")
	public static Object[][] createValidParameters()
	{
		return new Object[][]
				{
						{
								map("A", "@{id1.Qty}", "B", "@{id2.messageId}", "C", "@{id3.TradingVenue}"),
								map("A", "40", "B", "msg-12345", "C", "AIX")
						},
						{
								map("A", "@{currentId.B}", "B", "@{currentId.C}", "C", "@{id3.TradeCurrency}"),
								map("A", "KZT", "B", "KZT", "C", "KZT")
						},
						{
								map("A", "@{thisAction.B}", "B", "@{thisAction.C}", "C", "@{id3.TradeCurrency}"),
								map("A", "KZT", "B", "KZT", "C", "KZT")
						}
				};
	}


	@Test(dataProvider = "createValidParameters")
	public void testCalculateParameter(Map<String, String> inputParams, Map<String, String> expectedResult)
	{
		assertEquals(calculateParams(inputParams), expectedResult);
	}

	@Test
	public void testRefToFutureTime()
	{
		Map<String, String> inputParams = map("FirstTime", "@{thisAction.SecondTime}",
				"ComplexParam", "@{sleep(1)}",
				"SecondTime", "@{format(time(0),'HH:mm:ss.SSS')}");

		Map<String, String> calculatedParams = calculateParams(inputParams);

		String firstTime = calculatedParams.get("FirstTime");
		String secondTime = calculatedParams.get("SecondTime");
		assertNotNull(firstTime);
		assertNotNull(secondTime);
		assertEquals(firstTime, secondTime);
	}


	private Map<String, String> calculateParams(Map<String, String> inputParams)
	{
		ActionParamsCalculator calculator = new ActionParamsCalculator(functions);

		Action action = mock(Action.class);
		when(action.getIdInMatrix()).thenReturn("currentId");
		LinkedHashMap<String, String> params = new LinkedHashMap<>(inputParams);
		when(action.getInputParams()).thenReturn(params);
		when(action.getMatrix()).thenReturn(matrix);

		calculator.calculateParameters(action, true);

		List<String> errors = calculator.getErrors();
		assertTrue(isEmpty(errors), join(errors));

		return params;
	}


	public static class TestMatrixFunctions extends MatrixFunctions
	{
		public TestMatrixFunctions(Map<String, Boolean> holidays, Date businessDay, Date baseTime,
		                           boolean weekendHoliday, ValueGenerator valueGenerator)
		{
			super(holidays, businessDay, baseTime, weekendHoliday, valueGenerator);
		}

		@SuppressWarnings("unused")
		public long sleep(long seconds) throws InterruptedException
		{
			TimeUnit.SECONDS.sleep(seconds);
			return seconds;
		}
	}
}
