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

package com.exactprosystems.clearth.automation.actions.json;

import com.exactprosystems.clearth.automation.actions.MessageComparator;
import com.exactprosystems.clearth.automation.exceptions.ParametersException;
import com.exactprosystems.clearth.automation.report.ResultDetail;
import com.exactprosystems.clearth.connectivity.json.ClearThJsonMessage;
import com.exactprosystems.clearth.connectivity.json.JsonField;
import com.exactprosystems.clearth.connectivity.json.JsonNumericField;
import org.apache.commons.lang.StringUtils;

import java.math.BigDecimal;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang.StringUtils.isEmpty;

public class JsonMessageComparator extends MessageComparator<ClearThJsonMessage>
{
	private static final Logger logger = LoggerFactory.getLogger(JsonMessageComparator.class);
	
	public JsonMessageComparator(Set<String> serviceParams, boolean checkExtraRgs, boolean saveFields, boolean saveSubFields)
	{
		super(serviceParams, checkExtraRgs, saveFields, saveSubFields);
	}
	
	
	@Override
	protected boolean fieldsEqual(String name, ClearThJsonMessage expectedMessage, ClearThJsonMessage actualMessage) throws ParametersException
	{
		String expectedValue = expectedMessage.getField(name);
		if (isEmpty(expectedValue))
			return true;
		
		return compareFieldValues(name, expectedMessage, actualMessage);
	}
	
	@Override
	protected ResultDetail compareFields(String name, ClearThJsonMessage expectedMessage, ClearThJsonMessage actualMessage)
	{
		String expectedValue = expectedMessage.getField(name);
		if (isEmpty(expectedValue))
			return null;
		
		String actualValue = actualMessage.getField(name);
		ResultDetail rd = new ResultDetail(name, expectedValue, actualValue, true);
		try
		{
			rd.setIdentical(compareFieldValues(name, expectedMessage, actualMessage));
		}
		catch (ParametersException e)
		{
			rd.setIdentical(false);
			rd.setErrorMessage(e.getMessage());
		}
		return rd;
	}
	
	
	private boolean compareFieldValues(String name, ClearThJsonMessage expectedMessage, ClearThJsonMessage actualMessage) throws ParametersException
	{
		String expectedValue = expectedMessage.getField(name),
				actualValue = actualMessage.getField(name);
		if (cu.isForCompareValues(expectedValue))
			return cu.compareValues(expectedValue, actualValue);
		
		JsonField actualField = actualMessage.getJsonField(name);
		if (!(actualField instanceof JsonNumericField))
			return StringUtils.equals(expectedValue, actualValue);
		
		try
		{
			BigDecimal expectedNumber = new BigDecimal(expectedValue);
			BigDecimal actualNumber = ((JsonNumericField) actualField).getValue();
			return expectedNumber.compareTo(actualNumber) == 0;
		}
		catch (NumberFormatException e)
		{
			logger.warn("Error while reading value of field '{}' as number", name, e);
			return StringUtils.equals(expectedValue, actualValue);
		}
	}
}
