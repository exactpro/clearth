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

package com.exactprosystems.clearth.connectivity.json;

import com.exactprosystems.clearth.connectivity.DecodeException;
import com.exactprosystems.clearth.connectivity.EncodeException;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.ICodec;
import com.exactprosystems.clearth.connectivity.iface.MessageValidator;
import com.exactprosystems.clearth.connectivity.iface.MessageValidatorCondition;
import com.exactprosystems.clearth.connectivity.json.validation.JsonMessageValidator;
import com.exactprosystems.clearth.utils.Utils;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.Map.Entry;

import static com.exactprosystems.clearth.connectivity.Dictionary.*;
import static com.exactprosystems.clearth.connectivity.iface.ClearThMessage.*;
import static java.lang.String.format;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

public class JsonCodec implements ICodec
{	
	protected static final Logger logger = LoggerFactory.getLogger(JsonCodec.class);
	
	public static final char PATH_SEPARATOR = '/';
	public static final String EMPTY = "@{isEmpty}";
	public static final String DEFAULT_CODEC_NAME = "Json";
	public static final String DEFAULT_KEY_NAME = "MapKey";
	protected final JsonDictionary dictionary;
	protected final JsonNodeFactory jsonNodeFactory = new JsonNodeFactory(false);
	protected final ObjectMapper objectMapper= new ObjectMapper();
	protected final MessageValidator messageValidator;
	protected final JsonMessageValidator jsonMessageValidator;
	private final Map<String, String> codecParameters;
	
	{
		objectMapper.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
	}
	
	@Deprecated
	public JsonCodec(JsonDictionary dictionary)
	{
		this(dictionary, null);
	}
	
	public JsonCodec(JsonDictionary dictionary, Map<String, String> codecParameters)
	{
		this.dictionary = dictionary;
		this.messageValidator = createMessageValidator();
		this.jsonMessageValidator = createJsonMessageValidator(dictionary);
		this.codecParameters = codecParameters;
	}
	
	public JsonDictionary getDictionary()
	{
		return dictionary;
	}
	
	public Map<String, String> getCodecParameters()
	{
		return codecParameters;
	}
	
	
	protected MessageValidator createMessageValidator()
	{
		return new MessageValidator();
	}
	
	protected JsonMessageValidator createJsonMessageValidator(JsonDictionary dictionary)
	{
		return new JsonMessageValidator(dictionary);
	}

	protected ClearThJsonMessage createEmptyMessage()
	{
		return new ClearThJsonMessage();
	}
	
	protected boolean isServiceField(String name)
	{
		return MSGTYPE.equals(name) || SUBMSGTYPE.equals(name) || SUBMSGSOURCE.equals(name);
	}
	
	protected boolean isContainerField(JsonFieldDesc fd)
	{
		return fd.isAllowUndefinedFields() || isNotEmpty(fd.getFieldDesc());
	}

	protected boolean isMapType(JsonFieldDesc fieldDesc)
	{
		return JsonFieldType.MAP.equals(fieldDesc.getType());
	}

	protected String getKeyName(JsonFieldDesc fieldDesc)
	{
		String keyName = fieldDesc.getKeyName();
		return StringUtils.isEmpty(keyName) ? DEFAULT_KEY_NAME : keyName;
	}

	///////////////////////////////// DECODING ////////////////////////////
	
	@Override
	public ClearThMessage<?> decode(String encodedMessage) throws DecodeException
	{
		return decode(encodedMessage, null);
	}

	@Override
	public ClearThMessage<?> decode(String encodedMessage, String messageType) throws DecodeException
	{
		if (logger.isTraceEnabled())
		{
			String logMsg = "Trying to decode JSON message";
			if (messageType != null)
				logMsg += ", defined type '"+messageType+"'";
			logger.trace(logMsg+":"+SystemUtils.LINE_SEPARATOR+encodedMessage);
		}
		
		JsonNode root = readTree(encodedMessage);

		JsonMessageDesc messageDesc;

		if (messageType == null)
		{
			messageDesc = findMessageDesc(root, encodedMessage);
			if(messageDesc == null)
				throw new DecodeException(MSG_DESC_NOT_FOUND_IN_DICTIONARY);
		}
		else if (msgDescTypeFits(messageType, root, encodedMessage))
		{
			messageDesc = dictionary.getMessageDesc(messageType);
		}
		else
			throw new DecodeException(msgDescDoesNotFitError(messageType));

		return decode(root, messageDesc, encodedMessage);
	}

		public ClearThJsonMessage decode(JsonNode root, JsonMessageDesc messageDesc, String encodedMessage) throws DecodeException
	{
		messageValidator.validate(encodedMessage, dictionary.getConditions(messageDesc.getType()));
		ClearThJsonMessage result = decodeMessage(root, messageDesc);
		result.setEncodedMessage(encodedMessage);
		logger.trace("Decoded message:{}{}", Utils.EOL, result);
		return result;
	}

	protected JsonNode readTree(String encodedMessage) throws DecodeException
	{
		try
		{
			return objectMapper.readTree(encodedMessage);
		}
		catch (IOException e)
		{
			throw new DecodeException("Error occurred while reading JSON from string", e);
		}
	}
	
	protected JsonMessageDesc findMessageDesc(JsonNode root, String messageText)
	{
		for (String type : dictionary.getMessageDescMap().keySet())
		{
			if(msgDescTypeFits(type, root, messageText))
				return dictionary.getMessageDesc(type);
		}
		return null;
	}

	private boolean msgDescTypeFits(String messageDescType, JsonNode root, String messageText)
	{
		List<MessageValidatorCondition> typeConditions = dictionary.getTypeConditions(messageDescType);

		return ((typeConditions != null && messageValidator.isValid(messageText, typeConditions))
				|| ((root.hasNonNull(MSGTYPE)) && (messageDescType.equals(root.get(MSGTYPE).asText()))));
	}
	
	protected ClearThJsonMessage decodeMessage(JsonNode rootNode, JsonMessageDesc messageDesc) throws DecodeException
	{
		ClearThJsonMessage message = createEmptyMessage();		
		rootNode = preDecode(rootNode, message, messageDesc);		
		message.addField(MSGTYPE, messageDesc.getType());
		decodeFields(message, rootNode, messageDesc.getFieldDesc(), messageDesc.isAllowUndefinedFields());
		message = postDecode(rootNode, message, messageDesc);
		return message;
	}

	@SuppressWarnings("unused")
	protected JsonNode preDecode(JsonNode root, ClearThJsonMessage message, JsonMessageDesc messageDesc) throws DecodeException
	{
		return root;
	}

	@SuppressWarnings("unused")
	protected ClearThJsonMessage postDecode(JsonNode root, ClearThJsonMessage message, JsonMessageDesc messageDesc) throws DecodeException
	{
		return message;
	}
	
	protected void decodeFields(ClearThJsonMessage message, JsonNode parentNode, List<JsonFieldDesc> fieldDescs,
			boolean allowUndefinedFields) throws DecodeException
	{
		Set<String> processedNodes = new HashSet<>();
		for (JsonFieldDesc fd : fieldDescs)
			decodeField(message, parentNode, fd, processedNodes, allowUndefinedFields);

		if (allowUndefinedFields)
			decodeUndefinedFields(message, parentNode, processedNodes);
	}

	protected void decodeField(ClearThJsonMessage message, JsonNode parentNode, JsonFieldDesc fieldDesc, Set<String> processedNodes,
		boolean allowUndefinedFields) throws DecodeException
	{
		JsonNode node = findNode(parentNode, fieldDesc);
		if (node == null)
			return;
		processedNodes.add(fieldDesc.getSource());

		if (isMapType(fieldDesc))
			decodeMap(message, node, fieldDesc, allowUndefinedFields);
		else if (fieldDesc.isRepeat())
			decodeRepeatedField(message, node, fieldDesc);
		else 
			decodeSingleField(message, node, fieldDesc);
	}
	
	protected JsonNode findNode(JsonNode parentNode, JsonFieldDesc fieldDesc)
	{
		String source = fieldDesc.getSource();
		if ((source == null) || (source.isEmpty()))
			return parentNode;
		
		String[] sources = StringUtils.split(source, PATH_SEPARATOR);
		JsonNode node = parentNode;
		for (String s : sources)
		{
			node = node.get(s);
			if (node == null)
				break;
		}
		return node;
	}

	protected void decodeMap(ClearThJsonMessage message, JsonNode node, JsonFieldDesc fieldDesc, boolean allowUndefinedFields) throws DecodeException
	{
		Iterator<Map.Entry<String, JsonNode>> iterator = node.fields();
		while (iterator.hasNext())
		{
			Map.Entry<String, JsonNode> nodeValue = iterator.next();
			ClearThJsonMessage subMessage = new ClearThJsonMessage();

			subMessage.addField(SUBMSGTYPE, fieldDesc.getName());
			subMessage.addField(getKeyName(fieldDesc), nodeValue.getKey());

			decodeFields(subMessage, nodeValue.getValue(), fieldDesc.getFieldDesc(), allowUndefinedFields);
			message.addSubMessage(subMessage);
		}
	}

	protected void decodeRepeatedField(ClearThJsonMessage message, JsonNode node, JsonFieldDesc fieldDesc)
		throws DecodeException
	{
		if (node.isArray())
		{
			ArrayNode array = (ArrayNode) node;
			for (int i = 0; i < array.size(); i++)
			{
				ClearThJsonMessage subMessage = createEmptyMessage();
				subMessage.addField(SUBMSGTYPE, getMsgFieldName(fieldDesc));
				message.addSubMessage(subMessage);
				decodeSingleField(subMessage, array.get(i), fieldDesc);
			}
		}
		else 
		{
			throw new DecodeException(format("Field '%s' is specified as repeated (Array) field in the dictionary " +
					"but in the message %s is found", fieldNameForError(fieldDesc), node.getNodeType()));
		}
	}
	
	protected void decodeSingleField(ClearThJsonMessage message, JsonNode node, JsonFieldDesc fieldDesc)
		throws DecodeException
	{
		if (isContainerField(fieldDesc))
			decodeObjectField(message, node, fieldDesc);
		else
			decodeSimpleField(message, node, fieldDesc);
	}
	
	protected void decodeObjectField(ClearThJsonMessage message, JsonNode node, JsonFieldDesc fieldDesc)
		throws DecodeException
	{
		if (node.isObject())
		{
			decodeFields(message, node, fieldDesc.getFieldDesc(), fieldDesc.isAllowUndefinedFields());
		}
		else
		{
			throw new DecodeException(format("Field '%s' is specified as container (Object) field in the dictionary" +
					" but in the message %s is found", fieldNameForError(fieldDesc), node.getNodeType()));
		}
	}
	
	protected void decodeSimpleField(ClearThJsonMessage message, JsonNode node, JsonFieldDesc fieldDesc)
	{
		String name = fieldDesc.isRepeat() ? ClearThJsonMessage.ARRAY_ITEM_NAME : getMsgFieldName(fieldDesc), value = "";
		if (node != null)
			if (node.isArray())
			{
				StringJoiner strJoin = new StringJoiner(", ");
				for (JsonNode jn : node)
					strJoin.add(getSimpleNodeValue(jn));
				
				value = strJoin.toString();
			}
			else
				value = getSimpleNodeValue(node);
		
		message.addField(name, new JsonTextField(value));
	}
	
	private String getSimpleNodeValue(JsonNode jn)
	{
		return jn.isBigDecimal() ? jn.decimalValue().toPlainString() : jn.asText();
	}
	
	protected String getMsgFieldName(JsonFieldDesc fd)
	{
		if (fd.getName() != null)
			return fd.getName();
		else 
			return fd.getSource();
	}
	
	protected void decodeUndefinedFields(ClearThJsonMessage message, JsonNode parentNode, Set<String> processedNodes)
	{
		Iterator<Map.Entry<String, JsonNode>> iterator = parentNode.fields();
		while (iterator.hasNext())
		{
			Map.Entry<String, JsonNode> e = iterator.next();
			JsonNode node = e.getValue();
			if (node.isValueNode() && !processedNodes.contains(e.getKey()))
				message.addField(e.getKey(), node.asText());
		}
	}

	///////////////////////////////// ENCODING ////////////////////////////
	
	@Override
	public String encode(ClearThMessage<?> message) throws EncodeException
	{
		logger.trace("Trying to encode JSON message:{}{}", Utils.EOL, message);
		
		String msgType = getMessageType(message);
		JsonMessageDesc messageDesc = dictionary.getMessageDesc(msgType);
		if (messageDesc == null)
			throw new EncodeException(msgDescWithTypeNotFoundError(msgType));

		if (message instanceof ClearThJsonMessage)
		{
			ClearThJsonMessage jsonMessage = (ClearThJsonMessage)message;
			if (jsonMessage.isValidate())
				validate(jsonMessageValidator, jsonMessage, msgType);
		}
		
		String encodedMessage = encodeMessage(message, messageDesc);
		logger.trace("Encoded message:{}{}", SystemUtils.LINE_SEPARATOR, encodedMessage);
		return encodedMessage;
	}
	
	protected void validate(JsonMessageValidator validator, ClearThJsonMessage message, String msgType) throws EncodeException
	{
		String validationErrors = validator.validateMessage(message, msgType);
		if (!validationErrors.isEmpty())
			throw new EncodeException(validationErrors);
	}
	
	protected String getMessageType(ClearThMessage message) throws EncodeException
	{
		String type = message.getField(MSGTYPE);
		if (StringUtils.isEmpty(type))
			throw new EncodeException(MSG_TYPE_NOT_FOUND);
		else 
			return type;
	}
	
	protected String encodeMessage(ClearThMessage message, JsonMessageDesc messageDesc) throws EncodeException
	{
		ContainerNode root = jsonNodeFactory.objectNode();
		root = preEncode(message, root, messageDesc);
		
		for (JsonFieldDesc fieldDesc : messageDesc.getFieldDesc())
			encodeField(message, root, fieldDesc);
		
		if (messageDesc.isAllowUndefinedFields())
			encodeUndefinedFields(message, root, messageDesc.getFieldDesc());
		
		root = postEncode(message, root, messageDesc);
		return jsonTreeToString(root);
	}

	@SuppressWarnings("unused")
	protected ContainerNode preEncode(ClearThMessage message, ContainerNode root, JsonMessageDesc messageDesc) throws EncodeException
	{
		return root;
	}

	@SuppressWarnings("unused")
	protected ContainerNode postEncode(ClearThMessage message, ContainerNode root, JsonMessageDesc messageDesc) throws EncodeException
	{
		return root;
	}
	
	protected String jsonTreeToString(JsonNode root) throws EncodeException
	{
		try
		{
			return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
		}
		catch (IOException e)
		{
			throw new EncodeException("Error occurred while writing JSON to string", e);
		}
	}
	
	protected void encodeField(ClearThMessage message, ContainerNode parentNode, JsonFieldDesc fieldDesc) throws EncodeException
	{
		if (isMapType(fieldDesc))
			encodeMapField(message, parentNode, fieldDesc);
		else if (fieldDesc.isRepeat())
			encodeRepeatedField(message, parentNode, fieldDesc);
		else 
			encodeSingleField(message, parentNode, fieldDesc);
	}

	protected void encodeMapField(ClearThMessage message, ContainerNode parentNode, JsonFieldDesc fieldDesc) throws EncodeException
	{
		List<ClearThMessage> subMessages = message.getSubMessages(getMsgFieldName(fieldDesc));
		if (CollectionUtils.isEmpty(subMessages))
			return;

		if (StringUtils.isEmpty(fieldDesc.getSource()) && parentNode.isEmpty())
			addKeyNode(subMessages, (ObjectNode) parentNode, fieldDesc);
		else
		{
			ObjectNode objectNode = jsonNodeFactory.objectNode();
			addKeyNode(subMessages, objectNode, fieldDesc);
			addNode(parentNode, objectNode, fieldDesc);
		}
	}

	protected void addKeyNode(List<ClearThMessage> subMessages, ObjectNode parentNode, JsonFieldDesc fieldDesc) throws EncodeException
	{
		for (ClearThMessage subMessage : subMessages)
		{
			ObjectNode objectNode = jsonNodeFactory.objectNode();
			for (JsonFieldDesc desc : fieldDesc.getFieldDesc())
				encodeField(subMessage, objectNode, desc);

			String keyName = getKeyName(fieldDesc);
			String key = subMessage.getField(keyName);

			if (!StringUtils.isEmpty(key))
				parentNode.set(key, objectNode);
			else
				throw new EncodeException(String.format("Sub-message '%s' does not have field '%s' required for encoding",
						subMessage.getField(SUBMSGTYPE), keyName));
		}
	}

	protected void encodeRepeatedField(ClearThMessage message, ContainerNode parentNode, JsonFieldDesc fieldDesc)
		throws EncodeException
	{
		String subMessageType = getMsgFieldName(fieldDesc);
		List<ClearThMessage> subMessages = message.getSubMessages(subMessageType);
		if (CollectionUtils.isEmpty(subMessages))
			return;
		
		ArrayNode arrayNode = jsonNodeFactory.arrayNode();
		addNode(parentNode, arrayNode, fieldDesc);
		
		for (ClearThMessage subMessage : subMessages)
			encodeSingleField(subMessage, arrayNode, fieldDesc);
	}
	
	protected void encodeSingleField(ClearThMessage message, ContainerNode parentNode, JsonFieldDesc fieldDesc)
		throws EncodeException
	{
		if (isContainerField(fieldDesc))
			encodeObjectField(message, parentNode, fieldDesc);
		else
			encodeSimpleField(message, parentNode, fieldDesc);
	}
	
	protected void encodeObjectField(ClearThMessage message, ContainerNode parentNode, JsonFieldDesc fieldDesc)
		throws EncodeException
	{
		ObjectNode node = jsonNodeFactory.objectNode();
		for (JsonFieldDesc subFieldDesc : fieldDesc.getFieldDesc())
			encodeField(message, node, subFieldDesc);
		
		if (fieldDesc.isRepeat() && fieldDesc.isAllowUndefinedFields())
			encodeUndefinedFields(message, node, fieldDesc.getFieldDesc());
		
		if (node.size() > 0)  // won't add node with no sub-nodes
			addNode(parentNode, node, fieldDesc);
	}
	
	protected void encodeSimpleField(ClearThMessage message, ContainerNode parentNode, JsonFieldDesc fieldDesc)
		throws EncodeException
	{
		String msgFieldName = fieldDesc.isRepeat() ? ClearThJsonMessage.ARRAY_ITEM_NAME : getMsgFieldName(fieldDesc);
		String value = message.getField(msgFieldName);
		if (StringUtils.isEmpty(value))
			return;
		
		addNode(parentNode, valueToNode(convertSpecialValues(value), fieldDesc), fieldDesc);
	}
	
	protected JsonNode valueToNode(String value, JsonFieldDesc fieldDesc)
	{
		JsonFieldType type = fieldDesc.getType();
		if (JsonFieldType.NUMBER == type)
			return valueToNumberNode(value);
		if (JsonFieldType.BOOLEAN == type)
			return valueToBooleanNode(value);
		if (JsonFieldType.TEXT_ARRAY == type || JsonFieldType.NUMBER_ARRAY == type)
			return valueToArrayNode(value, type);
		else 
			return new TextNode(value);
	}
	
	protected JsonNode valueToArrayNode(String value, JsonFieldType type) 
	{
			ArrayNode arrayNode = jsonNodeFactory.arrayNode();
			List<JsonNode> nodes = new ArrayList<>();
			for(String val : value.split(","))
			{
				val = val.trim();
				switch(type) 
				{
				case TEXT_ARRAY:
					nodes.add(new TextNode(val));
					break;
				case NUMBER_ARRAY:
					nodes.add(valueToNumberNode(val));
					break;
				default:
					break;
				}
			}
			arrayNode.addAll(nodes);
			return arrayNode;
	}
	
	protected JsonNode valueToNumberNode(String value)
	{
		if (NumberUtils.isNumber(value))
		{
			try
			{
				return new DecimalNode(new BigDecimal(value));
			}
			catch (NumberFormatException e)
			{
				logger.warn("Could not encode '{}' as number", value, e);
			}
		}
		return new TextNode(value);
	}
	
	protected JsonNode valueToBooleanNode(String value)
	{
		return Boolean.parseBoolean(value) ? BooleanNode.TRUE : BooleanNode.FALSE;
	}
	
	protected void addSubNodes(ObjectNode parentNode, JsonNode subNodesContainer)
	{
		Iterator<Entry<String, JsonNode>> it = subNodesContainer.fields();
		while (it.hasNext())
		{
			Entry<String, JsonNode> e = it.next();
			parentNode.set(e.getKey(), e.getValue());
		}
	}
	
	protected void addNode(ContainerNode parentNode, JsonNode value, JsonFieldDesc fieldDesc)
	{
		if (parentNode instanceof ArrayNode)
		{
			((ArrayNode) parentNode).add(value);
		}
		else if (parentNode instanceof ObjectNode)
		{
			ObjectNode parentObjNode = (ObjectNode) parentNode;
			
			String source = fieldDesc.getSource();
			if (source == null)
			{
				addSubNodes(parentObjNode, value);
				return;
			}
			
			String jsonFieldName;
			if (StringUtils.contains(source, PATH_SEPARATOR))
			{
				String[] s = StringUtils.split(source, PATH_SEPARATOR);
				jsonFieldName = s[s.length - 1];
				parentObjNode = createNestedObjects(s, parentObjNode);
			}
			else
				jsonFieldName = source;
			
			parentObjNode.set(jsonFieldName, value);
		}
	}
	
	protected ObjectNode createNestedObjects(String[] path, ObjectNode parentNode)
	{
		ObjectNode lastNode = parentNode;
		for (int i = 0; i < path.length - 1; i++)
		{
			ObjectNode node;
			if (lastNode.has(path[i]))
				node = (ObjectNode) lastNode.get(path[i]);
			else 
			{
				node = jsonNodeFactory.objectNode();
				lastNode.set(path[i], node);
			}
			lastNode = node;
		}
		return lastNode;
	}
	
	protected String convertSpecialValues(String value)
	{
		if (EMPTY.equals(value))
			return "";
		else 
			return value;
	}
	
	protected void encodeUndefinedFields(ClearThMessage<?> message, JsonNode parentNode, List<JsonFieldDesc> definedFields)
	{
		for (String fieldName : message.getFieldNames())
		{
			if (isServiceField(fieldName))
				continue;
			
			String value = message.getField(fieldName);
			if (StringUtils.isEmpty(value))
				continue;
			
			if (isFieldDefined(definedFields, fieldName))
				continue;
			
			((ObjectNode)parentNode).set(fieldName, new TextNode(value));
		}
	}
	
	protected boolean isFieldDefined(List<JsonFieldDesc> definedFields, String name)
	{
		for (JsonFieldDesc fd : definedFields)
		{
			if (name.equals(getMsgFieldName(fd)))
				return true;
		}
		for (JsonFieldDesc fd : definedFields)
		{
			if (isNotEmpty(fd.getFieldDesc()) && isFieldDefined(fd.getFieldDesc(), name))
				return true;
		}
		return false;
	}
	
	protected String fieldNameForError(JsonFieldDesc fd)
	{
		String name = fd.getName();
		String source = fd.getSource();
		if ((name != null) && (source != null))
			return format("%s (%s)", name, source);
		else if (name != null)
			return name;
		else if (source != null)
			return source;
		else 
			return "UNNAMED";
	}
}
