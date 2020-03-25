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

package com.exactprosystems.clearth.automation.actions;

import static com.exactprosystems.clearth.automation.actions.MessageAction.CONNECTIONNAME;
import static com.exactprosystems.clearth.automation.actions.MessageAction.FILENAME;
import static com.exactprosystems.clearth.connectivity.listeners.ClearThMessageCollector.MESSAGE;

import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.GlobalContext;
import com.exactprosystems.clearth.automation.MatrixContext;
import com.exactprosystems.clearth.automation.StepContext;
import com.exactprosystems.clearth.automation.exceptions.FailoverException;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.results.DefaultResult;
import com.exactprosystems.clearth.messages.ConnectionFinder;
import com.exactprosystems.clearth.messages.StringMessageFileSender;
import com.exactprosystems.clearth.messages.StringMessageSender;
import com.exactprosystems.clearth.utils.inputparams.InputParamsUtils;

import java.io.File;

public class SendPlainMessage extends Action
{
	
	@Override
	protected Result run(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext) throws ResultException, FailoverException
	{
		String message = InputParamsUtils.getRequiredString(getInputParams(), MESSAGE);
		StringMessageSender sender = getMessageSender(stepContext, matrixContext, globalContext);
		try
		{
			sender.sendMessage(message);
		}
		catch (Exception e)
		{
			return DefaultResult.failed("Could not send message", e);
		}
		
		return null;
	}

	protected StringMessageSender getMessageSender(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext)
	{
		if (!getInputParam(CONNECTIONNAME, "").isEmpty())
			return new ConnectionFinder().findConnection(getInputParams());
		if (!getInputParam(FILENAME, "").isEmpty())
			return getFileSender();

		StringMessageSender result = getCustomMessageSender(globalContext);
		if (result == null)
			throw ResultException.failed("No '"+CONNECTIONNAME+"' and '"+FILENAME+"' parameters specified");

		return result;
	}

	private StringMessageSender getFileSender()
	{
		File file = InputParamsUtils.getRequiredFile(getInputParams(), FILENAME);
		return new StringMessageFileSender(file);
	}

	protected StringMessageSender getCustomMessageSender(GlobalContext globalContext)
	{
		return null;
	}
}
