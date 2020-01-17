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
    
    protected static final String JETTY_XML_FRAGMENT =
            "<Item>\n" +
                    "    <New class=\"org.eclipse.jetty.server.handler.ContextHandler\">\n" +
                    "        <Set name=\"contextPath\">%s</Set>\n" +
                    "        <Set name=\"handler\">\n" +
                    "            <New class=\"org.eclipse.jetty.server.handler.ResourceHandler\">\n" +
                    "                <Set name=\"directoriesListed\">false</Set>\n" +
                    "                <Set name=\"resourceBase\">../automation/reports</Set>\n" +
                    "            </New>\n" +
                    "        </Set>\n" +
                    "    </New>\n" +
                    "</Item>";
    
    
    public void updateJettyXml(String pathToJettyXml, String newContextPath)
    {
        File jettyXml = new File(pathToJettyXml);
        if (jettyXml.exists())
        {
            try
            {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
                DocumentBuilder documentBuilder = factory.newDocumentBuilder();
                
                if(newContextPath == null)
	                newContextPath = ClearThCoreApplicationBean.getInstance().getAppContextPath() + "/reports";
	            Document jettyXmlDocument = documentBuilder.parse(jettyXml);
                NodeList list = jettyXmlDocument.getElementsByTagName("Array");
                for (int i = 0; i < list.getLength(); i++)
                {
                    Element array = (Element)list.item(i);
                    if ("org.eclipse.jetty.server.Handler".equals(array.getAttribute("type")))
                    {
                        NodeList items = array.getElementsByTagName("Item");
                        String contextPathValue = null;
                        Node currentContextPath = null;

                        for(int k = 0; k < items.getLength(); k++)
                        {
                            Element item = (Element)items.item(k);
                            NodeList news = item.getElementsByTagName("New");
                            for (int j = 0; j < news.getLength(); j++)
                            {
                                Element n = (Element) news.item(j);
                                if ("org.eclipse.jetty.server.handler.ContextHandler".equals(n.getAttribute("class")))
                                {
                                    currentContextPath = n.getElementsByTagName("Set").item(0);
                                    contextPathValue = currentContextPath.getTextContent();
                                    break;
                                }
                            }
                            if(currentContextPath != null)
                                break;
                        }
                        if (currentContextPath == null)
                        {
                            log.info("Trying to update {}.", jettyXml);
                            insertXmlFragment(documentBuilder, jettyXmlDocument, array, newContextPath);
                            saveJettyXml(jettyXmlDocument, jettyXml);
                        }
                        else if (!newContextPath.equals(contextPathValue))
                        {
                            log.info("Trying to update context path in {}.", jettyXml);
                            currentContextPath.setTextContent(newContextPath);
                            saveJettyXml(jettyXmlDocument, jettyXml);
                        }
                        else
                            log.info("jetty.xml has been already updated.");
                        break;
                    }
                }
            }
            catch (Exception e)
            {
                log.error("An error occurred while updating of jetty.xml.", e);
            }
        }
        else
            log.warn("File jetty.xml wasn't found by path '{}'.", jettyXml);
    }

    protected static void insertXmlFragment(DocumentBuilder builder, Document parentDocument, Element parent, String contextPath)
            throws IOException, SAXException
    {
        Node fragment = builder.parse(new InputSource(new StringReader(String.format(JETTY_XML_FRAGMENT, contextPath)))).getDocumentElement();
        fragment = parentDocument.importNode(fragment, true);
        parent.insertBefore(fragment, parent.getFirstChild());
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

	public void updateJettyXml(String pathToJettyXml)
	{
		updateJettyXml(pathToJettyXml, null);
	}

    public static void main(String[] args)
    {
    	if(args.length != 2){
		    log.error("The number of arguments must be 2. jetty.xml has not been updated");
			return;
	    }
    	
        JettyXmlUpdater xmlUpdater = new JettyXmlUpdater();
        xmlUpdater.updateJettyXml(args[0], args[1]);
        log.info("Updated jetty.xml created");
    }
}
