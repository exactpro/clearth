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

package com.exactprosystems.clearth.web.misc;

import com.csvreader.CsvWriter;
import com.exactprosystems.clearth.automation.Step;
import com.exactprosystems.clearth.utils.Utils;
import org.testng.annotations.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class StepUploadHandlerTest
{
	private static final List<Step.StepParams> successfulHeader = new ArrayList<Step.StepParams>()
	{{
		add(Step.StepParams.GLOBAL_STEP);
		add(Step.StepParams.STEP_KIND);
		add(Step.StepParams.START_AT);
		add(Step.StepParams.START_AT_TYPE);
		add(Step.StepParams.WAIT_NEXT_DAY);
		add(Step.StepParams.PARAMETER);
		add(Step.StepParams.ASK_FOR_CONTINUE);
		add(Step.StepParams.ASK_IF_FAILED);
		add(Step.StepParams.EXECUTE);
		add(Step.StepParams.COMMENT);
	}};
	
	@Test
	public void testReadConfigHeader() throws IOException
	{
		File config = null;
		CsvWriter csvWriter = null;
		try
		{
			config = File.createTempFile("step_config_", ".csv");
			csvWriter = new CsvWriter(new BufferedWriter(new FileWriter(config, false)), ',');
			csvWriter.writeRecord(successfulHeader.stream().map(Step.StepParams::getValue).toArray(String[]::new));
			csvWriter.flush();
			
			List<String> undefinedFields = StepUploadHandler.readConfigHeaders(config.getAbsolutePath());
			assertTrue("Undefined fields found: " + undefinedFields, undefinedFields.isEmpty());
		}
		finally
		{
			Utils.closeResource(csvWriter);
			if (config != null)
				Files.delete(config.toPath());
		}
	}
}
