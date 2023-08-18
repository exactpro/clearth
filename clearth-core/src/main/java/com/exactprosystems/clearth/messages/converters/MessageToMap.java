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

package com.exactprosystems.clearth.messages.converters;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.text.StringTokenizer;

import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.SimpleClearThMessage;

public class MessageToMap implements MessageConverter<Map<String, Object>>
{
	public static final String SUBMSGKIND = "SubMsgKind",
			KIND_MAP = "Map",
			KIND_LIST = "List",
			FLAT_DELIMITER_DEFAULT = "_";
	
	private static final Set<String> SERVICE_FIELDS = Set.of(ClearThMessage.MSGTYPE, ClearThMessage.SUBMSGTYPE, 
			ClearThMessage.SUBMSGSOURCE, SUBMSGKIND);
	
	
	private final String flatDelimiter;
	private final Set<String> serviceFields;
	
	public MessageToMap()
	{
		this(FLAT_DELIMITER_DEFAULT, SERVICE_FIELDS);
	}
	
	public MessageToMap(String flatDelimiter)
	{
		this(flatDelimiter, SERVICE_FIELDS);
	}
	
	public MessageToMap(Set<String> serviceFields)
	{
		this(FLAT_DELIMITER_DEFAULT, serviceFields);
	}
	
	public MessageToMap(String flatDelimiter, Set<String> serviceFields)
	{
		this.flatDelimiter = flatDelimiter;
		this.serviceFields = serviceFields;
	}
	
	
	public Map<String, Object> convert(SimpleClearThMessage message) throws ConversionException
	{
		Map<String, Object> fieldsMap = new LinkedHashMap<>();
		addFieldsToMap(message, fieldsMap);
		return fieldsMap;
	}
	
	
	protected void addFieldsToMap(SimpleClearThMessage message, Map<String, Object> dest) throws ConversionException
	{
		for (String fn : message.getFieldNames())
		{
			if (serviceFields.contains(fn))
				continue;
			
			addFieldToMap(fn, message.getField(fn), dest);
		}
		
		addSubMessages(message.getSubMessages(), dest);
	}
	
	protected void addFieldToMap(String fieldName, String fieldValue, Map<String, Object> dest) throws ConversionException
	{
		if (StringUtils.isEmpty(flatDelimiter) || !fieldName.contains(flatDelimiter))
		{
			dest.put(fieldName, fieldValue);
			return;
		}
		
		addFieldToMap(new StringTokenizer(fieldName, flatDelimiter), fieldValue, dest);
	}
	
	protected void addFieldToMap(StringTokenizer tokens, String fieldValue, Map<String, Object> dest) throws ConversionException
	{
		String name = tokens.nextToken();
		if (!tokens.hasNext())  //No next token = current token is field name
		{
			dest.put(name, fieldValue);
			return;
		}
		
		Map<String, Object> map;
		String nextToken = tokens.nextToken();
		if (NumberUtils.isParsable(nextToken))  //Token is number = it is index in list with sub-messages
		{
			List<Map<String, Object>> list = getList(name, dest);
			int index = getIndex(nextToken, tokens.getContent());
			map = getMap(list, index, name);
		}
		else
		{
			//Token was not list index, i.e. we are not working with list. 
			//Rewinding to previous token so that it will be used as field or map name.
			tokens.previousToken();
			map = getMap(name, dest);
		}
		
		addFieldToMap(tokens, fieldValue, map);
	}
	
	protected void addSubMessages(List<SimpleClearThMessage> subMessages, Map<String, Object> dest) throws ConversionException
	{
		if (subMessages.isEmpty())
			return;
		
		for (SimpleClearThMessage sub : subMessages)
		{
			String type = getMandatoryParamField(ClearThMessage.SUBMSGTYPE, sub),
					kind = getKind(sub);
			
			Map<String, Object> subMessageFields = createFieldsMap();
			if (KIND_LIST.equals(kind))
			{
				List<Map<String, Object>> list = getList(type, dest);
				list.add(subMessageFields);
			}
			else
			{
				if (dest.containsKey(type))
					throw subMessageReassignsField(type, dest.get(type));
				dest.put(type, subMessageFields);
			}
			
			addFieldsToMap(sub, subMessageFields);
		}
	}
	
	protected final String getMandatoryParamField(String fieldName, SimpleClearThMessage message) throws ConversionException
	{
		String result = message.getField(fieldName);
		if (StringUtils.isEmpty(result))
			throw new ConversionException(String.format("Action '%s' doesn't have mandatory parameter #%s", 
					message.getField(ClearThMessage.SUBMSGSOURCE), fieldName));
		return result;
	}
	
	protected String getKind(SimpleClearThMessage message) throws ConversionException
	{
		String kind = message.getField(SUBMSGKIND);
		if (StringUtils.isEmpty(kind))
			return KIND_MAP;
		
		if (KIND_MAP.equals(kind) || KIND_LIST.equals(kind))
			return kind;
		
		String actionId = message.getField(ClearThMessage.SUBMSGSOURCE);
		if (!StringUtils.isEmpty(actionId))
			throw new ConversionException(String.format("Action '%s' has unsupported value of parameter #%s (%s). Only %s and %s are supported", 
					actionId, SUBMSGKIND, kind, KIND_MAP, KIND_LIST));
		throw new ConversionException(String.format("Unsupported %s (%s). Only %s and %s are supported",
				SUBMSGKIND, kind, KIND_MAP, KIND_LIST));
	}
	
	protected int getIndex(String number, String value) throws ConversionException
	{
		try
		{
			int index = Integer.parseInt(number)-1;  //Index is zero-based, but user will specify it as one-based
			if (index < 0)
				throw invalidIndex(number, value, null);
			return index;
		}
		catch (NumberFormatException e)
		{
			throw invalidIndex(number, value, e);
		}
	}
	
	
	protected Map<String, Object> getMap(String name, Map<String, Object> fields) throws ConversionException
	{
		Object container = fields.get(name);
		if (container == null)
		{
			Map<String, Object> map = createFieldsMap();
			fields.put(name, map);
			return map;
		}
		
		if (!(container instanceof Map))
			throw subMessageReassignsField(name, container);
		
		return (Map<String, Object>) container;
	}
	
	protected Map<String, Object> getMap(List<Map<String, Object>> list, int index, String listName) throws ConversionException
	{
		if (index > list.size())
			throw new ConversionException(String.format("Not enough elements in list '%s' (%s) to set element %s", listName, list.size(), index));
		
		if (index < list.size())
			return list.get(index);
		
		Map<String, Object> map = createFieldsMap();
		list.add(map);
		return map;
	}
	
	protected List<Map<String, Object>> getList(String name, Map<String, Object> fields) throws ConversionException
	{
		Object container = fields.get(name);
		if (container == null)
		{
			List<Map<String, Object>> list = createMessagesList();
			fields.put(name, list);
			return list;
		}
		
		if (!(container instanceof List))
			throw subMessageListReassignsField(name, container);
		
		return (List<Map<String, Object>>) container;
	}
	
	protected Map<String, Object> createFieldsMap()
	{
		return new LinkedHashMap<>();
	}
	
	protected List<Map<String, Object>> createMessagesList()
	{
		return new ArrayList<>();
	}
	
	
	private ConversionException subMessageReassignsField(String subMessageType, Object existingFieldValue)
	{
		return new ConversionException(String.format("Cannot add sub-message as field '%s' to the message. Message already contains field '%s' of class %s",
				subMessageType, subMessageType, existingFieldValue.getClass()));
	}
	
	private ConversionException subMessageListReassignsField(String subMessageType, Object existingFieldValue)
	{
		return new ConversionException(String.format("Cannot add sub-message as element of list '%s' to the message. Message already contains field '%s' of class %s",
				subMessageType, subMessageType, existingFieldValue.getClass()));
	}
	
	private ConversionException invalidIndex(String number, String value, Throwable cause)
	{
		String msg = String.format("Invalid list index %s in '%s'", number, value);
		return cause != null ? new ConversionException(msg, cause) : new ConversionException(msg);
	}
}
