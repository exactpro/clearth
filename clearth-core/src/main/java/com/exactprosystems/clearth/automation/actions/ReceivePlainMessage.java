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

package com.exactprosystems.clearth.automation.actions;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.*;
import com.exactprosystems.clearth.automation.exceptions.FailoverException;
import com.exactprosystems.clearth.automation.exceptions.ParametersException;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.results.DefaultResult;
import com.exactprosystems.clearth.connectivity.ConnectivityException;
import com.exactprosystems.clearth.connectivity.ListenerType;
import com.exactprosystems.clearth.connectivity.MessageListener;
import com.exactprosystems.clearth.connectivity.connections.ClearThMessageConnection;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.EncodedClearThMessage;
import com.exactprosystems.clearth.connectivity.listeners.ClearThMessageCollector;
import com.exactprosystems.clearth.messages.CollectorMessageSource;
import com.exactprosystems.clearth.messages.ConnectionFinder;
import com.exactprosystems.clearth.messages.FileMessageSource;
import com.exactprosystems.clearth.messages.MessageSource;
import com.exactprosystems.clearth.utils.ComparisonUtils;
import com.exactprosystems.clearth.utils.Stopwatch;
import com.exactprosystems.clearth.utils.inputparams.InputParamsUtils;

import java.io.File;
import java.io.IOException;

import static com.exactprosystems.clearth.connectivity.listeners.ClearThMessageCollector.MESSAGE;

public class ReceivePlainMessage extends Action implements TimeoutAwaiter
{
	protected long awaitedTimeout;

	@Override
	protected Result run(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext) throws ResultException, FailoverException
	{
		String messageToFind = InputParamsUtils.getRequiredString(getInputParams(), MESSAGE);
		MessageSource source = getMessageSource();
		
		ComparisonUtils cu = ClearThCore.comparisonUtils();
		long sleepTime = 200;
		Stopwatch sw = Stopwatch.createAndStart(timeout);
		try
		{
			while (true)
			{
				try
				{
					ClearThMessage<?> msg;
					while ((msg = source.nextMessage()) != null)
					{
						if (cu.compareValues(messageToFind, msg.getEncodedMessage()))
						{
							Result result = DefaultResult.passed("Message found");
							result.addLinkedMessage(new EncodedClearThMessage(msg.getEncodedMessage(), msg.getMetadata()));
							return result;
						}
					}
				}
				catch (ParametersException e)
				{
					return DefaultResult.failed(e);
				}
				catch (IOException e)
				{
					return DefaultResult.failed("Error while getting next message", e);
				}
				
				if (sw.isExpired())
					break;
				
				try
				{
					Thread.sleep(sleepTime);
				}
				catch (InterruptedException e)
				{
					Thread.currentThread().interrupt();
					return DefaultResult.failed("Search for message was interrupted before the timeout expired.");
				}
			}
		}
		finally
		{
			awaitedTimeout = sw.stop();
		}

		return DefaultResult.failed("Message not found");
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

	
	protected CollectorMessageSource createCollectorSource(ClearThMessageCollector collector)
	{
		return new CollectorMessageSource(collector, true);
	}
	
	protected MessageSource getFileSource() 
	{
		File file = InputParamsUtils.getRequiredFile(getInputParams(), MessageAction.FILENAME);
		return new FileMessageSource(file, null);
	}
	
	protected MessageSource getCollectorMessageSource() throws FailoverException
	{
		ConnectionFinder finder = getConnectionFinder();
		String conName = finder.getConnectionName(getInputParams());
		
		try
		{
			ClearThMessageConnection connection = finder.findConnection(conName);
			MessageListener collector = connection.findListener(ListenerType.Collector.getLabel());
			if (collector == null)
				throw ResultException.failed("No collector defined for connection '" + conName + "'");
			
			if (collector instanceof ClearThMessageCollector)
				return createCollectorSource((ClearThMessageCollector)collector);
			throw ResultException.failed("Collector is an instance of unexpected class '" + collector.getClass().getName() + "'");
		}
		catch (ConnectivityException e)
		{
			throw new FailoverException(e.getMessage(), FailoverReason.CONNECTION_ERROR, conName);
		}
	}
	
	protected MessageSource getMessageSource() throws FailoverException
	{
		if (!getInputParam(MessageAction.CONNECTIONNAME, "").isEmpty())
			return getCollectorMessageSource();
		if (!getInputParam(MessageAction.FILENAME, "").isEmpty())
			return getFileSource();
		throw ResultException.failed("No '"+MessageAction.CONNECTIONNAME+"' and '"+MessageAction.FILENAME+"' parameters specified");
	}
}
