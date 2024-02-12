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

package com.exactprosystems.clearth.messages;

import com.exactprosystems.clearth.automation.*;
import com.exactprosystems.clearth.automation.actions.MessageAction;
import com.exactprosystems.clearth.automation.actions.TestAction;
import com.exactprosystems.clearth.automation.actions.metadata.SimpleMetaFieldsGetter;
import com.exactprosystems.clearth.connectivity.iface.SimpleClearThMessage;
import com.exactprosystems.clearth.utils.CollectionUtils;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static com.exactprosystems.clearth.connectivity.iface.ClearThMessage.*;

import static org.testng.Assert.*;

public class SimpleClearThMessageFactoryTest
{
	private static final Set<String> SERVICE_PARAMS = Set.of(MSGTYPE, SUBMSGTYPE, SUBMSGSOURCE, MSGCOUNT, MessageAction.REPEATINGGROUPS);
	private static final String MSG_TYPE_VALUE = "MsgType1",
			RG_NAME = "rg1";
	
	private SimpleClearThMessageFactory factory;
	private Map<String, String> messageFields,
			inputParams,
			subInputParams,
			subMessageFields;
	private TestAction action,
			subAction;
	
	@BeforeClass
	public void init()
	{
		factory = new SimpleClearThMessageFactory(SERVICE_PARAMS, new SimpleMetaFieldsGetter(false));
		
		messageFields = Map.of("Field1", "111", "Field2", "222", "Field3", "333");
		
		inputParams = new LinkedHashMap<>(messageFields);
		inputParams.put(MSGTYPE, MSG_TYPE_VALUE);
		inputParams.put(MessageAction.REPEATINGGROUPS, RG_NAME);
		
		subInputParams = CollectionUtils.map(SUBMSGTYPE, "SubType1", "SubField1", "123", "SubField2", "234");
		
		subMessageFields = new HashMap<>(subInputParams);
		subMessageFields.put(SUBMSGSOURCE, RG_NAME);
		
		Matrix matrix = new Matrix(new MvelVariablesFactory(null, null));
		action = createAction(inputParams, matrix);
		subAction = createAction(subInputParams, matrix);
	}
	
	@Test
	public void testCreateMessage()
	{
		SimpleClearThMessage msg = factory.createMessage(inputParams, createMatrixContext(subAction, RG_NAME), action);
		
		Map<String, String> expectedFields = new LinkedHashMap<>(messageFields);
		expectedFields.put(MSGTYPE, MSG_TYPE_VALUE);
		
		assertEquals(msg, createMessage(expectedFields, subMessageFields));
	}
	
	@Test
	public void testCreateMessageWithoutType()
	{
		SimpleClearThMessage msg = factory.createMessageWithoutType(inputParams, createMatrixContext(subAction, RG_NAME), action);
		assertEquals(msg, createMessage(messageFields, subMessageFields));
	}
	
	
	private MatrixContext createMatrixContext(Action subAction, String subActionId)
	{
		MatrixContext mc = new MatrixContext();
		mc.setSubActionData(subActionId, new SubActionData(subAction));
		return mc;
	}
	
	private SimpleClearThMessage createMessage(Map<String, String> fields, Map<String, String> subMsgFields)
	{
		SimpleClearThMessage msg = new SimpleClearThMessage(fields);
		msg.addSubMessage(new SimpleClearThMessage(subMsgFields));
		
		return msg;
	}
	
	private TestAction createAction(Map<String, String> inputParams, Matrix matrix)
	{
		ActionSettings settings = new ActionSettings();
		settings.setParams(inputParams);
		settings.setMatrix(matrix);
		
		TestAction action = new TestAction();
		action.init(settings);
		return action;
	}
}