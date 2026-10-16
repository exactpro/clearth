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

package com.exactprosystems.clearth.automation.actions.http;

import java.util.Set;

import com.exactprosystems.clearth.automation.GlobalContext;
import com.exactprosystems.clearth.automation.MatrixContext;
import com.exactprosystems.clearth.automation.StepContext;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.results.DetailedResult;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.messages.KeyFieldsData;
import com.exactprosystems.clearth.messages.MessageBuilder;
import com.exactprosystems.clearth.utils.inputparams.InputParamsHandler;

import static com.exactprosystems.clearth.ClearThCore.comparisonUtils;

public class VerifyPlainHttpResponse extends VerifyHttpResponse
{
	@Override
	public MessageBuilder getMessageBuilder(Set serviceParameters, Set metaFields)
	{
		return null;  //Not used
	}
	
	@Override
	protected KeyFieldsData getKeys(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext, ClearThMessage message)
	{
		//Not used
		return null;
	}
	
	@Override
	protected String getDefaultCodecName()
	{
		//Not used
		return "";
	}

	@Override
	protected Result verifyResponse(HttpResponseData response, StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext)
	{
		InputParamsHandler handler = new InputParamsHandler(getInputParams());
		String expectedResponse = handler.getRequiredString(PARAM_RESPONSE_TEXT);
		Integer expectedCode = handler.getRequiredInteger(PARAM_RESPONSE_CODE);
		handler.check();
		
		DetailedResult result = new DetailedResult();
		result.addResultDetail(comparisonUtils().createResultDetail(PARAM_RESPONSE_TEXT, expectedResponse, response.getResponse()));
		result.addResultDetail(comparisonUtils().createResultDetail(PARAM_RESPONSE_CODE, expectedCode.toString(), Integer.toString(response.getCode())));
		return result;
	}
}
