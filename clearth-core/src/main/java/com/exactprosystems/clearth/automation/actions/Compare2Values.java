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

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.GlobalContext;
import com.exactprosystems.clearth.automation.MatrixContext;
import com.exactprosystems.clearth.automation.StepContext;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.results.DetailedResult;
import com.exactprosystems.clearth.utils.ComparisonUtils;
import com.exactprosystems.clearth.utils.inputparams.InputParamsHandler;

public class Compare2Values extends Action
{
	private static final String EXPECTED_HEADER = "Expected";
	private static final String ACTUAL_HEADER = "Actual";

	@Override
	protected Result run(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext) throws ResultException 
	{
		InputParamsHandler handler = new InputParamsHandler(inputParams);
		String expected;
		String actual;
		try
		{
			expected = handler.getRequiredString(EXPECTED_HEADER);
			actual = handler.getRequiredString(ACTUAL_HEADER);
		} finally {
			handler.check();
		}

		DetailedResult result = new DetailedResult();
		ComparisonUtils cu = ClearThCore.comparisonUtils();
		result.addResultDetail(cu.createResultDetail("Value", expected, actual));
		return result;
	}
}
