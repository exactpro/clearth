/******************************************************************************
 * Copyright 2009-2020 Exactpro Systems Limited
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
package com.exactprosystems.clearth.woodpecker.configuration.settings.loading; 

import com.exactprosystems.clearth.utils.CommaBuilder;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerSettingsException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

public class LoadingScheduleTest
{
	
	@DataProvider(name = "validData")
	public Object[][] createValidParams()
	{
		return new Object[][]
				{
						{"10/10 20/10 30/10",               createSchedule(10, 10,  20, 10,  30, 10)},
						{"0/10 20/10 30/0",                 createSchedule(0, 10,  20, 10,  30, 0)},
						{" 10/10     20/10\r\n30/10\t",     createSchedule(10, 10,  20, 10,  30, 10)},
						{"1.5/10 2.5/10 3.5/10",            createSchedule(1.5, 10,  2.5, 10,  3.5, 10)},
						{createLongScheduleLine(10000),     createLongSchedule(10000)}
				};
	}
	
	@DataProvider(name = "invalidData")
	public Object[][] createInvalidParams()
	{
		return new Object[][]
				{
						{
								"10 / 10 20 / 10 30 / 10", // <- It is difficult to read such schedules.
								"Period #1 has unexpected format - [10]. Valid format is [rate/duration]. Use spaces to separate periods."
						},
						{
								"10/10 /10 30/10",
								"Period #2 has unexpected format - [/10]. Valid format is [rate/duration]. Use spaces to separate periods."
						},
						{
								"10/10 20 10 30/10",
								"Period #2 has unexpected format - [20]. Valid format is [rate/duration]. Use spaces to separate periods."
						},
						{
								"10/10 20/10 30/",
								"Period #3 has unexpected format - [30/]. Valid format is [rate/duration]. Use spaces to separate periods."
						},
						{
								"10/10, 20/10, 30/10",
								"Duration [10,] in period #1 [10/10,] should be integer number. Period format is [rate/duration]. Use spaces to separate periods."
						},
						{
								"1O/10 20/10 30/10", // Letter 'O' instead of zero in first period.
								"Rate [1O] in period #1 [1O/10] should be integer or decimal with '.'. Period format is [rate/duration]. Use spaces to separate periods."
						},
						{
								"10/10 20/1O 30/10", // Letter 'O' instead of zero in second period.
								"Duration [1O] in period #2 [20/1O] should be integer number. Period format is [rate/duration]. Use spaces to separate periods."
						},
						{
								"10/10 20/10.5 30/10",
								"Duration [10.5] in period #2 [20/10.5] should be integer number. Period format is [rate/duration]. Use spaces to separate periods."
						},
						{
								"1/10 1,5/10 2/10",
								"Rate [1,5] in period #2 [1,5/10] should be integer or decimal with '.'. Period format is [rate/duration]. Use spaces to separate periods."
						}
				};
	}


	@Test(dataProvider = "validData")
	public void checkScheduleParsing(String inputScheduleLine, LoadingSchedule expectedSchedule)
			throws WoodpeckerSettingsException
	{
		LoadingSchedule actualSchedule = LoadingSchedule.parse(inputScheduleLine);

		assertThat(actualSchedule)
				.usingRecursiveComparison()
				.isEqualTo(expectedSchedule);
	}
	
	@Test(dataProvider = "invalidData")
	public void checkParsingErrors(String inputScheduleLine, String expectedErrorMessage)
	{
		assertThatExceptionOfType(WoodpeckerSettingsException.class)
				.isThrownBy(() -> LoadingSchedule.parse(inputScheduleLine))
				.withMessage(expectedErrorMessage)
				.withNoCause();
	}
	
	
	private static String createLongScheduleLine(int pairsCount)
	{
		CommaBuilder cb = new CommaBuilder(" ");
		for (int i = 1; i <= pairsCount; i++)
		{
			cb.append(i).add('/').add(i);
		}
		return cb.toString();
	}
	
	private static LoadingSchedule createLongSchedule(int pairsCount)
	{
		LoadingSchedule schedule = new LoadingSchedule();
		for (int i = 1; i <= pairsCount; i++)
		{
			schedule.addPeriod(i, i);
		}
		return schedule;
	}
	
	private static LoadingSchedule createSchedule(double... rateDurationPairs)
	{
		LoadingSchedule schedule = new LoadingSchedule();
		for (int i = 1; i < rateDurationPairs.length; i += 2)
		{
			schedule.addPeriod(rateDurationPairs[i - 1], (int) rateDurationPairs[i]);
		}
		return schedule;
	}
}