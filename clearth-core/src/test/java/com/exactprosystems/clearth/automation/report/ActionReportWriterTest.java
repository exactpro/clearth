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

package com.exactprosystems.clearth.automation.report;

import com.exactprosystems.clearth.automation.*;
import com.exactprosystems.clearth.automation.exceptions.AutomationException;
import com.exactprosystems.clearth.automation.report.html.template.ReportTemplatesProcessor;
import com.exactprosystems.clearth.xmldata.XmlSchedulerLaunchInfo;
import freemarker.template.TemplateModelException;
import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.exactprosystems.clearth.ApplicationManager;
import com.exactprosystems.clearth.utils.ClearThException;
import com.exactprosystems.clearth.utils.SettingsException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static com.exactprosystems.clearth.ApplicationManager.*;
import static com.exactprosystems.clearth.automation.report.ActionReportWriter.HTML_FAILED_REPORT_NAME;
import static com.exactprosystems.clearth.automation.report.ActionReportWriter.HTML_REPORT_NAME;

public class ActionReportWriterTest
{
	private static final Path RES_DIR = TEST_RES_DIR.resolve(ActionReportWriterTest.class.getSimpleName()),
			 OUTPUT_DIR = USER_DIR.resolve("testOutput").resolve(ActionReportWriterTest.class.getSimpleName());
	private static final String USER = "async1", STEP = "Step1", ASYNC_FAILED = "async_failed.csv", ASYNC_PASSED = "async_passed.csv";
	private ApplicationManager manager;
	
	@BeforeClass
	public void init() throws ClearThException, IOException, SettingsException, TemplateModelException
	{
		manager = builder()
				.templatesProcessor(new ReportTemplatesProcessor(RES_DIR.resolve("templates")))
				.build();
	}
	
	@AfterClass
	public void dispose() throws IOException
	{
		if (manager != null)
			manager.dispose();
	}
	
	@Test
	public void testReportsUpdatePreparation() throws IOException
	{
		String dirName = "update",
				matrixName = "matrix1.csv",
				stepName = "step1";
		Path sourceDir = RES_DIR.resolve(dirName),
				targetDir = OUTPUT_DIR.resolve(dirName).resolve(matrixName),
				targetReport = targetDir.resolve(stepName+".json");
		FileUtils.deleteDirectory(targetDir.toFile());
		Files.createDirectories(targetDir);
		Files.copy(sourceDir.resolve("report.json"), targetReport);
		
		ActionReportWriter reportWriter = new ActionReportWriter(new ReportsConfig(true, true, true), null);
		reportWriter.prepareReportsToUpdate(targetDir.getParent().toString(), matrixName, stepName);
		
		String expectedReport = FileUtils.readFileToString(sourceDir.resolve("expected.json").toFile(), StandardCharsets.UTF_8),
				updatedReport = FileUtils.readFileToString(targetReport.toFile(), StandardCharsets.UTF_8);
		Assert.assertEquals(updatedReport, expectedReport);
		
		long files = Files.list(targetReport.getParent()).count();
		Assert.assertEquals(files, 1, "Number of files in report directory");
	}
	
	@Test
	public void testReportsGenerateForAsyncActions() throws ClearThException, AutomationException, IOException
	{
		Scheduler scheduler = manager.getScheduler(USER, USER);
		manager.loadSteps(scheduler, RES_DIR.resolve("config.cfg").toFile());
		manager.loadMatrices(scheduler, RES_DIR.resolve("matrices").toFile());
		
		scheduler.start(USER);
		waitForSchedulerToStop(scheduler, 100, 4000);
		
		List<XmlSchedulerLaunchInfo> launchesInfo = scheduler.getSchedulerData().getLaunches().getLaunchesInfo();
		Assert.assertFalse(launchesInfo == null || launchesInfo.isEmpty());
		
		Path expRepDir = RES_DIR.resolve("expected"),
				actRepDir = PROJ_DIR.resolve(Path.of("testOutput", "SchedulerTestData", "automation", "reports", launchesInfo.get(0).getReportsPath()));
		
		assertReports(actRepDir, expRepDir, ASYNC_PASSED);
		assertReports(actRepDir, expRepDir, ASYNC_FAILED);
	}
	
	private void assertReports(Path actRepDir, Path expRepDir, String matrixName) throws IOException
	{
		Path actual = actRepDir.resolve(matrixName),
				expected = expRepDir.resolve(matrixName);
		
		AssertReports.assertCompleteHtmlReports(actual.resolve(HTML_REPORT_NAME), expected.resolve(HTML_REPORT_NAME), true);
		AssertReports.assertFailedHtmlReports(actual.resolve(HTML_FAILED_REPORT_NAME), expected.resolve(HTML_FAILED_REPORT_NAME), true);
	}
}