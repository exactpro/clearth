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

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.utils.ComparisonUtils;
import com.exactprosystems.clearth.utils.Utils;
import org.apache.commons.io.FileUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static com.exactprosystems.clearth.utils.CollectionUtils.map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

public class MatrixMakerFromConfigToolTest
{
	private static final Path USER_DIR = Paths.get(System.getProperty("user.dir"));
	private static final Path RES_DIR =  USER_DIR.resolve("src").resolve( "test").resolve("resources");
	private static final Path TEST_DIR =  Paths.get("testOutput", MatrixMakerFromConfigToolTest.class.getSimpleName());
	private static final Path RES_FILES =  RES_DIR.resolve("functions");
	private static final File RES_CFG_FILE = RES_DIR.resolve(MatrixMakerFromConfigToolTest.class.getSimpleName()).resolve("config.csv").toFile();
	private static final String INSTANCE = "instance";
	private MatrixMakerFromConfigTool mmTool;

	@BeforeClass
	public void init() throws Exception
	{
		ClearThCore application = mock(ClearThCore.class, CALLS_REAL_METHODS);
		setStaticField(ClearThCore.class, INSTANCE, application);
		
		ComparisonUtils comparisonUtils = new ComparisonUtils();
		when(application.getComparisonUtils()).thenReturn(comparisonUtils);
		
		FileUtils.deleteDirectory(TEST_DIR.toFile());
		Files.createDirectories(TEST_DIR);
		
		mmTool = createMMToolConfig();
	}
	
	@AfterClass
	public void clear() throws ReflectiveOperationException
	{
		setStaticField(ClearThCore.class, INSTANCE, null);
	}
	
	@Test
	public void testReadConfigsFromFile() throws Exception
	{
		List<Map<String, String>> list = mmTool.readConfigsFromFile(new ArrayList<>(), RES_CFG_FILE);
		assertThat(list).usingRecursiveComparison().isEqualTo(createConfigList());
	}
	
	@Test
	public void testGenerate() throws Exception
	{
		mmTool.generate();
		String actual = FileUtils.readFileToString(TEST_DIR.resolve("GeneratedMatrices").resolve("matrixDir").resolve("1.csv").toFile(), Utils.UTF8);
		String expected = FileUtils.readFileToString(RES_FILES.resolve("expected.csv").toFile(), Utils.UTF8);
		assertEquals(actual, expected);
	}

	private List<Map<String, String>> createConfigList()
	{
		List<Map<String, String>> list = new ArrayList<>();
		list.add(map("Template", "src/test/resources/functions/template.csv", "Config", "src/test/resources/functions/data.csv",
				"TemplateName", "Matrix1"));
		return list;
	}
	
	private MatrixMakerFromConfigTool createMMToolConfig() throws Exception
	{
		return new MatrixMakerFromConfigTool(List.of(RES_CFG_FILE), TEST_DIR.toFile(),
				TEST_DIR.toFile(), null, USER_DIR.toFile(), true, true);
	}

	private void setStaticField(Class<?> clazz, String fieldName, Object fieldValue) throws ReflectiveOperationException
	{
		Field field = clazz.getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(clazz, fieldValue);
	}
}