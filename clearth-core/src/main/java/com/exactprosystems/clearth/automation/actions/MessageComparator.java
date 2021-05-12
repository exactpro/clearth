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

package com.exactprosystems.clearth.automation.actions;

import static com.exactprosystems.clearth.automation.report.results.ContainerResult.createBlockResult;
import static com.exactprosystems.clearth.automation.report.results.ContainerResult.createPlainResult;
import static com.exactprosystems.clearth.connectivity.iface.ClearThMessage.SUBMSGTYPE;
import static com.exactprosystems.clearth.connectivity.iface.ClearThMessage.SUBMSGSOURCE;
import static com.exactprosystems.clearth.connectivity.iface.ClearThMessage.MSGTYPE;
import static java.lang.String.format;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang.StringUtils.isEmpty;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.exceptions.ParametersException;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.automation.report.FailReason;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.ResultDetail;
import com.exactprosystems.clearth.automation.report.results.ContainerResult;
import com.exactprosystems.clearth.automation.report.results.DefaultResult;
import com.exactprosystems.clearth.automation.report.results.DetailedResult;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.messages.RgKeyFieldNames;
import com.exactprosystems.clearth.utils.ComparisonUtils;

public class MessageComparator<T extends ClearThMessage<T>>
{
	protected final Set<String> serviceParams;
	protected final ComparisonUtils cu;
	protected final boolean checkExtraRgs,
			saveFields,
			saveSubFields;
	protected boolean failExtraRgs;
	protected RgKeyFieldNames rgKeyFieldNames;

	protected LinkedHashMap<String, String> outputFields;
	protected LinkedHashMap<String, LinkedHashMap<String, String>> subOutputFields;


	public MessageComparator(Set<String> serviceParams, boolean checkExtraRgs, boolean saveFields, boolean saveSubFields)
	{
		this(serviceParams, checkExtraRgs, false, saveFields, saveSubFields);
	}

	public MessageComparator(Set<String> serviceParams, boolean checkExtraRgs, boolean failExtraRgs, boolean saveFields, boolean saveSubFields)
	{
		this.serviceParams = serviceParams;
		cu = ClearThCore.comparisonUtils();
		this.checkExtraRgs = checkExtraRgs;
		this.failExtraRgs = failExtraRgs;
		this.saveFields = saveFields;
		this.saveSubFields = saveSubFields;
	}
	
	
	public Result compareMessages(T expectedMessage, T actualMessage, RgKeyFieldNames rgKeyFieldNames)
	{
		this.rgKeyFieldNames = rgKeyFieldNames;
		return compareMessages(expectedMessage, actualMessage, false);
	}
	
	public LinkedHashMap<String, String> getOutputFields()
	{
		return outputFields;
	}
	
	public LinkedHashMap<String, LinkedHashMap<String, String>> getSubOutputFields()
	{
		return subOutputFields;
	}
	
	
	protected boolean isServiceParameter(String name)
	{
		return serviceParams != null && serviceParams.contains(name);
	}
	
	
	private Result compareMessages(T expectedMessage, T actualMessage, boolean isSubMessage)
	{
		try {
			if(!cu.compareValues(expectedMessage.getField(MSGTYPE), actualMessage.getField(MSGTYPE)))
				return DefaultResult.failed("Message types don't match.");
		} catch (ParametersException e) {
			throw ResultException.failed("Error while comparing message types.", e);
		}

		ContainerResult result;

		if (isSubMessage)
		{
			String subActionId = expectedMessage.getField(SUBMSGSOURCE);
			result = createBlockResult(subActionId);
			if (saveSubFields)
				saveSubOutputFields(actualMessage, subActionId);
		}
		else
		{
			result = createPlainResult("Message check result");
			if (saveFields)
				saveOutputFields(actualMessage);
		}
		
		result.addDetail(compareMainFields(expectedMessage, actualMessage, isSubMessage));
		if (expectedMessage.hasSubMessages())
			result.addDetail(compareSubMessages(expectedMessage, actualMessage));
		
		if (!result.isSuccess())
			result.setFailReason(FailReason.COMPARISON);
		return result;
	}
	
	
	protected boolean fieldsEqual(String name, T expectedMessage, T actualMessage) throws ParametersException
	{
		String expectedValue = expectedMessage.getField(name);
		if (isEmpty(expectedValue))
			return true;
		
		String actualValue = actualMessage.getField(name);;
		if (cu.isForCompareValues(expectedValue))
			return cu.compareValues(expectedValue, actualValue);
		return StringUtils.equals(expectedValue, actualValue);
	}
	
	protected boolean messagesEqual(T expectedMessage, T actualMessage, Set<String> keys)
	{
		for (String key : keys)
		{
			try
			{
				if (!fieldsEqual(key, expectedMessage, actualMessage))
					return false;
			}
			catch (ParametersException e)
			{
				throw ResultException.failed(String.format("Error while checking key field '%s': %s", key, e.getMessage()));
			}
		}
		return true;
	}
	
	protected boolean messagesEqual(T expectedMessage, T actualMessage)
	{
		for (String fieldName : expectedMessage.getFieldNames())
		{
			if (isServiceParameter(fieldName))
				continue;
			
			try
			{
				if (!fieldsEqual(fieldName, expectedMessage, actualMessage))
					return false;
			} catch (ParametersException e)
			{
				throw ResultException.failed(String.format("Error while checking values: %s", e.getMessage()));
			}
		}
		return true;
	}
	
	protected ResultDetail compareFields(String name, T expectedMessage, T actualMessage)
	{
		String expectedValue = expectedMessage.getField(name);
		if (isEmpty(expectedValue))
			return null;
		
		String actualValue = actualMessage.getField(name);
		return cu.createResultDetail(name, expectedValue, actualValue);
	}
	
	protected T findSubMessage(List<T> actualList, T expected, Set<String> keys)
	{
		Iterator<T> iterator = actualList.iterator();
		while (iterator.hasNext())
		{
			T actual = iterator.next();
			if (messagesEqual(expected, actual, keys))
			{
				iterator.remove();
				return actual;
			}
		}
		return null;
	}
	
	protected T findSubMessage(List<T> actualList, T expected)
	{
		Iterator<T> iterator = actualList.iterator();
		while (iterator.hasNext())
		{
			T actual = iterator.next();
			if (messagesEqual(expected, actual))
			{
				iterator.remove();
				return actual;
			}
		}
		return null;
	}
	
	
	private Result compareMainFields(T expectedMessage, T actualMessage, boolean isSubMessage)
	{
		DetailedResult result = new DetailedResult();
		if (isSubMessage)
		{
			String subMessageType = expectedMessage.getField(ClearThMessage.SUBMSGTYPE);
			result.addResultDetail(new ResultDetail(ClearThMessage.SUBMSGTYPE, subMessageType, subMessageType, true));
		}
		
		for (String fieldName : expectedMessage.getFieldNames())
		{
			if (isServiceParameter(fieldName))
				continue;
			
			ResultDetail rd = compareFields(fieldName, expectedMessage, actualMessage);
			if (rd != null)
				result.addResultDetail(rd);
		}
		return result;
	}
	
	private Result compareSubMessages(T expectedMessage, T actualMessage)
	{
		ContainerResult container = createBlockResult("Repeating groups");
		Set<String> subMessageTypes = expectedMessage.getSubMessageTypes();
		for (String type : subMessageTypes)
		{
			List<T> expectedList = expectedMessage.getSubMessages(type),
					actualList = actualMessage.getSubMessages(type);
			
			Set<String> rgKeys = rgKeyFieldNames != null ? rgKeyFieldNames.getRgKeyFields(type) : null;
			if (isNotEmpty(rgKeys))
				compareSubMessages(expectedList, actualList, rgKeys, container);
			else
				compareSubMessages(expectedList, actualList, container);
			
			if (checkExtraRgs && isNotEmpty(actualList))
				container.addDetail(processExtraSubMessages(actualList, type));
		}
		return container;
	}
	
	private void compareSubMessages(List<T> expectedList, List<T> actualList, ContainerResult resultContainer)
	{
		for (T expected : expectedList)
		{
			T actual = findSubMessage(actualList, expected);
			if (actual != null)
				resultContainer.addDetail(compareMessages(expected, actual, true));
			else
				resultContainer.addDetail(whenSubMessageNotFound(expected));
		}
	}
	
	private void compareSubMessages(List<T> expectedList, List<T> actualList, Set<String> rgKeys, ContainerResult resultContainer)
	{
		for (T expected : expectedList)
		{
			T actual = findSubMessage(actualList, expected, rgKeys);
			if (actual != null)
				resultContainer.addDetail(compareMessages(expected, actual, true));
			else
				resultContainer.addDetail(whenSubMessageNotFound(expected));
		}
	}
	
	protected Result whenSubMessageNotFound(T expectedMessage)
	{
		return DefaultResult.failed(format(
				"Repeating group with type '%s' from sub-action '%s' not found in received message",
				expectedMessage.getField(SUBMSGTYPE),
				expectedMessage.getField(SUBMSGSOURCE)));
	}
	
	private void addOutputField(String name, String value)
	{
		if (outputFields == null)
			outputFields = new LinkedHashMap<String, String>();
		outputFields.put(name, value);
	}
	
	private void addSubOutputFields(String subActionId, LinkedHashMap<String, String> subFields)
	{
		if (subOutputFields == null)
			subOutputFields = new LinkedHashMap<String, LinkedHashMap<String, String>>();
		subOutputFields.put(subActionId, subFields);
	}
	
	
	private Result processExtraSubMessages(List<T> subMessages, String type)
	{
		ContainerResult result = createBlockResult(format("Extra repeating groups with type '%s'", type));
		if (failExtraRgs)
			result.setSuccess(false);
		for (T sm : subMessages)
			result.addDetail(processExtraSubMessage(sm));
		return result;
	}

	private Result processExtraSubMessage(T subMessage)
	{
		DetailedResult result = new DetailedResult();
		for (String fieldName : subMessage.getFieldNames())
			result.addResultDetail(new ResultDetail(fieldName, null, subMessage.getField(fieldName), !failExtraRgs));
		return result;
	}
	
	
	protected void saveOutputFields(T message)
	{
		for (Entry<String, String> field : message.getFields().entrySet())
		{
			String value = field.getValue();
			if (StringUtils.isNotEmpty(value))
				addOutputField(field.getKey(), value);
		}
	}
	
	protected void saveSubOutputFields(T subMessage, String subActionId)
	{
		LinkedHashMap<String, String> fields = new LinkedHashMap<String, String>();
		for (Entry<String, String> field : subMessage.getFields().entrySet())
		{
			String value = field.getValue();
			if (StringUtils.isNotEmpty(value))
				fields.put(field.getKey(), value);
		}
		addSubOutputFields(subActionId, fields);
	}
}
