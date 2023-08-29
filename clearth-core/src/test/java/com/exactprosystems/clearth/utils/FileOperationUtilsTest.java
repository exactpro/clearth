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

package com.exactprosystems.clearth.utils;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.*;

public class FileOperationUtilsTest
{
	private static final Path TEST_OUTPUT = Paths.get("testOutput").resolve(FileOperationUtilsTest.class.getSimpleName());

	@BeforeClass
	public void init() throws IOException
	{
		FileUtils.deleteDirectory(TEST_OUTPUT.toFile());
		Files.createDirectory(TEST_OUTPUT);
	}

	@Test
	public void testCreateCsvFile() throws IOException
	{
		String[] header = new String[] {"Param1", "Param2", "Param3"};
		File file = FileOperationUtils.createCsvFile(TEST_OUTPUT.resolve("file.csv").toFile(), header, createTable());

		String actualData = FileUtils.readFileToString(file, Utils.UTF8);
		String expectedData = "Param1,Param2,Param3\n" +
								"Val1,Val2,Val3\n" +
								"Field1,Field2,Field3\n" +
								"123,456,789\n";

		assertEquals(actualData, expectedData);
	}

	private List<String[]> createTable()
	{
		List<String[]> list = new ArrayList<>();
		list.add(new String[] {"Val1", "Val2", "Val3"});
		list.add(new String[] {"Field1", "Field2", "Field3"});
		list.add(new String[] {"123", "456", "789"});
		return list;
	}
}