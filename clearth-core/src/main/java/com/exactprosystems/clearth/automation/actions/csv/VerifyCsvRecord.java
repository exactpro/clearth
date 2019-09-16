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

import static com.exactprosystems.clearth.ClearThCore.comparisonUtils;
import static com.exactprosystems.clearth.automation.actions.csv.LoadDataFromCsvFile.*;
import static com.exactprosystems.clearth.automation.actions.csv.LoadDataFromCsvFile.DATA_CONTEXT;
import static com.exactprosystems.clearth.automation.actions.csv.LoadDataFromCsvFile.MATRIXCONTEXT;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.ContextReader;
import com.exactprosystems.clearth.automation.GlobalContext;
import com.exactprosystems.clearth.automation.MatrixContext;
import com.exactprosystems.clearth.automation.StepContext;
import com.exactprosystems.clearth.automation.exceptions.FailoverException;
import com.exactprosystems.clearth.automation.exceptions.ParametersException;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.ResultDetail;
import com.exactprosystems.clearth.automation.report.results.DetailedResult;
import com.exactprosystems.clearth.utils.inputparams.InputParamsHandler;
import com.exactprosystems.clearth.utils.tabledata.StringTableData;
import com.exactprosystems.clearth.utils.tabledata.TableRow;

public class VerifyCsvRecord extends Action implements ContextReader
{
	private static final String KEY_FIELDS = "KeyFields";
	
	protected static StringTableData getDataFromContext(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext, String getFrom)
	{
		StringTableData tableData;
		if (StringUtils.equalsIgnoreCase(GLOBALCONTEXT, getFrom))
			tableData = (StringTableData) globalContext.getLoadedContext(STORED_CSV);
		else if (StringUtils.equalsIgnoreCase(MATRIXCONTEXT, getFrom))
			tableData = (StringTableData) matrixContext.getContext(STORED_CSV);
		else
			tableData = (StringTableData) stepContext.getContext(STORED_CSV);
		
		if (tableData == null)
			throw ResultException.failed("Data was not loaded. Use " + LoadDataFromCsvFile.class.getSimpleName() + "-like action before");
		
		return tableData;
	}

	@Override
	protected Result run(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext) throws ResultException, FailoverException
	{
		StringTableData tableData = getData(stepContext, matrixContext, globalContext, getInputParam(DATA_CONTEXT, MATRIXCONTEXT));
		Map<String, String> keys = getKeyFields(getInputParams());
		TableRow<String, String> neededTableRow = findRow(tableData, keys);
		Result r = verifyRow(neededTableRow);
		if (keys.isEmpty())
			r.setMessage("No "+KEY_FIELDS+" parameter specified, verified first available row");
		return r;
	}

	@Override
	public String[] readContextNames()
	{
		return new String[] {STORED_CSV};
	}
	
	
	protected StringTableData getData(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext, String getFrom)
	{
		return getDataFromContext(stepContext, matrixContext, globalContext, getFrom);
	}
	
	protected boolean isKeysMatches(TableRow<String, String> tableRow, Map<String, String> keys)
	{
		for (String keyName : keys.keySet())
		{
			try
			{
				if (!comparisonUtils().compareValues(keys.get(keyName), tableRow.getValue(keyName)))
					return false;
			} catch (ParametersException e)
			{
				throw ResultException.failed(String.format("Error while checking key field '%s': %s", keyName, e.getMessage()));
			}
		}
		return true;
	}

	protected Map<String, String> getKeyFields(Map<String, String> inputParams)
	{
		Set<String> keyNames = new InputParamsHandler(inputParams).getSet(KEY_FIELDS, ",");
		Map<String, String> keyMap = new HashMap<String, String>();
		for (String keyName : keyNames)
		{
			String value = inputParams.get(keyName);
			if (value == null)
				throw ResultException.failed("Key field '"+keyName+"' is absent in action parameters");
			keyMap.put(keyName, value);
		}
		return keyMap;
	}
	
	protected TableRow<String, String> findRow(StringTableData data, Map<String, String> keys)
	{
		Iterator<TableRow<String, String>> it = data.iterator();
		while (it.hasNext())
		{
			TableRow<String, String> r = it.next();
			if (isKeysMatches(r, keys))
			{
				TableRow<String, String> foundRow = r;
				it.remove();
				return foundRow;
			}
		}
		throw ResultException.failed("No record found by keys: " + keys);
	}
	
	protected Result verifyRow(TableRow<String, String> row)
	{
		DetailedResult result = new DetailedResult();
		for (String paramName : getInputParams().keySet())
		{
			if (KEY_FIELDS.equals(paramName) || DATA_CONTEXT.equals(paramName))
				continue;
			
			ResultDetail detail = comparisonUtils().createResultDetail(paramName, inputParams.get(paramName), row.getValue(paramName));
			result.addResultDetail(detail);
		}
		return result;
	}
}
