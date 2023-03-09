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

package com.exactprosystems.clearth.automation.actions.json;

import com.exactprosystems.clearth.automation.GlobalContext;
import com.exactprosystems.clearth.automation.MatrixContext;
import com.exactprosystems.clearth.automation.StepContext;
import com.exactprosystems.clearth.automation.actions.SendMessageAction;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.connectivity.json.ClearThJsonMessage;
import com.exactprosystems.clearth.connectivity.json.ClearThJsonMessageBuilder;
import com.exactprosystems.clearth.connectivity.json.JsonCodec;
import com.exactprosystems.clearth.messages.MessageBuilder;
import com.exactprosystems.clearth.utils.inputparams.InputParamsUtils;
import org.apache.commons.lang.StringUtils;

import java.util.Set;

public class SendJsonMessage extends SendMessageAction<ClearThJsonMessage>
{
	public static final String IGNORE_VALIDATION = "IgnoreValidation";
	
	@Override
	public MessageBuilder<ClearThJsonMessage> getMessageBuilder(Set<String> serviceParameters, Set<String> metaFields)
	{
		return new ClearThJsonMessageBuilder(serviceParameters, metaFields);
	}
	
	
	@Override
	protected void beforeSend(ClearThJsonMessage msg, StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext) 
			throws ResultException 
	{
		if (InputParamsUtils.YES.contains(StringUtils.lowerCase(msg.getField(IGNORE_VALIDATION))))
			msg.setValidate(false);
		msg.removeField(IGNORE_VALIDATION);
	}

	@Override
	protected String getDefaultCodecName()
	{
		return JsonCodec.DEFAULT_CODEC_NAME;
	}
}
