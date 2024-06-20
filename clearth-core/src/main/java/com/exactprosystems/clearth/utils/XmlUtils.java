/******************************************************************************
 * Copyright 2009-2024 Exactpro Systems Limited
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

package com.exactprosystems.clearth.utils;

import static com.exactprosystems.clearth.utils.Utils.closeResource;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.thoughtworks.xstream.XStream;

public class XmlUtils
{
	private static final Logger logger = LoggerFactory.getLogger(XmlUtils.class);
	
	private static Class<?>[] appendElementToArray(Class<?>[] array, Class<?> newElement)
	{
		Class<?>[] newArray = new Class<?>[array.length + 1];
		System.arraycopy(array, 0, newArray, 0, array.length);
		newArray[array.length] = newElement;
		return newArray;
	}
	
	public static <T> T unmarshalObject(Class<T> cls, String inputFileName, Class<?>[] contextClasses) throws JAXBException, IOException
	{
		JAXBContext context = JAXBContext.newInstance(appendElementToArray(contextClasses, cls));
		Unmarshaller um = context.createUnmarshaller();
		FileInputStream is = null;
		try
		{
			is = new FileInputStream(inputFileName);
			return cls.cast(um.unmarshal(is));
		}
		finally
		{
			closeResource(is);
		}
	}
	
	public static <T> T unmarshalObject(Class<T> cls, String inputFileName) throws JAXBException, IOException
	{
		return unmarshalObject(cls, inputFileName, new Class[0]);
	}
	
	public static <T> T unmarshalObject(Class<T> cls, InputStream inputStream) throws JAXBException
	{
		JAXBContext context = JAXBContext.newInstance(cls);
		Unmarshaller um = context.createUnmarshaller();
		return cls.cast(um.unmarshal(inputStream));
	}
	
	public static void marshalObject(Object object, String outputFileName, Class<?>[] contextClasses) throws JAXBException
	{
		JAXBContext context = JAXBContext.newInstance(appendElementToArray(contextClasses, object.getClass()));
		Marshaller m = context.createMarshaller();
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		m.marshal(object, new File(outputFileName));
	}
	
	public static void marshalObject(Object object, String outputFileName) throws JAXBException
	{
		marshalObject(object, outputFileName, new Class[0]);
	}
	
	public static void marshalObject(Object object, OutputStream outputStream) throws JAXBException
	{
		JAXBContext context = JAXBContext.newInstance(object.getClass());
		Marshaller m = context.createMarshaller();
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		m.marshal(object, outputStream);
	}
	
	
	@SuppressWarnings("rawtypes")
	public static void objectToXml(Object object, OutputStream outputStream, Class[] annotatedClasses, Class[] allowedClasses) throws IOException
	{
		XStream xs = new XStream();
		if (annotatedClasses != null)
			xs.processAnnotations(annotatedClasses);
		if (allowedClasses != null)
			xs.allowTypes(allowedClasses);
		xs.toXML(object, outputStream);
	}
	
	@SuppressWarnings("rawtypes")
	public static void objectToXmlFile(Object object, File outputFile, Class[] annotatedClasses, Class[] allowedClasses) throws IOException
	{
		try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outputFile)))
		{
			objectToXml(object, bos, annotatedClasses, allowedClasses);
		}
	}
	
	@SuppressWarnings("rawtypes")
	public static Object xmlToObject(InputStream inputStream, Class[] annotatedClasses, Class[] allowedClasses) throws IOException
	{
		XStream xs = new XStream();
		if (annotatedClasses != null)
			xs.processAnnotations(annotatedClasses);
		if (allowedClasses != null)
			xs.allowTypes(allowedClasses);
		return xs.fromXML(inputStream);
	}
	
	@SuppressWarnings("rawtypes")
	public static Object xmlFileToObject(File sourceFile, Class[] annotatedClasses, Class[] allowedClasses) throws IOException
	{
		try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(sourceFile)))
		{
			return xmlToObject(bis, annotatedClasses, allowedClasses);
		}
	}
	
	
	public static DocumentBuilder getXmlDocumentBuilder() throws ParserConfigurationException
	{
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();     
		docBuilderFactory.setNamespaceAware(true);
		return docBuilderFactory.newDocumentBuilder();
	}
	
	public static Transformer getXmlTransformer() throws TransformerConfigurationException, TransformerFactoryConfigurationError
	{
		Transformer result = TransformerFactory.newInstance().newTransformer();
		// result.setOutputProperty(OutputKeys.INDENT, "yes");
		return result;
	}
	
	public static void appendAll(Node node, Collection<Node> subNodes)
	{
		for (Node subNode : subNodes)
			node.appendChild(subNode);
	}
	
	public static Document createDocument() throws ParserConfigurationException
	{
		Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		document.setXmlStandalone(true);
		return document;
	}
	
	
	public static String xmlToString(Document xml, Transformer transformer) throws TransformerException
	{
		StringWriter writer = new StringWriter();
		transformer.transform(new DOMSource(xml), new StreamResult(writer));
		return writer.getBuffer().toString();
	}
	
	public static String xmlToString(Document xml) throws TransformerConfigurationException, TransformerException, TransformerFactoryConfigurationError
	{
		return xmlToString(xml, getXmlTransformer());
	}
	
	public static void writeXml(Document document, Writer writer)
	{
		try
		{
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
			transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "yes");
			transformer.transform(new DOMSource(document), new StreamResult(writer));
		}
		catch (TransformerConfigurationException e)
		{
			// This exception is unlikely to be thrown because factory has default settings
			logger.error("Invalid settings of TransformerFactory", e);
		}
		catch (TransformerException e)
		{
			logger.error("An error occurred while conversion DOM to text representation", e);
		}
	}
}
