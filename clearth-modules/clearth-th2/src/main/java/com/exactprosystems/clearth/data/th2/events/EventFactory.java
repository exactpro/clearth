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

package com.exactprosystems.clearth.data.th2.events;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringJoiner;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.th2.common.event.Event.Status;
import com.exactpro.th2.common.event.bean.Table;
import com.exactpro.th2.common.event.bean.builder.TableBuilder;
import com.exactpro.th2.common.grpc.ConnectionID;
import com.exactpro.th2.common.grpc.Event;
import com.exactpro.th2.common.grpc.EventBatch;
import com.exactpro.th2.common.grpc.EventID;
import com.exactpro.th2.common.grpc.MessageID;
import com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.Direction;
import com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.MessageId;
import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.StepMetadata;
import com.exactprosystems.clearth.automation.SubActionData;
import com.exactprosystems.clearth.automation.TimeoutAwaiter;
import com.exactprosystems.clearth.automation.report.ReportParamValue;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.connectivity.iface.EncodedClearThMessage;
import com.exactprosystems.clearth.data.HandledMessageId;
import com.exactprosystems.clearth.data.MessageHandlingUtils;
import com.exactprosystems.clearth.data.th2.SchedulerExecutionInfo;
import com.exactprosystems.clearth.data.th2.config.EventsConfig;
import com.exactprosystems.clearth.data.th2.config.StorageConfig;
import com.exactprosystems.clearth.data.th2.messages.Th2MessageId;
import com.exactprosystems.clearth.data.th2.tables.KeyValueRow;
import com.exactprosystems.clearth.data.th2.tables.ParamRow;
import com.google.protobuf.Timestamp;

public class EventFactory
{
	private static final Logger logger = LoggerFactory.getLogger(EventFactory.class);
	public static final String TYPE_SCHEDULER = "Scheduler",
			TYPE_MATRIX = "Matrix",
			TYPE_GLOBAL_STEP = "GlobalStep",
			TYPE_PARAMETERS = "Parameters",
			TYPE_ACTION = "Action",
			TYPE_SUB_ACTION = "Sub-action",
			TYPE_STATUS = "Status",
			
			NAME_INPUT_PARAMETERS = "Input parameters",
			NAME_SPECIAL_PARAMETERS = "Special parameters",
			NAME_OUTPUT_PARAMETERS = "Output parameters",
			NAME_ACTION_STATUS = "Action status",
			COMPARISON_NAME = "Comparison name", 
			COMPARISON_RESULT = "Comparison result",
			ROW_KIND = "Row kind";
	
	private final String bookName,
			scope;
	
	public EventFactory(StorageConfig config)
	{
		EventsConfig events = config.getEvents();
		this.bookName = config.getBook();
		this.scope = events != null ? events.getScope() : null;
	}
	
	public Event createSchedulerEvent(SchedulerExecutionInfo executionInfo) throws EventCreationException
	{
		try
		{
			return ClearThEvent.from(executionInfo.getStartTimestamp())
					.name("'" + executionInfo.getName() + "' scheduler execution")
					.type(TYPE_SCHEDULER)
					.description("Started by '" + executionInfo.getStartedByUser()+"'")
					.toProto(bookName, scope);
		}
		catch (Exception e)
		{
			throw new EventCreationException("Error while creating scheduler event", e);
		}
	}
	
	public Event createMatrixEvent(String matrixName, Instant startTimestamp, EventID parentId) throws EventCreationException
	{
		try
		{
			return ClearThEvent.from(startTimestamp)
					.name(matrixName)
					.type(TYPE_MATRIX)
					.toProto(parentId);
		}
		catch (Exception e)
		{
			throw new EventCreationException("Error while creating matrix event", e);
		}
	}
	
	public Event createStepEvent(StepMetadata metadata, EventID parentId) throws EventCreationException
	{
		try
		{
			return ClearThEvent.from(metadata.getStarted())
					.name(metadata.getName())
					.type(TYPE_GLOBAL_STEP)
					.toProto(parentId);
		}
		catch (Exception e)
		{
			throw new EventCreationException("Error while creating global step event", e);
		}
	}
	
	public Event createActionEvent(Action action, EventID parentId) throws EventCreationException
	{
		try
		{
			Instant startTimestamp = EventUtils.getActionStartTimestamp(action),
					endTimestamp = EventUtils.getActionEndTimestamp(action);
			
			com.exactpro.th2.common.event.Event event = ClearThEvent.fromTo(startTimestamp, endTimestamp)
					.name(buildActionName(action.getIdInMatrix(), action.getName()))
					.type(TYPE_ACTION)
					.description(buildActionDescription(action));
			return event.toProto(parentId);
		}
		catch (Exception e)
		{
			throw new EventCreationException("Error while creating action event", e);
		}
	}
	
	public EventBatch createActionSubEvents(Action action, Event parent) throws EventCreationException
	{
		try
		{
			EventID parentId = parent.getId();
			Instant start = EventUtils.getTimestamp(parentId.getStartTimestamp()),
					end = EventUtils.getTimestamp(parent.getEndTimestamp());
			
			Event inputParams = createInputParams(action, start, end, parentId),
					specialParams = createSpecialParams(action, start, end, parentId);
			Collection<Event> subActions = createSubActions(action, start, end, parentId);
			
			EventBatch.Builder builder = EventBatch.newBuilder()
					.setParentEventId(parentId)
					.addEvents(inputParams);
			if (specialParams != null)
				builder.addEvents(specialParams);
			builder.addAllEvents(subActions);
			
			return builder.build();
		}
		catch (Exception e)
		{
			throw new EventCreationException("Error while creating action sub-events", e);
		}
	}
	
	public Event createOutputParamsEvent(Action action, Th2EventMetadata metadata) throws EventCreationException
	{
		Map<String, String> outputs = action.getOutputParams();
		if (MapUtils.isEmpty(outputs))
			return null;
		
		try
		{
			return ClearThEvent.fromTo(metadata.getStartTimestamp(), metadata.getEndTimestamp())
					.name(NAME_OUTPUT_PARAMETERS)
					.type(TYPE_PARAMETERS)
					.bodyData(createKeyValueTable(outputs))
					.toProto(metadata.getId());
		}
		catch (Exception e)
		{
			throw new EventCreationException("Error while creating action output parameters event", e);
		}
	}
	
	public Event createActionStatusEvent(Action action, Result result, Th2EventMetadata metadata)
			throws EventCreationException
	{
		try
		{
			com.exactpro.th2.common.event.Event event = ClearThEvent.fromTo(metadata.getStartTimestamp(), metadata.getEndTimestamp())
					.name(NAME_ACTION_STATUS)
					.type(TYPE_STATUS);
			
			if (result == null)
			{
				//In ClearTH non-executable action is gray and failed, not affecting the whole report
				//In th2 we don't have such option, so making it green and passed + adding a comment about action skip (see buildActionDescription())
				event.status(action.isExecutable() ? EventUtils.getStatus(action.isPassed()) : Status.PASSED);
			}
			else
			{
				event.status(EventUtils.getStatus(result.isSuccess()));
				Throwable error = result.getError();
				if (error != null)
					event.exception(error, true);
				addLinkedMessages(result, action.getIdInMatrix(), event);
			}
			
			Table body = createStatusTable(action, result);
			if (body != null)
				event.bodyData(body);
			
			return event.toProto(metadata.getId());
		}
		catch (Exception e)
		{
			throw new EventCreationException("Error while creating action status event", e);
		}
	}
	
	
	protected Event createInputParams(Action action, Instant startTimestamp, Instant endTimestamp, EventID parentId) throws IOException
	{
		return ClearThEvent.fromTo(startTimestamp, endTimestamp)
				.name(NAME_INPUT_PARAMETERS)
				.type(TYPE_PARAMETERS)
				.bodyData(createParamsTable(action.extractMatrixInputParams(), action.getMatrixInputParams()))
				.toProto(parentId);
	}
	
	protected Event createSpecialParams(Action action, Instant startTimestamp, Instant endTimestamp, EventID parentId) throws IOException
	{
		Set<String> specials = action.getSpecialParamsNames();
		if (CollectionUtils.isEmpty(specials))
			return null;
		
		return ClearThEvent.fromTo(startTimestamp, endTimestamp)
				.name(NAME_SPECIAL_PARAMETERS)
				.type(TYPE_PARAMETERS)
				.bodyData(createParamsTable(action.extractSpecialParams(), specials))
				.toProto(parentId);
	}
	
	protected Collection<Event> createSubActions(Action action, Instant startTimestamp, Instant endTimestamp, EventID parentId) throws IOException
	{
		Map<String, SubActionData> subActions = action.getSubActionData();
		if (MapUtils.isEmpty(subActions))
			return Collections.emptyList();
		
		Collection<Event> result = new ArrayList<>(subActions.size());
		for (Entry<String, SubActionData> sa : subActions.entrySet())
		{
			SubActionData subData = sa.getValue();
			Event subEvent = ClearThEvent.fromTo(startTimestamp, endTimestamp)
					.name(buildActionName(sa.getKey(), subData.getName()))
					.type(TYPE_SUB_ACTION)
					.status(EventUtils.getStatus(subData.getSuccess().isPassed()))
					.bodyData(createParamsTable(subData.extractMatrixInputParams(), subData.getMatrixInputParams()))
					.toProto(parentId);
			result.add(subEvent);
		}
		return result;
	}
	
	
	protected final Table createParamsTable(Map<String, ReportParamValue> params, Set<String> orderedNames)
	{
		TableBuilder<ParamRow> builder = new TableBuilder<>();
		for (String name : orderedNames)
		{
			ReportParamValue value = params.get(name);
			ParamRow row = new ParamRow(name, safe(value.getValue()), safe(value.getFormula()));
			builder = builder.row(row);
		}
		return builder.build();
	}
	
	protected final Table createKeyValueTable(Map<String, String> values)
	{
		TableBuilder<KeyValueRow> builder = new TableBuilder<>();
		for (Entry<String, String> v : values.entrySet())
		{
			KeyValueRow row = new KeyValueRow(v.getKey(), safe(v.getValue()));
			builder = builder.row(row);
		}
		return builder.build();
	}
	
	protected final Table createStatusTable(Action action, Result result)
	{
		long actualTimeout = 0,
				waitBeforeAction = 0;
		if (action instanceof TimeoutAwaiter)
		{
			TimeoutAwaiter ta = (TimeoutAwaiter)action;
			if (ta.isUsesTimeout())
				actualTimeout = ta.getAwaitedTimeout();
			else
				waitBeforeAction = action.getTimeOut();
		}
		else
			waitBeforeAction = action.getTimeOut();
		
		if (actualTimeout == 0 && waitBeforeAction == 0 && result == null)
			return null;
		
		TableBuilder<KeyValueRow> builder = new TableBuilder<>();
		if (actualTimeout > 0)
			builder = builder.row(new KeyValueRow("Actual timeout", Long.toString(actualTimeout)));
		if (waitBeforeAction > 0)
			builder = builder.row(new KeyValueRow("Wait before action", Long.toString(waitBeforeAction)));
		
		if (result == null)
			return builder.build();
		
		String comment = result.getComment();
		if (StringUtils.isNotEmpty(comment))
			builder = builder.row(new KeyValueRow("Comment", comment));
		
		String message = result.getMessage();
		if (StringUtils.isNotEmpty(message))
			builder = builder.row(new KeyValueRow("Message", message));
		
		if (!result.isSuccess())
			builder = builder.row(new KeyValueRow("Fail reason", result.getFailReason().name()));
		
		return builder.build();
	}
	
	
	protected final void addLinkedMessages(Result result, String actionId, com.exactpro.th2.common.event.Event event)
	{
		for (EncodedClearThMessage message : result.getLinkedMessages())
		{
			HandledMessageId messageId = MessageHandlingUtils.getMessageId(message.getMetadata());
			if (messageId == null)
			{
				logger.warn("Message linked to action '{}' doesn't have handled ID", actionId);
				continue;
			}
			
			if (!(messageId instanceof Th2MessageId))
			{
				logger.error("ID of message linked to action '{}' has unexpected class: {}", actionId, messageId.getClass());
				continue;
			}
			
			MessageId idValue = ((Th2MessageId)messageId).getId();
			event.messageID(convertMessageId(idValue));
		}
	}
	
	
	private String safe(String value)
	{
		return value == null ? "" : value;
	}
	
	private String buildActionName(String id, String name)
	{
		return id+" - "+name;
	}
	
	private String buildActionDescription(Action action)
	{
		if (action.isExecutable() && StringUtils.isEmpty(action.getComment()))
			return null;
		
		StringJoiner joiner = new StringJoiner(". ");
		if (!action.isExecutable())
			joiner.add("Not executed");
		if (!StringUtils.isEmpty(action.getComment()))
			joiner.add(action.getComment());
		return joiner.toString();
	}
	
	private MessageID convertMessageId(MessageId id)
	{
		Instant timestamp = id.getTimestamp();
		return MessageID.newBuilder()
				.setBookName(bookName)
				.setConnectionId(ConnectionID.newBuilder()
						.setSessionAlias(id.getSessionAlias())
						.build())
				.setTimestamp(Timestamp.newBuilder()
						.setSeconds(timestamp.getEpochSecond())
						.setNanos(timestamp.getNano())
						.build())
				.setDirection(id.getDirection() == Direction.INCOMING
						? com.exactpro.th2.common.grpc.Direction.FIRST
						: com.exactpro.th2.common.grpc.Direction.SECOND)
				.setSequence(id.getSequence())
				.build();
	}
}