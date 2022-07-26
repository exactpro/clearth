/******************************************************************************
 * Copyright 2009-2022 Exactpro Systems Limited
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

package com.exactprosystems.clearth.connectivity.validation;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.exactprosystems.clearth.connectivity.flat.FlatMessageDictionary;
import com.exactprosystems.clearth.utils.DictionaryLoadException;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.exactprosystems.clearth.xmldata.XmlAdditionalParameter;

public class FlatDictionaryTest
{
	private static final Path FLAT_DICTIONARY_TEST_OUTPUT_DIR = Paths.get("src/test/resources/CodecTests/dicts");
	private static final String SETTINGS_FILE = "flatDict.xml";

	@DataProvider(name = "flat-dictionaries")
	public Object[][] createTestDictionaries()
			throws IOException, DictionaryLoadException
	{
		FlatMessageDictionary simpleDictionary = new FlatMessageDictionary(getFilename(), null);
		Map<String, String> parameters = getTestParameters();
		FlatMessageDictionary dictionaryWithParameters = new FlatMessageDictionary(getFilename(), parameters);
		return new Object[][] 
			{
				{simpleDictionary, null},
				{dictionaryWithParameters, parameters}
			};
	}

	@Test(dataProvider = "flat-dictionaries")
	public void checkIfDictionaryConstructorWorks(FlatMessageDictionary dict, Map<String, String> expectedParameters)
	{	
		Assert.assertEquals(dict.getAdditionalParameters(), expectedParameters);
	}

	private String getFilename() 
	{
		return FLAT_DICTIONARY_TEST_OUTPUT_DIR.resolve(SETTINGS_FILE).toString();
	}

	private Map<String, String> getTestParameters() 
	{
		Map<String, String> result = new LinkedHashMap<String, String>();
		result.put("aaa", "bbb");
		result.put("ccc", "ddd");
		return result;
	}
}