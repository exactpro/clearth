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

import com.csvreader.CsvReader;
import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.GlobalContext;
import com.exactprosystems.clearth.automation.MatrixContext;
import com.exactprosystems.clearth.automation.StepContext;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.results.DefaultResult;
import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.utils.inputparams.InputParamsHandler;
import org.apache.commons.lang.StringUtils;

import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;

import static com.exactprosystems.clearth.ClearThCore.rootRelative;

public class GetCsvLine extends Action
{
	private final String FILE_NAME = "FileName";

	private String[] removeInvalidSymbols(String[] header)
	{
		String[] clearHeader = new String[header.length];
		for (int i = 0; i < header.length; i++)
			clearHeader [i] = header[i].replaceAll("[^\\w$]", "");

		return clearHeader;
	}

	protected LinkedHashMap<String, String> prepareOutputParams(LinkedHashMap<String, String> params)
	{
		// Nothing to do by default
		return params;
	}

	@Override
	protected Result run(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext) throws ResultException
	{
		InputParamsHandler paramsHandler = new InputParamsHandler(inputParams);
		String fileName = paramsHandler.getRequiredString(FILE_NAME);
		paramsHandler.check();

		int foundLinesCount = 0;
		LinkedHashMap<String, String> foundLine = new LinkedHashMap<String, String>();

		CsvReader reader = null;
		try
		{
			reader = new CsvReader(new FileReader(rootRelative(fileName)));
			reader.setSkipEmptyRecords(false);
			reader.setTrimWhitespace(true);

			reader.readHeaders();
			String[] header = reader.getHeaders(),
					headerForMatrix = removeInvalidSymbols(header);

			while (reader.readRecord())
			{
				boolean isFound = false;
				LinkedHashMap<String, String> currentRecord = new LinkedHashMap<String, String>();

				for (int i = 0; i < header.length; i++)
				{
					String paramValue = reader.get(header[i]),
							headerParamForOutput = headerForMatrix[i],
							keyValue = paramsHandler.getString(headerParamForOutput);
					currentRecord.put(headerParamForOutput, paramValue);

					if (StringUtils.isNotBlank(keyValue))
					{
						if (keyValue.equalsIgnoreCase(paramValue))
							isFound = true;
						else
						{
							isFound = false;
							break;
						}
					}
				}

				if (isFound)
				{
					foundLinesCount++;
					if (foundLinesCount == 1)
						foundLine.putAll(currentRecord);
				}
			}
		}
		catch (IOException e)
		{
			return DefaultResult.failed("Error while reading file", e);
		}
		finally
		{
			Utils.closeResource(reader);
		}

		Result result;

		if (!foundLine.isEmpty())
		{
			setOutputParams(prepareOutputParams(foundLine));
			result = DefaultResult.passed("Line found");

			if (foundLinesCount > 1)
				result.setMessage(foundLinesCount + " lines found for defined key fields");
		}
		else
		{
			result = DefaultResult.failed("Line not found");
		}

		return result;
	}
}
