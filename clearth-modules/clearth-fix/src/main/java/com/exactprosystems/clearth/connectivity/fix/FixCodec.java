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

import com.exactprosystems.clearth.connectivity.DecodeException;
import com.exactprosystems.clearth.connectivity.EncodeException;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.ICodec;
import com.exactprosystems.clearth.connectivity.iface.SimpleClearThMessage;
import com.exactprosystems.clearth.utils.SpecialValue;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.*;
import quickfix.DataDictionary.GroupInfo;
import quickfix.Message.Header;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Simple codec that uses QuickFIX/J dictionary to process FIX messages to/from strings. 
 * Useful for "Message parser" and other such tools.
 */
public class FixCodec implements ICodec
{
	private static final Logger logger = LoggerFactory.getLogger(FixCodec.class);
	
	public static final String DEFAULT_CODEC_NAME = "FIX";
	
	protected final ClearThDataDictionary appDictionary,
			transportDictionary;
	protected final int typeTag;
	protected final Set<String> serviceFields;
	private final Map<String, String> codecParameters;
	
	public FixCodec(FixDictionary dictionary)
	{
		this(dictionary, null);
	}
	
	public FixCodec(FixDictionary dictionary, Map<String, String> codecParameters)
	{
		this.appDictionary = dictionary.getAppDictionary();
		this.transportDictionary = dictionary.getTransportDictionary();
		typeTag = dictionary.getTypeTag();
		serviceFields = createServiceFields();
		this.codecParameters = codecParameters;
	}
	
	public Map<String, String> getCodecParameters()
	{
		return codecParameters;
	}
	
	@Override
	public String encode(ClearThMessage<?> message) throws EncodeException
	{
		Message result = encodeToFixMessage(message);
		return result.toString();
	}
	
	@Override
	public ClearThMessage<?> decode(String message) throws DecodeException
	{
		return decode(message, null);
	}

	@Override
	public ClearThMessage<?> decode(String message, String messageType) throws DecodeException
	{
		Message fixMessage;
		try
		{
			fixMessage = FixMessage.createFromStringByDictionary(message, null, transportDictionary, appDictionary, false);
			checkValidity(fixMessage);
			
			if (messageType == null)
				messageType = getMessageType(fixMessage);
		}
		catch (InvalidMessage e)
		{
			throw new DecodeException("Could not parse message", e);
		}
		catch (ConfigError e)
		{
			throw new DecodeException("Message is not described in dictionary", e);
		}
		catch (FieldNotFound e)
		{
			throw new DecodeException("Could not find 'MsgType' field in message", e);
		}
		
		FieldsInfo fieldsInfo = appDictionary.getMessageFieldsInfo(messageType);
		Set<Integer> fields = fieldsInfo != null ? fieldsInfo.getFields() : null;
		
		SimpleClearThMessage result = createMessage();
		result.setEncodedMessage(message);
		result.addField(ClearThMessage.MSGTYPE, messageType);
		decodeHeader(fixMessage, messageType, result);
		decodeFields(fixMessage, fields, messageType, null, result);
		return result;
	}
	
	
	public String getMessageType(Message fixMessage) throws FieldNotFound
	{
		return fixMessage.getHeader().getString(typeTag);
	}
	
	public boolean isGroupField(FieldMap fields, int tag)
	{
		return fields.getGroupCount(tag) > 0;
	}
	
	public Message encodeToFixMessage(ClearThMessage<?> message) throws EncodeException
	{
		FieldsInfo fieldsInfo = getFieldsInfo(message);
		
		Message result = new FixMessage(fieldsInfo);
		encodeFields(message, result);
		encodeGroups(message, result);
		return result;
	}
	
	
	protected void checkValidity(Message fixMessage) throws DecodeException
	{
		Exception e = fixMessage.getException();
		if (e != null)
			throw new DecodeException("Could not decode message", e);
	}
	
	protected Set<String> createServiceFields()
	{
		Set<String> result = new HashSet<String>();
		result.add(ClearThMessage.MSGTYPE);
		result.add(ClearThMessage.SUBMSGTYPE);
		result.add(ClearThMessage.SUBMSGSOURCE);
		return result;
	}
	
	protected int getTagFromDictionary(DataDictionary dictionary, String name) throws EncodeException  //"throws" makes extensions able to add strict validation
	{
		return dictionary.getFieldTag(name);
	}
	
	protected int getTagFromDictionary(String name) throws EncodeException
	{
		int tag;
		try
		{
			tag = getTagFromDictionary(appDictionary, name);
		}
		catch (EncodeException e)
		{
			if (transportDictionary == null)
				throw e;
			
			try
			{
				return getTagFromDictionary(transportDictionary, name);
			}
			catch (EncodeException e1)
			{
				e1.addSuppressed(e);
				throw e1;
			}
		}
		
		if (tag < 0 && transportDictionary != null)
			return getTagFromDictionary(transportDictionary, name);
		return tag;
	}
	
	
	protected boolean isHeaderField(int tag)
	{
		if (appDictionary.isHeaderField(tag))
			return true;
		return transportDictionary != null && transportDictionary.isHeaderField(tag);
	}
	
	protected boolean isTrailerField(int tag)
	{
		if (appDictionary.isTrailerField(tag))
			return true;
		return transportDictionary != null && transportDictionary.isTrailerField(tag);
	}

	
	//*** ClearThFixMessage to FIX message conversion (encoding)***
	
	protected FieldsInfo getFieldsInfo(ClearThMessage<?> message) throws EncodeException
	{
		String msgType = message.getField(ClearThMessage.MSGTYPE);
		if (StringUtils.isEmpty(msgType))
			throw new EncodeException("'"+ClearThMessage.MSGTYPE+"' is not defined");
		
		FieldsInfo info = appDictionary.getMessageFieldsInfo(msgType);
		if (info == null)
			throw new EncodeException("No fields info in dictionary for message '"+msgType+"'");
		return info;
	}
	
	protected boolean isServiceField(String fieldName)
	{
		return serviceFields.contains(fieldName);
	}
	
	protected void encodeField(StringField field, Message result)
	{
		int tag = field.getTag();
		if (isHeaderField(tag))
		{
			result.getHeader().setField(field);
			return;
		}
		
		try
		{
			if (isTrailerField(tag))
			{
				result.getTrailer().setField(field);
				return;
			}
		}
		catch (Exception e)  //In some cases access to trailer results in NullPointerException
		{
			logger.warn("Could not add field '{}' to trailer", field, e);
		}
		
		result.setField(field);
	}
	
	protected boolean skipEncodingField(String name, String value)
	{
		return StringUtils.isBlank(value);  //Blank fields may lead to FIX errors. Skipping them by default
	}
	
	protected String transformValue(String value)
	{
		return SpecialValue.convert(value);
	}
	
	protected StringField createField(String name, String value) throws EncodeException
	{
		if (skipEncodingField(name, value))
			return null;
		
		int tag = getTagFromDictionary(name);
		if (tag < 0)
			return null;
		
		value = transformValue(value);
		return new StringField(tag, value);
	}
	
	protected void encodeFields(ClearThMessage<?> message, Message result) throws EncodeException
	{
		for (String fieldName : message.getFieldNames())
		{
			StringField field = createField(fieldName, message.getField(fieldName));
			if (field == null)
				continue;
			
			encodeField(field, result);
		}
	}
	
	protected void encodeFields(ClearThMessage<?> message, Group result) throws EncodeException
	{
		for (String fieldName : message.getFieldNames())
		{
			StringField field = createField(fieldName, message.getField(fieldName));
			if (field == null)
				continue;
			
			result.setField(field);
		}
	}
	
	protected String getSubMsgDelimiterField(ClearThMessage<?> subMsg)
	{
		for (String fn : subMsg.getFieldNames())
		{
			if (isServiceField(fn))
				continue;
			return fn;  //In FIX messages first RG field is a groups delimiter
		}
		return null;
	}
	
	protected Group createGroup(int type, int delimiter)
	{
		return new Group(type, delimiter);
	}
	
	protected Group createGroup(ClearThMessage<?> subMsg) throws EncodeException
	{
		String subMsgType = subMsg.getField(ClearThMessage.SUBMSGTYPE),
				delimiterField = getSubMsgDelimiterField(subMsg);
		if (delimiterField == null)
			return null;
		
		int rgTag = getTagFromDictionary(subMsgType),
				rgDelimiter = getTagFromDictionary(delimiterField);
		
		if (rgTag < 0)
			throw new EncodeException("Unknown RG tag '"+subMsgType+"'");
		
		if (rgDelimiter < 0)
			throw new EncodeException("Unknown RG delimiter '"+delimiterField+"'");
		
		return createGroup(rgTag, rgDelimiter);
	}
	
	protected void encodeGroups(ClearThMessage<?> message, FieldMap result) throws EncodeException
	{
		if (!message.hasSubMessages())
			return;
		
		for (ClearThMessage<?> subMsg : message.getSubMessages())
		{
			Group g = createGroup(subMsg);
			encodeFields(subMsg, g);
			encodeGroups(subMsg, g);
			
			result.addGroup(g);
		}
	}
	
	
	//*** FIX message to ClearThMessage conversion (decoding) ***
	
	protected SimpleClearThMessage createMessage()
	{
		return new SimpleClearThMessage();
	}
	
	protected boolean decodeField(Field<?> f, DataDictionary dictionary, ClearThMessage<?> result)
	{
		String fieldName = dictionary.getFieldName(f.getTag());
		if (fieldName == null)
			return false;
		
		result.addField(fieldName, f.getObject().toString());
		return true;
	}
	
	protected boolean decodeField(Field<?> f, ClearThMessage<?> result)
	{
		if (decodeField(f, appDictionary, result))
			return true;
		return transportDictionary != null ? decodeField(f, transportDictionary, result) : false;
	}
	
	protected void decodeHeader(Message fixMessage, String msgType, SimpleClearThMessage result)
	{
		Header h = fixMessage.getHeader();
		if (h == null)
			return;
		
		decodeFields(h, null, msgType, null, result);
	}
	
	protected String getGroupType(Field<?> f, DataDictionary dictionary)
	{
		return dictionary.getFieldName(f.getTag());
	}
	
	protected String getGroupType(Field<?> f)
	{
		return getGroupType(f, appDictionary);
	}
	
	
	protected void decodeGroup(Group g, String groupType, Set<Integer> groupFields, String msgType, int[] groupPath, SimpleClearThMessage result)
	{
		SimpleClearThMessage subMsg = createMessage();
		subMsg.addField(ClearThMessage.SUBMSGTYPE, groupType);
		decodeFields(g, groupFields, msgType, groupPath, subMsg);
		
		result.addSubMessage(subMsg);
	}
	
	protected void decodeFields(FieldMap fields, Set<Integer> availableFields, String msgType, int[] groupPath, SimpleClearThMessage result)
	{
		Iterator<Field<?>> it = fields.iterator();
		while (it.hasNext())
		{
			Field<?> f = it.next();
			int tag = f.getTag();
			if (availableFields != null && !availableFields.contains(tag))
				continue;
			
			if (!isGroupField(fields, tag))
			{
				decodeField(f, result);
				continue;
			}
			
			int[] subGroupPath = createGroupPath(groupPath, tag);
			String groupType = getGroupType(f);
			Set<Integer> groupFields = getGroupFields(msgType, subGroupPath);
			//groupFields can be null if group is not defined in dictionary. We still can decode the group because dictionary has separate definitions for its fields.
			//groupFields is used only to limit list of fields to decode when group definition differs from actual group we received
			
			List<Group> groups = fields.getGroups(tag);
			for (Group g : groups)
				decodeGroup(g, groupType, groupFields, msgType, subGroupPath, result);
		}
	}
	
	
	private int[] createGroupPath(int[] groupPath, int groupTag)
	{
		if (groupPath == null)
			return new int[]{groupTag};
		
		int[] result = Arrays.copyOf(groupPath, groupPath.length+1);
		result[result.length-1] = groupTag;
		return result;
	}
	
	private Set<Integer> getGroupFields(String msgType, int[] groupPath)
	{
		GroupInfo info = getGroupInfo(msgType, groupPath);
		if (info == null)
			return null;
		
		List<Integer> fields = Arrays.stream(info.getDataDictionary().getOrderedFields())
				.boxed()
				.collect(Collectors.toList());
		return new HashSet<>(fields);
	}
	
	private GroupInfo getGroupInfo(String msgType, int[] groupPath)
	{
		if (groupPath == null || groupPath.length == 0)
			return null;
		
		GroupInfo info = appDictionary.getGroup(msgType, groupPath[0]);
		for (int i = 1; i < groupPath.length; i++)
		{
			info = info.getDataDictionary().getGroup(msgType, groupPath[i]);
			if (info == null)
				return null;
		}
		return info;
	}
}
