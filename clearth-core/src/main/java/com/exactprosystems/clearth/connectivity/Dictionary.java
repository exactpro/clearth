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

package com.exactprosystems.clearth.connectivity;

import com.exactprosystems.clearth.connectivity.iface.MessageValidatorCondition;
import com.exactprosystems.clearth.connectivity.iface.RegexCondition;
import com.exactprosystems.clearth.connectivity.message.DictionaryDesc;
import com.exactprosystems.clearth.connectivity.message.MessageCondition;
import com.exactprosystems.clearth.connectivity.message.MessageDesc;
import com.exactprosystems.clearth.utils.DictionaryLoadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.exactprosystems.clearth.connectivity.iface.ClearThMessage.MSGTYPE;
import static java.lang.String.format;

public abstract class Dictionary<T extends MessageDesc, D extends DictionaryDesc>
{
	private static final Logger logger = LoggerFactory.getLogger(Dictionary.class);
	
	public static final String MSG_TYPE_NOT_FOUND = "Unable to identify message. Field '" + MSGTYPE + "' is absent.";
	public static final String MSG_DESC_NOT_FOUND_IN_DICTIONARY = "Message definition not found in dictionary.";
	public static final String MSG_DESC_TYPE_NOT_FOUND_IN_DICTIONARY = "Message definition with type '%s' not found in dictionary.";
	public static final String MSG_DESC_DOES_NOT_FITS = "Message definition with type '%s' doesn't fit.";
	public static final String ERROR_WHILE_PARSING = "Error occurred while loading dictionary";

	protected final D dictionaryDesc;

	protected Map<String, List<MessageValidatorCondition>> typeConditionsMap = new LinkedHashMap<String, List<MessageValidatorCondition>>();
	protected Map<String, List<MessageValidatorCondition>> conditionsMap = new LinkedHashMap<String, List<MessageValidatorCondition>>();
	protected Map<String, T> messageDescMap;
	protected List<T> messageDescList;

	protected Dictionary(String fileName) throws DictionaryLoadException
	{
		dictionaryDesc = loadDictionary(fileName);
		processDictionary(dictionaryDesc);
	}

	protected Dictionary(Reader reader) throws DictionaryLoadException
	{
		dictionaryDesc = loadDictionary(reader);
		processDictionary(dictionaryDesc);
	}

	public static String msgDescWithTypeNotFoundError(String msgType)
	{
		return format(MSG_DESC_TYPE_NOT_FOUND_IN_DICTIONARY, msgType);
	}

	public static String msgDescDoesNotFitError(String msgType)
	{
		return format(MSG_DESC_DOES_NOT_FITS, msgType);
	}
	
	protected Unmarshaller createUnmarshaller() throws JAXBException
	{
		return JAXBContext.newInstance(getClassesToBeBound()).createUnmarshaller();
	}
	
	abstract protected Class[] getClassesToBeBound();

	protected D getDictionaryDesc()
	{
		return dictionaryDesc;
	}
	
	@SuppressWarnings("unchecked")
	protected D loadDictionary(String fileName) throws DictionaryLoadException
	{
		logger.trace("Reading dictionary from file '{}'", fileName);
		try
		{
			Unmarshaller u = createUnmarshaller();
			return (D)u.unmarshal(new StreamSource(new File(fileName)));
		}
		catch (Exception e)
		{
			throw new DictionaryLoadException(ERROR_WHILE_PARSING, e);
		}
	}
	
	@SuppressWarnings("unchecked")
	protected D loadDictionary(Reader reader) throws DictionaryLoadException
	{
		logger.trace("Reading dictionary");
		try
		{
			Unmarshaller u = createUnmarshaller();
			return (D)u.unmarshal(new StreamSource(reader));
		}
		catch (Exception e)
		{
			throw new DictionaryLoadException(ERROR_WHILE_PARSING, e);
		}
	}
	
	
	public Map<String, List<MessageValidatorCondition>> getTypeConditionsMap()
	{
		return typeConditionsMap;
	}

	public Map<String, List<MessageValidatorCondition>> getConditionsMap()
	{
		return conditionsMap;
	}

	public List<MessageValidatorCondition> getTypeConditions(String messageDescType)
	{
		return typeConditionsMap.get(messageDescType);
	}

	public List<MessageValidatorCondition> getConditions(String messageDescType)
	{
		return conditionsMap.get(messageDescType);
	}

	protected void addToConditionsMaps(T messageDesc)
	{
		List<MessageValidatorCondition> typeConditionList = buildConditions(messageDesc.getTypeCondition());
		if(typeConditionList != null)
			typeConditionsMap.put(messageDesc.getType(), typeConditionList);

		List<MessageValidatorCondition> conditionList = buildConditions(messageDesc.getCondition());
		if(conditionList != null)
			conditionsMap.put(messageDesc.getType(), conditionList);
		
		buildExtendedConditions(messageDesc);
	}

	protected List<MessageValidatorCondition> buildConditions(List<MessageCondition> conditions)
	{
		if (conditions.isEmpty())
			return null;

		List<MessageValidatorCondition> result = new ArrayList<MessageValidatorCondition>();
		for (MessageCondition c : conditions)
			result.add(new RegexCondition(c.getValue(), c.isInvert()));
		return result;
	}

	/**
	 * Override this method to build custom codec-specific conditions
	 * @param messageDesc Message desc
	 */
	protected void buildExtendedConditions(@SuppressWarnings("unused") T messageDesc) {/*Nothing to do by default*/}

	public List<T> getMessageDescs()
	{
		return messageDescList;
	}

	public Map<String, T> getMessageDescMap()
	{
		return messageDescMap;
	}

	public T getMessageDesc(String messageDescType)
	{
		return messageDescMap.get(messageDescType);
	}

	protected Map<String, T> convertToMessageDescMap(List<T> messageDescList) throws DictionaryLoadException
	{
		Map<String, T> messageDescMap = new LinkedHashMap<String, T>();
		for (T messageDesc : messageDescList)
		{
			String messageDescType = messageDesc.getType();
			if(messageDescMap.containsKey(messageDescType))
			{
				throw new DictionaryLoadException("Dictionary contains multiple descriptions of type '"+messageDescType+"'");
			}
			addToConditionsMaps(messageDesc);
			messageDescMap.put(messageDescType, messageDesc);
		}
		return messageDescMap;
	}

	protected abstract List<T> getMessageDescs(D dictionaryDesc);

	protected void initMessageDescList(D dictionaryDesc)
	{
		messageDescList = getMessageDescs(dictionaryDesc);
	}

	protected void initMessageDescMap(List<T> messageDescList) throws DictionaryLoadException
	{
		messageDescMap = convertToMessageDescMap(messageDescList);
	}

	protected void solveReferencesToCommonFields(D dictionary) throws DictionaryLoadException
	{
	}
	
	protected void processDictionary(D dictionary) throws DictionaryLoadException
	{
		solveReferencesToCommonFields(dictionary);
		initMessageDescList(dictionary);
		initMessageDescMap(messageDescList);
	}
}
