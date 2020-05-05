/******************************************************************************
 * Copyright 2009-2020 Exactpro Systems Limited
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

import com.exactprosystems.clearth.automation.*;
import com.exactprosystems.clearth.automation.exceptions.FailoverException;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.results.DefaultResult;
import com.exactprosystems.clearth.utils.inputparams.InputParamsHandler;
import com.exactprosystems.clearth.utils.tabledata.StringTableData;
import com.exactprosystems.clearth.utils.tabledata.readers.CsvDataReader;
import org.apache.commons.lang.StringUtils;

import java.io.File;

import static com.exactprosystems.clearth.automation.actions.MessageAction.FILENAME;

public class LoadDataFromCsvFile extends Action implements ContextWriter {

	public static final String DATA_CONTEXT = "DataContext",
			STORED_CSV = "CsvRecords",
			GLOBALCONTEXT = "GlobalContext",
			MATRIXCONTEXT = "MatrixContext",
			STEPCONTEXT = "StepContext";

	@Override
	protected Result run(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext) throws ResultException, FailoverException
	{
		InputParamsHandler handler = new InputParamsHandler(inputParams);
		File loadedDataFile;
		try
		{
			loadedDataFile = handler.getRequiredFile(FILENAME);
		}
		finally
		{
			handler.check();
		}

		StringTableData tableData;
		try
		{
			tableData = CsvDataReader.read(loadedDataFile);
		}
		catch (Exception e)
		{
			return DefaultResult.failed("Couldn`t load data from file", e);
		}

		saveDataToContext(tableData, stepContext, matrixContext, globalContext, getInputParam(DATA_CONTEXT, MATRIXCONTEXT));

		return DefaultResult.passed("Data was loaded successfully");
	}

	private void saveDataToContext(StringTableData tableData, StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext, String saveTo) throws ResultException
	{
		if (StringUtils.equalsIgnoreCase(GLOBALCONTEXT, saveTo))
			globalContext.setLoadedContext(STORED_CSV, tableData);
		else if (StringUtils.equalsIgnoreCase(STEPCONTEXT, saveTo))
			stepContext.setContext(STORED_CSV, tableData);
		else
			matrixContext.setContext(STORED_CSV, tableData);
	}

	@Override
	public String[] writtenContextNames()
	{
		return new String[] {STORED_CSV};
	}
}
