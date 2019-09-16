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

import java.util.Set;

import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.GlobalContext;
import com.exactprosystems.clearth.automation.MatrixContext;
import com.exactprosystems.clearth.automation.StepContext;
import com.exactprosystems.clearth.automation.exceptions.FailoverException;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.ResultDetail;
import com.exactprosystems.clearth.automation.report.results.MultiDetailedResult;
import com.exactprosystems.clearth.utils.multidata.MultiRowStringData;
import com.exactprosystems.clearth.utils.multidata.MultiStringData;

public class NoMoreXlsRows extends Action
{
	@Override
	protected Result run(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext) throws ResultException, FailoverException
	{
		MultiRowStringData data = getStoredData(stepContext, matrixContext, globalContext);
		Set<MultiStringData> verifiedRows = getVerifiedRowsData(stepContext, matrixContext, globalContext);
		
		Result result = verifyNoMoreData(data, verifiedRows);
		resetStoredData(stepContext, matrixContext, globalContext);
		return result;
	}
	
	protected MultiRowStringData getStoredData(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext) throws ResultException
	{
		return XlsUtils.getStoredRowsData(matrixContext);
	}
	
	protected Set<MultiStringData> getVerifiedRowsData(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext)
	{
		return XlsUtils.getVerifiedRowsData(matrixContext);
	}
	
	protected Result verifyNoMoreData(MultiRowStringData data, Set<MultiStringData> verifiedRows)
	{
		MultiDetailedResult result = new MultiDetailedResult();
		result.setComment(verifiedRows.size()+"/"+data.getData().size()+" verified");
		int rowIndex = 0;
		for (MultiStringData row : data.getData())
		{
			rowIndex++;
			if (verifiedRows.contains(row))
				continue;
			
			result.startNewBlock("Row #"+rowIndex);
			for (int i = row.getFirstIndex(); i < row.getData().size()+row.getFirstIndex(); i++)
				result.addResultDetail(new ResultDetail(XlsUtils.cellIndexToName(i), "", row.getData().get(i), false));
		}
		return result;
	}
	
	protected void resetStoredData(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext)
	{
		XlsUtils.storeRowsData(null, matrixContext);
		XlsUtils.storeVerifiedRowsData(null, matrixContext);
	}
}
