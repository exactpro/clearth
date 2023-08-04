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

package com.exactprosystems.clearth.automation.actions.th2;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.exactpro.th2.common.grpc.EventID;
import com.exactpro.th2.common.schema.message.MessageRouter;
import com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.EventId;
import com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.GroupBatch;
import com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.MessageGroup;
import com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.ParsedMessage;
import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.GlobalContext;
import com.exactprosystems.clearth.automation.MatrixContext;
import com.exactprosystems.clearth.automation.Preparable;
import com.exactprosystems.clearth.automation.SchedulerStatus;
import com.exactprosystems.clearth.automation.StepContext;
import com.exactprosystems.clearth.automation.actions.MessageAction;
import com.exactprosystems.clearth.automation.actions.metadata.MetaFieldsGetter;
import com.exactprosystems.clearth.automation.actions.metadata.SimpleMetaFieldsGetter;
import com.exactprosystems.clearth.automation.exceptions.FailoverException;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.results.DefaultResult;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.SimpleClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.SimpleClearThMessageBuilder;
import com.exactprosystems.clearth.data.DataHandlersFactory;
import com.exactprosystems.clearth.data.HandledTestExecutionId;
import com.exactprosystems.clearth.data.th2.Th2DataHandlersFactory;
import com.exactprosystems.clearth.data.th2.events.EventUtils;
import com.exactprosystems.clearth.data.th2.events.Th2EventId;
import com.exactprosystems.clearth.messages.converters.ConversionException;
import com.exactprosystems.clearth.messages.th2.SendingProperties;
import com.exactprosystems.clearth.messages.th2.Th2MessageFactory;
import com.exactprosystems.clearth.utils.SettingsException;
import com.exactprosystems.clearth.utils.inputparams.InputParamsHandler;

public class SendTh2Message extends Action implements Preparable
{
	public static final String PARAM_SESSION_ALIAS = "SessionAlias",
			PARAM_SESSION_GROUP = "SessionGroup",
			PARAM_BOOK = "Book",
			PARAM_ROUTER_ATTRIBUTES = "RouterAttributes",
			PARAM_FLAT_DELIMITER = "FlatDelimiter",
			CONTEXT_ROUTER = "Th2MessageRouter";
	
	private static final Set<String> SERVICE_PARAMS = Set.of(PARAM_SESSION_ALIAS,
		PARAM_SESSION_GROUP, PARAM_BOOK, PARAM_ROUTER_ATTRIBUTES, PARAM_FLAT_DELIMITER,
		ClearThMessage.MSGTYPE, ClearThMessage.SUBMSGTYPE, ClearThMessage.SUBMSGSOURCE, 
		MessageAction.REPEATINGGROUPS, MessageAction.META_FIELDS);
	
	@Override
	public void prepare(GlobalContext globalContext, SchedulerStatus schedulerStatus) throws Exception
	{
		DataHandlersFactory handlersFactory = ClearThCore.getInstance().getDataHandlersFactory();
		if (!(handlersFactory instanceof Th2DataHandlersFactory))
			throw new SettingsException("dataHandlersFactory is not "+Th2DataHandlersFactory.class.getCanonicalName()+". Check clearth.cfg");
	}
	
	@Override
	protected Result run(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext)
			throws ResultException, FailoverException
	{
		//Class of DataHandlersFactory was verified in prepare(), cast is safe
		Th2DataHandlersFactory th2Factory = (Th2DataHandlersFactory)ClearThCore.getInstance().getDataHandlersFactory();
		
		InputParamsHandler handler = new InputParamsHandler(inputParams);
		String sessionAlias = handler.getRequiredString(PARAM_SESSION_ALIAS),
				sessionGroup = handler.getString(PARAM_SESSION_GROUP, sessionAlias),
				book = handler.getString(PARAM_BOOK, th2Factory.getBook()),
				flatDelimiter = handler.getString(PARAM_FLAT_DELIMITER);
		Set<String> routerAttrs = handler.getSet(PARAM_ROUTER_ATTRIBUTES, ",");
		handler.check();
		
		SimpleClearThMessage message = createClearThMessage(matrixContext);
		ParsedMessage th2Message = createTh2Message(message, sessionAlias, flatDelimiter);
		SendingProperties props = createSendingProperties(book, sessionGroup, routerAttrs);
		
		logger.trace("Sending message to {}: {}", props, th2Message);
		
		//Router is not closed here. It is stored in GlobalContext for re-use by other actions and will be closed when Scheduler finishes execution
		MessageRouter<GroupBatch> router = getRouter(globalContext, th2Factory);
		try
		{
			return sendMessage(th2Message, router, props);
		}
		catch (Exception e)
		{
			return DefaultResult.failed("Could not send message", e);
		}
	}
	
	
	protected SimpleClearThMessage createClearThMessage(MatrixContext matrixContext)
	{
		Map<String, String> ip = getInputParams();
		return getMessageBuilder(getServiceParameters(), getMetaFields(ip))
				.fields(ip)
				.metaFields(ip)
				.rgs(matrixContext, this)
				.type(ip.get(ClearThMessage.MSGTYPE))
				.build();
	}
	
	protected ParsedMessage createTh2Message(SimpleClearThMessage message, String sessionAlias, String flatDelimiter)
	{
		EventId eventId = getActionEventId();
		try
		{
			return getTh2MessageFactory(flatDelimiter).createParsedMessage(message, sessionAlias, eventId);
		}
		catch (ConversionException e)
		{
			throw ResultException.failed("Could not create th2 message", e);
		}
	}
	
	protected SendingProperties createSendingProperties(String book, String sessionGroup, Collection<String> routerAttrs)
	{
		SendingProperties result = new SendingProperties();
		result.setBook(book);
		result.setSessionGroup(sessionGroup);
		result.setRouterAttrs(routerAttrs);
		return result;
	}
	
	protected MessageRouter<GroupBatch> getRouter(GlobalContext globalContext, Th2DataHandlersFactory th2Factory)
	{
		MessageRouter<GroupBatch> router = globalContext.getCloseableContext(CONTEXT_ROUTER);
		if (router == null)
		{
			router = th2Factory.createGroupBatchRouter();
			globalContext.setCloseableContext(CONTEXT_ROUTER, router);
		}
		return router;
	}
	
	
	protected Result sendMessage(ParsedMessage message, MessageRouter<GroupBatch> router, SendingProperties props) throws IOException
	{
		GroupBatch batch = GroupBatch.builder()
				.setBook(props.getBook())
				.addGroup(MessageGroup.builder()
						.addMessage(message)
						.build())
				.setSessionGroup(props.getSessionGroup())
				.build();
		
		Collection<String> attrs = props.getRouterAttrs();
		if (!attrs.isEmpty())
		{
			String[] attrsArray = attrs.toArray(new String[0]);
			router.send(batch, attrsArray);
		}
		else
			router.send(batch);
		
		return null;
	}
	
	
	protected Set<String> getServiceParameters()
	{
		return SERVICE_PARAMS;
	}
	
	protected Set<String> getMetaFields(Map<String, String> params)
	{
		MetaFieldsGetter metaGetter = getMetaFieldsGetter();
		Set<String> result = metaGetter.getFields(params);
		metaGetter.checkFields(result, params);
		return result;
	}
	
	
	protected SimpleClearThMessageBuilder getMessageBuilder(Set<String> serviceParameters, Set<String> metaFields)
	{
		return new SimpleClearThMessageBuilder(serviceParameters, metaFields);
	}
	
	protected MetaFieldsGetter getMetaFieldsGetter()
	{
		return new SimpleMetaFieldsGetter(false);
	}
	
	protected Th2MessageFactory getTh2MessageFactory(String flatDelimiter)
	{
		//Checking for null, not with StringUtils.isEmpty(), to allow switching flat mode off by specifying #FlatDelimiter=""
		return flatDelimiter == null ? new Th2MessageFactory() : new Th2MessageFactory(flatDelimiter);
	}
	
	protected EventId getActionEventId()
	{
		HandledTestExecutionId id = getTestExecutionId();
		if (id == null)
			throw ResultException.failed("Action has no handled test execution ID");
		if (!(id instanceof Th2EventId))
			throw ResultException.failed("Action's handled test execution ID must be of class "+Th2EventId.class.getCanonicalName()+", but it is "+id.getClass().getCanonicalName());
		
		EventID eventId = ((Th2EventId)id).getId();
		return EventId.builder()
				.setBook(eventId.getBookName())
				.setId(eventId.getId())
				.setScope(eventId.getScope())
				.setTimestamp(EventUtils.getTimestamp(eventId.getStartTimestamp()))
				.build();
	}
}
