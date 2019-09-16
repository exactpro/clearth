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

package com.exactprosystems.clearth.converters;

import com.exactprosystems.clearth.automation.actions.MessageAction;
import com.exactprosystems.clearth.connectivity.DecodeException;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.ICodec;
import com.exactprosystems.clearth.utils.Pair;
import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.xmldata.XmlExcludedParamsList;
import com.exactprosystems.clearth.xmldata.XmlMessageConverterConfig;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

public class DefaultMessageConverter extends MessageConverter
{
	@Override
	public String convert(String messageToConvert, XmlMessageConverterConfig config, ICodec codec) throws Exception
	{
		if (messageToConvert.length() == 0)
			return "";

		List<ActionData> actions = loadActions();
		lastScriptHeader = null;
		StringBuilder convertedMessage = new StringBuilder();
		MessageSplitter splitter = createMessageSplitter(config);
		for (String msgText : splitter.splitMessages(messageToConvert))
			appendFieldsToConvertedMessage(msgText, codec, config, actions, convertedMessage);

		return convertedMessage.toString();
	}
	
	protected MessageSplitter createMessageSplitter(XmlMessageConverterConfig config)
	{
		return new DefaultMessageSplitter();
	}
	
	
	protected StringBuilder buildHeaderPart(XmlMessageConverterConfig config, ClearThMessage<?> msg, ActionData actionData)
	{
		return new StringBuilder(config.getScriptHeaderStrings() + ",#" + MessageAction.CONNECTIONNAME);
	}
	
	protected StringBuilder buildValuesPart(XmlMessageConverterConfig config, ClearThMessage<?> msg, ActionData actionData)
	{
		return new StringBuilder(config.getScriptValueStrings().replaceAll(IDFIELD, "id" + idNumber++).replaceAll(ACTIONFIELD, actionData.actionName.toString())+",");
	}

	/**
	 * Appends fields from message to result script (converted message).
	 * @param msgText message text
	 * @param codec to parse message with
	 * @param config converter configuration
	 * @param actions list to find corresponding action(s)
	 * @param convertedMessage result script to append fields from message text
	 * @throws Exception
	 */
	protected void appendFieldsToConvertedMessage(String msgText, ICodec codec, 
			XmlMessageConverterConfig config, List<ActionData> actions, StringBuilder convertedMessage) throws Exception
	{
		if (msgText.trim().length()==0)
			return;
		
		if (config.isTrimMessage())
			msgText = msgText.trim();

		ClearThMessage<?> msg;
		try
		{
			msg = codec.decode(msgText);
		}
		catch (DecodeException e)
		{
			throw e;
		}

		ActionData actionData = null;
		XmlExcludedParamsList excludedParamsList = config.getExcludedParamsList();
		Set<String> excludedParams = excludedParamsList == null ? null : new HashSet<String>(excludedParamsList.getToExclude());
		boolean exclusionsSpecified = excludedParams != null;
		ACTION_LOOP: for (ActionData action : actions)
		{
			if (action.params.isEmpty())
				continue ACTION_LOOP;

			List<String> params = new ArrayList<String>();
			boolean paramsChecked = false;  //Indicates that action parameters were checked, not only skipped due to exclusions
			for (Entry<String, String> entry : action.params.entrySet())  //Checking default parameters of current action against the values in message
			{
				if (exclusionsSpecified && excludedParams.contains(entry.getKey()))
					continue;
				
				paramsChecked = true;
				if (!entry.getValue().equals(msg.getField(entry.getKey())))
					continue ACTION_LOOP;
				params.add(entry.getKey());
			}
			
			if (!paramsChecked)
				continue;

			for (String param : params)  //Removing default parameters from message thus result script won't contain them
				msg.removeField(param);

			actionData = action;
			break;
		}

		if (actionData == null)
			throw new Exception("Message type is not supported");

		String rgActionName;
		if (!actionData.incoming)
			rgActionName = config.getSendSubMessageAction();
		else
			rgActionName = config.getReceiveSubMessageAction();

		List<String> subMsgIds = new ArrayList<String>();
		for (ClearThMessage<?> subMsg : msg.getSubMessages())
		{
			String id = "id"+idNumber++;
			convertedMessage.append(deepFieldsToStrings(subMsg, id, rgActionName, config)).append(Utils.EOL+Utils.EOL);
			subMsgIds.add(id);
		}

		final StringBuilder
				startHeaderPart = buildHeaderPart(config, msg, actionData),
				startValuesPart = buildValuesPart(config, msg, actionData);
		if (msg.getSubMessages().size()>0)
		{
			startHeaderPart.append(",#"+MessageAction.REPEATINGGROUPS);
			startValuesPart.append(",\"");
			startValuesPart.append(StringUtils.join(subMsgIds, ","));
			startValuesPart.append("\"");
		}

		final Pair<String, String>
				startScriptHeadAndVal = new Pair<String, String>(startHeaderPart.toString(), startValuesPart.toString()),
				endScriptHeadAndVal = fieldsToStrings(msg, config);
		final String preparedMessage = fieldsToScript(startScriptHeadAndVal, endScriptHeadAndVal);
		convertedMessage.append(preparedMessage).append(Utils.EOL + Utils.EOL);
	}
}
