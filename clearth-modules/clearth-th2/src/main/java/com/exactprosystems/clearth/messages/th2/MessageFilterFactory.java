/******************************************************************************
 * Copyright 2009-2024 Exactpro Systems Limited
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

package com.exactprosystems.clearth.messages.th2;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.exactpro.th2.common.grpc.FilterOperation;
import com.exactpro.th2.common.grpc.ListValueFilter;
import com.exactpro.th2.common.grpc.MessageFilter;
import com.exactpro.th2.common.grpc.ValueFilter;
import com.exactprosystems.clearth.automation.exceptions.ParametersException;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.SimpleClearThMessage;
import com.exactprosystems.clearth.messages.RgKeyFieldNames;
import com.exactprosystems.clearth.messages.converters.ConversionException;
import com.exactprosystems.clearth.messages.converters.MessageToMap;
import com.exactprosystems.clearth.utils.ComparisonUtils;
import org.apache.commons.lang.StringUtils;

import static com.exactpro.th2.common.grpc.FilterOperation.*;
import static com.exactprosystems.clearth.utils.ComparisonUtils.*;
import static com.exactprosystems.clearth.utils.ParametersUtils.*;

public class MessageFilterFactory
{
	private static final Set<String> SERVICE_FIELDS = Set.of(ClearThMessage.MSGTYPE, ClearThMessage.SUBMSGTYPE, 
			ClearThMessage.SUBMSGSOURCE, MessageToMap.SUBMSGKIND);
	private static final Map<String, FilterOperation> FILTER_OPERATION_MAP = Map.of(
			IS_NOT_EQUAL_TEXT, NOT_EQUAL,
			IS_EMPTY, EMPTY,
			IS_NOT_EMPTY, NOT_EMPTY,
			IS_GREATER_THAN, MORE,
			IS_LESS_THAN, LESS,
			AS_NUMBER_START, EQ_DECIMAL_PRECISION,
			PATTERN_START, LIKE);
	private final MessageToMap converter;
	private final ComparisonUtils comparisonUtils;
	
	public MessageFilterFactory(ComparisonUtils comparisonUtils)
	{
		this.comparisonUtils = comparisonUtils;
		converter = new MessageToMap(SERVICE_FIELDS);
	}
	
	public MessageFilterFactory(ComparisonUtils comparisonUtils, String flatDelimiter)
	{
		this.comparisonUtils = comparisonUtils;
		converter = new MessageToMap(flatDelimiter, SERVICE_FIELDS);
	}
	
	public MessageFilter createMessageFilter(SimpleClearThMessage message, Set<String> keyFields, RgKeyFieldNames rgKeyFields) throws ConversionException
	{
		Map<String, Object> fieldsMap = converter.convert(message);
		return createMessageFilter(fieldsMap, keyFields, rgKeyFields);
	}
	
	
	protected MessageFilter createMessageFilter(Map<String, Object> fieldsMap, Set<String> thisMapKeyFields, RgKeyFieldNames allKeyFields) throws ConversionException
	{
		MessageFilter.Builder builder = MessageFilter.newBuilder();
		for (Entry<String, Object> f : fieldsMap.entrySet())
		{
			String name = f.getKey();
			ValueFilter filter = createValueFilter(name, f.getValue(), name, thisMapKeyFields, allKeyFields);
			builder = builder.putFields(name, filter);
		}
		return builder.build();
	}
	
	protected ValueFilter createValueFilter(String name, Object value, String fieldDetails, Set<String> thisMapKeyFields, RgKeyFieldNames allKeyFields) throws ConversionException
	{
		if (value instanceof String)
		{
			String s = (String) value;
			return createSimpleValueFilter(s, isKey(name, s, thisMapKeyFields));
		}
		
		if (value instanceof List)
			return createListValueFilter(name, (List<Object>) value, getRgKeyFields(name, allKeyFields), allKeyFields);
		
		if (value instanceof Map)
			return createMapValueFilter((Map<String, Object>) value, getRgKeyFields(name, allKeyFields), allKeyFields);
		
		throw unsupportedValueClass(fieldDetails, value);
	}
	
	protected ListValueFilter createListFilter(String name, List<Object> list, Set<String> thisMapKeyFields, RgKeyFieldNames allKeyFields) throws ConversionException
	{
		ListValueFilter.Builder builder = ListValueFilter.newBuilder();
		int index = 0;
		for (Object element : list)
		{
			index++;
			
			ValueFilter elementFilter = createValueFilter(name, element, name+" #"+index, thisMapKeyFields, allKeyFields);
			builder = builder.addValues(elementFilter);
		}
		return builder.build();
	}
	
	protected boolean isKey(String name, String value, Set<String> keyFields)
	{
		return keyFields != null && keyFields.contains(name);
	}
	
	protected ValueFilter.Builder initValueFilterBuilder()
	{
		return ValueFilter.newBuilder();
	}
	
	protected ValueFilter.Builder createValueFilterBuilder(String function) throws ConversionException
	{
		if (function == null)
			return initValueFilterBuilder();
		
		FilterOperation operation = getFilterOperation(function);
		if (operation == EMPTY || operation == NOT_EMPTY)
			return initValueFilterBuilder().setOperation(operation);
		
		String value = processValue(function, operation);
		return initValueFilterBuilder().setOperation(operation).setSimpleFilter(value);
	}
	
	protected String processValue(String function, FilterOperation operation) throws ConversionException
	{
		String value = comparisonUtils.prepareExpectedValue(function);
		if (value == null)
			return function;
		
		if (operation == LIKE)
			return removeQuotesAndSpaces(value);
		
		if (operation == EQ_DECIMAL_PRECISION)
		{
			String[] params = removeQuotesAndSpaces(StringUtils.split(value, ','));
			try
			{
				checkNumberOfParams(function, params, 1, 1);
				return params[0];
			}
			catch (ParametersException e)
			{
				throw new ConversionException("Comparison with accuracy is not implemented", e);
			}
		}
		return value;
	}
	
	protected FilterOperation getFilterOperation(String value)
	{
		for (Map.Entry<String, FilterOperation> entry : FILTER_OPERATION_MAP.entrySet())
		{
			if (StringUtils.startsWith(value, entry.getKey()))
				return entry.getValue();
		}
		return EQUAL;
	}
	
	protected ValueFilter createSimpleValueFilter(String value, boolean key) throws ConversionException
	{
		return createValueFilterBuilder(value).setKey(key).build();
	}
	
	protected ValueFilter createListValueFilter(String name, List<Object> list, Set<String> thisMapKeyFields, RgKeyFieldNames allKeyFields) throws ConversionException
	{
		ListValueFilter filter = createListFilter(name, list, thisMapKeyFields, allKeyFields);
		return initValueFilterBuilder()
				.setListFilter(filter)
				.build();
	}
	
	protected ValueFilter createMapValueFilter(Map<String, Object> fields, Set<String> thisMapKeyFields, RgKeyFieldNames allKeyFields) throws ConversionException
	{
		MessageFilter filter = createMessageFilter(fields, thisMapKeyFields, allKeyFields);
		return initValueFilterBuilder()
				.setMessageFilter(filter)
				.build();
	}
	
	private ConversionException unsupportedValueClass(String name, Object value)
	{
		return new ConversionException(String.format("Class of field %s (%s) is not supported",
				name, value.getClass()));
	}
	
	private Set<String> getRgKeyFields(String name, RgKeyFieldNames allKeyFields)
	{
		return allKeyFields != null ? allKeyFields.getRgKeyFields(name) : null;
	}
}