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

import java.util.Collections;
import java.util.List;

import com.exactprosystems.clearth.automation.GlobalContext;
import com.exactprosystems.clearth.automation.MatrixContext;
import com.exactprosystems.clearth.automation.StepContext;
import com.exactprosystems.clearth.automation.actions.MessageComparator;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.connectivity.DecodeException;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.ICodec;

public abstract class VerifyRestResponse<T extends ClearThMessage<T>> extends VerifyHttpResponse<T>
{
	@Override
	protected Result verifyResponse(HttpResponseData response, StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext)
	{
		T msg = parseResponse(response, stepContext, matrixContext, globalContext);
		T expectedMsg = buildMessage(matrixContext);
		
		List<T> messages = Collections.singletonList(msg);
		
		MessageComparator<T> comparator = getMessageComparator();
		Result result = compareMessages(comparator, expectedMsg, messages, null);
		if (result.isSuccess() || isSaveOutputIfFailed())
			saveOutputParams(messages, comparator);

		return result;
	}
	
	
	protected T parseResponse(HttpResponseData response, StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext) throws ResultException
	{
		ICodec codec = getCodec(globalContext);
		try
		{
			T result = (T)codec.decode(response.getResponse());
			//Adding HTTP response code of the request to "message" to check
			result.addField(PARAM_RESPONSE_CODE, Integer.toString(response.getCode()));
			return result;
		}
		catch (DecodeException e)
		{
			throw ResultException.failed("Error while decoding stored response", e);
		}
	}
}
