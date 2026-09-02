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

package com.exactprosystems.clearth.woodpecker.execution.loading.generator; 

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.mockito.ArgumentCaptor;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.exactprosystems.clearth.woodpecker.configuration.settings.loading.LoadingSchedule;
import com.exactprosystems.clearth.woodpecker.execution.distribution.NoValidTasksAvailable;
import com.exactprosystems.clearth.woodpecker.execution.workQueue.ScheduledTaskState;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class LoadingPriorityStrategyTest
{
	// Note: LoadingPriorityStrategy is used only in scheduled mode. In this mode rateUnit = durationUnit.
	
	@DataProvider
	public Object[][] createParams()
	{
		return new Object[][] 
				{
						// schedule - rate and duration pairs, passed to strategy.generateLoading()
						// restOperationsCounts - count of uncompleted operations after each period
						// countsInPeriod - count for each period calculated by LoadingGenerator
						// ticks - ticks for each period calculated by LoadingGenerator
						// expectedLoadingGeneratorParams - tick and count pairs passed to LoadingGenerator.generateTasks(),
						// rateUnit - for strategy.generateLoading()
						
						// Here we can reach the specified rate.
						{
								createSchedule(5, 1, 10, 1, 20, 1),
								new int[] {0, 0, 0},
								new int[] {5, 10, 20},
								new long[] {200_000_000, 100_000_000, 50_000_000},
								asList(new LoadingGeneratorParams(200_000_000, 5),
										new LoadingGeneratorParams(100_000_000, 10),
										new LoadingGeneratorParams(50_000_000, 20)),
								TimeUnit.SECONDS
						},
						// Here we can send only ~20 operations per second.
						{
								createSchedule(25, 1, 10, 1),
								new int[] {5, 0, 0},
								new int[] {25, 10},
								new long[] {40_000_000, 100_000_000},
								asList(new LoadingGeneratorParams(40_000_000, 25),
										// tick * rest > 100 ms  ->   we can send rest as additional period
										new LoadingGeneratorParams(40_000_000, 5),
										new LoadingGeneratorParams(100_000_000, 10)),
								TimeUnit.SECONDS
						},
						// Here we can send only ~23 operations per second.
						{
								createSchedule(25, 1, 10, 1),
								new int[] {2, 0},
								new int[] {25, 10},
								new long[] {40_000_000, 100_000_000},
								asList(new LoadingGeneratorParams(40_000_000, 25),
										// tick * rest < 100 ms  ->  need to move rest to the next period (10 + 2 = 12)
										new LoadingGeneratorParams(100_000_000, 12)),
								TimeUnit.SECONDS
						},
						// Here we can send only ~23 operations per second.
						{
								createSchedule(25, 1),
								new int[] {2, 0},
								new int[] {25},
								new long[] {40_000_000},
								asList(new LoadingGeneratorParams(40_000_000, 25),
										// tick * rest < 100ms, but we must send rest for last period anyway
										new LoadingGeneratorParams(40_000_000, 2)),
								TimeUnit.SECONDS
						},
						// Combination of previous cases.
						{
								createSchedule(21, 1,  22, 1,  23, 1),
								new int[] {1, 3, 0, 1, 0},
								new int[] {21, 22, 23},
								new long[] {47_619_047, 45_454_545, 43_478_260},
								asList(new LoadingGeneratorParams(47_619_047, 21),
										new LoadingGeneratorParams(45_454_545, 23),
										new LoadingGeneratorParams(45_454_545, 3),
										new LoadingGeneratorParams(43_478_260, 23),
										new LoadingGeneratorParams(43_478_260, 1)),
								TimeUnit.SECONDS
						},
						
						// Let's check rate in minutes.
						// Here we can reach the specified rate.
						{
								createSchedule(6, 1, 10, 1),
								new int[] {0, 0},
								new int[] {6, 10},
								new long[] {10_000_000_000L, 6_000_000_000L},
								asList(new LoadingGeneratorParams(10_000_000_000L, 6),
										new LoadingGeneratorParams(6_000_000_000L, 10)),
								TimeUnit.MINUTES
						},
						// Here we can send ~38 ops per minute
						{
								createSchedule(60, 1, 30, 1),
								new int[] {22, 0, 0},
								new int[] {60, 30},
								new long[] {1_000_000_000, 2_000_000_000},
								asList(new LoadingGeneratorParams(1_000_000_000, 60),
										new LoadingGeneratorParams(1_000_000_000, 22),
										new LoadingGeneratorParams(2_000_000_000, 30)),
								TimeUnit.MINUTES
						},
						// Here we can send ~59 ops per minute
						{
								createSchedule(30, 1, 60, 1),
								new int[] {0, 1, 0},
								new int[] {30, 60},
								new long[] {2_000_000_000, 1_000_000_000},
								asList(new LoadingGeneratorParams(2_000_000_000, 30),
										new LoadingGeneratorParams(1_000_000_000, 60),
										new LoadingGeneratorParams(1_000_000_000, 1)),
								TimeUnit.MINUTES
						}
				};
	}
	

	@Test(dataProvider = "createParams")
	public void checkGenerateLoading(LoadingSchedule schedule, 
	                                 int[] restOperationsCounts, int[] countsInPeriod, long[] ticks,
	                                 List<LoadingGeneratorParams> expectedLoadingGeneratorParams, 
	                                 TimeUnit rateDurationUnit)
			throws NoValidTasksAvailable, InterruptedException
	{
		LoadingGenerator generator = mockLoadingGenerator(restOperationsCounts, countsInPeriod, ticks);

		LoadingPriorityStrategy strategy = new LoadingPriorityStrategy(generator);
		runSchedule(strategy, schedule, rateDurationUnit);

		List<LoadingGeneratorParams> actualLoadingGeneratorParams = captureActualLoadingGeneratorParams(generator,
				expectedLoadingGeneratorParams.size());
		
		assertThat(actualLoadingGeneratorParams)
				.usingRecursiveComparison()
				.isEqualTo(expectedLoadingGeneratorParams);
	}
	
	private LoadingGenerator mockLoadingGenerator(int[] restOperationsCounts, int[] countsInPeriod, long[] ticks)
			throws NoValidTasksAvailable, InterruptedException
	{
		LoadingGenerator generator = mock(LoadingGenerator.class);

		ScheduledTaskState firstState = mockState(restOperationsCounts[0]);
		ScheduledTaskState[] otherStates = new ScheduledTaskState[restOperationsCounts.length - 1];
		for (int i = 1; i < restOperationsCounts.length; i++)
		{
			otherStates[i - 1] = mockState(restOperationsCounts[i]);
		}
		when(generator.generateTasks(anyLong(), anyInt(), anyLong()))
				.thenReturn(firstState, otherStates);
		
		Integer[] otherCounts = new Integer[countsInPeriod.length - 1];
		for (int i = 1; i < countsInPeriod.length; i++)
		{
			otherCounts[i - 1] = countsInPeriod[i];
		}
		when(generator.calculateCountInPeriod(anyLong(), anyInt(), anyObject()))
				.thenReturn(countsInPeriod[0], otherCounts);
		
		Long[] otherTicks = new Long[ticks.length - 1];
		for (int i = 1; i < ticks.length; i++)
		{
			otherTicks[i - 1] = ticks[i];
		}
		when(generator.calculateTick(anyDouble(), anyObject()))
				.thenReturn(ticks[0], otherTicks);
		
		return generator;
	}
	
	private ScheduledTaskState mockState(int restOpsCount)
	{
		ScheduledTaskState state = mock(ScheduledTaskState.class);
		when(state.getRestOperationsCount()).thenReturn(restOpsCount);
		return state;
	}
	
	private void runSchedule(LoadingPriorityStrategy strategy, LoadingSchedule schedule, 
	                         TimeUnit rateDurationUnit)
			throws NoValidTasksAvailable, InterruptedException
	{
		int periodsCount = schedule.getPeriodsCount();
		for (int i = 0; i < periodsCount; i++)
		{
			boolean isLastPeriod = (i + 1) == periodsCount;
			LoadingSchedule.Period period = schedule.getPeriod(i);
			
			strategy.generateLoading(period.getRate(), rateDurationUnit,
					period.getDuration(), rateDurationUnit, isLastPeriod);
		}
	}
	
	private List<LoadingGeneratorParams> captureActualLoadingGeneratorParams(LoadingGenerator generatorMock,
	                                                                         int expectedInvocationTimes)
			throws NoValidTasksAvailable, InterruptedException
	{
		ArgumentCaptor<Long> ticksCaptor = ArgumentCaptor.forClass(long.class);
		ArgumentCaptor<Integer> countsCaptor = ArgumentCaptor.forClass(int.class);
		
		verify(generatorMock, times(expectedInvocationTimes))
				.generateTasks(ticksCaptor.capture(), countsCaptor.capture(), anyLong());
		
		List<Long> ticks = ticksCaptor.getAllValues();
		List<Integer> counts = countsCaptor.getAllValues();

		List<LoadingGeneratorParams> params = new ArrayList<>(ticks.size());
		for (int i = 0; i < ticks.size(); i++)
		{
			params.add(new LoadingGeneratorParams(ticks.get(i), counts.get(i)));
		}
		return params;
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
	
	
	@SuppressWarnings("unused")
	private static class LoadingGeneratorParams
	{
		private final long tick;
		private final int count;

		public LoadingGeneratorParams(long tick, int count)
		{
			this.tick = tick;
			this.count = count;
		}

		public long getTick()
		{
			return tick;
		}

		public int getCount()
		{
			return count;
		}
	}
}