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

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.connectivity.flat.FlatMessageCodec;
import com.exactprosystems.clearth.connectivity.flat.FlatMessageDictionary;
import com.exactprosystems.clearth.connectivity.iface.DefaultCodecFactory;
import com.exactprosystems.clearth.utils.DictionaryLoadException;
import com.exactprosystems.clearth.xmldata.XmlAdditionalParameter;
import com.exactprosystems.clearth.xmldata.XmlCodecConfig;
import com.exactprosystems.clearth.xmldata.XmlParameterList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class FlatCodecTest
{
	private static final Path FLAT_DICTIONARY_TEST_OUTPUT_DIR = Paths.get("src/test/resources/CodecTests/dicts");
	private static final String SETTINGS_FILE = "flatDict.xml";
	private int argNumber = 0;

	@DataProvider(name = "flat-codecs-configs")
	public Object[][] createTestCodecs()
			throws IOException, DictionaryLoadException
	{
		copySettingsDict();
		Map<String, String> dictionaryParameters = getTestParameters();
		Map<String, String> codecParameters = getTestParameters();
		return new Object[][] 
			{
				{generateConfig(null, null), Collections.emptyMap(), Collections.emptyMap()},
				{generateConfig(null, codecParameters), Collections.emptyMap(), codecParameters},
				{generateConfig(dictionaryParameters, codecParameters), dictionaryParameters, codecParameters},
				{generateConfig(dictionaryParameters, null), dictionaryParameters, Collections.emptyMap()}
			};
	}

	@Test(dataProvider = "flat-codecs-configs")
	public void checkIfCodecFactoryWorks(XmlCodecConfig config, Map<String, String> expectedDictionaryParameters, 
			Map<String, String> expectedCodecParameters) throws Exception
	{	
		DefaultCodecFactory factory = new DefaultCodecFactory();
		FlatMessageCodec codec = (FlatMessageCodec) factory.createCodec(config);
		Assert.assertEquals(codec.getCodecParameters(), expectedCodecParameters);
		Assert.assertEquals(codec.getDictionaryParameters(), expectedDictionaryParameters);
	} 

	private Path getFilename() 
	{
		return FLAT_DICTIONARY_TEST_OUTPUT_DIR.resolve(SETTINGS_FILE);
	}

	private void copySettingsDict() throws IOException 
	{
		Files.copy(getFilename(), Paths.get(ClearThCore.getInstance().getDictsPath()).resolve(SETTINGS_FILE), REPLACE_EXISTING);
	}

	private Map<String, String> getTestParameters() 
	{
		Map<String, String> result = new LinkedHashMap<String, String>();
		result.put("aaa" + String.valueOf(++argNumber), "bbb");
		result.put("ccc" + String.valueOf(++argNumber), "ddd");
		return result;
	}

	private XmlAdditionalParameter generateParameter(String key, String value) 
	{
		XmlAdditionalParameter result = new XmlAdditionalParameter();
		result.setName(key);
		result.setValue(value);
		return result;
	}

	private XmlCodecConfig generateConfig(Map<String, String> dictionaryParameters, Map<String, String> codecParameters) 
	{
		XmlCodecConfig result = new XmlCodecConfig();
		if (dictionaryParameters != null) 
		{
			result.setDictionaryParameters(convertParameters(dictionaryParameters));
		}
		if (codecParameters != null) 
		{
			result.setCodecParameters(convertParameters(codecParameters));
		}
		result.setCodec("com.exactprosystems.clearth.connectivity.flat.FlatMessageCodec");
		result.setName("FlatMessage");
		result.setDictionary("com.exactprosystems.clearth.connectivity.flat.FlatMessageDictionary");
		result.setDictionaryFile(SETTINGS_FILE);
		return result;
	}

	private XmlParameterList convertParameters(Map<String, String> toAdd) 
	{
		List<XmlAdditionalParameter> parameters = new ArrayList<XmlAdditionalParameter>();
		for (Map.Entry<String, String> element : toAdd.entrySet()) 
		{
			parameters.add(generateParameter(element.getKey(), element.getValue()));
		}
		XmlParameterList result = new XmlParameterList();
		result.getParameter().addAll(parameters);
		return result;
	}
}