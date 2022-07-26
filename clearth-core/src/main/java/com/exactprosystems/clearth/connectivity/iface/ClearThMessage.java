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

package com.exactprosystems.clearth.connectivity.iface;

import com.exactprosystems.clearth.utils.LineBuilder;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.apache.commons.lang.ObjectUtils;

import java.util.*;

import static java.util.Collections.emptySet;

import java.time.Instant;

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS)
public abstract class ClearThMessage<T extends ClearThMessage<T>>
{
	public static final String MSGTYPE = "MsgType",
			SUBMSGTYPE = "SubMsgType", 
			SUBMSGSOURCE = "SubMsgSource",
			MSGCOUNT = "MsgCount";
	
	private List<T> subMessages = null;
	private ClearThMessageMetadata metadata = null;
	
	private String encodedMessage;

	/**
	 * Add field to message
	 * @param name name of field (not null)
	 * @param value value of field
	 */
	public abstract void addField(final String name, final String value);
	
	/**
	 * Remove field from message
	 * @param name name of field (not null)
	 */
	public abstract void removeField(final String name);
	
	/**
	 * Get message field by name
	 * @param name name of field (not null)
	 * @return value of field
	 */
	public abstract String getField(final String name);

	/**
	 * Get all fields as text
	 * @return fields as text converted if needed or empty map
	 */
	@JsonIgnore
	public abstract Map<String, String> getFields();
	
	/**
	 * Check that field is set 
	 * @param name field name
	 * @return true if field set, else false
	 */
	public abstract boolean isFieldSet(final String name);
	
	/**
	 * Get set of message fields
	 * @return unmodifiable set of message fields
	 */
	@JsonIgnore
	public Set<String> getFieldNames()
	{
		return Collections.unmodifiableSet(getFieldsKeySet());
	}

	@JsonIgnore
	protected abstract Set<String> getFieldsKeySet();

	/**
	 * Get copy of message
	 * @return new IMessage with copied fields
	 */
	public abstract T cloneMessage();
	
	/**
	 * Get internal object that stores field value. This method is used for equals().
	 * @param fieldName field name
	 * @return internal field object
	 */
	protected abstract Object getFieldObject(String fieldName);

	/**
	 * Get the source message saved by codec (optional).
	 * @return The source message.
	 */
	public String getEncodedMessage()
	{
		return encodedMessage;
	}

	/**
	 * Set the source message from codec (optional).
	 * @param encodedMessage
	 */
	public void setEncodedMessage(String encodedMessage)
	{
		this.encodedMessage = encodedMessage;
	}

	private void checkExist()
	{
		if (subMessages == null)
			subMessages = new ArrayList<T>();
	}
	
	public void addSubMessage(T message)
	{
		checkExist();
		subMessages.add(message);
	}

	public void addSubMessages(List<T> messages)
	{
		checkExist();
		subMessages.addAll(messages);
	}
	
	public void removeSubMessage(T message)
	{
		checkExist();
		subMessages.remove(message);
	}
	
	public void removeSubMessage(int index)
	{
		checkExist();
		subMessages.remove(index);
	}
	
	public T getSubMessage(int index)
	{
		checkExist();
		return subMessages.get(index);
	}
	
	@JsonIgnore
	public Set<String> getSubMessageTypes()
	{
		if (!hasSubMessages())
			return emptySet();
		
		Set<String> types = new LinkedHashSet<String>();
		for (T subMessage : getSubMessages())
			types.add(subMessage.getField(SUBMSGTYPE));
		return types;
	}
	
	public List<T> getSubMessages(String type)
	{
		checkExist();
		List<T> result = new ArrayList<T>();
		
		for(T subMessage : subMessages)
		{
			String mesType = subMessage.getField(SUBMSGTYPE);
			if ((mesType==null) || (mesType.equals(type)))
				result.add(subMessage);
		}
		
		return result;
	}
	
	public List<T> getSubMessages() 
	{
		checkExist();
		return subMessages;
	}
	
	public boolean hasSubMessages()
	{
		return subMessages != null && !subMessages.isEmpty();
	}
	
	
	public ClearThMessageDirection getDirection()
	{
		return metadata != null ? metadata.getDirection() : null;
	}
	
	public void setDirection(ClearThMessageDirection direction)
	{
		checkMetadataExists();
		metadata.setDirection(direction);
	}
	
	
	public Instant getTimestamp()
	{
		return metadata != null ? metadata.getTimestamp() : null;
	}
	
	public void setTimestamp(Instant timestamp)
	{
		checkMetadataExists();
		metadata.setTimestamp(timestamp);
	}
	
	
	public Object getMetaField(String name)
	{
		return metadata != null ? metadata.getField(name) : null;
	}
	
	public void addMetaField(String name, Object value)
	{
		checkMetadataExists();
		metadata.addField(name, value);
	}
	
	public ClearThMessageMetadata getMetadata()
	{
		return metadata;
	}
	
	public void setMetadata(ClearThMessageMetadata metadata)
	{
		this.metadata = metadata;
	}
	
	
	private void checkMetadataExists()
	{
		if (metadata == null)
			metadata = new ClearThMessageMetadata();
	}
	
	
	protected void appendFieldToString(String fieldName, String fieldValue, String indent, LineBuilder lb)
	{
		lb.add(indent).add(fieldName).add(" = '").add(fieldValue).append("'");
	}
	
	protected String fieldsToString(ClearThMessage<T> message, Set<String> fieldNames, String indent)
	{
		LineBuilder lb = new LineBuilder();
		for (String fn : fieldNames)
			appendFieldToString(fn, message.getField(fn), indent, lb);
		return lb.toString();
	}
	
	protected String subMessagesToString(ClearThMessage<T> message, String indent)
	{
		if (!message.hasSubMessages())
			return null;
		
		String subIndent = indent+"  ";
		
		LineBuilder lb = new LineBuilder();
		for (T subMessage : message.getSubMessages())
		{
			lb.add(indent).add("Sub-message '").add(subMessage.getField(SUBMSGTYPE)).append("':");
			
			Set<String> subFields = new LinkedHashSet<String>(subMessage.getFieldNames());
			subFields.remove(SUBMSGTYPE);
			
			String subStr = messageToString(subMessage, subFields, subIndent);  //Can't use toString() here because need to exclude typeField from field names
			if (!subStr.isEmpty())
				lb.append(subStr);
		}
		return lb.toString();
	}
	
	protected String messageToString(ClearThMessage<T> message, Set<String> fieldNames, String indent)
	{
		LineBuilder lb = new LineBuilder();
		
		String fieldsStr = fieldsToString(message, fieldNames, indent);
		if ((fieldsStr != null) && (!fieldsStr.isEmpty()))
			lb.append(fieldsStr);
		
		String subsStr = subMessagesToString(message, indent);
		if ((subsStr != null) && (!subsStr.isEmpty()))
			lb.append(subsStr);
		return lb.toString();
	}
	
	@Override
	public String toString()
	{
		return messageToString(this, getFieldNames(), "");
	}


	@Override
	public boolean equals(Object object)
	{
		if (this == object)
			return true;

		if (!(object instanceof ClearThMessage))
			return false;
		
		// ClearThJsonMessage !eq ClearThXmlMessage
		Class<?> thisClass = this.getClass();
		Class<?> thatClass = object.getClass();
		if (!thisClass.isAssignableFrom(thatClass) && !thatClass.isAssignableFrom(thisClass))
			return false;

		ClearThMessage<?> message = (ClearThMessage<?>) object;

		if (!this.getFieldNames().equals(message.getFieldNames()))
			return false;

		for (String fieldName : this.getFieldNames())
		{
			if (!ObjectUtils.equals(this.getFieldObject(fieldName), message.getFieldObject(fieldName)))
				return false;
		}
		
		if (!Objects.equals(this.getMetadata(), message.getMetadata()))
			return false;

		if (this.hasSubMessages())
			return message.hasSubMessages() && this.getSubMessages().equals(message.getSubMessages());
		else
			return !message.hasSubMessages();
	}
}
