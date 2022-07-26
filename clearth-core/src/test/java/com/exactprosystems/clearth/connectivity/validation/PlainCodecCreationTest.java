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

import org.testng.annotations.Test;

import com.exactprosystems.clearth.connectivity.iface.DefaultCodecFactory;
import com.exactprosystems.clearth.xmldata.XmlAdditionalParameter;
import com.exactprosystems.clearth.xmldata.XmlCodecConfig;
import com.exactprosystems.clearth.xmldata.XmlParameterList;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PlainCodecCreationTest
{
	
	@Test
	public void buildCodec() throws Exception
	{	
		DefaultCodecFactory factory = new DefaultCodecFactory();
		factory.createCodec(generateConfig(null, null));
	}

	@Test(expectedExceptions=IllegalArgumentException.class)
	public void testBuildFail() throws Exception
	{	
		DefaultCodecFactory factory = new DefaultCodecFactory();
		factory.createCodec(generateConfig(null, getTestParameters()));
	}

	private Map<String, String> getTestParameters() 
	{
		Map<String, String> result = new LinkedHashMap<String, String>();
		result.put("aaa", "bbb");
		result.put("ccc", "ddd");
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
		result.setCodec("com.exactprosystems.clearth.connectivity.validation.PlainCodec");
		result.setName("TestCodecConfig");
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