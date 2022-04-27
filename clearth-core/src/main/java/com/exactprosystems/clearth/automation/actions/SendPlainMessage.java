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

package com.exactprosystems.clearth.automation.actions;

import com.exactprosystems.clearth.automation.*;
import com.exactprosystems.clearth.automation.actions.metadata.MetaFieldsGetter;
import com.exactprosystems.clearth.automation.actions.metadata.SimpleMetaFieldsGetter;
import com.exactprosystems.clearth.automation.exceptions.FailoverException;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.results.DefaultResult;
import com.exactprosystems.clearth.connectivity.ConnectivityException;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessageMetadata;
import com.exactprosystems.clearth.connectivity.iface.EncodedClearThMessage;
import com.exactprosystems.clearth.messages.ConnectionFinder;
import com.exactprosystems.clearth.messages.PlainMessageFileSender;
import com.exactprosystems.clearth.messages.PlainMessageSender;
import com.exactprosystems.clearth.utils.inputparams.InputParamsUtils;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import static com.exactprosystems.clearth.automation.actions.MessageAction.CONNECTIONNAME;
import static com.exactprosystems.clearth.automation.actions.MessageAction.FILENAME;
import static com.exactprosystems.clearth.connectivity.listeners.ClearThMessageCollector.MESSAGE;

public class SendPlainMessage extends Action
{
	@Override
	protected Result run(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext) throws ResultException, FailoverException
	{
		String message = InputParamsUtils.getRequiredString(getInputParams(), MESSAGE);
		ClearThMessageMetadata metadata = buildMetadata();
		PlainMessageSender sender = getMessageSender(stepContext, matrixContext, globalContext);
		try
		{
			sendMessage(sender, message, metadata);
		}
		catch (Exception e)
		{
			return DefaultResult.failed("Could not send message", e);
		}
		
		return null;
	}

	protected PlainMessageSender getMessageSender(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext)
			throws FailoverException
	{
		String connName = getInputParam(CONNECTIONNAME, "");
		if (!connName.isEmpty())
		{
			try
			{
				return new ConnectionFinder().findConnection(connName);
			}
			catch (ConnectivityException e)
			{
				throw new FailoverException(e.getMessage(), FailoverReason.CONNECTION_ERROR, connName);
			}
		}
		if (!getInputParam(FILENAME, "").isEmpty())
			return getFileSender();
		
		PlainMessageSender result = getCustomMessageSender(globalContext);
		if (result == null)
			throw ResultException.failed("No '" + CONNECTIONNAME + "' or '" + FILENAME + "' parameters specified");
		return result;
	}

	private PlainMessageSender getFileSender()
	{
		File file = InputParamsUtils.getRequiredFile(getInputParams(), FILENAME);
		return new PlainMessageFileSender(file);
	}

	protected PlainMessageSender getCustomMessageSender(GlobalContext globalContext)
	{
		return null;
	}
	
	protected MetaFieldsGetter getMetaFieldsGetter()
	{
		return new SimpleMetaFieldsGetter(false);
	}
	
	
	protected ClearThMessageMetadata buildMetadata()
	{
		Set<String> metaFields = getMetaFields();
		if (CollectionUtils.isEmpty(metaFields))
			return null;
		
		ClearThMessageMetadata result = null;
		for (String f : metaFields)
		{
			String value = inputParams.get(f);
			if (!StringUtils.isEmpty(value))
			{
				if (result == null)
					result = new ClearThMessageMetadata();
				result.addField(f, value);
			}
		}
		return result;
	}
	
	protected Set<String> getMetaFields()
	{
		MetaFieldsGetter getter = getMetaFieldsGetter();
		Set<String> metaFields = getter.getFields(inputParams);
		getter.checkFields(metaFields, inputParams);
		return metaFields;
	}
	
	private void sendMessage(PlainMessageSender sender, String message, ClearThMessageMetadata metadata) throws ConnectivityException, IOException
	{
		if (metadata != null)
			sender.sendMessage(new EncodedClearThMessage(message, metadata));
		else
			sender.sendMessage(message);
	}
}
