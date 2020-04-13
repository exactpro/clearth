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

import com.exactprosystems.clearth.automation.*;
import com.exactprosystems.clearth.automation.exceptions.FailoverException;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.results.DefaultResult;
import com.exactprosystems.clearth.connectivity.ConnectivityException;
import com.exactprosystems.clearth.connectivity.EncodeException;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.ICodec;
import com.exactprosystems.clearth.messages.*;
import com.exactprosystems.clearth.utils.Stopwatch;
import com.exactprosystems.clearth.utils.inputparams.InputParamsUtils;

import java.io.File;

public abstract class SendMessageAction<T extends ClearThMessage<T>> extends MessageAction<T> implements TimeoutAwaiter
{
	protected long awaitedTimeout;

	@Override
	protected Result run(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext) throws ResultException, FailoverException
	{
		T msg = buildMessage(matrixContext);
		MessageSender<T> sender = getMessageSender(stepContext, matrixContext, globalContext);
		
		try
		{
			beforeSend(msg, stepContext, matrixContext, globalContext);
			String ans = sender.sendMessage(msg);
			afterSend(msg, ans, stepContext, matrixContext, globalContext);
			
			if (timeout > 0)
			{
				Stopwatch sw = Stopwatch.createAndStart();
				try
				{
					Thread.sleep(timeout);  //This can be interrupted, so we measure actual timeout with stopwatch
				}
				catch (InterruptedException e)
				{
					return DefaultResult.failed("Wait after message sending interrupted");
				}
				finally
				{
					awaitedTimeout = sw.stop();
				}
			}
			
			return createResult(msg, ans);
		}
		catch (EncodeException e)
		{
			logger.error("Error while encoding message", e);
			return DefaultResult.failed(e.getMessage());
		}
		catch (Exception e)
		{
			return DefaultResult.failed(e);
		}
	}

	@Override
	public boolean isIncoming()
	{
		return false;
	}
	
	@Override
	public long getAwaitedTimeout()
	{
		return awaitedTimeout;
	}
	
	
	protected ConnectionFinder getConnectionFinder()
	{
		return new ConnectionFinder();
	}
	
	protected StringMessageSender getStringSender(GlobalContext globalContext) throws FailoverException
	{
		if (!getInputParam(CONNECTIONNAME, "").isEmpty())
		{
			try
			{
				return getConnectionFinder().findConnection(getInputParams());
			}
			catch (ConnectivityException e)
			{
				throw new FailoverException(e.getMessage(), FailoverReason.CONNECTION_ERROR);
			}
		}
		if (!getInputParam(FILENAME, "").isEmpty())
			return getFileSender();
		
		StringMessageSender result = getCustomMessageSender(globalContext);
		if (result == null)
			throw ResultException.failed("No '" + CONNECTIONNAME + "' or '" + FILENAME + "' parameters specified");
		
		return result;
	}

	private StringMessageSender getFileSender()
	{
		File file = InputParamsUtils.getRequiredFile(getInputParams(), FILENAME);
		return new StringMessageFileSender(file);
	}

	protected MessageSender<T> getMessageSender(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext)
			throws FailoverException
	{
		ICodec codec = getCodec(globalContext);
		StringMessageSender stringSender = getStringSender(globalContext);
		return new ClearThMessageSender<T>(codec, stringSender);
	}
	
	protected void beforeSend(T msg, StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext) throws ResultException
	{
	}
	
	protected void afterSend(T msg, String answer, StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext) throws ResultException
	{
	}

	protected StringMessageSender getCustomMessageSender(GlobalContext globalContext)
	{
		return null;
	}
	
	protected Result createResult(T msg, String sendingResult)
	{
		return null;
	}
}
