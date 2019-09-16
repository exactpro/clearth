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

package com.exactprosystems.clearth.web.beans.tools;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.connectivity.iface.DefaultMessageHelperFactory;
import com.exactprosystems.clearth.connectivity.iface.MessageColumnNode;
import com.exactprosystems.clearth.connectivity.iface.MessageHelper;
import com.exactprosystems.clearth.connectivity.iface.MessageHelperFactory;
import com.exactprosystems.clearth.utils.ExceptionUtils;
import com.exactprosystems.clearth.web.beans.ClearThBean;
import com.exactprosystems.clearth.web.misc.MessageUtils;
import com.exactprosystems.clearth.xmldata.XmlMessageHelperConfig;
import org.apache.commons.lang.StringUtils;

import javax.annotation.PostConstruct;
import javax.faces.model.SelectItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by alexander.magomedov on 10/31/16.
 */
public class MessageHelperToolBean extends ClearThBean
{
	protected Map<String, XmlMessageHelperConfig> messageHelperConfigs;
	protected MessageHelper messageHelper;
	protected String currentMessageHelpFormat = "", currentMessageHelperType = "", messageDirection = null;
	protected MessageHelperFactory messageHelperFactory;
	protected List<MessageColumnNode> messageHelperData;
	protected List<MessageColumnNode> filteredMessageHelperData;
	protected List<String> messageTypeKeysDesc = null;
	protected List<String> messageHelperColumns = null;
	protected List<MessageColumnNode> messageHelperRepGroups = null;
	
	@PostConstruct
	public void init()
	{
		messageHelperFactory = initMessageHelperFactory();
		messageHelperConfigs = ClearThCore.getInstance().getMessageHelpers();
		currentMessageHelpFormat = getMessageHelpFormatDefault();
	}
	
	protected MessageHelperFactory initMessageHelperFactory()
	{
		return new DefaultMessageHelperFactory();
	}
	
	public String getMessageHelpFormatDefault()
	{
		if (messageHelperConfigs == null)
			return "";
		
		if (messageHelperConfigs.size() == 1)
		{
			for (String value : messageHelperConfigs.keySet())
			{
				setCurrentMessageHelpFormat(value);
				return value;
			}
		}
		
		return "";
	}
	
	
	public String getCurrentMessageHelpFormat()
	{
		return this.currentMessageHelpFormat;
	}
	
	public void setCurrentMessageHelpFormat(String currentMessageHelpFormat)
	{
		XmlMessageHelperConfig messageHelperConfig = messageHelperConfigs.get(currentMessageHelpFormat);
		
		try
		{
			messageHelper = createMessageHelper(messageHelperConfig.getMessageHelper(), messageHelperConfig.getDictionaryFile());
		}
		catch (Exception e)
		{
			getLogger().warn("could not create message helper for '" + messageHelperConfig.getName() + "'", e);
			MessageUtils.addErrorMessage("Could not create message helper for '" + messageHelperConfig.getName() + "'", ExceptionUtils.getDetailedMessage(e));
		}
		this.currentMessageHelpFormat = currentMessageHelpFormat;
		
		setCurrentMessageHelperType("");
	}
	
	private MessageHelper createMessageHelper(String messageHelper, String dictionaryFile) throws Exception
	{
		return messageHelperFactory.createMessageHelper(messageHelper, dictionaryFile);
	}
	
	
	public String getCurrentMessageHelperType()
	{
		return this.currentMessageHelperType;
	}
	
	public void setCurrentMessageHelperType(String value)
	{
		currentMessageHelperType = value;
		messageDirection = null;
		messageHelperData = new ArrayList<MessageColumnNode>();
		messageTypeKeysDesc = null;
		messageHelperColumns = null;
		
		if (StringUtils.isEmpty(currentMessageHelperType))
			return;
		
		messageHelper.getMessageDescription(messageHelperData, currentMessageHelperType);
		messageDirection = messageHelper.getDirection();
		messageTypeKeysDesc = messageHelper.getKeys();
		messageHelperColumns = messageHelper.getColumns();
	}
	
	
	public ArrayList<String> getMessageHelpers()
	{
		return new ArrayList<String>(messageHelperConfigs.keySet());
	}
	
	public List<String> getMessageTypeKeysDesc() { return messageTypeKeysDesc; }
	
	public List<MessageColumnNode> getMessageHelperData() { return messageHelperData; }
	
	public List<String> getMessageHelperColumns()
	{
		return messageHelperColumns;
	}
	
	
	private SelectItem[] wrapSelectItems(String[] array)
	{
		SelectItem[] items = new SelectItem[array.length];
		for (int i = 0; i < array.length; i++)
			items[i] = new SelectItem(array[i]);
		return items;
	}
	
	public SelectItem[] getMessageHelperMandatoryOptions()
	{
		return wrapSelectItems(messageHelper.getMandatoryOptions());
	}
	
	public SelectItem[] getMessageHelperRepetitiveOptions()
	{
		return wrapSelectItems(messageHelper.getRepetitiveOptions());
	}
	
	public Map<String, XmlMessageHelperConfig> getMessageHelperConfigs()
	{
		return this.messageHelperConfigs;
	}
	
	public String getMessageDirection()
	{
		return this.messageDirection;
	}
	
	public List<String> getMessageHelperTypes()
	{
		return messageHelper.getMessagesNames();
	}
	
	
	public List<MessageColumnNode> getMessageHelperRepGroups()
	{
		return this.messageHelperRepGroups;
	}
	
	public void setMessageHelperRepGroups(List<MessageColumnNode> messageHelperRepGroups)
	{
		this.messageHelperRepGroups = messageHelperRepGroups;
	}
	
	
	public List<MessageColumnNode> getFilteredMessageHelperData()
	{
		return filteredMessageHelperData;
	}
	
	public void setFilteredMessageHelperData(List<MessageColumnNode> filteredMessageHelperData)
	{
		this.filteredMessageHelperData = filteredMessageHelperData;
	}
}
