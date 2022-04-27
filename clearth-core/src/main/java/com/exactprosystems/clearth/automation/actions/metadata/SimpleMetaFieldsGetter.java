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

package com.exactprosystems.clearth.automation.actions.metadata;

import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;

import com.exactprosystems.clearth.automation.actions.MessageAction;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.utils.inputparams.InputParamsHandler;

public class SimpleMetaFieldsGetter implements MetaFieldsGetter
{
	public static final String META_FIELDS = MessageAction.META_FIELDS;
	
	private final boolean metaMandatory;
	
	public SimpleMetaFieldsGetter(boolean metaMandatory)
	{
		this.metaMandatory = metaMandatory;
	}
	
	
	@Override
	public Set<String> getFields(Map<String, String> inputParams)
	{
		return new InputParamsHandler(inputParams).getSet(META_FIELDS);
	}
	
	@Override
	public void checkFields(Set<String> metaFields, Map<String, String> inputParams) throws ResultException
	{
		if (!metaMandatory || CollectionUtils.isEmpty(metaFields))
			return;
		
		InputParamsHandler handler = new InputParamsHandler(inputParams);
		try
		{
			for (String f : metaFields)
				handler.getRequiredString(f);
		}
		finally
		{
			handler.check();
		}
	}
	
	
	public Set<String> getAndCheckFields(Map<String, String> inputParams) throws ResultException
	{
		Set<String> result = getFields(inputParams);
		checkFields(result, inputParams);
		return result;
	}
}