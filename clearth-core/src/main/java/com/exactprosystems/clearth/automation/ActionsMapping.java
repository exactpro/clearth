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
package com.exactprosystems.clearth.automation;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.utils.KeyValueUtils;
import com.exactprosystems.clearth.utils.Pair;
import com.exactprosystems.clearth.utils.SettingsException;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ActionsMapping
{
	private static final String REFERENCE_CHAR = "%";

	private final Path configFilePath;
	private final Map<String, ActionMetaData> descriptions;

	public ActionsMapping(Path configFilePath, boolean actionNameToLowerCase) throws SettingsException
	{
		this.configFilePath = configFilePath;
		try 
		{
			Map<String,String> actionsMap = KeyValueUtils.loadKeyValueFile(configFilePath, false);
			descriptions = parseActions(actionsMap, actionNameToLowerCase);
		}
		catch (IOException e)
		{
			throw new SettingsException("Could not load actions mapping", e);
		}
		
	}

	public ActionsMapping(boolean actionNameToLowerCase) throws SettingsException
	{
		this(Paths.get(ClearThCore.rootRelative(ClearThCore.configFiles().getActionsMappingFileName())), actionNameToLowerCase);
	}

	public Path getConfigFilePath()
	{
		return configFilePath;
	}

	public Map<String, ActionMetaData> getDescriptions()
	{
		return descriptions;
	}

	public Map<String, String> getParamsByActionName(String actionName)
	{
		return getDescriptions().get(actionName).getDefaultInputParams();
	}

	//For these two methods we can also use a package name instead of action name
	public String getClassByActionName(String actionName)
	{
		return getDescriptions().get(actionName).getClazz();
	}
	public String getNameByActionName(String actionName)
	{
		return getDescriptions().get(actionName).getName();
	}


	private Map<String, ActionMetaData> parseActions(Map<String,String> actionsMap, boolean actionNameToLowerCase) throws SettingsException
	{
		Map<String, ActionMetaData> descriptions = new LinkedHashMap<>();

		Pattern patternForParamName = Pattern.compile("^[a-zA-Z_][\\w]*$");

		for (Map.Entry<String, String> actionEntry : actionsMap.entrySet())
		{
			String name = actionEntry.getKey();
			String data = actionEntry.getValue().trim(); //class + default input parameters
			String clazz;
			List<String> defaultInputParamsList = new ArrayList<>();

			do {
				clazz = splitActionClassAndParams(data, defaultInputParamsList);
				clazz = replaceReferenceCharacters(clazz, actionsMap);
				data = clazz;
			} while (clazz.contains(REFERENCE_CHAR));

			//Build Action Description
			ActionMetaData metaData = new ActionMetaData(name, clazz);
			LinkedHashMap<String, String> defaultInputParams = parseDefaultInputParams(defaultInputParamsList, name, patternForParamName);
			metaData.addDefaultInputParams(defaultInputParams);
			descriptions.put(actionNameToLowerCase ? name.toLowerCase() : name, metaData);
		}
		return descriptions;
	}

	private String splitActionClassAndParams(String data, List<String> defaultInputParamsList) throws SettingsException
	{
		String clazz;
		String defaultInputParamsStr;
		int bracketPos = data.indexOf('(');

		if (bracketPos < 0)
			return data;

		clazz = data.substring(0, bracketPos).trim();
		defaultInputParamsStr = data.substring(bracketPos).trim();
		if (defaultInputParamsStr.charAt(defaultInputParamsStr.length()-1) != ')')
		{
			throw new SettingsException("Action parameter line contains unclosed brace: '" + defaultInputParamsStr + "'. " +
					"Parameters should be closed with ')'");
		}
		else
			defaultInputParamsStr = defaultInputParamsStr.substring(1, defaultInputParamsStr.length() - 1);

		if(defaultInputParamsStr.isEmpty())
			throw new SettingsException("Action parameter line contains braces, " +
					"but does not have any parameters in it");

		defaultInputParamsList.add(defaultInputParamsStr);
		return clazz;
	}

	private String replaceReferenceCharacters(String clazz, Map<String,String> actions) throws SettingsException
	{
			int start = clazz.indexOf(REFERENCE_CHAR);
			if(start < 0)
				return clazz;

			start = start + 1;
			int end = clazz.indexOf(REFERENCE_CHAR, start);
			if (end<0)
			{
				throw new SettingsException("Actions mapping contains unclosed reference: '"+clazz+"'. " +
						"Reference should be closed with " + REFERENCE_CHAR);
			}

			String ref = clazz.substring(start, end);
			String refValue = actions.get(ref);
			if (refValue == null)
			{
				throw new SettingsException("Actions mapping contains unsolvable reference: '"+ref+"'. " +
						"It's value should be declared before referencing it");
			}

			clazz = clazz.replace(REFERENCE_CHAR + ref + REFERENCE_CHAR, refValue);

		return clazz;
	}

	private LinkedHashMap<String, String> parseDefaultInputParams(List<String> defaultInputParamsList, String actionName,
																  	Pattern patternForParamName) throws SettingsException
	{
		LinkedHashMap<String, String> params = new LinkedHashMap<>();

		for (String line :
				defaultInputParamsList)
		{
			line = line.trim();
			int start;
			int current = 0;
			try {
				do
				{
					start = 0;

					// Parse parameter name
					Pair<String, String> pair = KeyValueUtils.parseKeyValueString(line, false);
					String pairKey = pair.getFirst();
					if (pairKey == null)
						throwExcIfNoEqualsChar(line, actionName);

					pairKey = pairKey.trim();
					validateParamName(patternForParamName, pairKey);

					// Parse parameter value
					String pairValue = pair.getSecond();
					if (pairValue == null)
						throwExcIfNoEqualsChar(line, actionName);

					pairValue = pairValue.trim();
					String value;

					//If value is quoted
					if(pairValue.startsWith("\""))
					{
							current++;
							while (pairValue.charAt(current) != '\"')
							{
								if (pairValue.charAt(current) == '\\') // Skip 2 characters for escape characters
									current++;
								current++;
							}
							value = pairValue.substring(start + 1, current);

							// Replace escape characters
							if (value.contains("\\"))
							{
								value = value.replace("\\\\", "\\").
										replace("\\\"", "\"").
										replace("\\t", "\t").
										replace("\\n", "\n").
										replace("\\r", "\r");
							}

							while (current != pairValue.length() && pairValue.charAt(current) != ',')
								current++;

							if (current != pairValue.length())
								current++;

							line = pairValue.substring(current);
					}
					else
					{
						// Simple value: name=SimpleName
						while (current != pairValue.length() && pairValue.charAt(current) != ',')
							current++;

						if (current != pairValue.length())
						{
							current++;
							value = pairValue.substring(start, current-1).trim();
						}else
							value = pairValue.substring(start, current).trim();

						line = pairValue.substring(current);
					}
					line = line.trim();
					current = 0;

					//If we want to override some default parameter in the nested action
					if(!params.containsKey(pairKey))
						params.put(pairKey, value);

				}while (line.length() > 0);
			} catch (StringIndexOutOfBoundsException e) {
				throw new SettingsException("Error while parsing definition of '" + actionName + "' action. " +
						"Quoted parameter value is not closed: " + line);
			}
		}
		return params;
	}


	private void validateParamName(Pattern patternForParamName, String paramName) throws SettingsException
	{
		Matcher matcherParamName = patternForParamName.matcher(paramName);
		if (!matcherParamName.matches())
		{
			throw new SettingsException("Parameter name must contain letters, " +
					"numbers and '_' character only. The first character of the parameter name " +
					"can be a letter or '_' character only." + " Actual parameter name: '" + paramName + "'");
		}
	}

	private void throwExcIfNoEqualsChar(String line,String actionName) throws SettingsException
	{
		throw new SettingsException("Error while parsing default input parameters for '" + actionName + "' action."
				+ "Wrong default input parameters format. "
				+ "There is no '=' character: '" + line + "', it has to be in format 'key=value'");
	}
}
