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

import com.exactprosystems.clearth.automation.*;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.results.DetailedResult;
import com.exactprosystems.clearth.connectivity.iface.SimpleClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.SimpleClearThMessageBuilder;
import com.exactprosystems.clearth.messages.MessageBuilder;
import com.exactprosystems.clearth.utils.ComparisonUtils;

import java.util.*;

import static com.exactprosystems.clearth.ClearThCore.comparisonUtils;
import static com.exactprosystems.clearth.automation.actions.MessageAction.REPEATINGGROUPS;

public class TestActionWithSubActions extends Action
{
	@Override
	protected Result run(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext)
	{
		MessageBuilder<SimpleClearThMessage> builder = new SimpleClearThMessageBuilder(Collections.singleton(REPEATINGGROUPS), null);
		SimpleClearThMessage message = builder
				.fields(getInputParams())
				.rgs(matrixContext, this)
				.build();
		
		DetailedResult result = new DetailedResult();
		for (Map.Entry<String, String> fields : message.getFields().entrySet())
		{
			String param = fields.getKey();
			String actual = fields.getValue();
			result.addResultDetail(comparisonUtils().createResultDetail(param, null, actual, 
					ComparisonUtils.InfoIndication.NULL));
			addOutputParam(param, actual);
		}
		return result;
	}
}
