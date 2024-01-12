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

import com.exactpro.th2.check1.grpc.Check1Service;
import com.exactpro.th2.check1.grpc.CheckpointRequest;
import com.exactpro.th2.check1.grpc.CheckpointResponse;
import com.exactpro.th2.common.grpc.EventID;
import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.GlobalContext;
import com.exactprosystems.clearth.automation.MatrixContext;
import com.exactprosystems.clearth.automation.Preparable;
import com.exactprosystems.clearth.automation.SchedulerStatus;
import com.exactprosystems.clearth.automation.StepContext;
import com.exactprosystems.clearth.automation.actions.th2.check1.Check1Utils;
import com.exactprosystems.clearth.automation.exceptions.FailoverException;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.results.DefaultResult;
import com.exactprosystems.clearth.data.th2.Th2DataHandlersFactory;
import com.exactprosystems.clearth.utils.inputparams.InputParamsUtils;

public class CreateTh2Check1Checkpoint extends Action implements Preparable
{
	public static final String PARAM_DESC = "Desc";
	
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
		
		String desc = InputParamsUtils.getStringOrDefault(inputParams, PARAM_DESC, null);
		
		CheckpointRequest request = createRequest(desc);
		Check1Service service = getService(globalContext, th2Factory);
		
		logger.trace("Sending request: {}", request);
		try
		{
			CheckpointResponse response = sendRequest(request, service);
			logger.trace("Response: {}", response);
			return processResponse(response, matrixContext);
		}
		catch (Exception e)
		{
			return DefaultResult.failed("Could not send request", e);
		}
	}
	
	
	protected CheckpointRequest createRequest(String desc)
	{
		EventID eventId = getActionEventId();
		CheckpointRequest.Builder builder = CheckpointRequest.newBuilder()
				.setParentEventId(eventId);
		if (desc != null)
			builder = builder.setDescription(desc);
		
		return builder.build();
	}
	
	protected Check1Service getService(GlobalContext globalContext, Th2DataHandlersFactory th2Factory)
	{
		return Check1Utils.getService(globalContext, th2Factory);
	}
	
	protected CheckpointResponse sendRequest(CheckpointRequest request, Check1Service service) throws Exception
	{
		return service.createCheckpoint(request);
	}
	
	protected Result processResponse(CheckpointResponse response, MatrixContext context)
	{
		Check1Utils.setCheckpoint(getIdInMatrix(), context, response.getCheckpoint());
		return null;
	}
	
	
	protected EventID getActionEventId()
	{
		return Th2ActionUtils.getGrpcEventId(this);
	}
}
