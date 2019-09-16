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

package com.exactprosystems.clearth.automation.actions.xls;

import java.util.Map.Entry;

import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.GlobalContext;
import com.exactprosystems.clearth.automation.MatrixContext;
import com.exactprosystems.clearth.automation.StepContext;
import com.exactprosystems.clearth.automation.exceptions.FailoverException;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.results.DetailedResult;
import com.exactprosystems.clearth.utils.ComparisonUtils;
import com.exactprosystems.clearth.utils.inputparams.InputParamsUtils;
import com.exactprosystems.clearth.utils.multidata.MultiStringData;
import com.exactprosystems.clearth.utils.multidata.MultiRowStringData;

import static com.exactprosystems.clearth.ClearThCore.comparisonUtils;

public class VerifyXlsRow extends Action
{
	public static final String PARAM_ROWINDEX = "RowIndex";
	
	@Override
	protected Result run(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext) throws ResultException, FailoverException
	{
		int rowIndex = InputParamsUtils.getRequiredInt(getInputParams(), PARAM_ROWINDEX)-1;
		
		MultiRowStringData data = getStoredData(stepContext, matrixContext, globalContext);
		MultiStringData row = getRow(data, rowIndex);
		
		Result result = verifyRow(row, rowIndex, data);
		storeVerifiedRowData(row, result, stepContext, matrixContext, globalContext);
		return result;
	}
	
	protected MultiRowStringData getStoredData(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext) throws ResultException
	{
		return XlsUtils.getStoredRowsData(matrixContext);
	}
	
	protected MultiStringData getRow(MultiRowStringData data, int rowIndex)
	{
		return data.getData(rowIndex);
	}
	
	
	protected boolean isServiceParameter(String paramName)
	{
		return PARAM_ROWINDEX.equals(paramName);
	}
	
	protected Result verifyRow(MultiStringData row, int rowIndex, MultiRowStringData data) throws ResultException
	{
		DetailedResult result = new DetailedResult();
		for (Entry<String, String> param : getInputParams().entrySet())
		{
			String paramName = param.getKey();
			if (isServiceParameter(paramName))
				continue;
			
			int cellIndex = XlsUtils.nameToCellIndex(paramName);
			result.addResultDetail(comparisonUtils().createResultDetail(paramName, 
					param.getValue(), row == null ? null : row.getData(cellIndex)));
		}
		return result;
	}
	
	protected void storeVerifiedRowData(MultiStringData row, Result result, 
			StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext)
	{
		XlsUtils.storeVerifiedRowData(row, matrixContext);
	}
}
