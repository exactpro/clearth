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

import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.GlobalContext;
import com.exactprosystems.clearth.automation.MatrixContext;
import com.exactprosystems.clearth.automation.StepContext;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.results.DefaultResult;
import com.exactprosystems.clearth.utils.csv.readers.ClearThCsvReader;
import com.exactprosystems.clearth.utils.csv.readers.ClearThCsvReaderConfig;
import com.exactprosystems.clearth.utils.inputparams.InputParamsHandler;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import static com.exactprosystems.clearth.ClearThCore.rootRelative;

public class GetCsvLine extends Action
{
	private final String FILE_NAME = "FileName";

	private Set<String> removeInvalidSymbols(Set<String> header)
	{
		Set<String> clearHeader = new LinkedHashSet<>();
		for (String s :header)
			clearHeader.add(s.replaceAll("[^\\w$]", ""));

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

		try (ClearThCsvReader reader = new ClearThCsvReader(rootRelative(fileName), createCsvReaderConfig()))
		{
			if (!reader.hasHeader())
				throw new IOException("File does not have header");

			Set<String> header = reader.getHeader(),
					cleanedHeader = removeInvalidSymbols(header);

			while (reader.hasNext())
			{
				boolean isFound = false;
				LinkedHashMap<String, String> currentRecord = new LinkedHashMap<String, String>();
				Iterator<String> headerForMatrix = cleanedHeader.iterator();

				for (String fieldName : header)
				{
					String paramValue = reader.get(fieldName),
							headerParamForOutput = headerForMatrix.next(),
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

	private ClearThCsvReaderConfig createCsvReaderConfig()
	{
		ClearThCsvReaderConfig config = new ClearThCsvReaderConfig();
		config.setSkipEmptyRecords(false);
		config.setIgnoreSurroundingSpaces(true);
		config.setFirstLineAsHeader(true);
		return config;
	}
}
