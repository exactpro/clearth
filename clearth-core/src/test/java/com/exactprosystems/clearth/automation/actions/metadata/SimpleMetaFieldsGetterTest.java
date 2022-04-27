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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.exactprosystems.clearth.automation.exceptions.ResultException;

public class SimpleMetaFieldsGetterTest
{
	private SimpleMetaFieldsGetter getter;
	private final String field1 = "MetaField1",
			field2 = "MetaField2",
			msgField = "MsgField";
	
	@BeforeClass
	public void init()
	{
		getter = new SimpleMetaFieldsGetter(true);
	}
	
	@Test
	public void emptyMetaAllowed()
	{
		Map<String, String> params = new HashMap<>();
		getter.getAndCheckFields(params);
	}
	
	@Test
	public void metaFields()
	{
		Map<String, String> params = new HashMap<>();
		addMetaFields(params);
		
		Set<String> fields = getter.getFields(params),
				expected = new HashSet<>(Arrays.asList(field1, field2));
		Assertions.assertThat(fields).isEqualTo(expected);
	}
	
	@Test
	public void validMeta()
	{
		Map<String, String> params = new HashMap<>();
		addMetaFields(params);
		params.put(field1, "123");
		params.put(field2, "234");
		params.put(msgField, "mmmm");
		
		getter.getAndCheckFields(params);
	}
	
	@Test(expectedExceptions = ResultException.class, expectedExceptionsMessageRegExp = ".* '"+field2+"'")
	public void absentFields()
	{
		Map<String, String> params = new HashMap<>();
		addMetaFields(params);
		params.put(field1, "123");
		params.put(msgField, "mmmm");
		
		getter.getAndCheckFields(params);
	}
	
	
	private void addMetaFields(Map<String, String> params)
	{
		params.put(SimpleMetaFieldsGetter.META_FIELDS, StringUtils.join(new String[]{field1, field2}, ';'));
	}
}
