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

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.GlobalContext;
import com.exactprosystems.clearth.automation.MatrixContext;
import com.exactprosystems.clearth.automation.StepContext;
import com.exactprosystems.clearth.automation.actions.MessageAction;
import com.exactprosystems.clearth.automation.actions.MessageComparator;
import com.exactprosystems.clearth.automation.actions.metadata.MetaFieldsGetter;
import com.exactprosystems.clearth.automation.actions.metadata.SimpleMetaFieldsGetter;
import com.exactprosystems.clearth.automation.actions.th2.act.ActUtils;
import com.exactprosystems.clearth.automation.exceptions.FailoverException;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.SimpleClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.SimpleClearThMessageBuilder;
import com.exactprosystems.clearth.messages.RgKeyFieldNames;
import com.exactprosystems.clearth.messages.th2.GrpcResponseConverter;
import com.exactprosystems.clearth.utils.Pair;
import com.exactprosystems.clearth.utils.inputparams.InputParamsHandler;

public abstract class VerifyTh2ActMessage<RS> extends Action
{
	public static final String PARAM_RG_KEY_FIELDS = "RGKeyFields",
			PARAM_SEND_ACTIONID = "SendActionID";
	
	private static final Set<String> SERVICE_PARAMS = Set.of(MessageAction.REPEATINGGROUPS, MessageAction.META_FIELDS,
			ClearThMessage.SUBMSGTYPE, ClearThMessage.SUBMSGSOURCE,
			PARAM_RG_KEY_FIELDS, PARAM_SEND_ACTIONID);
	
	@Override
	protected Result run(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext)
			throws ResultException, FailoverException
	{
		InputParamsHandler handler = new InputParamsHandler(inputParams);
		String sendActionId = handler.getRequiredString(PARAM_SEND_ACTIONID);
		RgKeyFieldNames rgKeyFields = RgKeyFieldNames.parse(handler.getSet(PARAM_RG_KEY_FIELDS));
		handler.check();
		
		RS response = getResponse(sendActionId, matrixContext);
		SimpleClearThMessage actualMessage = convertResponse(response),
				expectedMessage = createExpectedMessage(matrixContext);
		
		MessageComparator<SimpleClearThMessage> comparator = getMessageComparator();
		Result result = comparator.compareMessages(expectedMessage, actualMessage, rgKeyFields);
		cleanResponse(sendActionId, matrixContext);
		saveOutputParams(actualMessage, comparator);
		return result;
	}
	
	protected abstract GrpcResponseConverter<RS> getGrpcResponseConverter();
	
	
	protected RS getResponse(String sendActionId, MatrixContext matrixContext)
	{
		Object r = ActUtils.getResponse(sendActionId, matrixContext);
		if (r == null)
			throw ResultException.failed("No response stored by action '"+sendActionId+"'. Check its result");
		return (RS) r;
	}
	
	protected SimpleClearThMessage convertResponse(RS response)
	{
		try
		{
			return getGrpcResponseConverter().convert(response);
		}
		catch (Exception e)
		{
			throw ResultException.failed("Could not convert response to comparable message", e);
		}
	}
	
	protected SimpleClearThMessage createExpectedMessage(MatrixContext matrixContext)
	{
		Map<String, String> ip = getInputParams();
		return getMessageBuilder(getServiceParameters(), getMetaFields(ip))
				.fields(ip)
				.metaFields(ip)
				.rgs(matrixContext, this)
				.build();
	}
	
	protected MessageComparator<SimpleClearThMessage> getMessageComparator()
	{
		return new MessageComparator<>(getServiceParameters(), false, true, true);
	}
	
	protected void cleanResponse(String sendActionId, MatrixContext matrixContext)
	{
		matrixContext.removeContext(ActUtils.getResponseName(sendActionId));
	}
	
	protected void saveOutputParams(SimpleClearThMessage actualMessage, MessageComparator<SimpleClearThMessage> comparator)
	{
		List<Pair<String, String>> outputFields = getOutputFields();
		if (outputFields == null)
		{
			setOutputParams(comparator.getOutputFields());
			setSubOutputParams(comparator.getSubOutputFields());
			return;
		}
		
		for (Pair<String, String> pair : outputFields)
		{
			String field = actualMessage.getField(pair.getSecond());
			addOutputParam(pair.getFirst(), field);
		}
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
}
