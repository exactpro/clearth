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

package com.exactprosystems.clearth.connectivity.fix;

import com.exactprosystems.clearth.connectivity.iface.ClearThMessageMetadata;
import com.exactprosystems.clearth.utils.KeyValueUtils;
import quickfix.*;

import java.util.Map;
import java.util.Set;

public class FixMessage extends Message
{
	private static final long serialVersionUID = 8600927875065120379L;
	public static final String SOH = String.valueOf('\001'),
			PIPE = "|";
	
	private final ClearThMessageMetadata metadata;
	
	/**
	 * Creates empty message. Order of added fields will be natural, i.e. fields will be sorted by tag number
	 */
	public FixMessage()
	{
		super();
		this.metadata = null;
	}
	
	/**
	 * Creates empty message. Order of added fields will correspond to given fields information
	 * @param fieldsInfo information about fields that allows to have them in desired order
	 */
	public FixMessage(FieldsInfo fieldsInfo)
	{
		this(fieldsInfo, null);
	}
	
	/**
	 * Creates empty message. Order of added fields will correspond to given fields information
	 * @param fieldsInfo information about fields that allows to have them in desired order
	 * @param metadata to attach to created message
	 */
	public FixMessage(FieldsInfo fieldsInfo, ClearThMessageMetadata metadata)
	{
		super(fieldsInfo.getFieldsArray());
		this.metadata = metadata;
	}
	
	/**
	 * Creates message by given string. Fields will be ordered according to given fieldsInfo
	 * @param message to take tags and values from
	 * @param metadata to attach to created message
	 * @param fieldsInfo information about fields that allows to have them in desired order
	 * @param sessionDictionary to find header and trailer tag names
	 * @param appDictionary to find body tag names
	 * @param validate flag to perform validation of given message against dictionary
	 * @throws InvalidMessage in case when given message string doesn't represent valid FIX message
	 */
	public FixMessage(String message, ClearThMessageMetadata metadata, FieldsInfo fieldsInfo, 
			DataDictionary sessionDictionary, DataDictionary appDictionary, boolean validate) throws InvalidMessage
	{
		this(fieldsInfo, metadata);
		message = prepareMessageText(message);
		fromString(message, sessionDictionary != null ? sessionDictionary : appDictionary, appDictionary, validate);
	}
	
	/**
	 * Creates message by given string. Fields will be ordered according to information obtained from dictionary by message type
	 * @param message to take tags and values from
	 * @param metadata to attach to created message
	 * @param sessionDictionary to find header and trailer tag names. If message is not defined in appDictionary, will look for its tags order in sessionDictionary
	 * @param appDictionary to find order of tags that should be in message body. Also is used to find tag names while parsing message
	 * @param validate flag to perform validation of given message against dictionary
	 * @return FixMessage object with fields ordered according to dictionary
	 * @throws InvalidMessage in case when given message string doesn't represent valid FIX message
	 * @throws ConfigError when type of message is not defined in dictionary
	 */
	public static FixMessage createFromStringByDictionary(String message, ClearThMessageMetadata metadata,
			ClearThDataDictionary sessionDictionary, ClearThDataDictionary appDictionary, boolean validate) throws InvalidMessage, ConfigError
	{
		message = prepareMessageText(message);
		String type = MessageUtils.getMessageType(message);
		FieldsInfo fieldsInfo = appDictionary.getMessageFieldsInfo(type);
		if (fieldsInfo == null)
		{
			if (sessionDictionary != null)
				fieldsInfo = sessionDictionary.getMessageFieldsInfo(type);
			
			if (fieldsInfo == null)
				throw new ConfigError("No fields info stored for message type '"+type+"'");
		}
		
		return new FixMessage(message, metadata, fieldsInfo, sessionDictionary, appDictionary, validate);
	}
	
	/**
	 * Creates message by given string. Order of fields will be the same as in given message string
	 * @param message to take tags and values from
	 * @param metadata to attach to created message
	 * @param sessionDictionary to find header and trailer tag names
	 * @param appDictionary to find body tag names
	 * @param validate flag to perform validation of given message against dictionary
	 * @return FixMessage object with fields order preserved
	 * @throws InvalidMessage in case when given message string doesn't represent valid FIX message
	 */
	public static FixMessage createFromString(String message, ClearThMessageMetadata metadata,
			DataDictionary sessionDictionary, DataDictionary appDictionary, boolean validate) throws InvalidMessage
	{
		message = prepareMessageText(message);
		FieldsInfo fieldsInfo = parseFieldsInfo(message);
		return new FixMessage(message, metadata, fieldsInfo, sessionDictionary, appDictionary, validate);
	}
	
	
	protected static FieldsInfo parseFieldsInfo(String message)
	{
		Map<String, String> tags = KeyValueUtils.parseKeyValueString(message, SOH, false, new String[0]);
		FieldsInfo info = new FieldsInfo();
		Set<Integer> infoFields = info.getFields();
		for (String t : tags.keySet())
			infoFields.add(Integer.parseInt(t));
		return info;
	}
	
	
	/**
	 * Fixes message text to be parseable as FIX message.
	 * This way we support messages with fields delimited by |, not only by SOH. 
	 * FIX messages are often shown delimited with | just to be clearly visible. We can get such text as input.
	 * @param message text to prepare for parsing
	 * @return message text acceptable by {@link Message}
	 */
	protected static String prepareMessageText(String message)
	{
		if (!message.contains(SOH))
			return message.replace(PIPE, SOH);
		return message;
	}
	
	
	public ClearThMessageMetadata getMetadata()
	{
		return metadata;
	}
	
	public String toPipedString()
	{
		String s = toString();
		return s.replace(SOH, PIPE);
	}
}
