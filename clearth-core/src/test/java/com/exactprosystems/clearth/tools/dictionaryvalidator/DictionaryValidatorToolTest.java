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

package com.exactprosystems.clearth.tools.dictionaryvalidator;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.exactprosystems.clearth.connectivity.CodecsStorage;
import com.exactprosystems.clearth.connectivity.iface.DefaultCodecFactory;
import com.exactprosystems.clearth.connectivity.iface.ICodecFactory;
import com.exactprosystems.clearth.tools.DictionaryValidationResult;
import com.exactprosystems.clearth.tools.DictionaryValidatorError;
import com.exactprosystems.clearth.tools.DictionaryValidatorTool;
import com.exactprosystems.clearth.tools.MessageParserTool;
import com.exactprosystems.clearth.xmldata.XmlCodecConfig;

public class DictionaryValidatorToolTest
{
	private final Path resources = Path.of("src", "test", "resources", DictionaryValidatorToolTest.class.getSimpleName());
	
	@Test
	public void xmlValidation() throws Exception
	{
		String msg = FileUtils.readFileToString(resources.resolve("msg.xml").toFile(), StandardCharsets.UTF_8);
		
		String codecName = "OneLine";
		
		CodecsStorage codecs = createCodecsStorage(codecName, OneLineCodec.class);
		ICodecFactory codecFactory = new DefaultCodecFactory();
		DictionaryValidatorTool tool = new DictionaryValidatorTool(() -> new MessageParserTool(codecs, codecFactory));
		
		List<DictionaryValidatorError> errors = new ArrayList<>();
		DictionaryValidationResult result = tool.validateDictionary(msg, codecName, DictionaryValidatorTool.XML_CONFIG, errors);
		
		Assert.assertEquals(result.isValidatedSuccessfully(), true, "Validation result");
		Assert.assertEquals(errors.size(), 0, "Errors count");
	}
	
	
	private CodecsStorage createCodecsStorage(String codecName, Class<?> codecClass)
	{
		XmlCodecConfig config = new XmlCodecConfig();
		config.setName(codecName);
		config.setCodec(codecClass.getCanonicalName());
		
		return new CodecsStorage(Collections.singletonList(config));
	}
}