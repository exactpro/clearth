/******************************************************************************
 * Copyright 2009-2020 Exactpro Systems Limited
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

package com.exactprosystems.clearth.connectivity.iface;

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.xmldata.XmlAdditionalParameter;
import com.exactprosystems.clearth.xmldata.XmlCodecConfig;
import com.exactprosystems.clearth.xmldata.XmlParameterList;

public class DefaultCodecFactory implements ICodecFactory
{
	private static final Logger logger = LoggerFactory.getLogger(DefaultCodecFactory.class);

	public ICodec createCodec(XmlCodecConfig config) throws Exception
	{
		Map<String, String> dictionaryParameters = convertParametersToMap(getParameterList(config.getDictionaryParameters()));
		Map<String, String> codecParameters = convertParametersToMap(getParameterList(config.getCodecParameters()));

		if (StringUtils.isEmpty(config.getDictionaryFile())) 
		{
			try 
			{
				return (ICodec) Class.forName(config.getCodec()).getDeclaredConstructor(Map.class).newInstance(codecParameters);
			} 
			catch (NoSuchMethodException e) 
			{
				if (codecParameters.isEmpty())
				{
					logger.warn("Codecs of class " + config.getCodec() + 
						" use outdated constructor. Please consider supporting constructor with additional arguments", e);
					return (ICodec) Class.forName(config.getCodec()).getDeclaredConstructor().newInstance();
				}
				throw new IllegalArgumentException("Additional arguments are not supported for codecs of class " + config.getCodec(), e);
			}
		}
		
		String xmlFile = ClearThCore.getInstance().getDictsPath()+config.getDictionaryFile();
		
		Object dictionary = null;
		try 
		{
			dictionary = Class.forName(config.getDictionary()).getDeclaredConstructor(String.class, Map.class).newInstance(xmlFile, dictionaryParameters);
		} 
		catch (NoSuchMethodException e) 
		{
			if (dictionaryParameters.isEmpty())
			{
				logger.warn("Dictionaries of class " + config.getDictionary() + 
						" use outdated constructor. Please consider supporting constructor with additional arguments", e);
				dictionary = Class.forName(config.getDictionary()).getDeclaredConstructor(String.class).newInstance(xmlFile);
			}
			else
			{
				throw new IllegalArgumentException("Additional arguments are not supported for dictionaries of class " + config.getDictionary(), e);
			}
		}

		try 
		{
			return (ICodec) Class.forName(config.getCodec()).getDeclaredConstructor(dictionary.getClass(), Map.class).newInstance(dictionary, codecParameters);
		} 
		catch (NoSuchMethodException e) 
		{
			if (codecParameters.isEmpty())
			{
				logger.warn("Codecs of class " + config.getCodec() + 
						" use outdated constructor. Please consider supporting constructor with additional arguments", e);
				return (ICodec) Class.forName(config.getCodec()).getDeclaredConstructor(dictionary.getClass()).newInstance(dictionary);
			}
			throw new IllegalArgumentException("Additional arguments are not supported for codecs of class " + config.getCodec(), e);
		}
	}

	private List<XmlAdditionalParameter> getParameterList(XmlParameterList source) {
		if (source == null)
			return null;
		return source.getParameter();
	}

	private Map<String, String> convertParametersToMap(List<XmlAdditionalParameter> parameters) {
		Map<String, String> result = new HashMap<>();
		if (parameters == null)
			return result;
		for (XmlAdditionalParameter tmp : parameters) {
			result.put(tmp.getName(), tmp.getValue());
		}
		return result;
	}
}
