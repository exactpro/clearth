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

package com.exactprosystems.clearth.web.beans;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.faces.model.SelectItem;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import com.exactprosystems.clearth.web.misc.WebUtils;
import org.apache.commons.lang3.StringUtils;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.connectivity.MessageTemplate;
import com.exactprosystems.clearth.connectivity.connections.ClearThMessageConnection;
import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.web.misc.MessageUtils;
import com.exactprosystems.clearth.web.misc.UserInfoUtils;

import static com.exactprosystems.clearth.ClearThCore.connectionStorage;

public class SendMessagesBean extends ClearThBean
{
	private final String userName = UserInfoUtils.getUserName();
	private final Path userTemplatesDir = Paths.get(ClearThCore.templatesPath(), userName);
	private ClearThMessageConnection selectedConnection = null;
	private List<MessageTemplate> templates = new ArrayList<MessageTemplate>();
	private MessageTemplate selectedTemplate = null;
	
	private String messageBody = "", messageName = "";

	public SendMessagesBean() throws IOException
	{
		Files.createDirectories(userTemplatesDir);

		Unmarshaller u = null;
		try
		{
			JAXBContext jc = JAXBContext.newInstance(MessageTemplate.class);
			u = jc.createUnmarshaller();
		}
		catch (JAXBException e)
		{
			getLogger().warn("Error while creating unmarshaller", e);
		}

		File[] filesList = userTemplatesDir.toFile().listFiles();
		if (filesList!=null)
		{
			for (File file : filesList)
			{
				try
				{
					MessageTemplate t = (MessageTemplate)u.unmarshal(file);
					templates.add(t);
				}
				catch (JAXBException e)
				{
					getLogger().warn("Error while unmarshalling message template", e);
				}
			}
		}
	}
	
	
	
	public SelectItem[] getTemplates()
	{
		SelectItem[] result = new SelectItem[templates.size()];
		int i = -1;
		for (MessageTemplate t : templates)
		{
			i++;
			result[i] = new SelectItem(t.name);
		}
		return result;
	}
	
	public int getTemplatesNumber()
	{
		return templates.size();
	}

	
	public String getConnectionName()
	{
		if (selectedConnection==null)
		{
			List<ClearThMessageConnection> msgCons =
					connectionStorage().getConnections((con) -> con instanceof ClearThMessageConnection,
							ClearThMessageConnection.class);
			if (msgCons.size()>0)
				selectedConnection = msgCons.get(0);
		}
		
		if (selectedConnection!=null)
			return selectedConnection.getName();
		else
			return null;
	}

	public void setConnectionName(String connectionName)
	{
		selectedConnection = (ClearThMessageConnection) connectionStorage().getConnection(connectionName);
	}
	
	
	public String getSelectedTemplateName()
	{
		if (selectedTemplate==null)
			if (templates.size()>0)
			{
				selectedTemplate = templates.get(0);
				onTemplateSelect();
			}
		
		if (selectedTemplate!=null)
			return selectedTemplate.name;
		else
			return null;
	}

	public void setSelectedTemplateName(String templateName)
	{
		selectedTemplate = null;
		for (MessageTemplate t : templates)
			if (t.name.equals(templateName))
			{
				selectedTemplate = t;  //onTemplateSelect() is called from page on select event
				break;
			}
	}

	
	public void removeTemplate()
	{
		if (selectedTemplate==null)
		{
			MessageUtils.addWarningMessage("Template not selected", "Please select message template from list");
			return;
		}
		
		try
		{
			Path p = Paths.get(userTemplatesDir.toString(), selectedTemplate.name+".xml");
			Files.delete(p);
		}
		catch (IOException e)
		{
			WebUtils.logAndGrowlException("Could not remove template", e, getLogger());
			return;
		}
		
		templates.remove(selectedTemplate);
		if (templates.size()>0)
		{
			selectedTemplate = templates.get(0);
			onTemplateSelect();
		}
		else
			selectedTemplate = null;
		MessageUtils.addInfoMessage("Success", "Message template removed");
	}

	public void saveTemplate()
	{
		if (StringUtils.isEmpty(messageName))
		{
			MessageUtils.addWarningMessage("Template name not specified", "Please specify template name");
			return;
		}
		
		try
		{
			JAXBContext context = JAXBContext.newInstance(MessageTemplate.class);
			Marshaller m = context.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			
			MessageTemplate t = null;
			for (MessageTemplate template : templates)
			{
				if (template.name.equals(messageName))
				{
					t = template;
					break;
				}
			}

			if (t==null)
			{
				t = new MessageTemplate();
				t.body = messageBody;
				t.name = messageName;
				templates.add(t);
			}
			else
				t.body = messageBody;
			selectedTemplate = t;

			m.marshal(t, new File(userTemplatesDir.toString(), messageName+".xml"));
			getLogger().info("saved template '"+messageName+"'");
			MessageUtils.addInfoMessage("Info", "Message template saved");
		}
		catch (JAXBException e)
		{
			WebUtils.logAndGrowlException("Error while saving template", e, getLogger());
		}
	}
	
	
	public void sendMessage()
	{
		if (selectedConnection==null)
		{
			MessageUtils.addWarningMessage("Connection not selected", "Please select connection from list");
			return;
		}
		
		String message = messageBody.replace("\n", Utils.EOL).replace("\r"+Utils.EOL, Utils.EOL);
		try
		{
			selectedConnection.sendMessage(message);
			getLogger().info("sent message");
			MessageUtils.addInfoMessage("Message sent", "via '" + selectedConnection.getName()+"'");
		}
		catch (Exception e)
		{
			WebUtils.logAndGrowlException("Error while sending message", e, getLogger());
		}
	}
	
	
	public String getMessageBody()
	{
		return messageBody;
	}
	
	public void setMessageBody(String messageBody)
	{
		this.messageBody = messageBody;
	}
	
	
	public String getMessageName()
	{
		return messageName;
	}
	
	public void setMessageName(String messageName)
	{
		this.messageName = messageName;
	}
	
	
	public void onTemplateSelect()
	{
		messageName = selectedTemplate.name;
		messageBody = selectedTemplate.body;
	}
}
