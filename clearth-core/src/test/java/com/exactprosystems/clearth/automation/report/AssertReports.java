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

import com.exactprosystems.clearth.automation.SchedulerTest;
import com.exactprosystems.clearth.helpers.JsonAssert;
import com.exactprosystems.clearth.utils.Utils;
import org.apache.commons.io.FileUtils;
import org.testng.Assert;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class AssertReports
{
	public static void assertAllReports(Path actualReportsDir, Path expectedReportsDir, boolean html, boolean failed, boolean json) throws IOException
	{
		assertCompleteHtmlReports(actualReportsDir.resolve(ActionReportWriter.HTML_REPORT_NAME),
				expectedReportsDir.resolve(ActionReportWriter.HTML_REPORT_NAME),
				html);
		assertFailedHtmlReports(actualReportsDir.resolve(ActionReportWriter.HTML_FAILED_REPORT_NAME),
				expectedReportsDir.resolve(ActionReportWriter.HTML_FAILED_REPORT_NAME),
				failed);
		assertCompleteJsonReports(actualReportsDir.resolve(ActionReportWriter.JSON_REPORT_NAME),
				expectedReportsDir.resolve(ActionReportWriter.JSON_REPORT_NAME),
				json);
	}
	
	public static void assertCompleteHtmlReports(Path actualReport, Path expectedReport, boolean exists) throws IOException
	{
		assertHtmlReports(actualReport, expectedReport, exists, "HTML report");
	}
	
	public static void assertFailedHtmlReports(Path actualReport, Path expectedReport, boolean exists) throws IOException
	{
		assertHtmlReports(actualReport, expectedReport, exists, "Failed HTML report");
	}
	
	public static void assertCompleteJsonReports(Path actualReport, Path expectedReport, boolean exists) throws IOException
	{
		assertJsonReports(actualReport, expectedReport, exists, "JSON report");
	}
	
	
	private static void assertHtmlReports(Path actualReport, Path expectedReport, boolean exists, String reportType) throws IOException
	{
		if (exists)
		{
			Assert.assertEquals(readFileToString(actualReport),
					readFileToString(expectedReport),
					reportType);
		}
		else
			Assert.assertFalse(Files.exists(actualReport), reportType+" exists");
	}
	
	private static void assertJsonReports(Path actualReport, Path expectedReport, boolean exists, String reportType) throws IOException
	{
		if (exists)
		{
			new JsonAssert().setIgnoredValueNames(SchedulerTest.IGNORED_EXPECTED_PARAMS)
					.assertEquals(expectedReport.toFile(), actualReport.toFile());
		}
		else
			Assert.assertFalse(Files.exists(actualReport), reportType+" exists");
	}
	
	private static String readFileToString(Path file) throws IOException
	{
		return FileUtils.readFileToString(file.toFile(), Utils.UTF8)
				.replace("\r\n", "\n");
	}
}