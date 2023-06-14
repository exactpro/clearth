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

package com.exactprosystems.clearth.connectivity.remotehand;

import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class RhUtilsTest
{
	@Test
	public void parseParameters()
	{
		Map<String, String> expected = new LinkedHashMap<>();
		expected.put("file", "myFile");
		expected.put("param1", "value1");
		expected.put("param 2", " value 2");
		expected.put("invalidParam", "");
		Map<String, String> actual = 
				RhUtils.parseParameters(" file='myFile',\tparam1=value1,  'param 2' = ' value 2', invalidParam");
		Assert.assertEquals(actual, expected);
	}
	
	@Test
	public void parseParametersFromNull()
	{
		Map<String, String> result = RhUtils.parseParameters(null);
		Assert.assertNotNull(result);
		Assert.assertTrue(result.isEmpty());
	}
	
	@Test
	public void parseParametersFromBlankLine()
	{
		Map<String, String> result = RhUtils.parseParameters("\t ");
		Assert.assertNotNull(result);
		Assert.assertTrue(result.isEmpty());
	}
	
	
	@Test
	public void scriptWithIncludes() throws Exception
	{
		ClassLoader cl = ClassLoader.getSystemClassLoader();
		File file = FileUtils.toFile(cl.getResource("rh/mainScript.csv")),
				completeFile = FileUtils.toFile(cl.getResource("rh/completeScript.csv"));
		
		String actual = RhUtils.getScriptFromFile(file.toPath());
		Assert.assertNotNull(actual);
		
		String expected = FileUtils.readFileToString(completeFile, StandardCharsets.UTF_8)
				.replace("\r\n", "\n");
		actual = actual.replace(RhUtils.LINE_SEPARATOR, "\n");
		Assert.assertEquals(actual, expected);
	}
}
