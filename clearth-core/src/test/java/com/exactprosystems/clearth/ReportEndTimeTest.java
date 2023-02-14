/******************************************************************************
 * Copyright 2009-2023 Exactpro Systems Limited
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

package com.exactprosystems.clearth;

import com.exactprosystems.clearth.automation.*;
import com.exactprosystems.clearth.automation.actions.TestAction;
import com.exactprosystems.clearth.data.DefaultTestExecutionHandler;
import com.exactprosystems.clearth.utils.ClearThException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;


public class ReportEndTimeTest
{
	private static final String SCHEDULER_NAME = "reportChecker", USER_NAME = "ReportChecker";
	private static Scheduler scheduler;
	private static Executor executor;
	private static Step firstStep, secondStep, thirdStep;
	private static ApplicationManager clearThManager;
	private static final long START_IN_MILLIS = 1605081600000L;
	private static final Date START = new Date(START_IN_MILLIS);
	private static final Date START_PLUS_100 = new Date(START_IN_MILLIS + 100);
	private static final Date START_PLUS_200 = new Date(START_IN_MILLIS + 200);
	private static final Date START_PLUS_300 = new Date(START_IN_MILLIS + 300);

	@BeforeClass
	public static void startTestApp() throws ClearThException
	{
		clearThManager = new ApplicationManager();
		scheduler = clearThManager.getScheduler(SCHEDULER_NAME, USER_NAME);
		executor = new DefaultExecutorFactory(null).createExecutor(scheduler,null,USER_NAME,
				null, new DefaultTestExecutionHandler());


		List<Action> firstStepActions = initializeActions(START, START_PLUS_100,START_PLUS_100,START_PLUS_200);
		firstStep = initializeStep(START,START_PLUS_200,firstStepActions);

		List<Action> secondStepActions = initializeActions(START_PLUS_200,START_PLUS_300,START_PLUS_300,null);
		secondStep = initializeStep(START_PLUS_200,null,secondStepActions);

		List<Action> thirdStepActions  = initializeActions(null,null,null,null);
		thirdStep = initializeStep(null,null,thirdStepActions);
	}

	private static Step initializeStep(Date stepStart, Date stepFinish, List<Action> actions)
	{
		Step step = Mockito.mock(DefaultStep.class);
		when(step.isExecute()).thenReturn(true);
		when(step.getStarted()).thenReturn(stepStart);
		when(step.getFinished()).thenReturn(stepFinish);
		when(step.getActions()).thenReturn(actions);
		return step;
	}

	private static List<Action> initializeActions(Date firsActionStart, Date firstActionFinish,
	                                              Date secondActionStart, Date secondActionFinish)
	{
		Action firstAction = Mockito.mock(TestAction.class),
				secondAction = Mockito.mock(TestAction.class);
		List<Action> actions = new ArrayList<>();
		actions.add(firstAction);
		actions.add(secondAction);
		when(firstAction.getStarted()).thenReturn(firsActionStart);
		when(firstAction.getFinished()).thenReturn(firstActionFinish);
		when(secondAction.getStarted()).thenReturn(secondActionStart);
		when(secondAction.getFinished()).thenReturn(secondActionFinish);
		return actions;
	}

	@Test
	public void testExecutorThatIsEnded()
	{
		Executor testExecutor = spy(executor);
		List<Step> steps = new ArrayList<>();
		steps.add(firstStep);
		steps.add(secondStep);
		when(testExecutor.getSteps()).thenReturn(steps);
		when(testExecutor.isTerminated()).thenReturn(true);
		when(testExecutor.getEnded()).thenReturn(START_PLUS_200);

		Date actual = testExecutor.getReportEndTime();
		assertEquals(actual, START_PLUS_200);
	}

	@Test
	public void testExecutorWithEndedStep()
	{
		List<Step> steps = new ArrayList<>();
		steps.add(firstStep);
		steps.add(thirdStep);
		Executor testExecutor = spy(executor);
		when(testExecutor.getSteps()).thenReturn(steps);

		Date actual = testExecutor.getReportEndTime();
		assertEquals(actual, START_PLUS_200);
	}

	@Test
	public void testExecutorWithEndedActions()
	{
		List<Step> steps = new ArrayList<>();
		steps.add(firstStep);
		steps.add(secondStep);
		Executor testExecutor = spy(executor);
		when(testExecutor.getSteps()).thenReturn(steps);

		Date actual = testExecutor.getReportEndTime();
		assertEquals(actual, START_PLUS_300);
	}
}