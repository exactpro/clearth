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

package com.exactprosystems.clearth.woodpecker.daemons.message;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.exactprosystems.clearth.woodpecker.execution.distribution.NoValidTasksAvailable;
import com.exactprosystems.clearth.woodpecker.execution.loading.generator.LoadingGenerator;
import com.exactprosystems.clearth.woodpecker.execution.workQueue.ScheduledTask;
import com.exactprosystems.clearth.woodpecker.execution.workQueue.ScheduledTaskState;
import com.exactprosystems.clearth.woodpecker.execution.workQueue.WorkQueue;
import com.exactprosystems.clearth.woodpecker.utils.TimeOperator;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class MessageLoadingGeneratorTest
{
	private static final String COMMON_TASK_DESC_ID = "T1";
	private static final Collection<String> COMMON_TASK_DESC_IDS = Collections.singleton(COMMON_TASK_DESC_ID);
	
	private MessageDaemonSettings settings;
	
	private MessageTaskDescription commonTaskDesc;
	private MessageTaskDescriptionsTable table;
	
	private TimeOperator timeOperator;
	
	
	@BeforeClass
	public void init() throws NoValidTasksAvailable, InterruptedException
	{
		settings = new MessageDaemonSettings();
		settings.setMinBatchSize(1);
		settings.setMaxBatchSize(1);

		commonTaskDesc = mock(MessageTaskDescription.class);
		when(commonTaskDesc.getTaskId())
				.thenReturn(COMMON_TASK_DESC_ID);
		table = mock(MessageTaskDescriptionsTable.class);
		when(table.nextTaskDescription(anyInt()))
				.thenReturn(commonTaskDesc);
		when(table.getValidTaskDescIds())
				.thenReturn(COMMON_TASK_DESC_IDS);

		timeOperator = mock(TimeOperator.class);
		doNothing().when(timeOperator).sleep(anyLong(), any());
	}
	
	
	@DataProvider(name = "schedulingParameters")
	public Object[][] createParametersForScheduling()
	{
		return new Object[][]
				{
						{
								asList(new ExecutionPeriod(0, 200_000_000, 5, -1,
												asList(new TaskLifeTime(0, 200_000_000),
														new TaskLifeTime(200_000_000, 400_000_000),
														new TaskLifeTime(400_000_000, 600_000_000),
														new TaskLifeTime(600_000_000, 800_000_000),
														new TaskLifeTime(800_000_000, 1_000_000_000))),
										new ExecutionPeriod(1_000_000_000, 333_333_333, 3, -1,
												asList(new TaskLifeTime(1_000_000_000, 1_333_333_333),
														new TaskLifeTime(1_333_333_333, 1_666_666_666),
														new TaskLifeTime(1_666_666_666, 1_999_999_999)))),
						},
						{	
								asList(new ExecutionPeriod(10_000_000_000L, 222_222_222, 4, 1_000_000_000,
												asList(new TaskLifeTime(10_000_000_000L, 10_250_000_000L),
														new TaskLifeTime(10_222_222_222L, 10_472_222_222L),
														new TaskLifeTime(10_444_444_444L, 10_694_444_444L),
														new TaskLifeTime(10_666_666_666L, 10_916_666_666L))),
										new ExecutionPeriod(11_000_000_000L, 1_600_000_000L, 2, 4_000_000_000L,
												asList(new TaskLifeTime(11_000_000_000L, 13_000_000_000L),
														new TaskLifeTime(12_600_000_000L, 14_600_000_000L))))
						}
				};
	}
	
	
	@Test(dataProvider = "schedulingParameters")
	public void checkTasksScheduling(List<ExecutionPeriod> executionPeriods) throws NoValidTasksAvailable, InterruptedException
	{
		List<ScheduledTask> expectedTasks = generateExpectedTasks(executionPeriods);

		WorkQueue workQueue = mock(WorkQueue.class);
		LoadingGenerator loadingGenerator = new MessageLoadingGenerator(settings, workQueue, table, timeOperator);
		
		for (ExecutionPeriod p : executionPeriods)
		{
			when(timeOperator.nanoTime()).thenReturn(p.getStartTime());

			loadingGenerator.generateTasks(p.getTickNs(), p.getCount(), p.getPeriodDurationNs());
		}
		ArgumentCaptor<ScheduledTask> tasksCaptor = ArgumentCaptor.forClass(ScheduledTask.class);
		verify(workQueue, times(expectedTasks.size()))
				.submit(tasksCaptor.capture());
		List<ScheduledTask> actualTasks = tasksCaptor.getAllValues();
		
		assertThat(actualTasks)
				.usingRecursiveComparison()
				.isEqualTo(expectedTasks);
	}


	private List<ScheduledTask> generateExpectedTasks(List<ExecutionPeriod> executionPeriods)
	{
		List<ScheduledTask> tasks = new ArrayList<>();
		for (ExecutionPeriod p : executionPeriods)
		{
			List<TaskLifeTime> taskLifeTimes = p.getTaskLifeTimes();

			ScheduledTaskState state = new ScheduledTaskState(COMMON_TASK_DESC_IDS);
			state.addExpectedOpsCount(COMMON_TASK_DESC_ID, taskLifeTimes.size());

			for (TaskLifeTime t : taskLifeTimes)
			{
				tasks.add(new MessageScheduledTask(commonTaskDesc, state, t.getExecuteAfterNs(), t.getExecuteBeforeNs(), 1, false));
			}
		}
		return tasks;
	}
	
	
	private static class ExecutionPeriod
	{
		private final long startTime;
		private final long tickNs;
		private final int count;
		private final long periodDurationNs;
		private final List<TaskLifeTime> taskLifeTimes;

		ExecutionPeriod(long startTime, long tickNs, int count, long periodDurationNs, List<TaskLifeTime> taskLifeTimes)
		{
			this.startTime = startTime;
			this.tickNs = tickNs;
			this.count = count;
			this.periodDurationNs = periodDurationNs;
			this.taskLifeTimes = taskLifeTimes;
		}

		public long getStartTime()
		{
			return startTime;
		}

		public long getTickNs()
		{
			return tickNs;
		}

		public int getCount()
		{
			return count;
		}

		public long getPeriodDurationNs()
		{
			return periodDurationNs;
		}

		public List<TaskLifeTime> getTaskLifeTimes()
		{
			return taskLifeTimes;
		}
	}
	
	
	private static class TaskLifeTime
	{
		private final long executeAfterNs;
		private final long executeBeforeNs;

		TaskLifeTime(long executeAfterNs, long executeBeforeNs)
		{
			this.executeAfterNs = executeAfterNs;
			this.executeBeforeNs = executeBeforeNs;
		}

		public long getExecuteAfterNs()
		{
			return executeAfterNs;
		}

		public long getExecuteBeforeNs()
		{
			return executeBeforeNs;
		}
	}
}