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

package com.exactprosystems.clearth.utils;

import java.io.File;
import java.io.FileWriter;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class UsersRepairer
{
	private static final Logger logger = LoggerFactory.getLogger(UsersRepairer.class);
	
	public static final String USERS = "Users",
			USER = "User",
			NAME = "Name",
			PASSWORD = "Password",
			ROLE = "Role";
	
	public boolean repairUsersFile(String usersFileName)
	{
		File broken = new File(usersFileName);
		if (!backupFile(broken, new File(usersFileName + "_backup")))
			return false;
		
		logger.info("Broken file is kept as backup in the same directory");
		
		FileWriter writer = null;
		try
		{
			String xmlDocument = FileUtils.readFileToString(new File(usersFileName), "UTF-8");
			
			Document doc = repairUsersXml(xmlDocument);
			writer = new FileWriter(usersFileName);
			XmlUtils.writeXml(doc, writer);
			return true;
		}
		catch (Exception e)
		{
			logger.error("Could not fix '"+usersFileName+"' config file", e);
			return false;
		}
		finally
		{
			Utils.closeResource(writer);
		}
	}
	
	public boolean resetUsersFile(String fileName)
	{
		FileWriter writer = null;
		try
		{
			writer = new FileWriter(fileName);
			writer.write(TagUtils.openTag(USERS, null));
			writer.write(Utils.EOL);
			writer.write(TagUtils.closeTag(USERS));
			return true;
		}
		catch (Exception e)
		{
			logger.error("Could not reset '"+fileName+"' config file to empty state", e);
			return false;
		}
		finally
		{
			Utils.closeResource(writer);
		}
	}
	
	
	protected boolean backupFile(File file, File backup)
	{
		try
		{
			FileUtils.copyFile(file, backup);
			return true;
		}
		catch (Exception e)
		{
			logger.error("Could not create backup copy of file '"+file.getAbsolutePath()+"'", e);
			return false;
		}
	}
	
	protected Document repairUsersXml(String xml) throws ParserConfigurationException
	{
		Document doc = XmlUtils.createDocument();
		Element root = doc.createElement(USERS);
		while (TagUtils.tagExists(USER, xml)) {
			Element user = doc.createElement(USER);
			String userInfo, temp;
			if (TagUtils.tagExists("/" + USER, xml)) {
				if (TagUtils.checkTagClosed(USER, xml)) {
					userInfo = TagUtils.getTagValue(USER, xml);
					temp = xml.substring(TagUtils.getTagEnd(USER, xml, -1, true));
				} else {
					userInfo = TagUtils.getTagValue(USER, xml);
					userInfo = userInfo.substring(0, TagUtils.getTagStart(USER, userInfo, -1));
					temp = xml.substring(xml
							.substring(TagUtils.getTagStart(USER, xml, -1) + USER.length() + 2)
							.indexOf(TagUtils.openTag(USER, null)) + TagUtils.getTagStart(USER, xml, -1)
							+ USER.length() + 2);
				}
			} else {
				if (TagUtils.tagExists(USER, xml
						.substring(TagUtils.getTagStart(USER, xml, -1) + USER.length() + 2))) {
					userInfo = TagUtils.getTagValue(USER, xml);
					userInfo = userInfo.substring(0, TagUtils.getTagStart(USER, userInfo, -1));
					temp = xml.substring(xml
							.substring(TagUtils.getTagStart(USER, xml, -1) + USER.length() + 2)
							.indexOf(TagUtils.openTag(USER, null)) + TagUtils.getTagStart(USER, xml, -1)
							+ USER.length() + 2);
				} else {
					userInfo = TagUtils.getTagValue(USER, xml);
					temp = "";
				}
			}
			Element name = doc.createElement(NAME);
			Element password = doc.createElement(PASSWORD);
			Element role = doc.createElement(ROLE);
			String n = TagUtils.getTagValue(NAME, userInfo);
			if (n != null) {
				if (!userInfo.contains(TagUtils.closeTag(NAME))) {
					if (userInfo.contains(TagUtils.openTag(PASSWORD, null)))
						n = n.substring(0, TagUtils.getTagStart(PASSWORD, n, -1));
					else if (userInfo.contains(TagUtils.openTag(ROLE, null)))
						n = n.substring(0, TagUtils.getTagStart(ROLE, n, -1));
				}
				if (n.contains(Utils.EOL))
					n = n.substring(0, n.indexOf(Utils.EOL));
			} else
				n = "";
			name.setTextContent(n);
			String p = TagUtils.getTagValue(PASSWORD, userInfo);
			if (p != null) {
				if (!userInfo.contains(TagUtils.closeTag(PASSWORD))) {
					if (userInfo.contains(TagUtils.openTag(ROLE, null)))
						p = p.substring(0, TagUtils.getTagStart(ROLE, p, -1));
				}
				if (p != null && p.contains(Utils.EOL))
					p = p.substring(0, p.indexOf(Utils.EOL));
			} else
				p = "";
			password.setTextContent(p);
			String r = TagUtils.getTagValue(ROLE, userInfo);
			if (r != null && r.contains(Utils.EOL))
				r = r.substring(0, r.indexOf(Utils.EOL));
			if (r == null)
				r = "";
			role.setTextContent(r);
			user.appendChild(name);
			user.appendChild(password);
			user.appendChild(role);
			root.appendChild(user);
			xml = temp;
		}
		doc.appendChild(root);
		return doc;
	}
}
