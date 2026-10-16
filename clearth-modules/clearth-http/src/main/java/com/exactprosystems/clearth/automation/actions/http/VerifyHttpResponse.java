/******************************************************************************
 * Copyright 2009-2019 Exactpro Systems Limited
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

package com.exactprosystems.clearth.automation.actions.http;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.exactprosystems.clearth.automation.GlobalContext;
import com.exactprosystems.clearth.automation.MatrixContext;
import com.exactprosystems.clearth.automation.StepContext;
import com.exactprosystems.clearth.automation.actions.ReceiveMessageAction;
import com.exactprosystems.clearth.automation.exceptions.FailoverException;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.utils.inputparams.InputParamsHandler;

public abstract class VerifyHttpResponse<T extends ClearThMessage<T>> extends ReceiveMessageAction<T>
{
	public static final String PARAM_SENDACTIONID = "SendActionID",
			PARAM_RESPONSE_TEXT = "ResponseText",
			PARAM_RESPONSE_CODE = "ResponseCode";
	
	protected abstract Result verifyResponse(HttpResponseData response, StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext);
	
	@Override
	protected void afterSearch(GlobalContext globalContext, List<T> messages) throws ResultException
	{
	}
	
	@Override
	public boolean isIncoming()
	{
		return true;
	}

	@Override
	public Set<String> getServiceParameters() {
		//TODO if multiple calls cache or move that value to static
		Set<String> service = new HashSet<String>();
		service.addAll(super.getServiceParameters());
		service.add(PARAM_SENDACTIONID);
		return service;
	}

	@Override
	protected Result run(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext) throws ResultException, FailoverException
	{
		String sendActionId = getSendActionId();
		
		HttpResponseData response = getResponse(sendActionId, stepContext, matrixContext, globalContext);
		return verifyResponse(response, stepContext, matrixContext, globalContext);
	}
	
	
	protected String getSendActionId()
	{
		InputParamsHandler handler = new InputParamsHandler(getInputParams());
		String sendActionId = handler.getRequiredString(PARAM_SENDACTIONID);
		handler.check();
		return sendActionId;
	}
	
	protected HttpResponseData getResponse(String sendActionId, StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext)
	{
		HttpResponseData response = (HttpResponseData)matrixContext.getContext(SendHttpRequest.getResponseDataContextName(sendActionId));
		if (response == null)
			throw ResultException.failed("Response is not stored by action '"+sendActionId+"'. Please check result of action '"+sendActionId+"'");
		return response;
	}
}
