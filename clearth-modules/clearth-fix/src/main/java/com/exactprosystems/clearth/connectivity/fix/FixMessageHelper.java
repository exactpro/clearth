/******************************************************************************
 * Copyright 2009-2023 Exactpro Systems Limited
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

package com.exactprosystems.clearth.connectivity.fix;

import com.exactprosystems.clearth.connectivity.iface.MessageColumnNode;
import com.exactprosystems.clearth.connectivity.iface.MessageHelper;
import com.exactprosystems.clearth.utils.DictionaryLoadException;
import quickfix.ConfigError;
import quickfix.DataDictionary;
import quickfix.DataDictionary.GroupInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class FixMessageHelper extends MessageHelper
{
	private final FixDictionary dictionary;
	private final List<String> columns = Arrays.asList("Name(tag)/Subvalue(tag)", "Mandatory", "Repetition", "Type");
	private final List<String> messages;
	
	public FixMessageHelper(String dictionary) throws DictionaryLoadException, ConfigError
	{
		super(dictionary);
		this.dictionary = new FixDictionary(dictionary);
		this.messages = new ArrayList<>(this.dictionary.getAppDictionary().getMessagesInfo().getMessageFieldsInfo().keySet());
	}
	
	@Override
	public void getMessageDescription(List<MessageColumnNode> output, String typeMessage)
	{
		ClearThDataDictionary appDict = dictionary.getAppDictionary();
		FieldsInfo fields = appDict.getMessageFieldsInfo(typeMessage);
		if (fields == null)
			return;
		
		processTags(fields.getFields(), typeMessage, null, appDict, appDict, output);
	}
	
	@Override
	public List<String> getColumns()
	{
		return columns;
	}
	
	@Override
	public List<String> getMessagesNames()
	{
		return messages;
	}
	
	@Override
	public String getDirection()
	{
		return null;
	}
	
	@Override
	public List<String> getKeys()
	{
		return null;
	}
	
	
	private void processTags(Collection<Integer> tags, String msgType, int[] groupPath, DataDictionary dict, DataDictionary appDict, List<MessageColumnNode> output)
	{
		for (int tag : tags)
		{
			String name = buildName(tag, groupPath, appDict),
					type = appDict.getFieldType(tag).name();
			boolean mandatory = dict.isRequiredField(msgType, tag);
			
			if (!dict.isGroup(msgType, tag))  //In appDict it can be not a group, but dict shows it in context where tags came from (dict can be groupDict, see below)
			{
				output.add(new MessageColumnNode(name, mandatory, false, type, false));
				continue;
			}
			
			output.add(new MessageColumnNode(name, mandatory, true, type, false));
			
			int[] subGroupPath = buildGroupPath(groupPath, tag);
			DataDictionary groupDict = getGroupDictionary(msgType, subGroupPath, appDict);
			if (groupDict != null)
			{
				List<Integer> groupFields = Arrays.stream(groupDict.getOrderedFields())
						.boxed()
						.collect(Collectors.toList());
				processTags(groupFields, msgType, subGroupPath, groupDict, appDict, output);
			}
		}
	}
	
	
	private String buildName(String name, int tag)
	{
		return new StringBuilder().append(name).append("(").append(tag).append(")").toString();
	}
	
	private String buildName(int tag, int[] groupPath, DataDictionary dictionary)
	{
		String fieldName = buildName(dictionary.getFieldName(tag), tag);
		if (groupPath == null || groupPath.length == 0)
			return fieldName;
		
		StringBuilder sb = new StringBuilder();
		for (int g : groupPath)
			sb.append(buildName(dictionary.getFieldName(g), g)).append("/");
		return sb.append(fieldName).toString();
	}
	
	private int[] buildGroupPath(int[] groupPath, int groupTag)
	{
		if (groupPath == null)
			return new int[]{groupTag};
		
		int[] result = Arrays.copyOf(groupPath, groupPath.length+1);
		result[result.length-1] = groupTag;
		return result;
	}
	
	private DataDictionary getGroupDictionary(String msgType, int[] groupPath, DataDictionary dictionary)
	{
		GroupInfo info = getGroupInfo(msgType, groupPath, dictionary);
		if (info == null)
			return null;
		
		return info.getDataDictionary();
	}
	
	private GroupInfo getGroupInfo(String msgType, int[] groupPath, DataDictionary dictionary)
	{
		if (groupPath == null || groupPath.length == 0)
			return null;
		
		GroupInfo info = dictionary.getGroup(msgType, groupPath[0]);
		for (int i = 1; i < groupPath.length; i++)
		{
			info = info.getDataDictionary().getGroup(msgType, groupPath[i]);
			if (info == null)
				return null;
		}
		return info;
	}
}
