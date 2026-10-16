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

package com.exactprosystems.clearth.tools.matrixmaker;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.utils.ComparisonUtils;
import com.exactprosystems.clearth.utils.Utils;

import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

public class MatrixGeneratorTest
{
	private static final String CLEAR_TH_CORE_INSTANCE_NAME = "instance",
								PATH_WITH_INCORRECT_DATA = "incorrectData";
	private final Path resources = Path.of("src", "test", "resources"),
						output = Path.of("testOutput");
	
	@BeforeClass
	public void init() throws Exception
	{
		ClearThCore application = mock(ClearThCore.class, CALLS_REAL_METHODS);
		setStaticField(ClearThCore.class, CLEAR_TH_CORE_INSTANCE_NAME, application);
		
		ComparisonUtils comparisonUtils = new ComparisonUtils();
		when(application.getComparisonUtils()).thenReturn(comparisonUtils);
		
		FileUtils.deleteDirectory(output.toFile());
		Files.createDirectories(output);
	}
	
	@AfterClass
	public void clear() throws ReflectiveOperationException
	{
		setStaticField(ClearThCore.class, CLEAR_TH_CORE_INSTANCE_NAME, null);
	}
	
	@Test
	public void actionPropertiesInExpression() throws GenerateException, IOException
	{
		Path testResources = resources.resolve("actionProps");
		assertGenerateMatrix(testResources.resolve("template.csv"), testResources.resolve("data.csv"), output.resolve("actionProps").resolve("result.csv"),
				testResources.resolve("expected.csv"));
	}
	
	@Test
	public void functions() throws GenerateException, IOException
	{
		Path testResources = resources.resolve("functions");
		assertGenerateMatrix(testResources.resolve("template.csv"), testResources.resolve("data.csv"), output.resolve("functions").resolve("result.csv"),
				testResources.resolve("expected.csv"));
	}

	@DataProvider(name = "dataFiles")
	public Object[][] dataFiles()
	{
		return new Object[][]
		{
			{"data1.csv", "Error while reading row #1 from file 'data1.csv' with MVEL filter: row.exp1 > 10"},
			{"data2.csv", "File data2.csv, row #2: number of columns (4) does not match header (1)"}
		};
	}
	
	@Test(dataProvider = "dataFiles", expectedExceptions = GenerateException.class)
	public void incorrectData(String dataFile, String errMsg) throws IOException, GenerateException
	{
		Path testResources = resources.resolve(PATH_WITH_INCORRECT_DATA);
		try
		{
			assertGenerateMatrix(testResources.resolve("template1.csv"), testResources.resolve(dataFile),
					output.resolve(PATH_WITH_INCORRECT_DATA).resolve("result.csv"), null);
		}
		catch (GenerateException e)
		{
			assertEquals(e.getMessage(), errMsg);
			throw e;
		}
	}
	
	private void assertGenerateMatrix(Path template, Path dataFile, Path outputFile, Path expectedFile) throws GenerateException, IOException
	{
		Path outputDir = outputFile.getParent();
		Files.createDirectories(outputDir);
		
		MatrixGenerator mg = new MatrixGenerator(Collections.singletonList(template.toFile()), 
				Collections.singletonList(dataFile.toFile()), 
				outputDir.toFile(), false, false, outputFile.getFileName().toString());
		mg.generate();
		
		String expected = FileUtils.readFileToString(expectedFile.toFile(), Utils.UTF8),
				actual = FileUtils.readFileToString(outputDir.resolve(template.getFileName()).toFile(), Utils.UTF8);
		Assert.assertEquals(actual, expected);
	}
	
	private void setStaticField(Class<?> clazz, String fieldName, Object fieldValue) throws ReflectiveOperationException
	{
		Field field = clazz.getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(clazz, fieldValue);
	}
}
