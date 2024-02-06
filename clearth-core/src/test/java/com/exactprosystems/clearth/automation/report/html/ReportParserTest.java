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

import com.exactprosystems.clearth.utils.FileOperationUtils;
import com.exactprosystems.clearth.utils.StringOperationUtils;
import com.exactprosystems.clearth.utils.Utils;
import org.apache.commons.io.FileUtils;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.testng.Assert.assertEquals;

public class ReportParserTest
{
	private static final Path TEST_OUTPUT = Paths.get("testOutput").resolve(ReportParserTest.class.getSimpleName());
	private Path resDir;

	@BeforeClass
	public void init() throws IOException
	{
		resDir = Paths.get(FileOperationUtils.resourceToAbsoluteFilePath(ReportParserTest.class.getSimpleName()));

		FileUtils.deleteDirectory(TEST_OUTPUT.toFile());
		Files.createDirectory(TEST_OUTPUT);
	}

	@Test
	public void testWriteMatrix() throws IOException
	{
		ReportParser reportParser = new ReportParser();
		File matrixFile = reportParser.writeMatrix(resDir.resolve("report.html").toFile(), TEST_OUTPUT.toString());

		String actualData = FileUtils.readFileToString(matrixFile, Utils.UTF8);
		String expectedData = StringOperationUtils.multilineString(System.lineSeparator(),
				"#id,#globalstep,#action,#execute,#timeout,#Instrument,#Currency,#Quantity",
				"id1,Step1,SetStatic,y,500,AAD,BBQ,100");
		assertEquals(actualData, expectedData);
	}
}