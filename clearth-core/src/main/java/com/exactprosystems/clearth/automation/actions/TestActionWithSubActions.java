/******************************************************************************
 * Copyright (c) 2009-2020, Exactpro Systems LLC
 * www.exactpro.com
 * Build Software to Test Software
 *
 * All rights reserved.
 * This is unpublished, licensed software, confidential and proprietary 
 * information which is the property of Exactpro Systems LLC or its licensors.
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
		MessageBuilder<SimpleClearThMessage> builder = new SimpleClearThMessageBuilder(Collections.singleton(REPEATINGGROUPS));
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
