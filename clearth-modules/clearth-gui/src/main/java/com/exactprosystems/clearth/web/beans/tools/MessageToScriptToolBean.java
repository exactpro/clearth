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
import com.exactprosystems.clearth.connectivity.DecodeException;
import com.exactprosystems.clearth.tools.MessageToScriptTool;
import com.exactprosystems.clearth.utils.DictionaryLoadException;
import com.exactprosystems.clearth.utils.ExceptionUtils;
import com.exactprosystems.clearth.web.beans.ClearThBean;
import com.exactprosystems.clearth.web.misc.MessageUtils;
import com.exactprosystems.clearth.xmldata.XmlMessageConverterConfig;
import org.apache.commons.lang.StringUtils;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.Set;

import static com.exactprosystems.clearth.tools.MessageParserTool.AUTO_FORMAT;

/**
 * Created by alexander.magomedov on 10/31/16.
 */
public class MessageToScriptToolBean extends ClearThBean
{
	protected MessageToScriptTool messageToScriptTool;
	protected Map<String, XmlMessageConverterConfig> messageConverterConfigs;
	protected String messageConvertFormat = "", messageToConvert;
	protected String convertedMessage = "";
	
	@PostConstruct
	public void init()
	{
		messageToScriptTool = ClearThCore.getInstance().getToolsFactory().createMessageToScriptTool();
		messageConverterConfigs = messageToScriptTool.getMessageConverterConfigs();
		messageConvertFormat = getMessageConvertFormatDefault();
	}
	
	public String getMessageConvertFormatDefault()
	{
		if (messageConverterConfigs == null || messageConverterConfigs.isEmpty())
			return "";
		
		if (messageConverterConfigs.size() == 1)
		{
			for (String value : messageConverterConfigs.keySet())
				return value;
		}

		return AUTO_FORMAT;
	}
	
	public void convertMessage()
	{
		if (messageToConvert == null || messageToConvert.isEmpty())
		{
			MessageUtils.addErrorMessage("Nothing to convert", "Text to convert is empty.");
			return;
		}
		
		if (messageConvertFormat == null || messageConvertFormat.isEmpty())
		{
			MessageUtils.addErrorMessage("Converter is undefined!", "Choose converter before conversion.");
			return;
		}

		convertedMessage = "";
		try {
			convertedMessage = messageToScriptTool.convertMessage(messageToConvert, messageConvertFormat);
		}
		catch (DecodeException e) {
			handleException("Could not decode message", e);
		}
		catch (DictionaryLoadException e) {
			handleException("Could not load dictionary", e);
		}
		catch (Exception e) {
			handleException("Error while converting message", e);
		}
	}

	public String getCurrentCodecName()
	{
		return messageToScriptTool.getCurrentCodecName();
	}

	public boolean isFormatAuto()
	{
		return StringUtils.equals(messageConvertFormat, AUTO_FORMAT);
	}

	private void handleException(String message, Exception e) {
		getLogger().warn(message, e);
		MessageUtils.addErrorMessage(message, ExceptionUtils.getDetailedMessage(e));
	}
	
	public boolean isMessageConvertersAvailable()
	{
		return messageConverterConfigs.size() > 0;
	}	
	
	public Set<String> getMessageConverters()
	{
		return messageConverterConfigs.keySet();
	}
	
	public Map<String, XmlMessageConverterConfig> getMessageConverterConfigs()
	{
		return messageConverterConfigs;
	}
	
	
	public String getMessageConvertFormat()
	{
		return this.messageConvertFormat;
	}
	
	public void setMessageConvertFormat(String messageConvertFormat)
	{
		this.messageConvertFormat = messageConvertFormat;
	}
	
	
	public String getMessageToConvert()
	{
		return this.messageToConvert;
	}
	
	public void setMessageToConvert(String messageToConvert)
	{
		this.messageToConvert = messageToConvert;
	}
	
	
	public String getConvertedMessage()
	{
		return this.convertedMessage;
	}
}
