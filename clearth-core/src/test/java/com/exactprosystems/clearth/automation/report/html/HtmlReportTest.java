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

package com.exactprosystems.clearth.automation.report.html;

import com.exactprosystems.clearth.ApplicationManager;
import com.exactprosystems.clearth.EnvVars;
import com.exactprosystems.clearth.GlobalConstants;
import com.exactprosystems.clearth.automation.*;
import com.exactprosystems.clearth.automation.report.ReportException;
import com.exactprosystems.clearth.automation.report.html.template.ReportTemplatesProcessor;
import com.exactprosystems.clearth.data.DummyTestExecutionId;
import com.exactprosystems.clearth.utils.ClearThException;
import com.exactprosystems.clearth.utils.FileOperationUtils;
import com.exactprosystems.clearth.utils.Utils;
import freemarker.template.TemplateModelException;
import org.apache.commons.io.FileUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;

import static org.testng.Assert.assertEquals;

public class HtmlReportTest
{
	private static final String USER = "user",
			STEP_NAME = "Step1";
	private static final Date START_TIME = new Date(1580126821783L);
	private static final Date END_TIME = new Date(1580126822273L);
	private static final Path OUTPUT_DIR = Path.of("testOutput", HtmlReportTest.class.getSimpleName());
	private Path resDir;
	private ApplicationManager manager;
	
	@BeforeClass
	public void init() throws IOException, ClearThException
	{
		FileUtils.deleteDirectory(OUTPUT_DIR.toFile());
		Files.createDirectories(OUTPUT_DIR);
		
		manager = new ApplicationManager();
		resDir = Paths.get(FileOperationUtils.resourceToAbsoluteFilePath(HtmlReportTest.class.getSimpleName()));
	}
	
	@AfterClass
	public void dispose() throws IOException
	{
		if (manager != null)
			manager.dispose();
	}
	
	@Test
	public void testWriteReport() throws IOException, ReportException, TemplateModelException
	{
		HtmlReport report = new HtmlReport(createMatrix(), OUTPUT_DIR.toString(), USER, "report1", START_TIME, END_TIME,
				"dummy", new DummyTestExecutionId("Id-12345"), createTemplatesProcessor());
		
		report.writeReport(List.of(createStep()), List.of(STEP_NAME), OUTPUT_DIR.toFile(), false);
		
		assertReportFiles(OUTPUT_DIR.resolve("report1.html").toFile(), resDir.resolve("expected_report.html").toFile());
	}
	
	private void assertReportFiles(File actual, File expected) throws IOException
	{
		String actFile = FileUtils.readFileToString(actual, Utils.UTF8),
				expFile = FileUtils.readFileToString(expected, Utils.UTF8);
		assertEquals(actFile, expFile);
	}
	
	private Matrix createMatrix()
	{
		Matrix matrix = new Matrix(new MvelVariablesFactory(new EnvVars(), new GlobalConstants()));
		matrix.setName("");
		matrix.setFileName("");
		matrix.setSuccessful(true);
		matrix.setStepSuccessful(STEP_NAME, true);
		
		return matrix;
	}
	
	private Step createStep()
	{
		return new DefaultStep();
	}
	
	private ReportTemplatesProcessor createTemplatesProcessor() throws TemplateModelException, IOException
	{
		return new ReportTemplatesProcessor(resDir.resolve("templates"));
	}
}