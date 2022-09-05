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

package com.exactprosystems.clearth.connectivity.xml;

import com.exactprosystems.clearth.connectivity.DecodeException;
import com.exactprosystems.clearth.connectivity.EncodeException;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.ICodec;
import com.exactprosystems.clearth.connectivity.iface.MessageValidator;
import com.exactprosystems.clearth.connectivity.iface.MessageValidatorCondition;
import com.exactprosystems.clearth.utils.ComparisonUtils;
import com.exactprosystems.clearth.utils.XmlUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.exactprosystems.clearth.connectivity.iface.ClearThMessage.MSGTYPE;
import static java.lang.String.format;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

public class XmlCodec implements ICodec
{
	private static final Logger logger = LoggerFactory.getLogger(XmlCodec.class);
	protected static final XPath xPath = XPathFactory.newInstance().newXPath();

	private static final Pattern ROOT_TAG_PATTERN = Pattern.compile("\\A(?:<\\?(?i)xml(?-i).*>\\s*)?<(\\w+).*>");

	protected static final String FORMAT_NOMSGTYPE = "No message description with type '%s' in dictionary.";
	
	public static final String DEFAULT_CODEC_NAME = "Xml";

	protected final XmlDictionary dictionary;
	protected final boolean trimValues;
	protected final MessageValidator messageValidator;
	private final Map<String, String> codecParameters;
	
	protected static final ThreadLocal<DocumentBuilder> documentBuilderHolder = ThreadLocal.withInitial(() ->
	{
		try
		{
			return DocumentBuilderFactory.newInstance().newDocumentBuilder();
		}
		catch (ParserConfigurationException e)
		{
			/* Actually this exception unlikely to be thrown because of default settings */
			logger.error("Error while initialization.", e);
			return null;
		}
	});
	
	
	@Deprecated
	public XmlCodec(XmlDictionary dictionary)
	{
		this(dictionary, true, null);
	}
	
	public XmlCodec(XmlDictionary dictionary, Map<String, String> codecParameters)
	{
		this(dictionary, true, codecParameters);
	}
	
	public XmlCodec(XmlDictionary dictionary, boolean trimValues, Map<String, String> codecParameters)
	{
		this.dictionary = dictionary;
		this.trimValues = trimValues;
		this.messageValidator = createMessageValidator();
		this.codecParameters = codecParameters;
	}
	
	public boolean isEmptyValue(String value)
	{
		return ComparisonUtils.IS_EMPTY.equals(value) || ClearThXmlMessage.EMPTY_VALUE.equals(value);
	}
	
	public Map<String, String> getCodecParameters()
	{
		return codecParameters;
	}
	
	
	protected MessageValidator createMessageValidator()
	{
		return new MessageValidator();
	}

	////////////////// DECODING /////////////////

	@Override
	public ClearThXmlMessage decode(String encodedMessage) throws DecodeException
	{
		return decode(encodedMessage, null);
	}

	@Override
	public ClearThXmlMessage decode(String encodedMessage, String messageType) throws DecodeException
	{
		try
		{
			String sourceMessage = encodedMessage;
			encodedMessage = removeNamespaces(encodedMessage);

			XmlMessageDesc messageDesc = findMessageDesc(encodedMessage, messageType);

			messageValidator.validate(encodedMessage, dictionary.getConditions(messageDesc.getType()));
			ClearThXmlMessage parsedMessage = new ClearThXmlMessage();
			parsedMessage.setEncodedMessage(sourceMessage);

			encodedMessage = beforeDecode(encodedMessage, parsedMessage, messageDesc);
			parseMessage(encodedMessage, parsedMessage, messageDesc);
			return afterDecode(encodedMessage, parsedMessage, messageDesc);
		}
		catch (DecodeException e)
		{
			logger.error("Could not decode message", e);
			throw e;
		}
		catch (Exception e)
		{
			logger.error("Error occurred while decoding message", e);
			throw new DecodeException(e);
		}
	}

	@SuppressWarnings("unused")
	protected String beforeDecode(String encodedMessage, ClearThXmlMessage parsedMessage, XmlMessageDesc messageDesc)
			throws DecodeException
	{
		return encodedMessage;
	}

	@SuppressWarnings("unused")
	protected ClearThXmlMessage afterDecode(String encodedMessage, ClearThXmlMessage parsedMessage, XmlMessageDesc messageDesc)
			throws DecodeException
	{
		return parsedMessage;
	}


	protected XmlMessageDesc findMessageDesc(String messageText, String type) throws DecodeException
	{
		return (type != null) ? findMessageDescByType(messageText, type) : findMessageDescByText(messageText);
	}

	protected XmlMessageDesc findMessageDescByType(String messageText, String type) throws DecodeException
	{
		XmlMessageDesc md = dictionary.getMessageDesc(type);
		if (md == null)
			throw new DecodeException(format(FORMAT_NOMSGTYPE, type));

		if (messageValidator.isValid(messageText, dictionary.getTypeConditions(type)))
			return md;
		else
			throw new DecodeException(format("Message doesn't match conditions of type '%s'.", type));
	}

	protected XmlMessageDesc findMessageDescByText(String messageText) throws DecodeException
	{
		for (XmlMessageDesc md : dictionary.getMessageDescs())
		{
			String messageType = md.getType();
			List<MessageValidatorCondition> conditions = dictionary.getTypeConditions(messageType);

			if (isNotEmpty(conditions) && messageValidator.isValid(messageText, conditions))
				return md;
		}
		throw new DecodeException(format("Unknown message with root tag '%s'.", findRootTag(messageText)));
	}

	private String findRootTag(String message)
	{
		Matcher matcher = ROOT_TAG_PATTERN.matcher(message);
		if (matcher.find())
			return matcher.group(1);
		else
			return "";
	}


	protected String removeNamespaces(String encodedMessage)
	{
		if (encodedMessage.contains(":"))
			return encodedMessage.replaceAll("(</?)[\\w\\d]+:", "$1");
		else
			return encodedMessage;
	}

	protected Element parseDom(String message) throws ParserConfigurationException, IOException, SAXException
	{
		try (InputStream is = new ByteArrayInputStream(message.getBytes(StandardCharsets.UTF_8)))
		{
			DocumentBuilder builder = documentBuilderHolder.get();
			Document document = builder.parse(is);
			Element documentElement = document.getDocumentElement();
			documentElement.normalize();
			return documentElement;
		}
	}

	protected void parseMessage(String encodedMessage, ClearThXmlMessage parsedMessage, XmlMessageDesc messageDesc)
			throws DecodeException, IOException, SAXException, ParserConfigurationException
	{
		parsedMessage.addField(MSGTYPE, messageDesc.getType());

		Element documentElement = parseDom(encodedMessage);
		Set<Node> usedNodes = new HashSet<Node>();

		if (isNotEmpty(messageDesc.getAttrDesc()))
			parseAttributes(messageDesc.getAttrDesc(), documentElement, parsedMessage);

		for (XmlFieldDesc field : messageDesc.getFieldDesc())
			parseField(field, documentElement, parsedMessage, usedNodes);
	}

	protected void parseField(XmlFieldDesc fieldDesc, Node parentNode, ClearThXmlMessage parsedMessage, Set<Node> usedNodes) throws DecodeException
	{
		//For constant fields, no value parsing is needed.
		//These values are only set in the XML message during encoding.
		//For decoding, they don't matter.
		if (fieldDesc.getAlways() != null)
			return;

		List<Node> nodes = findNodes(parentNode, getSource(fieldDesc), fieldDesc.isXpath());
		if (nodes.isEmpty())
		{
			if (fieldDesc.isMandatory())
				throw new DecodeException(createNodeNotFoundMessage(fieldDesc));
		}
		else
		{
			for (Node node : nodes)
			{
				if (usedNodes.contains(node))
					continue;

				usedNodes.add(node);

				ClearThXmlMessage message = fieldDesc.isRepeat() ? createSubMessage(parsedMessage, fieldDesc) : parsedMessage;

				if (isNotEmpty(fieldDesc.getAttrDesc()))
					parseAttributes(fieldDesc.getAttrDesc(), node, message);

				List<XmlFieldDesc> subFieldDescs = fieldDesc.getFieldDesc();
				if (subFieldDescs.size() == 0)
				{
					if (shouldBePresentInObject(fieldDesc))
						parseSimpleField(fieldDesc, node, message);
				}
				else
				{
					for (XmlFieldDesc subFieldDesc : subFieldDescs)
						parseField(subFieldDesc, node, message, usedNodes);
				}

				if (!fieldDesc.isRepeat())
					break;
			}
		}
	}

	protected List<Node> findNodes(Node parentNode, String relativePath, boolean isXpath) throws DecodeException
	{
		ArrayList<Node> result = new ArrayList<>();

		if (isXpath)
		{
			try
			{
				NodeList nodes = (NodeList) xPath.evaluate(relativePath, parentNode, XPathConstants.NODESET);
				if (nodes == null || nodes.getLength() == 0)
					return result;

				for (int i = 0; i < nodes.getLength(); i++)
					result.add(nodes.item(i));
			}
			catch (XPathExpressionException e)
			{
				throw new DecodeException(format("XPath error occurred while looking for node '%s' (folded in '%s').",
												 relativePath, getNodeName(relativePath)), e);
			}
		}
		else
		{
			NodeList childNodes = parentNode.getChildNodes();
			for (int i = 0; i < childNodes.getLength(); i++)
			{
				Node node = childNodes.item(i);
				if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals(relativePath))
					result.add(node);
			}
		}

		return result;
	}

	protected boolean shouldBePresentInObject(XmlFieldDesc fieldDesc)
	{
		return fieldDesc.getName() != null;
	}

	protected void parseSimpleField(XmlFieldDesc fieldDesc, Node node, ClearThXmlMessage message)
	{
		String value = node.getFirstChild() != null ? node.getFirstChild().getNodeValue() : "";  //No first child in node usually means empty tag value
		if (trimValues && !value.isEmpty())
			value = value.trim();

		String name = fieldDesc.getName();
		if (name == null)
			name = node.getNodeName();
		message.addXMLField(name, new XmlField(value, fieldDesc.isNumeric()));
	}

	protected void parseAttributes(List<XmlAttributeDesc> attributeDescs, Node node, ClearThXmlMessage message)
	{
		NamedNodeMap attrNodeMap = node.getAttributes();
		if (attrNodeMap == null || attrNodeMap.getLength() == 0)
			return;

		for (XmlAttributeDesc xmlAttrDesc : attributeDescs)
		{
			Node attrNode = attrNodeMap.getNamedItem(getSource(xmlAttrDesc));
			if (attrNode != null)
			{
				XmlField field = new XmlField(attrNode.getNodeValue(), xmlAttrDesc.isNumeric());
				message.addXMLField(xmlAttrDesc.getName(), field);
			}
		}
	}

	protected ClearThXmlMessage createSubMessage(ClearThXmlMessage message, XmlFieldDesc fieldDesc)
	{
		ClearThXmlMessage result = new ClearThXmlMessage();
		result.addField(ClearThXmlMessage.SUBMSGTYPE, fieldDesc.getName());
		message.addSubMessage(result);
		return result;
	}

	protected String createNodeNotFoundMessage(XmlFieldDesc field)
	{
		String fieldName = field.getName();
		String fieldSource = field.getSource();
		return format("Node '%s' (folded in %s) for field with name='%s' not found in message.",
					  getNodeName(fieldSource), fieldSource, fieldName);
	}

	protected String getNodeName(String source)
	{
		int i = source.lastIndexOf('/');
		return i == -1 ? source : StringUtils.substring(source, i + 1);
	}

	//////////////////// ENCODING /////////////////////

	@Override
	public String encode(ClearThMessage<?> message) throws EncodeException
	{
		XmlMessageDesc messageDesc = findMessageDesc(message);

		message = beforeEncode(message, messageDesc);
		String encodedMessage = encodeMessage(message, messageDesc);
		return afterEncode(encodedMessage, message, messageDesc);
	}

	protected XmlMessageDesc findMessageDesc(ClearThMessage<?> message) throws EncodeException
	{
		String type = message.getField(MSGTYPE);
		if (type == null)
			throw new EncodeException(format("Unable to encode message. Field '%s' is absent.", MSGTYPE));

		XmlMessageDesc messageDesc = dictionary.getMessageDesc(type);
		if (messageDesc == null)
			throw new EncodeException(format(FORMAT_NOMSGTYPE, type));
		else
			return messageDesc;
	}

	@SuppressWarnings("unused")
	protected ClearThMessage beforeEncode(ClearThMessage message, XmlMessageDesc messageDesc) throws EncodeException
	{
		return message;
	}

	@SuppressWarnings("unused")
	protected String afterEncode(String encodedMessage, ClearThMessage message, XmlMessageDesc messageDesc) throws EncodeException
	{
		// DOM API writes empty tags as <a/> and we can't configure it to create them as <a></a>. 
		// So we need to put something into tags and then remove.
		if (encodedMessage.contains(ComparisonUtils.IS_EMPTY))
			encodedMessage = encodedMessage.replace(ComparisonUtils.IS_EMPTY, "");
		return encodedMessage;
	}

	protected String encodeMessage(ClearThMessage message, XmlMessageDesc messageDesc) throws EncodeException
	{
		try
		{
			Document document = XmlUtils.createDocument();
			Element root = document.createElement(getRootTag(messageDesc, message));
			document.appendChild(root);

			setAttributes(root, messageDesc.getAttrDesc(), messageDesc.getDefaultAttrDesc(), message);
			for (XmlFieldDesc fieldDesc : messageDesc.getFieldDesc())
			{
				XmlUtils.appendAll(root, encodeField(fieldDesc, message, document));
			}

			StringWriter result = new StringWriter();
			XmlUtils.writeXml(document, result);
			return result.toString();
		}
		catch (ParserConfigurationException e) // It is unlikely case because DocumentBuilderFactory has default settings
		{
			throw new EncodeException("An error occurred while preparing to encode.", e);
		}
	}

	/**
	 * General encode method for simple, complex and repeated fields. Method called recursively during DOM tree processing
	 * @return Can return several nodes if field is repeated.
	 * @throws EncodeException in case of errors in repeating groups
	 */
	protected List<Node> encodeField(XmlFieldDesc fieldDesc, ClearThMessage message, Document document) throws EncodeException
	{
		if (fieldDesc.isRepeat())
			return encodeGroup(fieldDesc, message, document);
		else
		{
			Node encoded = encodeSingleField(fieldDesc, message, document);
			return encoded == null ? Collections.emptyList() : Collections.singletonList(encoded);
		}
	}

	/**
	 * Encodes repeated fields
	 */
	protected List<Node> encodeGroup(XmlFieldDesc fieldDesc, ClearThMessage message, Document document) throws EncodeException
	{
		List<ClearThMessage> subMessages = message.getSubMessages(fieldDesc.getName());
		if (subMessages.isEmpty())
			return Collections.emptyList();

		List<Node> nodes = new ArrayList<Node>();
		for (ClearThMessage subMsg : subMessages)
		{
			Node encoded = encodeSingleField(fieldDesc, subMsg, document);

			if (encoded != null)
				nodes.add(encoded);
		}
		return nodes;
	}

	/**
	 * Encodes complex or simple field
	 */
	protected Node encodeSingleField(XmlFieldDesc fieldDesc, ClearThMessage message, Document document) throws EncodeException
	{
		boolean hasAttributes = hasAttributes(fieldDesc, message);
		List<Node> subNodes = new ArrayList<Node>();
		List<XmlFieldDesc> subFields = fieldDesc.getFieldDesc();
		if (isEmpty(subFields))
		{
			Node encodedValue = encodeValue(fieldDesc, message, document, hasAttributes);
			if (encodedValue != null)
				subNodes.add(encodedValue);
		}
		else
		{
			for (XmlFieldDesc subFieldDesc : subFields)
			{
				subNodes.addAll(encodeField(subFieldDesc, message, document));
			}
		}

		if (subNodes.isEmpty())
		{
			if (shouldBePresentInObject(fieldDesc) && isEmptyValue(message.getField(fieldDesc.getName())))
			{
				if (!fieldDesc.isUseSelfClosingTagForEmpty())
					subNodes.add(document.createTextNode(ComparisonUtils.IS_EMPTY));
			}
			else
				return null;
		}

		String source = getSource(fieldDesc);
		if (".".equals(source))
			return subNodes.isEmpty() ? null : subNodes.get(0);
		else
			return createTagElement(fieldDesc, message, document, subNodes, hasAttributes);
	}

	protected Node createTagElement(XmlFieldDesc fieldDesc, ClearThMessage message, Document document,
									List<Node> subNodes, boolean hasAttributes)
	{
		String[] tagNames = getSource(fieldDesc).split("/");
		Element top = null;
		Element bottom = null;
		String namespacePrefix = null;
		if(message instanceof ClearThXmlMessage)
			namespacePrefix = ((ClearThXmlMessage)message).getNamespacePrefix();
		boolean hasNamespace = namespacePrefix != null;
		for (int i = 0; i < tagNames.length; i++)
		{
			String tagName = hasNamespace ? namespacePrefix + ':' + tagNames[i] : tagNames[i];
			Element e = document.createElement(tagName);
			if (i == 0)
				top = bottom = e;
			else
			{
				bottom.appendChild(e);
				bottom = e;
			}
		}
		if (bottom != null)
		{
			if (hasAttributes)
				setAttributes(bottom, fieldDesc.getAttrDesc(), fieldDesc.getDefaultAttrDesc(), message);
			for (Node subNode : subNodes)
				bottom.appendChild(subNode);
		}
		return top;
	}

	protected Node encodeValue(XmlFieldDesc fieldDesc, ClearThMessage message, Document document, boolean generateAnyway)
	{
		String value = getValue(fieldDesc, message);
		if (value == null)
		{
			if (generateAnyway)
				value = "";
			else
				return null;
		}
		return document.createTextNode(value);
	}

	/**
	 * Override if you want to change default root tag.
	 */
	protected String getRootTag(XmlMessageDesc messageDesc, ClearThMessage message)
	{
		String namespacePrefix = null;
		if(message instanceof ClearThXmlMessage)
			namespacePrefix = ((ClearThXmlMessage)message).getNamespacePrefix();

		String rootTag = messageDesc.getRootTag();
		if (rootTag == null)
			rootTag = "xml";
		if (namespacePrefix != null)
			rootTag = namespacePrefix + ':' + rootTag;
		return rootTag;
	}

	////// Working with attributes

	protected boolean hasAttributes(XmlFieldDesc fieldDesc, ClearThMessage message)
	{
		for (XmlAttributeDesc attributeDesc : fieldDesc.getAttrDesc())
		{
			if (StringUtils.isNotEmpty(message.getField(attributeDesc.getName())))
				return true;
		}
		return isNotEmpty(fieldDesc.getDefaultAttrDesc());
	}

	protected void setAttributes(Element element, List<XmlAttributeDesc> attrDescs,
								 List<XmlDefaultAttrDesc> defaultAttrDescs, ClearThMessage message)
	{
		setDefaultAttributes(element, defaultAttrDescs);
		for (XmlAttributeDesc attributeDesc : attrDescs)
		{
			String value = message.getField(attributeDesc.getName());
			if (StringUtils.isNotEmpty(value))
				element.setAttribute(getSource(attributeDesc), value);
		}
	}

	protected void setDefaultAttributes(Element element, List<XmlDefaultAttrDesc> attrDescs)
	{
		for (XmlDefaultAttrDesc desc : attrDescs)
		{
			element.setAttribute(desc.getName(), desc.getValue());
		}
	}

	//// Working with tag values

	protected String getValue(XmlFieldDesc fieldDesc, ClearThMessage message)
	{
		String value = message.getField(fieldDesc.getName());

		if (StringUtils.isEmpty(value) && fieldDesc.getAlways() != null)
			value = fieldDesc.getAlways();

		if (StringUtils.isEmpty(value) && fieldDesc.getDefault() != null)
			value = prepareDefaultValue(fieldDesc);

		if (isEmptyValue(value) && fieldDesc.isUseSelfClosingTagForEmpty())
			value = "";

		return value;
	}

	protected String prepareDefaultValue(XmlFieldDesc fieldDesc)
	{
		String defaultValue = fieldDesc.getDefault();
		if ("".equals(defaultValue))
			return fieldDesc.isUseSelfClosingTagForEmpty() ? "" : ComparisonUtils.IS_EMPTY;
		else
			return defaultValue;
	}

	protected String getSource(XmlFieldDesc fieldDesc)
	{
		return (fieldDesc.getSource() != null) ? fieldDesc.getSource() : fieldDesc.getName();
	}

	protected String getSource(XmlAttributeDesc attributeDesc)
	{
		return (attributeDesc.getSource() != null) ? attributeDesc.getSource() : attributeDesc.getName();
	}

	@SuppressWarnings("unused")
	public boolean isTrimValues()
	{
		return trimValues;
	}


	public XmlDictionary getDictionary()
	{
		return dictionary;
	}
}
