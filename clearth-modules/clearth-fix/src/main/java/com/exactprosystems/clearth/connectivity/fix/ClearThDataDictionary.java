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

import com.exactprosystems.clearth.utils.XmlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;
import quickfix.ConfigError;
import quickfix.DataDictionary;
import quickfix.Message;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Extended DataDictionary that holds additional information about messages
 */
public class ClearThDataDictionary extends DataDictionary
{
	private static final Logger logger = LoggerFactory.getLogger(ClearThDataDictionary.class);
	public static final String TAG_MESSAGE = "message",
			TAG_FIELD = "field",
			TAG_GROUP = "group",
			TAG_COMPONENT = "component",
			ATTR_MSGTYPE = "msgtype",
			ATTR_NAME = "name";
	
	private final MessagesInfo messagesInfo;
	
	public ClearThDataDictionary(File dictionaryFile) throws ConfigError
	{
		super(dictionaryFile.getAbsolutePath());
		messagesInfo = loadMessagesInfo(dictionaryFile);
	}
	
	public ClearThDataDictionary(InputStream is) throws ConfigError, ParserConfigurationException
	{
		super(is);
		messagesInfo = loadMessagesInfo(is);
	}
	
	
	/**
	 * Finds information about fields of message with given type. Result can be used to create {@link Message} preserving order of fields
	 * @param msgType type of message to find fields information for
	 * @return information about message fields
	 */
	public FieldsInfo getMessageFieldsInfo(String msgType)
	{
		return messagesInfo.getMessageFieldsInfo().get(msgType);
	}
	
	/**
	 * Finds information about fields of component with given name.
	 * @param compName name of component to find fields information for
	 * @return information about component fields
	 */
	public FieldsInfo getComponentFieldsInfo(String compName)
	{
		return messagesInfo.getComponentFieldsInfo().get(compName);
	}
	

	protected Map<String, FieldsInfo> createMessagesMap()
	{
		return new HashMap<String, FieldsInfo>();
	}
	
	protected Map<String, FieldsInfo> createComponentsMap()
	{
		return new HashMap<String, FieldsInfo>();
	}
	
	/**
	 * @return Map with message type as key and corresponding fields info as value
	 */
	protected Map<String, FieldsInfo> getMessageFieldsInfo()
	{
		return messagesInfo.getMessageFieldsInfo();
	}
	
	/**
	 * @return Map with component name as key and corresponding fields info as value
	 */
	protected Map<String, FieldsInfo> getComponentFieldsInfo()
	{
		return messagesInfo.getComponentFieldsInfo();
	}
	
	/**
	 * @return information about fields of messages and components
	 */
	public MessagesInfo getMessagesInfo()
	{
		return messagesInfo;
	}

	protected MessagesInfo loadMessagesInfo(File dictionaryFile) throws ConfigError
	{
		try (InputStream is = new FileInputStream(dictionaryFile))
		{
			return loadMessagesInfo(is);
		}
		catch (Exception e)
		{
			throw new ConfigError("Error while loading ordered message fields from '"+dictionaryFile.getAbsolutePath()+"'", e);
		}
	}
	
	
	private FieldsInfo parseComponentNode(Node componentNode, String messageType, Map<String, FieldsInfo> componentsInfo)
	{
		FieldsInfo result = new FieldsInfo();
		String name = getXmlAttribute(componentNode, ATTR_NAME);
		if (name == null)
			logger.warn("Component of '{}' doesn't have '{}' attribute", messageType, ATTR_NAME);
		else
		{
			FieldsInfo found = componentsInfo.get(name);  //If component with this name already exists, copy information about it
			if (found != null)
				result.add(found);
			else
				logger.warn("Referenced component '{}' is not defined", name);
		}
		
		FieldsInfo fieldsInfo = parseMessageNode(componentNode, messageType, componentsInfo);
		result.add(fieldsInfo);
		return result;
	}
	
	private FieldsInfo parseMessageNode(Node messageNode, String type, Map<String, FieldsInfo> componentsInfo)
	{
		FieldsInfo result = new FieldsInfo();
		Set<Integer> resultFields = result.getFields();
		NodeList fieldNodes = messageNode.getChildNodes();
		for (int i = 0; i < fieldNodes.getLength(); i++)
		{
			Node fieldNode = fieldNodes.item(i);
			String tag = fieldNode.getNodeName();
			if (tag.equals(TAG_FIELD) || tag.equals(TAG_GROUP))
			{
				String name = getXmlAttribute(fieldNode, ATTR_NAME);
				if (name == null)
				{
					logger.warn("Field #{} of message '{}' doesn't have '{}' attribute", i+1, type, ATTR_NAME);
					continue;
				}
				
				int num = getFieldTag(name);
				resultFields.add(num);
			}
			else if (tag.equals(TAG_COMPONENT))
			{
				FieldsInfo componentFields = parseComponentNode(fieldNode, type, componentsInfo);
				result.add(componentFields);
			}
		}
		return result;
	}
	
	protected Map<String, FieldsInfo> parseMessagesNodes(NodeList messagesNode, Map<String, FieldsInfo> componentsInfo) throws ConfigError
	{
		Map<String, FieldsInfo> result = createMessagesMap();
		
		NodeList nodes = messagesNode.item(0).getChildNodes();
		int messageIndex = 0;
		for (int i = 0; i < nodes.getLength(); i++)
		{
			Node messageNode = nodes.item(i);
			if (!messageNode.getNodeName().equals(TAG_MESSAGE))
				continue;
			
			messageIndex++;
			
			String type = getXmlAttribute(messageNode, ATTR_MSGTYPE);
			if (type == null)
			{
				logger.warn("Message definition #{} doesn't contain '{}' attribute", messageIndex, ATTR_MSGTYPE);
				continue;
			}
			
			FieldsInfo fields = parseMessageNode(messageNode, type, componentsInfo);
			result.put(type, fields);
		}
		return result;
	}
	
	
	protected Map<String, FieldsInfo> parseComponentsNodes(NodeList componentsNode) throws ConfigError
	{
		Map<String, FieldsInfo> result = createComponentsMap();
		
		NodeList nodes = componentsNode.item(0).getChildNodes();
		int componentIndex = 0;
		for (int i = 0; i < nodes.getLength(); i++)
		{
			Node compNode = nodes.item(i);
			if (!compNode.getNodeName().equals(TAG_COMPONENT))
				continue;
			
			componentIndex++;
			
			String name = getXmlAttribute(compNode, ATTR_NAME);
			if (name == null)
			{
				logger.warn("Component definition #{} doesn't contain '{}' attribute", componentIndex, ATTR_NAME);
				continue;
			}
			
			FieldsInfo fields = parseMessageNode(compNode, name, result);  //Format of "component" tag contents is the same as "message" tag
			result.put(name, fields);
		}
		return result;
	}
	
	protected MessagesInfo parseMessagesInfo(Element root) throws ConfigError
	{
		NodeList componentsNode = root.getElementsByTagName("components");
		Map<String, FieldsInfo> componentsInfo;
		if (componentsNode.getLength() == 0)
			componentsInfo = createComponentsMap();
		else
			componentsInfo = parseComponentsNodes(componentsNode);
		
		NodeList messagesNode = root.getElementsByTagName("messages");
		Map<String, FieldsInfo> messagesInfo;
		if (messagesNode.getLength() == 0)
			messagesInfo = createMessagesMap();
		else
			messagesInfo = parseMessagesNodes(messagesNode, componentsInfo);
		
		return new MessagesInfo(messagesInfo, componentsInfo);
	}
	
	
	private MessagesInfo loadMessagesInfo(InputStream is) throws ParserConfigurationException, ConfigError
	{
		DocumentBuilder xmlBuilder = XmlUtils.getXmlDocumentBuilder();
		Document document;
		try
		{
			document = xmlBuilder.parse(is);
		}
		catch (Exception e)
		{
			throw new ConfigError("Error while parsing dictionary", e);
		}
		
		Element root = document.getDocumentElement();
		return parseMessagesInfo(root);
	}
	
	private String getXmlAttribute(Node node, String attrName)
	{
		NamedNodeMap attributes = node.getAttributes();
		if (attributes == null)
			return null;
		
		Node item = attributes.getNamedItem(attrName);
		return item != null ? item.getNodeValue() : null;
	}
}
