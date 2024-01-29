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

package com.exactprosystems.clearth.automation.actions.th2;

import java.util.Set;

import com.exactpro.th2.common.grpc.EventID;
import com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.EventId;
import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.actions.MessageAction;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.data.DataHandlersFactory;
import com.exactprosystems.clearth.data.HandledTestExecutionId;
import com.exactprosystems.clearth.data.th2.Th2DataHandlersFactory;
import com.exactprosystems.clearth.data.th2.events.EventUtils;
import com.exactprosystems.clearth.data.th2.events.Th2EventId;
import com.exactprosystems.clearth.messages.converters.MessageToMap;
import com.exactprosystems.clearth.utils.SettingsException;

public class Th2ActionUtils
{
	public static final String PARAM_SESSION_ALIAS = "SessionAlias",
			PARAM_SESSION_GROUP = "SessionGroup",
			PARAM_BOOK = "Book",
			PARAM_KEY_FIELDS = "KeyFields",
			PARAM_RG_KEYFIELDS = "RGKeyFields",
			PARAM_FLAT_DELIMITER = MessageToMap.FLAT_DELIMITER,
			PARAM_DIRECTION = "Direction";
	
	public static final Set<String> SENDING_SERVICE_PARAMS = Set.of(PARAM_SESSION_ALIAS, PARAM_SESSION_GROUP, PARAM_BOOK,
			PARAM_KEY_FIELDS, PARAM_RG_KEYFIELDS,
			PARAM_FLAT_DELIMITER, PARAM_DIRECTION,
			ClearThMessage.MSGTYPE, ClearThMessage.SUBMSGTYPE, ClearThMessage.SUBMSGSOURCE,
			MessageAction.REPEATINGGROUPS, MessageAction.META_FIELDS);
	
	public static Th2DataHandlersFactory getDataHandlersFactory() throws SettingsException
	{
		DataHandlersFactory handlersFactory = ClearThCore.getInstance().getDataHandlersFactory();
		if (!(handlersFactory instanceof Th2DataHandlersFactory))
			throw new SettingsException("dataHandlersFactory is not "+Th2DataHandlersFactory.class.getCanonicalName()+". Check clearth.cfg");
		return (Th2DataHandlersFactory)handlersFactory;
	}
	
	public static Th2DataHandlersFactory getDataHandlersFactoryOrResultException()
	{
		try
		{
			return Th2ActionUtils.getDataHandlersFactory();
		}
		catch (Exception e)
		{
			throw ResultException.failed("Could not get dataHandlersFactory", e);
		}
	}
	
	public static EventID getGrpcEventId(Action action)
	{
		HandledTestExecutionId id = action.getTestExecutionId();
		if (id == null)
			throw ResultException.failed("Action has no handled test execution ID");
		if (!(id instanceof Th2EventId))
			throw ResultException.failed("Action's handled test execution ID must be of class "+Th2EventId.class.getCanonicalName()+", but it is "+id.getClass().getCanonicalName());
		
		return ((Th2EventId)id).getId();
	}
	
	public static EventId getEventId(Action action)
	{
		EventID eventId = getGrpcEventId(action);
		return EventId.builder()
				.setBook(eventId.getBookName())
				.setId(eventId.getId())
				.setScope(eventId.getScope())
				.setTimestamp(EventUtils.getTimestamp(eventId.getStartTimestamp()))
				.build();
	}
}
