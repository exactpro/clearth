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

import java.util.Map;
import java.util.Set;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.utils.ComparisonUtils;
import org.apache.commons.lang3.StringUtils;

import com.exactpro.th2.check1.grpc.Check1Service;
import com.exactpro.th2.check1.grpc.CheckRuleRequest;
import com.exactpro.th2.check1.grpc.CheckRuleResponse;
import com.exactpro.th2.check1.grpc.WaitForResultRequest;
import com.exactpro.th2.check1.grpc.WaitForResultResponse;
import com.exactpro.th2.common.grpc.Checkpoint;
import com.exactpro.th2.common.grpc.ConnectionID;
import com.exactpro.th2.common.grpc.Direction;
import com.exactpro.th2.common.grpc.EventID;
import com.exactpro.th2.common.grpc.EventStatus;
import com.exactpro.th2.common.grpc.MessageFilter;
import com.exactpro.th2.common.grpc.RootMessageFilter;
import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.GlobalContext;
import com.exactprosystems.clearth.automation.MatrixContext;
import com.exactprosystems.clearth.automation.Preparable;
import com.exactprosystems.clearth.automation.SchedulerStatus;
import com.exactprosystems.clearth.automation.StepContext;
import com.exactprosystems.clearth.automation.TimeoutAwaiter;
import com.exactprosystems.clearth.automation.actions.MessageAction;
import com.exactprosystems.clearth.automation.actions.metadata.MetaFieldsGetter;
import com.exactprosystems.clearth.automation.actions.metadata.SimpleMetaFieldsGetter;
import com.exactprosystems.clearth.automation.actions.th2.check1.Check1Utils;
import com.exactprosystems.clearth.automation.exceptions.FailoverException;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.results.DefaultResult;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.SimpleClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.SimpleClearThMessageBuilder;
import com.exactprosystems.clearth.data.th2.Th2DataHandlersFactory;
import com.exactprosystems.clearth.messages.RgKeyFieldNames;
import com.exactprosystems.clearth.messages.converters.ConversionException;
import com.exactprosystems.clearth.messages.th2.MessageFilterFactory;
import com.exactprosystems.clearth.utils.Stopwatch;
import com.exactprosystems.clearth.utils.inputparams.InputParamsHandler;
import com.google.protobuf.Duration;

public class VerifyTh2Message extends Action implements Preparable, TimeoutAwaiter
{
	public static final String PARAM_SESSION_ALIAS = "SessionAlias",
			PARAM_SESSION_GROUP = "SessionGroup",
			PARAM_BOOK = "Book",
			PARAM_KEY_FIELDS = "KeyFields",
			PARAM_RG_KEYFIELDS = "RGKeyFields",
			PARAM_FLAT_DELIMITER = "FlatDelimiter",
			PARAM_DIRECTION = "Direction",
			PARAM_CHECKPOINT = "Checkpoint";
	
	private static final Set<String> SERVICE_PARAMS = Set.of(PARAM_SESSION_ALIAS, PARAM_SESSION_GROUP, PARAM_BOOK,
			PARAM_KEY_FIELDS, PARAM_RG_KEYFIELDS,
			PARAM_FLAT_DELIMITER, PARAM_DIRECTION, PARAM_CHECKPOINT,
			ClearThMessage.MSGTYPE, ClearThMessage.SUBMSGTYPE, ClearThMessage.SUBMSGSOURCE,
			MessageAction.REPEATINGGROUPS, MessageAction.META_FIELDS);
	
	private long awaitedTimeout;
	
	@Override
	public void prepare(GlobalContext globalContext, SchedulerStatus schedulerStatus) throws Exception
	{
		Th2ActionUtils.getDataHandlersFactory();
	}
	
	@Override
	protected Result run(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext)
			throws ResultException, FailoverException
	{
		Th2DataHandlersFactory th2Factory = Th2ActionUtils.getDataHandlersFactoryOrResultException();
		
		InputParamsHandler handler = new InputParamsHandler(inputParams);
		String sessionAlias = handler.getRequiredString(PARAM_SESSION_ALIAS),
				sessionGroup = handler.getString(PARAM_SESSION_GROUP, sessionAlias),
				book = handler.getString(PARAM_BOOK, th2Factory.getBook()),
				msgType = handler.getRequiredString(ClearThMessage.MSGTYPE),
				flatDelimiter = handler.getString(PARAM_FLAT_DELIMITER),
				checkpointActionId = handler.getString(PARAM_CHECKPOINT);
		Set<String> keyFields = handler.getSet(PARAM_KEY_FIELDS, ",");
		RgKeyFieldNames rgKeyFields = RgKeyFieldNames.parse(handler.getSet(PARAM_RG_KEYFIELDS));
		Direction direction = handler.getEnum(PARAM_DIRECTION, Direction.class, Direction.FIRST);
		handler.check();
		
		SimpleClearThMessage message = createClearThMessage(msgType, matrixContext);
		MessageFilter messageFilter = createMessageFilter(message, flatDelimiter, keyFields, rgKeyFields);
		RootMessageFilter rootFilter = createRootMessageFilter(msgType, messageFilter);
		
		Checkpoint checkpoint = getCheckpoint(checkpointActionId, matrixContext);
		EventID eventId = getActionEventId();
		CheckRuleRequest request = createRequest(book, sessionGroup, sessionAlias, eventId, direction, rootFilter, checkpoint);
		Check1Service service = getService(globalContext, th2Factory);
		
		CheckRuleResponse response;
		try
		{
			logger.trace("Sending request: {}", request);
			response = sendRequest(request, service);
			logger.trace("Response: {}", response);
		}
		catch (Exception e)
		{
			return DefaultResult.failed("Could not send request", e);
		}
		
		WaitForResultRequest waitRequest = createWaitRequest(response);
		Stopwatch sw = Stopwatch.createAndStart();
		try
		{
			logger.trace("Sending wait request: {}", waitRequest);
			WaitForResultResponse waitResponse = sendWaitRequest(waitRequest, service);
			logger.trace("Wait response: {}", waitResponse);
			return processWaitResponse(waitResponse, eventId);
		}
		catch (Exception e)
		{
			return DefaultResult.failed("Error while waiting for verification result", e);
		}
		finally
		{
			awaitedTimeout = sw.stop();
		}
	}
	
	@Override
	public long getAwaitedTimeout()
	{
		return awaitedTimeout;
	}
	
	
	protected SimpleClearThMessage createClearThMessage(String msgType, MatrixContext matrixContext)
	{
		Map<String, String> ip = getInputParams();
		return getMessageBuilder(getServiceParameters(), getMetaFields(ip))
				.fields(ip)
				.metaFields(ip)
				.rgs(matrixContext, this)
				.type(msgType)
				.build();
	}
	
	protected MessageFilter createMessageFilter(SimpleClearThMessage message, String flatDelimiter, Set<String> keyFields, RgKeyFieldNames rgKeyFields)
	{
		try
		{
			return getMessageFilterFactory(flatDelimiter)
					.createMessageFilter(message, keyFields, rgKeyFields);
		}
		catch (ConversionException e)
		{
			throw ResultException.failed("Could not create message filter", e);
		}
	}
	
	protected RootMessageFilter createRootMessageFilter(String msgType, MessageFilter messageFilter)
	{
		return RootMessageFilter.newBuilder()
				.setMessageType(msgType)
				.setMessageFilter(messageFilter)
				.build();
	}
	
	protected CheckRuleRequest createRequest(String book, String sessionGroup, String sessionAlias, EventID eventId,
			Direction direction, RootMessageFilter rootFilter, Checkpoint checkpoint)
	{
		CheckRuleRequest.Builder builder = CheckRuleRequest.newBuilder()
				.setParentEventId(eventId)
				.setBookName(book)
				.setConnectivityId(ConnectionID.newBuilder().setSessionAlias(sessionAlias).setSessionGroup(sessionGroup).build())
				.setDirection(direction)
				.setRootFilter(rootFilter)
				.setTimeout(timeout)
				.setStoreResult(true);
		if (checkpoint != null)
			builder = builder.setCheckpoint(checkpoint);
		
		return builder.build();
	}
	
	protected Check1Service getService(GlobalContext globalContext, Th2DataHandlersFactory th2Factory)
	{
		return Check1Utils.getService(globalContext, th2Factory);
	}
	
	protected CheckRuleResponse sendRequest(CheckRuleRequest request, Check1Service service) throws Exception
	{
		return service.submitCheckRule(request);
	}
	
	protected WaitForResultRequest createWaitRequest(CheckRuleResponse response)
	{
		java.time.Duration timeoutDuration = java.time.Duration.ofMillis(timeout);
		return WaitForResultRequest.newBuilder()
				.setRuleId(response.getRuleId())
				.setTimeout(Duration.newBuilder()
						.setSeconds(timeoutDuration.getSeconds())
						.setNanos(timeoutDuration.getNano()))
				.build();
	}
	
	protected WaitForResultResponse sendWaitRequest(WaitForResultRequest request, Check1Service service)
	{
		return service.waitForResult(request);
	}
	
	protected Result processWaitResponse(WaitForResultResponse response, EventID eventId)
	{
		EventStatus status = response.getRuleResult();
		if (status == EventStatus.SUCCESS)
			return null;
		return DefaultResult.failed("Message verification failed. For details, check children of test event "+eventId);
	}
	
	
	protected Checkpoint getCheckpoint(String actionId, MatrixContext matrixContext)
	{
		if (StringUtils.isEmpty(actionId))
			return null;
		
		Checkpoint result = Check1Utils.getCheckpoint(actionId, matrixContext);
		if (result == null)
			throw ResultException.failed("No Check1 checkpoint set by action '"+actionId+"'");
		return result;
	}
	
	protected EventID getActionEventId()
	{
		return Th2ActionUtils.getGrpcEventId(this);
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
	
	protected MessageFilterFactory getMessageFilterFactory(String flatDelimiter)
	{
		ComparisonUtils utils = ClearThCore.comparisonUtils();
		//Checking for null, not with StringUtils.isEmpty(), to allow switching flat mode off by specifying #FlatDelimiter=""
		return flatDelimiter == null ? new MessageFilterFactory(utils) : new MessageFilterFactory(utils, flatDelimiter);
	}
}
