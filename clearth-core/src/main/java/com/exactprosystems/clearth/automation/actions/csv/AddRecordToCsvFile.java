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

package com.exactprosystems.clearth.automation.actions.csv;

import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.GlobalContext;
import com.exactprosystems.clearth.automation.MatrixContext;
import com.exactprosystems.clearth.automation.StepContext;
import com.exactprosystems.clearth.automation.exceptions.FailoverException;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.results.DefaultResult;
import com.exactprosystems.clearth.utils.csv.readers.ClearThCsvReader;
import com.exactprosystems.clearth.utils.csv.readers.ClearThCsvReaderConfig;
import com.exactprosystems.clearth.utils.csv.writers.ClearThCsvWriter;
import com.exactprosystems.clearth.utils.inputparams.InputParamsHandler;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import static com.exactprosystems.clearth.automation.actions.MessageAction.FILENAME;

public class AddRecordToCsvFile extends Action {

	private static final String APPEND_DATA = "AppendData";
	private static final String ADD_HEADER = "AddHeader";
	private static final String ADD_ACCORDING_TO_HEADER = "AddAccordingToHeader";
	private static final Set<String> SERVICE_PARAMETERS =
			new HashSet<String>(Arrays.asList(FILENAME, APPEND_DATA, ADD_HEADER, ADD_ACCORDING_TO_HEADER));

	@Override
	protected Result run(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext) throws ResultException, FailoverException
	{
		InputParamsHandler handler = new InputParamsHandler(inputParams);
		File f;
		boolean append,
				addHeader,
				addAccordingToHeader;
		try
		{
			f = handler.getRequiredFile(FILENAME);
			append = handler.getBoolean(APPEND_DATA, true);
			addHeader = handler.getBoolean(ADD_HEADER, !f.isFile());  //Adding header to new files by default
			addAccordingToHeader = handler.getBoolean(ADD_ACCORDING_TO_HEADER, true);
		}
		finally
		{
			handler.check();
		}

		if (f.isFile() && (f.length() != 0) && append && addHeader)
			return DefaultResult.failed("Inconsistent parameters - unable to add header to existing non-empty file.");

		if ((!append || !f.isFile() || (f.isFile() && f.length() == 0)) && !addHeader && addAccordingToHeader)
			return DefaultResult.failed("Inconsistent parameters - unable to add data according to header without header in file.");

		try (ClearThCsvWriter csvWriter = new ClearThCsvWriter(new FileWriter(f, append)))
		{
			if (addHeader)
			{
				for (String param : inputParams.keySet())
				{
					if (!SERVICE_PARAMETERS.contains(param))
						csvWriter.write(param);
				}
				csvWriter.endRecord();
				csvWriter.flush();
			}

			Set<String> inputWithoutServiceParams = new LinkedHashSet<>(inputParams.keySet());
			inputWithoutServiceParams.removeAll(SERVICE_PARAMETERS);

			if (addAccordingToHeader)
			{
				Set<String> header = readHeaderFromFile(f);

				if (!header.containsAll(inputWithoutServiceParams))
					return DefaultResult.failed("Input parameters do not match the header in file.");

				for (String param : header)
					csvWriter.write(inputParams.get(param));
			}
			else
			{
				for (String param : inputWithoutServiceParams)
					csvWriter.write(inputParams.get(param));
			}
			csvWriter.endRecord();
		}
		catch (IOException e)
		{
			return DefaultResult.failed("Error while writing data", e);
		}

		return null;
	}

	protected Set<String> readHeaderFromFile(File f) throws IOException
	{
		try (ClearThCsvReader csvReader = new ClearThCsvReader(new FileReader(f), createCsvReaderConfig()))
		{
			if (!csvReader.hasHeader())
				throw new ResultException("Could not read header.");

			return csvReader.getHeader();
		}
	}

	protected ClearThCsvReaderConfig createCsvReaderConfig()
	{
		ClearThCsvReaderConfig config = new ClearThCsvReaderConfig();
		config.setFirstLineAsHeader(true);
		return config;
	}
}
