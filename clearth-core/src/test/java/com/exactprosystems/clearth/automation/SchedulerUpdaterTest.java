/******************************************************************************
 * Copyright 2009-2024 Exactpro Systems Limited
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.testng.Assert;
import org.testng.TestException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.exactprosystems.clearth.ApplicationManager;
import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.exceptions.ActionUpdateException;
import com.exactprosystems.clearth.automation.exceptions.AutomationException;
import com.exactprosystems.clearth.automation.exceptions.SchedulerUpdateException;
import com.exactprosystems.clearth.helpers.JsonAssert;
import com.exactprosystems.clearth.utils.ClearThException;
import com.exactprosystems.clearth.utils.FileOperationUtils;
import com.exactprosystems.clearth.xmldata.XmlSchedulerLaunchInfo;

public class SchedulerUpdaterTest
{
	private Path resourcesPath,
			matrixUpdateDir,
			stepsUpdateDir;
	private ApplicationManager appManager;
	private String userName;
	
	@BeforeClass
	public void init() throws FileNotFoundException, ClearThException
	{
		resourcesPath = Paths.get(FileOperationUtils.resourceToAbsoluteFilePath("SchedulerUpdaterTest"));
		matrixUpdateDir = resourcesPath.resolve("matrix_update");
		stepsUpdateDir = resourcesPath.resolve("steps_update");
		
		appManager = new ApplicationManager();
		userName = "user";
	}
	
	@AfterClass
	public void dispose() throws IOException
	{
		if (appManager != null)
			appManager.dispose();
	}
	
	@DataProvider(name = "steps")
	public Object[][] steps()
	{
		return new Object[][]
				{
					{ createStepData("Step100", true, false, false), "Global step 'Step100' is not used in the run" },
					{ createStepData("Step3", true, false, false), "Global step 'Step3' is not executable and cannot be changed to executable"}
				};
	}
	
	
	@Test
	public void updatedActionsInReport() throws ClearThException, AutomationException, SchedulerUpdateException, ActionUpdateException, IOException
	{
		Scheduler scheduler = createScheduler("matrixUpdate", matrixUpdateDir.resolve("steps.cfg"), matrixUpdateDir.resolve("matrices"));
		scheduler.start(userName);
		try
		{
			ApplicationManager.waitForSchedulerToSuspend(scheduler, 100, 2000);
			
			MatrixData updatedMatrixData = scheduler.getMatrixDataFactory()
					.createMatrixData("matrix1.csv", matrixUpdateDir.resolve("matrix1_updated.csv").toFile(), new Date(), true, false, null, null, false);
			SchedulerUpdater updater = new SchedulerUpdater(scheduler);
			updater.updateMatrices(List.of(updatedMatrixData));
			
			scheduler.continueExecution();
			ApplicationManager.waitForSchedulerToStop(scheduler, 100, 5000);
			
			List<XmlSchedulerLaunchInfo> launchesInfo = scheduler.getSchedulerData().getLaunches().getLaunchesInfo();
			Path actualReport = Path.of(ClearThCore.reportsPath(), launchesInfo.get(0).getReportsPath(), "matrix1.csv", "report.json"),
					expectedReport = matrixUpdateDir.resolve("matrix1_report.json");
			
			new JsonAssert().setIgnoredValueNames(SchedulerTest.IGNORED_EXPECTED_PARAMS)
					.assertEquals(expectedReport.toFile(), actualReport.toFile());
		}
		finally
		{
			scheduler.stop();
			ApplicationManager.waitForSchedulerToStop(scheduler, 100, 5000);
		}
	}
	
	
	@Test(dataProvider = "steps")
	public void invalidStepUpdate(StepData stepData, String errorMessage) throws ClearThException, IOException
	{
		Scheduler scheduler = createScheduler("invalidStepUpdate", stepsUpdateDir.resolve("steps.cfg"), null);
		
		SchedulerUpdater updater = new SchedulerUpdater(scheduler);
		try
		{
			updater.updateSteps(List.of(stepData));
		}
		catch (SchedulerUpdateException e)
		{
			Assert.assertEquals(e.getMessage(), errorMessage, "Error message");
			return;
		}
		throw new TestException("Expected exception of class "+SchedulerUpdateException.class+" with message '"+errorMessage+"'");
	}
	
	@Test(expectedExceptions = SchedulerUpdateException.class, expectedExceptionsMessageRegExp = "Global step 'Step1' is ended and cannot be updated")
	public void endedStepUpdate() throws ClearThException, IOException, AutomationException, SchedulerUpdateException
	{
		Scheduler scheduler = createScheduler("endedStepUpdate", stepsUpdateDir.resolve("steps2.cfg"), stepsUpdateDir.resolve("simple_matrices"));
		scheduler.start(userName);
		try
		{
			ApplicationManager.waitForSchedulerToSuspend(scheduler, 100, 5000);
			Assert.assertEquals(scheduler.getCurrentStep().getName(), "Step3", "Current step when paused");
			
			SchedulerUpdater updater = new SchedulerUpdater(scheduler);
			updater.updateSteps(List.of(createStepData("Step1", false, false, false)));
		}
		finally
		{
			scheduler.stop();
			ApplicationManager.waitForSchedulerToStop(scheduler, 100, 5000);
		}
	}
	
	@Test
	public void updateSteps() throws ClearThException, AutomationException, SchedulerUpdateException, IOException
	{
		//Matrix pauses scheduler
		//Update makes Step1 to use "Ask for continue", Step2 not to execute, Step3 not to use "Ask for continue", Step4 to use "Ask if failed"
		//Scheduler execution continues and pauses after Step1
		//Then, scheduler execution continues again and pauses after Step4 due to error in action execution
		//When scheduler finishes, report contains actions only from Step1, Step3 and Step4
		
		Scheduler scheduler = createScheduler("updateSteps", stepsUpdateDir.resolve("steps2.cfg"), stepsUpdateDir.resolve("pausing_matrices"));
		scheduler.start(userName);
		try
		{
			ApplicationManager.waitForSchedulerToSuspend(scheduler, 100, 2000);
			
			List<String> originalSteps = scheduler.getSteps().stream().map(Step::getName).collect(Collectors.toList());
			
			SchedulerUpdater updater = new SchedulerUpdater(scheduler);
			updater.updateSteps(List.of(
					createStepData("Step1", true, true, false),     //"Ask for continue" = true
					createStepData("Step2", false, false, false),   //Execute = false
					createStepData("Step3", true, false, false),    //"Ask for continue" = false
					createStepData("Step4", true, false, true)));   //"Ask if failed" = true
			
			List<String> newSteps = scheduler.getSteps().stream().map(Step::getName).collect(Collectors.toList());
			Assert.assertEquals(newSteps, originalSteps, "Steps list");  //Updater should not remove nor add any steps
			
			scheduler.continueExecution();
			ApplicationManager.waitForSchedulerToSuspend(scheduler, 100, 2000);
			Assert.assertEquals(scheduler.getCurrentStep().getName(), "Step1", "Current step when paused");
			
			scheduler.continueExecution();
			ApplicationManager.waitForSchedulerToSuspend(scheduler, 100, 2000);
			Assert.assertEquals(scheduler.getCurrentStep().getName(), "Step4", "Current step when paused");
			
			scheduler.continueExecution();
			ApplicationManager.waitForSchedulerToStop(scheduler, 100, 2000);
		}
		finally
		{
			scheduler.stop();
			ApplicationManager.waitForSchedulerToStop(scheduler, 100, 5000);
		}
		
		List<XmlSchedulerLaunchInfo> launchesInfo = scheduler.getSchedulerData().getLaunches().getLaunchesInfo();
		Path actualReport = Path.of(ClearThCore.reportsPath(), launchesInfo.get(0).getReportsPath(), "pausing_matrix.csv", "report.json"),
				expectedReport = stepsUpdateDir.resolve("pausing_matrix_report.json");
		
		new JsonAssert().setIgnoredValueNames(SchedulerTest.IGNORED_EXPECTED_PARAMS)
				.assertEquals(expectedReport.toFile(), actualReport.toFile());
	}
	
	
	private Scheduler createScheduler(String name, Path stepsFile, Path matricesDir) throws ClearThException
	{
		Scheduler scheduler = appManager.getScheduler(name, userName);
		appManager.loadSteps(scheduler, stepsFile.toFile());
		if (matricesDir != null)
			appManager.loadMatrices(scheduler, matricesDir.toFile());
		return scheduler;
	}
	
	private StepData createStepData(String name, boolean execute, boolean askForContinue, boolean askIfFailed)
	{
		StepData result = new StepData();
		result.setName(name);
		result.setExecute(execute);
		result.setAskForContinue(askForContinue);
		result.setAskIfFailed(askIfFailed);
		result.setKind(CoreStepKind.Default.getLabel());
		result.setStartAt("");
		return result;
	}
}