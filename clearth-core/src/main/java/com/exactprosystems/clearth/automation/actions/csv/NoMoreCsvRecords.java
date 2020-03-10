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

package com.exactprosystems.clearth.automation.actions.csv;

import static com.exactprosystems.clearth.automation.actions.csv.LoadDataFromCsvFile.DATA_CONTEXT;
import static com.exactprosystems.clearth.automation.actions.csv.LoadDataFromCsvFile.MATRIXCONTEXT;
import static com.exactprosystems.clearth.automation.actions.csv.LoadDataFromCsvFile.STORED_CSV;

import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.ContextReader;
import com.exactprosystems.clearth.automation.GlobalContext;
import com.exactprosystems.clearth.automation.MatrixContext;
import com.exactprosystems.clearth.automation.StepContext;
import com.exactprosystems.clearth.automation.exceptions.FailoverException;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.results.DefaultResult;
import com.exactprosystems.clearth.utils.tabledata.StringTableData;

public class NoMoreCsvRecords extends Action implements ContextReader {
	@Override
	protected Result run(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext) throws ResultException, FailoverException
	{
		String getFrom = getInputParam(DATA_CONTEXT, MATRIXCONTEXT);
		StringTableData tableData = VerifyCsvRecord.getDataFromContext(stepContext, matrixContext, globalContext, getFrom);
		int uncheckedRecords = tableData.getRows().size();
		if(uncheckedRecords != 0)
		{
			return DefaultResult.failed(uncheckedRecords + " record(s) weren`t checked");
		}
		return DefaultResult.passed("All records were checked");
	}

	@Override
	public String[] readContextNames()
	{
		return new String[] {STORED_CSV};
	}
}
