/******************************************************************************
 * Copyright 2009-2019 Exactpro Systems Limited
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

package com.exactprosystems.clearth.tools;

import com.exactprosystems.clearth.connectivity.EncodeException;
import com.exactprosystems.clearth.connectivity.iface.ICodec;
import com.exactprosystems.clearth.utils.Utils;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Difference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.UnmarshalException;

public class DictionaryValidatorTool
{
	private static final Logger logger = LoggerFactory.getLogger(DictionaryValidatorTool.class);
	
	public static final String AUTO_FORMAT = "auto",
			NO_LINE_SEPARATION_CONFIG = "No line separation", NO_LINE_SEPARATION_API = "nolineseparation",
			BY_CHARACTERS_CONFIG = "By characters", BY_CHARACTERS_API = "bycharacters",
			XML_CONFIG = "Xml", XML_API = "xml";
	
	protected static final String SUCCESS_MESSAGE = "Dictionary validated successfully";
	
	protected static final Set<String> VALIDATION_CONFIGS = new LinkedHashSet<String>(
			Arrays.asList(NO_LINE_SEPARATION_CONFIG, BY_CHARACTERS_CONFIG, XML_CONFIG));
	
	//errorsOutput is not part of result because method may fail with exception but other not so critical errors should be seen as well
	public DictionaryValidationResult validateDictionary(String originalText, String textToParseFormat,
			String validationConfig, List<DictionaryValidatorError> errorsOutput) throws Exception
	{
		if (StringUtils.isEmpty(originalText))
			throw new IllegalArgumentException("Nothing to validate: original text is empty");
		
		DictionaryValidationResult result = new DictionaryValidationResult();
		result.setDictionaryName(textToParseFormat);
		result.setOriginalText(originalText);
		
		encodeOriginalText(result, textToParseFormat, errorsOutput);
		
		if (StringUtils.isEmpty(validationConfig))
			validationConfig = getDefaultValidationConfig();
		result.setValidationConfigName(validationConfig);
		
		switch (validationConfig)
		{
			case NO_LINE_SEPARATION_CONFIG:
				validateWithoutFormating(result);
				break;
			case BY_CHARACTERS_CONFIG:
				validateWithFormating(result);
				break;
			case XML_CONFIG:
				validateXml(result);
				break;
			default:
				throw new IllegalArgumentException("Invalid config name: "+validationConfig);
		}
		return result;
	}
	
	protected void encodeOriginalText(DictionaryValidationResult result, String textToParseFormat, List<DictionaryValidatorError> errors) throws EncodeException
	{	
		String origingalText = result.getOriginalText();
		
		MessageParserTool msgParser = new MessageParserTool();
		msgParser.parseText(origingalText, textToParseFormat);
		
		boolean parsingError = false;
		
		if (StringUtils.isEmpty(msgParser.getParsedText()) && !msgParser.getExceptionMap().isEmpty())
		{
			parsingError = true;
			for (Map.Entry<String, Exception> entry : msgParser.getExceptionMap().entrySet())
			{
				if (entry.getValue() instanceof UnmarshalException)
					handleException(entry.getKey() + ": could not load dictionary", entry.getValue(), errors);
				else
					handleException(entry.getKey() + ": could not parse text", entry.getValue(), errors);
			}
		}
		
		ICodec codec = msgParser.getCodec();
		if (codec == null || parsingError)
			throw new IllegalArgumentException("Unable to parse text with '"+textToParseFormat+"' format");
		
		result.setDictionaryName(msgParser.getCodecName());
		result.setEncodedText(codec.encode(msgParser.getParsedMsg()));
	}
	
	protected void validateWithoutFormating(DictionaryValidationResult result)
	{
		validateText(result.getOriginalText().replace(Utils.EOL, ""), result.getEncodedText().replace(Utils.EOL, ""), result);
	}
	
	protected void validateWithFormating(DictionaryValidationResult result)
	{
		validateText(result.getOriginalText(), result.getEncodedText(), result);
	}
	
	protected void validateText(String original, String encoded, DictionaryValidationResult result)
	{
		Character[] originalChars = ArrayUtils.toObject(original.toCharArray());
		Character[] encodedChars = ArrayUtils.toObject(encoded.toCharArray());
		
		List<Integer> charDifferencesIndexes = compareArrays(originalChars, encodedChars);
		
		boolean validatedSuccessfully = charDifferencesIndexes.isEmpty();
		result.setValidatedSuccessfully(validatedSuccessfully);
		result.setValidationDetails(validatedSuccessfully ? SUCCESS_MESSAGE :
				"Different chars indexes: "+charDifferencesIndexes.toString().replaceAll("[\\[\\]]", ""));
	}
	
	protected void validateXml(DictionaryValidationResult result) throws SAXException, IOException
	{
		String original = result.getOriginalText().replace(Utils.EOL, "");
		String encoded = result.getEncodedText().replace(Utils.EOL, "");
		
		DetailedDiff detailedDiff = null;
		detailedDiff = new DetailedDiff(new Diff(original, encoded));
		
		List<Difference> allDifferences = new ArrayList<Difference>();
		if (detailedDiff != null)
			allDifferences = detailedDiff.getAllDifferences();
		
		boolean validatedSuccessfully = allDifferences.isEmpty();
		result.setValidatedSuccessfully(validatedSuccessfully);
		
		String validationDetails = SUCCESS_MESSAGE;
		if (!validatedSuccessfully)
		{
			StringBuilder detailsBuilder = new StringBuilder("Differences: ");
			for (Difference diff : allDifferences)
			{
				detailsBuilder.append(Utils.EOL+diff.toString());
			}
			validationDetails = detailsBuilder.toString();
		}
		result.setValidationDetails(validationDetails);
	}
	
	protected List<Integer> compareArrays(Object[] array1, Object[] array2)
	{
		List<Integer> differencesIndexes = new ArrayList<Integer>();
		
		int count = Math.min(array1.length, array2.length);
		for (int i = 0; i < count; i++)
		{
			if (!array1[i].equals(array2[i]))
				differencesIndexes.add(i);
		}
		
		Object[] largerArray = (array1.length == array2.length) ? null :
								(array1.length > array2.length) ? array1 : array2;
		if (largerArray != null)
		{
			for (int i = count; i < largerArray.length; i++)
			{
				differencesIndexes.add(i);
			}
		}
		
		return differencesIndexes;
	}
	
	
	protected void handleException(String message, Exception e, List<DictionaryValidatorError> errors)
	{
		logger.warn(message, e);
		if (errors != null)
			errors.add(new DictionaryValidatorError(message, e));
	}
	
	public static String getDefaultValidationConfig()
	{
		return NO_LINE_SEPARATION_CONFIG;
	}
	
	public static Set<String> getValidationConfigs()
	{
		return VALIDATION_CONFIGS;
	}
	
	public static String getValidationConfigByApi(String apiValue)
	{
		switch (apiValue)
		{
			case NO_LINE_SEPARATION_API:
				return NO_LINE_SEPARATION_CONFIG;
			case BY_CHARACTERS_API:
				return BY_CHARACTERS_CONFIG;
			case XML_API:
				return XML_CONFIG;
			default:
				throw new IllegalArgumentException("Invalid api value: "+apiValue);
		}
	}
}
