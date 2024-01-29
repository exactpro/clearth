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
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.exactpro.th2.common.grpc.EventID;
import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.GlobalContext;
import com.exactprosystems.clearth.automation.MatrixContext;
import com.exactprosystems.clearth.automation.Preparable;
import com.exactprosystems.clearth.automation.SchedulerStatus;
import com.exactprosystems.clearth.automation.StepContext;
import com.exactprosystems.clearth.automation.TimeoutAwaiter;
import com.exactprosystems.clearth.automation.actions.metadata.MetaFieldsGetter;
import com.exactprosystems.clearth.automation.actions.metadata.SimpleMetaFieldsGetter;
import com.exactprosystems.clearth.automation.actions.th2.act.GrpcResponseProcessor;
import com.exactprosystems.clearth.automation.exceptions.FailoverException;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.results.DefaultResult;
import com.exactprosystems.clearth.connectivity.ConnectionException;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.SimpleClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.SimpleClearThMessageBuilder;
import com.exactprosystems.clearth.connectivity.th2.GrpcServiceGetter;
import com.exactprosystems.clearth.data.th2.Th2DataHandlersFactory;
import com.exactprosystems.clearth.messages.th2.GrpcRequestFactory;
import com.exactprosystems.clearth.messages.th2.MessageProperties;
import com.exactprosystems.clearth.utils.Stopwatch;

import io.grpc.Context;
import io.grpc.Context.CancellableContext;
import io.grpc.Deadline;

public abstract class SendTh2ActMessage<S, RQ, RS> extends Action implements Preparable, TimeoutAwaiter
{
	protected abstract GrpcServiceGetter<S> getGrpcServiceGetter(GlobalContext globalContext, Th2DataHandlersFactory th2Factory);
	protected abstract GrpcRequestFactory<RQ> getGrpcRequestFactory(String flatDelimiter);
	protected abstract RS doSendRequest(RQ request, S service) throws Exception;
	protected abstract GrpcResponseProcessor<RS> getResponseProcessor(MatrixContext matrixContext);
	
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
		MessageProperties messageProperties = getMessageProperties(th2Factory);
		String flatDelimiter = getInputParam(Th2ActionUtils.PARAM_FLAT_DELIMITER);
		
		S service = getService(globalContext, th2Factory);
		SimpleClearThMessage message = createClearThMessage(matrixContext);
		RQ request = createRequest(message, messageProperties, flatDelimiter, globalContext);
		
		logger.trace("Sending request: {}", request);
		Stopwatch sw = Stopwatch.createAndStart();
		RS response;
		try
		{
			response = sendRequest(request, service);
		}
		catch (ResultException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			return DefaultResult.failed("Could not send request", e);
		}
		finally
		{
			awaitedTimeout = sw.stop();
		}
			
		logger.trace("Response: {}", response);
		return processResponse(response, matrixContext);
	}
	
	@Override
	public long getAwaitedTimeout()
	{
		return awaitedTimeout;
	}
	
	
	protected MessageProperties getMessageProperties(Th2DataHandlersFactory th2Factory)
	{
		return MessageProperties.fromInputParams(inputParams, th2Factory.getBook());
	}
	
	protected S getService(GlobalContext globalContext, Th2DataHandlersFactory th2Factory)
	{
		try
		{
			return getGrpcServiceGetter(globalContext, th2Factory)
					.get();
		}
		catch (ConnectionException e)
		{
			throw ResultException.failed("Could not get Act service", e);
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
	
	protected RQ createRequest(SimpleClearThMessage message, MessageProperties messageProperties, String flatDelimiter, GlobalContext globalContext)
	{
		EventID eventId = Th2ActionUtils.getGrpcEventId(this);
		try
		{
			return getGrpcRequestFactory(flatDelimiter)
					.createRequest(message, messageProperties, eventId);
		}
		catch (Exception e)
		{
			throw ResultException.failed("Could not create request", e);
		}
	}
	
	protected RS sendRequest(RQ request, S service) throws Exception
	{
		if (timeout <= 0)
			return doSendRequest(request, service);
		
		try (CancellableContext ctx = Context.current()
				.withDeadline(Deadline.after(timeout, TimeUnit.MILLISECONDS), Executors.newSingleThreadScheduledExecutor()))
		{
			return ctx.call(() -> doSendRequest(request, service));
		}
	}
	
	protected Result processResponse(RS response, MatrixContext context)
	{
		return getResponseProcessor(context)
				.process(response, getIdInMatrix());
	}
	
	
	protected Set<String> getServiceParameters()
	{
		return Th2ActionUtils.SENDING_SERVICE_PARAMS;
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
		return new SimpleMetaFieldsGetter(true);
	}
}
