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

package com.exactprosystems.clearth.connectivity.xml;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.exactprosystems.clearth.connectivity.Dictionary;
import com.exactprosystems.clearth.connectivity.iface.MessageValidatorCondition;
import com.exactprosystems.clearth.utils.DictionaryLoadException;
import org.apache.commons.lang.StringUtils;

import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.collections4.MapUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

public class XmlDictionary extends Dictionary<XmlMessageDesc, XmlDictionaryDesc>
{
	@Deprecated
	public XmlDictionary(String fileName) throws DictionaryLoadException
	{
		this(fileName, null);
	}
	
	@Deprecated
	public XmlDictionary(Reader reader) throws DictionaryLoadException
	{
		this(reader, null);
	}
	
	public XmlDictionary(String fileName, Map<String, String> parameters) throws DictionaryLoadException
	{
		super(fileName, parameters);
	}
	
	public XmlDictionary(Reader reader, Map<String, String> parameters) throws DictionaryLoadException
	{
		super(reader, parameters);
	}
	
	@Override
	protected Class[] getClassesToBeBound()
	{
		return new Class[] { XmlDictionaryDesc.class };
	}

	@Override
	protected List<XmlMessageDesc> getMessageDescs(XmlDictionaryDesc dictionaryDesc)
	{
		return dictionaryDesc.getMessageDesc();
	}

	@Override
	protected void solveReferencesToCommonFields(XmlDictionaryDesc dictionary) throws DictionaryLoadException
	{
		if (isEmpty(dictionary.getMessageDesc()))
			return;

		Map<String, XmlFieldDesc> commonFields = prepareCommonFields(dictionary);
		Map<String, XmlCommonFieldsGroupDesc> commonFieldsGroups = prepareCommonFieldsGroups(dictionary);
		if (isEmpty(commonFields) && isEmpty(commonFieldsGroups))
			return;
		
		for (XmlMessageDesc md : dictionary.getMessageDesc())
		{
			solveReferences(dictionary, md, md.getFieldDesc(), commonFields, commonFieldsGroups);
		}		
	}
	
	protected void solveReferences(XmlDictionaryDesc dictionary, 
	                               XmlMessageDesc md,
	                               List<XmlFieldDesc> fields,
	                               Map<String, XmlFieldDesc> commonFields,
	                               Map<String, XmlCommonFieldsGroupDesc> commonFieldsGroups) throws DictionaryLoadException
	{
		if (isEmpty(fields))
			return;

		for (int i = 0; i < fields.size(); )
		{
			XmlFieldDesc fieldToSolve = fields.get(i);
			String reference = fieldToSolve.getReference();
			int increment = 1;
			
			if (isNotBlank(reference))
			{
				if (commonFields.containsKey(reference))
				{
					fields.set(i, getSolvedCommonField(fieldToSolve, dictionary, md, commonFields, commonFieldsGroups));
				}
				else if (commonFieldsGroups.containsKey(reference))
				{
					List<XmlFieldDesc> toReplace = getSolvedCommonFields(dictionary, md, reference, commonFields, commonFieldsGroups);
					fields.remove(i);
					for (int j = 0; j < toReplace.size(); j++)
					{
						fields.add(i + j, toReplace.get(j));
					}
					increment = toReplace.size();
				}
				else 
					throw new DictionaryLoadException(prepareFieldNotFoundError(md, fieldToSolve));
			}
			else
				solveReferences(dictionary, md, fieldToSolve.getFieldDesc(), commonFields, commonFieldsGroups);
			
			i += increment;
		}
	}

	protected XmlFieldDesc getSolvedCommonField(XmlFieldDesc sourceField,
	                                            XmlDictionaryDesc dictionary,
	                                            XmlMessageDesc messageDesc,
	                                            Map<String, XmlFieldDesc> commonFields,
	                                            Map<String, XmlCommonFieldsGroupDesc> commonFieldsGroups) throws DictionaryLoadException
	{
		XmlFieldDesc commonField = commonFields.get(sourceField.getReference());
		solveReferences(dictionary, messageDesc, commonField.getFieldDesc(), commonFields, commonFieldsGroups);
		return mergeFields(sourceField, commonField);		
	}
	
	
	protected List<XmlFieldDesc> getSolvedCommonFields(XmlDictionaryDesc dictionary,
	                                                   XmlMessageDesc messageDesc,
	                                                   String reference,
		                                               Map<String, XmlFieldDesc> allCommonFields,
		                                               Map<String, XmlCommonFieldsGroupDesc> commonFieldsGroups) throws DictionaryLoadException
	{
		List<XmlFieldDesc> commonFields = commonFieldsGroups.get(reference).getFieldDescs();
		solveReferences(dictionary, messageDesc, commonFields, allCommonFields, commonFieldsGroups);
		return commonFields;
	}
	
	protected String prepareFieldNotFoundError(XmlMessageDesc messageDesc, XmlFieldDesc fieldDesc)
	{
		return format("Common field (commonFieldDesc) or common fields group (commonFieldsGroupDesc) " +
				"with name = '%s' referenced from field '%s' of message '%s' isn't defined in the dictionary.",
				fieldDesc.getReference(),
				(fieldDesc.getName() != null) ? fieldDesc.getName() : fieldDesc.getSource(),
				messageDesc.getType());
	}
	
	
	
	protected Map<String, XmlFieldDesc> prepareCommonFields(XmlDictionaryDesc dictionaryDesc)
	{
		List<XmlFieldDesc> commonFields = dictionaryDesc.getCommonFieldDescs();
		if (isEmpty(commonFields))
			return emptyMap();
		else 
		{
			Map<String, XmlFieldDesc> prepared = new HashMap<String, XmlFieldDesc>(commonFields.size());
			for (XmlFieldDesc fd : commonFields)
			{
				prepared.put(fd.getName(), fd);
			}
			commonFields.clear();
			return prepared;
		}
	}
	
	protected Map<String, XmlCommonFieldsGroupDesc> prepareCommonFieldsGroups(XmlDictionaryDesc dictionaryDesc)
	{
		List<XmlCommonFieldsGroupDesc> groups = dictionaryDesc.getCommonFieldsGroupDescs();
		if (isEmpty(groups))
			return emptyMap();
		else 
		{
			Map<String, XmlCommonFieldsGroupDesc> prepared = new HashMap<String, XmlCommonFieldsGroupDesc>(groups.size());
			for (XmlCommonFieldsGroupDesc group : groups)
			{
				prepared.put(group.getName(), group);
			}
			groups.clear();
			return prepared;
		}
	}



	/*
	   To avoid NPE while unboxing we have to specify default values in xsd for primitive types.
	   However we need to distinguish explicitly specified values from default values for correct merge.
	   So here we rely on generated by JAXB code.
	 */
	protected XmlFieldDesc mergeFields(XmlFieldDesc source, XmlFieldDesc common)
	{
		source.setReference(null);
		
		if (isNotEmpty(common.getFieldDesc()))
			source.getFieldDesc().addAll(common.getFieldDesc());
		if (isNotEmpty(common.getDefaultAttrDesc()))
			source.getDefaultAttrDesc().addAll(common.getDefaultAttrDesc());
		if (isNotEmpty(common.getAttrDesc()))
			source.getAttrDesc().addAll(common.getAttrDesc());

		if (isBlank(source.getName()) && isNotBlank(common.getName()))
			source.setName(common.getName());
		if (isBlank(source.getSource()) && isNotBlank(common.getSource()))
			source.setSource(common.getSource());

		if ((source.mandatory == null) && (common.mandatory != null))
			source.mandatory = common.mandatory;
		if ((source.repeat == null) && (common.repeat != null))
			source.repeat = common.repeat;

		if (isBlank(source.getAlways()) && isNotBlank(common.getAlways()))
			source.setAlways(common.getAlways());

		if ((source.numeric == null) && (common.numeric != null))
			source.numeric = common.numeric;

		if (isBlank(source.getDefault()) && isNotBlank(common.getDefault()))
			source.setDefault(common.getDefault());

		if ((source.useSelfClosingTagForEmpty == null) && (common.useSelfClosingTagForEmpty != null))
			source.useSelfClosingTagForEmpty = common.useSelfClosingTagForEmpty;

		return source;
	}


	@Override
	protected void buildExtendedConditions(XmlMessageDesc messageDesc)
	{
		String rootTag = messageDesc.getRootTag();
		if (StringUtils.isEmpty(rootTag))
			return;
		
		RootTagCondition condition = new RootTagCondition(rootTag);

		String type = messageDesc.getType();
		List<MessageValidatorCondition> tcList = typeConditionsMap.computeIfAbsent(type, t -> new ArrayList<>());
		tcList.add(condition);

		List<MessageValidatorCondition> cList = conditionsMap.computeIfAbsent(type, t -> new ArrayList<>());
		cList.add(condition);
	}
}
