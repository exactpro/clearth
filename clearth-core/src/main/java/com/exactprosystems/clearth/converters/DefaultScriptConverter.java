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

import com.csvreader.CsvReader;
import com.exactprosystems.clearth.automation.ActionGenerator;
import com.exactprosystems.clearth.automation.ActionMetaData;
import com.exactprosystems.clearth.automation.actions.MessageAction;
import com.exactprosystems.clearth.connectivity.CodecsStorage;
import com.exactprosystems.clearth.connectivity.EncodeException;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.ICodecFactory;
import com.exactprosystems.clearth.utils.Pair;
import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.xmldata.XmlCodecConfig;
import com.exactprosystems.clearth.xmldata.XmlScriptConverterConfig;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultScriptConverter extends ScriptConverter
{
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public String convert(String scriptToConvert, Map<String, XmlScriptConverterConfig> scriptConverterConfigs,
						  ICodecFactory codecFactory, CodecsStorage codecsConfig) throws Exception
	{
		if (scriptToConvert.length() == 0)
			return "";

		List<Pair<String[], String[]>> hv = new ArrayList<Pair<String[], String[]>>();
		CsvReader reader = null;
		try
		{
			reader = new CsvReader(new InputStreamReader(new ByteArrayInputStream(scriptToConvert.getBytes())));
			reader.setSafetySwitch(false);
			String[] currentHeader = null;
			int lineNumber = 0;
			while (reader.readRecord())
			{
				final String[] currentRecord = reader.getValues();
				lineNumber++;
				if (currentRecord.length==0 || currentRecord[0].startsWith(ActionGenerator.COMMENT_INDICATOR)) {
					continue;
				}

				if (isHeaderRecord(currentRecord)) {
					currentHeader = currentRecord;
					continue;
				}

				if (currentHeader != null) {
					if (currentHeader.length != currentRecord.length) {
						throw new EncodeException("Conversion error. Number of header columns " + currentHeader.length + " differs from number of values " + currentRecord.length + " in line " + lineNumber);
					}
					final Pair<String[], String[]> entry = new Pair<String[], String[]>();
					entry.setFirst(currentHeader);
					entry.setSecond(currentRecord);
					hv.add(entry);
				}
			}
		}
		finally
		{
			Utils.closeResource(reader);
		}

		if (hv.size() == 0)
			throw new Exception("No script strings found. Script should contain strings for header and values");

		Pair<String[], String[]> entry1 = hv.get(0),
				entry2 = hv.get(hv.size() - 1),
				mainEntry;
		String action1 = getAction(entry1.getFirst(), entry1.getSecond()),
				action2 = getAction(entry2.getFirst(), entry2.getSecond()),
				action;
		if ((action1 == null) || (action2 == null))
			throw new Exception("Could not get action name, 'Action' field not found in input text");

		String codecName;
		try
		{
			codecName = getCodecNameByActionName(action1);
		}
		catch (Exception e)
		{
			codecName = getCodecNameByActionName(action2);
		}

		XmlScriptConverterConfig config = scriptConverterConfigs.get(codecName);
		if (config == null)
			throw new Exception("Script converter for '" + codecName + "' codec not found");

		if ((!action1.equals(config.getSendSubMessageAction())) && (!action1.equals(config.getReceiveSubMessageAction())))
		{
			mainEntry = entry1;
			action = action1;
			hv.remove(0);
		}
		else
		{
			mainEntry = entry2;
			action = action2;
			hv.remove(hv.size() - 1);
		}

		Map<String, String> actionParams = null;
		for (ActionData actionData : loadActions())
		{
			if (actionData.actionName.equals(action))
			{
				actionParams = new HashMap<String, String>(actionData.params);
				break;
			}
		}

		if ((actionParams == null) || (actionParams.isEmpty()))
			throw new Exception("Action not supported. Fields of action '" + action + "' cannot be converted to message");

		Class<?> messageClass = Class.forName(config.getClearThMessageClass());
		MessageFiller filler = createMessageFiller(config);
		ClearThMessage message = (ClearThMessage) messageClass.newInstance();
		filler.fillMainFields(message, actionParams, mainEntry);

		List<ClearThMessage> subMessages = null;
		for (Pair<String[], String[]> entry : hv)  //Setting-up sub-messages i.e. repeating groups
		{
			ClearThMessage sm = (ClearThMessage) messageClass.newInstance();
			filler.fillByHeaderAndValues(sm, entry.getFirst(), entry.getSecond(), getIncludeList());
			if (subMessages == null)
				subMessages = new ArrayList<ClearThMessage>();
			subMessages.add(sm);
		}
		if (subMessages != null)
		{
			subMessages = arrangeRepeatingGroups(subMessages);
			for (ClearThMessage sm : subMessages)
				message.addSubMessage(sm);
		}

		XmlCodecConfig codecConfig = codecsConfig.getCodecConfig(codecName);
		if (codecConfig == null)
			throw new Exception("Codec config for '" + codecName + "' codec not found");

		return codecFactory.createCodec(codecConfig).encode(message);
	}

	protected MessageFiller createMessageFiller(XmlScriptConverterConfig config) throws InstantiationException, IllegalAccessException, ClassNotFoundException
	{
		String clazz = config.getMessageFillerClass();
		if (clazz == null)
			return new MessageFiller();
		return (MessageFiller)Class.forName(clazz).newInstance();
	}

	protected boolean isHeaderRecord(String[] record) {
		return record[0].startsWith(ActionGenerator.HEADER_DELIMITER);
	}

	@SuppressWarnings("unchecked")
	public String getCodecNameByActionName(String actionName) throws Exception
	{
		ActionMetaData metaData = getActions().get(actionName);
		Class<?> rawClass = null;
		try
		{
			rawClass = Class.forName(metaData.getClazz());
		}
		catch (ClassNotFoundException e)
		{
			throw new ClassNotFoundException("Class of action '" + actionName + "' not found");
		}
		Class<? extends MessageAction> actionClass;
		if (MessageAction.class.isAssignableFrom(rawClass))
			actionClass = (Class<? extends MessageAction>) rawClass;
		else
			throw new Exception("Class of action '" + actionName + "' must derive from class 'MessageAction'");

		return actionClass.newInstance().getCodecName();
	}

	@Override
	public List<String> getIncludeList()
	{
		List<String> result = new ArrayList<String>();
		result.add(ActionGenerator.COLUMN_ID);
		result.add(MessageAction.REPEATINGGROUPS.toLowerCase());
		return result;
	}

	/**
	 * Processes a list of repeating groups of different levels, making a tree from a flat list.
	 * @param list ClearThMessage flat list of repeating groups which need to be structured.
	 * @return list ClearThMessage structured list with only first level repeating groups.
	 * They have other repeating groups included into them if referenced by RepeatingGroups field.
	 * @throws EncodeException If a referenced repeating group is not presented in the list.
	 */
	//TODO: prevent possible loops of repeating groups, when one or more RGs reference themselves in a loop
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private List<ClearThMessage> arrangeRepeatingGroups(List<ClearThMessage> list) throws EncodeException
	{
		List<ClearThMessage> out = new ArrayList<ClearThMessage>(list);
		Map<String, ClearThMessage> map = new HashMap<String, ClearThMessage>();
		for (ClearThMessage<?> item : list)
		{
			String id = item.getField(ActionGenerator.COLUMN_ID);
			map.put(id, item);
		}

		for (int i = list.size()-1; i >= 0; i--)
		{
			ClearThMessage item = list.get(i);
			String rgNames = item.getField(MessageAction.REPEATINGGROUPS);
			item.removeField(MessageAction.REPEATINGGROUPS);

			String itemId = item.getField(ActionGenerator.COLUMN_ID);
			item.removeField(ActionGenerator.COLUMN_ID);

			String[] array = rgNames == null ? new String[0] : rgNames.split("\\,");
			for (String name : array)
			{
				ClearThMessage sub = map.get(name.trim());
				if (sub != null)
				{
					item.addSubMessage(sub);
					out.remove(sub);
				}
				else
					throw new EncodeException("Unable to find repeating group '"+name.trim()+"' requested in action '"+itemId+"'");
			}
		}
		return out;
	}
}
