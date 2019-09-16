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
import com.exactprosystems.clearth.connectivity.CodecsStorage;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.tools.MessageParserTool;
import com.exactprosystems.clearth.utils.ExceptionUtils;
import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.web.beans.ClearThBean;
import com.exactprosystems.clearth.web.beans.tree.MessageNode;
import com.exactprosystems.clearth.web.misc.MessageUtils;
import com.exactprosystems.clearth.xmldata.XmlCodecConfig;

import org.apache.commons.lang.StringUtils;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import javax.annotation.PostConstruct;
import javax.xml.bind.UnmarshalException;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.exactprosystems.clearth.tools.MessageParserTool.AUTO_FORMAT;

/**
 * Created by alexander.magomedov on 10/31/16.
 */
public class MessageParserToolBean extends ClearThBean
{
	protected MessageParserTool messageParserTool;
	protected CodecsStorage codecs;
	
	protected String textToParseFormat = "", textToParse = "", parsedText = "";
	protected TreeNode parsedTree = new DefaultTreeNode();
	
	@PostConstruct
	public void init()
	{
		messageParserTool = ClearThCore.getInstance().getToolsFactory().createMessageParserTool();
		codecs = messageParserTool.getCodecs();
		textToParseFormat = getTextToParseFormatDefault();
	}
	
	public String getTextToParseFormatDefault()
	{
		if (codecs == null || codecs.getConfigsList().isEmpty())
			return "";
		
		if (codecs.getConfigsList().size() == 1)
		{
			for (String value : codecs.getCodecNames())
				return value;
		}
		
		return AUTO_FORMAT;
	}
	
	public void parseText()
	{		
		getLogger().info("Parsing text: " + textToParse + Utils.EOL + "Parsing format: " + textToParseFormat);
		messageParserTool.parseText(textToParse, textToParseFormat);
		parsedText = messageParserTool.getParsedText();
		
		parsedTree = new DefaultTreeNode();
		if (!parsedText.isEmpty())
			messageToTree(messageParserTool.getParsedMsg(), parsedTree);

		handleExceptions();
	}
	
	protected void messageToTree(ClearThMessage<?> message, TreeNode parentNode)
	{		
		for (String key : message.getFieldNames())
		{
			new DefaultTreeNode(new MessageNode(key, message.getField(key)), parentNode);
		}
		for (ClearThMessage<?> subMessage : message.getSubMessages())
		{
			subMessageToTree(subMessage, parentNode);
		}
	}
	
	protected void subMessageToTree(ClearThMessage<?> message, TreeNode parentNode)
	{
		DefaultTreeNode subParent = new DefaultTreeNode(new MessageNode(message.getField(ClearThMessage.SUBMSGTYPE), ""), parentNode);
		
		for (String key : message.getFieldNames())
		{
			if (key != ClearThMessage.SUBMSGTYPE)
				new DefaultTreeNode(new MessageNode(key, message.getField(key)), subParent);
		}
		
		for (ClearThMessage<?> subMessage : message.getSubMessages())
			subMessageToTree(subMessage, subParent);
	}

	protected void handleExceptions()
	{
		Map<String, Exception> exceptionMap = messageParserTool.getExceptionMap();
		if (StringUtils.isEmpty(parsedText) && !exceptionMap.isEmpty())
		{	
			for (Map.Entry<String, Exception> entry : exceptionMap.entrySet())
			{
				if (entry.getValue() instanceof UnmarshalException)
					handleException(entry.getKey() + ": could not load dictionary", entry.getValue());
				else
					handleException(entry.getKey() + ": could not parse text", entry.getValue());
			}
		}
	}
	
	protected void handleException(String message, Exception e)
	{
		getLogger().warn(message, e);
		MessageUtils.addErrorMessage(message, ExceptionUtils.getDetailedMessage(e));
	}
	
	public boolean isCodecsAvailable()
	{
		return !codecs.getConfigsList().isEmpty();
	}
	
	public Set<String> getCodecs()
	{
		return codecs.getCodecNames();
	}
	
	public List<XmlCodecConfig> getCodecConfigs()
	{
		return codecs.getConfigsList();
	}
	
	public String getTextToParse()
	{
		return this.textToParse;
	}
	
	public void setTextToParse(String textToParse)
	{
		this.textToParse = textToParse;
	}
	
	
	public String getTextToParseFormat()
	{
		return this.textToParseFormat;
	}
	
	public void setTextToParseFormat(String textToParseFormat)
	{
		this.textToParseFormat = textToParseFormat;
	}
	
	
	public String getParsedText()
	{
		return this.parsedText;
	}
	
	public TreeNode getParsedTree()
	{
		return this.parsedTree;
	}
}
