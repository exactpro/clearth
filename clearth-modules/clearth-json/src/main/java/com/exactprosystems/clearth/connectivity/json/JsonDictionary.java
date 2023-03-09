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

package com.exactprosystems.clearth.connectivity.json;

import com.exactprosystems.clearth.connectivity.Dictionary;
import com.exactprosystems.clearth.utils.DictionaryLoadException;
import com.fasterxml.jackson.databind.JsonNode;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.io.Reader;
import java.util.List;
import java.util.Map;

public class JsonDictionary extends Dictionary<JsonMessageDesc, JsonDictionaryDesc>
{
	@Deprecated
	public JsonDictionary(String fileName) throws DictionaryLoadException
	{
		this(fileName, null);
	}
	
	@Deprecated
	public JsonDictionary(Reader reader) throws DictionaryLoadException
	{
		this(reader, null);
	}
	
	public JsonDictionary(String fileName, Map<String, String> parameters) throws DictionaryLoadException
	{
		super(fileName, parameters);
	}
	
	public JsonDictionary(Reader reader, Map<String, String> parameters) throws DictionaryLoadException
	{
		super(reader, parameters);
	}
	
	@Override
	protected Class[] getClassesToBeBound()
	{
		return new Class[] { JsonDictionaryDesc.class };
	}

	@Override
	protected List<JsonMessageDesc> getMessageDescs(JsonDictionaryDesc dictionaryDesc)
	{
		return dictionaryDesc.getMessageDesc();
	}

	protected JsonNode getNode(String[] path, int pathIndex, JsonNode parent)
	{
		JsonNode node = parent.get(path[pathIndex]);
		if (node == null)
			return null;
		
		if (pathIndex == path.length - 1)
			return node;
		
		return getNode(path, ++pathIndex, node.isArray() ? node.get(0) : node);
	}
	
	@Override
	protected void solveReferencesToCommonFields(JsonDictionaryDesc dictionary) throws DictionaryLoadException
	{
		if (CollectionUtils.isEmpty(dictionary.getMessageDesc()))
			return;

		for (JsonMessageDesc md : dictionary.getMessageDesc())
			solveReferences(dictionary, md.getFieldDesc());
	}

	protected void solveReferences(JsonDictionaryDesc dictionary, List<JsonFieldDesc> fields) throws DictionaryLoadException
	{
		if (CollectionUtils.isEmpty(fields))
			return;

		int i = 0;
		while (i < fields.size())
		{
			JsonFieldDesc field = fields.get(i);

			if (StringUtils.isNotEmpty(field.getReference()))
				field = findCommonField(dictionary, field.getReference());

			solveReferences(dictionary, field.getFieldDesc());

			// commonFieldDesc may skip "source" attribute to define list of common fields, 
			// but array fields with no source should use empty value of "source" attribute
			if (CollectionUtils.isEmpty(field.getFieldDesc()) || field.getSource() != null)
			{
				fields.set(i, field);
				i++;
			}
			else
			{
				fields.remove(i);
				for (int j = 0; j < field.getFieldDesc().size(); j++)
					fields.add(i+j, field.getFieldDesc().get(j));
				i += field.getFieldDesc().size();
			}
		}
	}
	
	protected JsonFieldDesc findCommonField(JsonDictionaryDesc dictionary, String reference) throws DictionaryLoadException
	{
		for (JsonFieldDesc commonField : dictionary.getCommonFieldDesc())
		{
			if (reference.equals(commonField.getName()))
				return commonField;
		}
		throw new DictionaryLoadException(String.format("Common field with name='%s' not found in dictionary", reference));
	}
}
