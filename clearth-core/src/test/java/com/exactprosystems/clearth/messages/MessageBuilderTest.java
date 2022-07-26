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

package com.exactprosystems.clearth.messages;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import com.exactprosystems.clearth.automation.actions.MessageAction;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessageDirection;
import com.exactprosystems.clearth.connectivity.iface.SimpleClearThMessageBuilder;

public class MessageBuilderTest
{
	private MessageBuilder<?> builder;
	private Map<String, String> inputParams;
	private final String META_FIELD = "DummyMetaField",
			META_FIELD_VALUE = "MetaValue",
			FIELD = "Field",
			FIELD_VALUE = "Value123",
			CON1 = "Con1";

	@BeforeClass
	public void init()
	{
		Set<String> serviceParams = new HashSet<>(Arrays.asList(MessageAction.CONNECTIONNAME, MessageAction.META_FIELDS)),
				metaFields = new HashSet<>(Arrays.asList(META_FIELD));
		builder = new SimpleClearThMessageBuilder(serviceParams, metaFields);

		inputParams = new HashMap<>();
		inputParams.put(MessageAction.CONNECTIONNAME, CON1);
		inputParams.put(MessageAction.META_FIELDS, META_FIELD);
		inputParams.put(FIELD, FIELD_VALUE);
		inputParams.put(META_FIELD, META_FIELD_VALUE);
	}

	@Test(description = "Only non-service and non-meta fields are added to message")
	public void fieldsAdded()
	{
		ClearThMessage<?> message = builder.fields(inputParams).build();
		
		SoftAssert soft = new SoftAssert();
		soft.assertEquals(message.getField(FIELD), FIELD_VALUE, "message field");
		soft.assertNull(message.getField(MessageAction.CONNECTIONNAME), "service parameter");
		soft.assertNull(message.getField(MessageAction.META_FIELDS), "service parameter");
		soft.assertNull(message.getField(META_FIELD), "meta field");
		soft.assertAll();
	}
	
	@Test(description = "Service and meta fields are skipped when trying to add them to message")
	public void skippedFields()
	{
		ClearThMessage<?> message = builder
				.field(MessageAction.CONNECTIONNAME, CON1)
				.field(META_FIELD, META_FIELD_VALUE)
				.build();
		
		SoftAssert soft = new SoftAssert();
		soft.assertNull(message.getField(MessageAction.CONNECTIONNAME));
		soft.assertNull(message.getField(META_FIELD));
		soft.assertAll();
	}
	
	@Test(description = "Only meta fields are added to message metadata")
	public void metaAdded()
	{
		ClearThMessage<?> message = builder.metaFields(inputParams).build();

		SoftAssert soft = new SoftAssert();
		soft.assertEquals(message.getMetaField(META_FIELD), META_FIELD_VALUE, "meta field");
		soft.assertNull(message.getMetaField(MessageAction.CONNECTIONNAME), "service paramter");
		soft.assertNull(message.getMetaField(MessageAction.META_FIELDS), "service paramter");
		soft.assertNull(message.getField(FIELD), "message field");
		soft.assertAll();
	}
	
	@Test(description = "Non-meta fields are skipped when trying to add them to message metadata")
	public void skippedMetaFields()
	{
		ClearThMessage<?> message = builder
				.metaField(MessageAction.CONNECTIONNAME, CON1)
				.metaField(FIELD, FIELD_VALUE)
				.build();
		
		SoftAssert soft = new SoftAssert();
		soft.assertNull(message.getMetaField(MessageAction.CONNECTIONNAME));
		soft.assertNull(message.getMetaField(FIELD));
		soft.assertAll();
	}
	
	@Test(description = "Direction and timestamp are added to message metadata")
	public void directionAndTimestamp()
	{
		Instant pastTime = Instant.now().minusSeconds(3000);
		ClearThMessage<?> message = builder
				.direction(ClearThMessageDirection.SENT)
				.timestamp(pastTime)
				.build();
		
		SoftAssert soft = new SoftAssert();
		soft.assertEquals(message.getDirection(), ClearThMessageDirection.SENT);
		soft.assertEquals(message.getTimestamp(), pastTime);
		soft.assertAll();
	}

	@Test
	public void testIfBuilderAffectMessage()
	{
		ClearThMessage<?> message = builder
				.field("field1", "value1")
				.field("field2", "value2")
				.field("field3", "value3")
				.build();

		builder.field("field100", "value100");

		Assert.assertEquals(message.getField("field100"), null);
	}
}
