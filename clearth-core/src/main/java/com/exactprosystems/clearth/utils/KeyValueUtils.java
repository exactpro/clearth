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

package com.exactprosystems.clearth.utils;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.exactprosystems.clearth.utils.Utils.closeResource;

public class KeyValueUtils
{
	private static final Logger logger = LoggerFactory.getLogger(KeyValueUtils.class);
	
	private static final String KEY_VALUE_DELIM = "=",
			PAIRS_DELIM = "\n",
			URL_PARAMS_START = "?",
			URL_PARAMS_DELIM = "&";
	
	private static String getSplittedKey(String pair, int index, boolean keyToLowerCase)
	{
		if (keyToLowerCase)
			return pair.substring(0, index).toLowerCase().trim();
		else
			return pair.substring(0, index).trim();
	}

	private static String getSplittedValue(String pair, int index)
	{
		return pair.substring(index + 1);
	}

	private static Pair<String, String> parseKVPair(String pair, boolean keyToLowerCase)
	{
		Pair<String, String> keyValue = new Pair<String, String>();
		int index = pair.indexOf(KEY_VALUE_DELIM);

		if (index < 0) {
			keyValue.setFirst(getSplittedKey(pair, pair.length(), keyToLowerCase));
			keyValue.setSecond(null);
			return keyValue;
		}
		
		keyValue.setFirst(getSplittedKey(pair, index, keyToLowerCase));
		keyValue.setSecond(getSplittedValue(pair, index));
		return keyValue;
	}
	
	private static void parseAndPutKVPair(Map<String, String> map, String pair, boolean keyToLowerCase, String... ignoredStartChars)
	{
		if (ignoredStartChars != null)
		{
			for (String startChar : ignoredStartChars)
			{
				if ((pair.startsWith(startChar)))
					return;
			}
		}

		if (StringUtils.isBlank(pair))
			return;

		Pair<String, String> keyValue = parseKVPair(pair, keyToLowerCase);
		if (keyValue.getFirst() == null)
			return;

		map.put(keyValue.getFirst(), keyValue.getSecond());
	}
	
	public static LinkedHashMap<String, String> loadKeyValueFile(String fileName, boolean keyToLowerCase) throws IOException
	{
		LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
		BufferedReader reader = null;
		try
		{
			reader = new BufferedReader(new FileReader(fileName));
			String line;
			while ((line = reader.readLine())!=null)
				parseAndPutKVPair(result, line, keyToLowerCase, "#");
		}
		finally
		{
			closeResource(reader);
		}
		return result;
	}

	public static LinkedHashMap<String, String> loadKeyValueFile(Path filePath, boolean keyToLowerCase) throws IOException
	{
		return loadKeyValueFile(filePath.toString(),keyToLowerCase);
	}

	public static LinkedHashMap<String, String> parseKeyValueString(String inputText, String pairDelimiter, boolean keyToLowerCase, String... ignoredStartChars)
	{
		LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
		for (String kvPair : inputText.split(pairDelimiter))
			parseAndPutKVPair(result, kvPair, keyToLowerCase, ignoredStartChars);
		return result;
	}

	public static LinkedHashMap<String, String> parseKeyValueString(String inputText, String pairDelimiter, boolean keyToLowerCase)
	{
		String ignoredChar = "#";
		return parseKeyValueString(inputText, pairDelimiter, keyToLowerCase, ignoredChar);
	}

	public static Pair<String, String> parseKeyValueString(String inputText)
	{
		return parseKVPair(inputText, false);
	}

	public static Pair<String, String> parseKeyValueString(String inputText, boolean keyToLowerCase)
	{
		return parseKVPair(inputText, keyToLowerCase);
	}
	
	public static void saveKeyValueFile(Map<String, String> data, String fileName) throws IOException
	{
		PrintWriter writer = null;
		try
		{
			writer = new PrintWriter(fileName);
			for (String key : data.keySet())
				writer.println(key+KEY_VALUE_DELIM+data.get(key));
			writer.flush();
		}
		finally
		{
			closeResource(writer);
		}
	}

	public static String getKeyValueString(Map<String, String> data)
	{
		if (data == null || data.isEmpty())
			return "";

		StringBuilder result = new StringBuilder();

		for (Map.Entry<String, String> entry : data.entrySet())
		{
			result.append(entry.getKey())
				  .append(KEY_VALUE_DELIM)
				  .append(entry.getValue())
				  .append(PAIRS_DELIM);
		}

		return result.toString();
	}
	
	/**
	 @see #parseKeyValueString(String, String, boolean, String...)
	 @deprecated This method is deprecated
	 */
	@Deprecated
	public static LinkedHashMap<String, String> splitKeysValues(String inputText, String pairDelimiter, boolean keyToLowerCase)
	{
		return parseKeyValueString(inputText, pairDelimiter, keyToLowerCase, new String[0]);
	}
	
	
	private static String getUrlParameters(String url)
	{
		int start = url.indexOf(URL_PARAMS_START);
		if (start < 0)
			return null;
		
		return url.substring(start+URL_PARAMS_START.length());
	}
	
	public static LinkedHashMap<String, String> parseUrlParameters(String url, boolean namesToLowerCase)
	{
		String params = getUrlParameters(url);
		if (params == null)
			return null;
		return parseKeyValueString(params, URL_PARAMS_DELIM, namesToLowerCase);
	}
	
	public static String getUrlParameter(String url, String paramName)
	{
		String params = getUrlParameters(url);
		if (params == null)
			return null;
		
		String[] keysValues = params.split(URL_PARAMS_DELIM);
		for (String kv : keysValues)
		{
			Pair<String, String> pair = parseKVPair(kv, false);
			if (paramName.equals(pair.getFirst()))
				return pair.getSecond();
		}
		return null;
	}
}
