/******************************************************************************
 * Copyright 2009-2023 Exactpro Systems Limited
 * https://www.exactpro.com
 * Build Software to Test Software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.exactprosystems.clearth.web.jetty;

import com.exactprosystems.clearth.web.beans.ClearThCoreApplicationBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;

public class JettyXmlUpdater
{
	private static final Logger log = LoggerFactory.getLogger(JettyXmlUpdater.class);
	
	public void updateJettyXml(String pathToJettyXml)
	{
		File jettyXml = new File(pathToJettyXml);
		if (jettyXml.exists())
		{
			try
			{
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
				DocumentBuilder documentBuilder = factory.newDocumentBuilder();
				
				Document jettyXmlDocument = documentBuilder.parse(jettyXml);
				NodeList list = jettyXmlDocument.getElementsByTagName("Array");
				for (int i = 0; i < list.getLength(); i++)
				{
					Element array = (Element)list.item(i);
					if ("org.eclipse.jetty.server.Handler".equals(array.getAttribute("type")))
					{
						NodeList items = array.getElementsByTagName("Item");

						for (int k = 0; k < items.getLength(); k++)
						{
							Element item = (Element)items.item(k);
							NodeList news = item.getElementsByTagName("New");
							for (int j = 0; j < news.getLength(); j++)
							{
								if (checkItemShouldBeRemoved((Element) news.item(j)))
								{
									array.removeChild(item);
									saveJettyXml(jettyXmlDocument, jettyXml);
									log.info("Jetty config has been updated. Outdated resource handler removed");
									return;
								}
							}
						}
					}
				}
			}
			catch (Exception e)
			{
				log.error("An error occurred while updating jetty.xml.", e);
			}
		}
		else
			log.warn("File jetty.xml not found by path '{}'.", jettyXml);
	}

	protected static boolean checkItemShouldBeRemoved(Element handlerDescription)
	{
		if (!"org.eclipse.jetty.server.handler.ContextHandler".equals(handlerDescription.getAttribute("class")))
			return false;
		NodeList items = handlerDescription.getElementsByTagName("Set");
		for (int j = 0; j < items.getLength(); j++)
		{ 
			Element item = (Element)items.item(j);
			if ("contextPath".equals(item.getAttribute("name"))
				&& "/clearth/reports".equals(item.getTextContent())) {
				return true;
			}
		}
		return false;
	}

	protected static void saveJettyXml(Document jettyXmlDocument, File oldJettyXml) throws TransformerException
	{
		oldJettyXml.renameTo(new File("jetty.xml.old"));

		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
		DocumentType doctype = jettyXmlDocument.getDoctype();
		if (doctype != null)
		{
			transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, doctype.getPublicId());
			transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, doctype.getSystemId());
		}
		transformer.transform(new DOMSource(jettyXmlDocument), new StreamResult(oldJettyXml));
	}

	public static void main(String[] args)
	{
		if(args.length != 1){
			log.error("The number of arguments must be 1. jetty.xml has not been updated");
			return;
		}
		
		JettyXmlUpdater xmlUpdater = new JettyXmlUpdater();
		xmlUpdater.updateJettyXml(args[0]);
		log.info("Updated jetty.xml created");
	}
}
