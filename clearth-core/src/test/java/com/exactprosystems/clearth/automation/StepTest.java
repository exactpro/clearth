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

package com.exactprosystems.clearth.automation;

import com.exactprosystems.clearth.utils.csv.readers.ClearThCsvReader;
import com.exactprosystems.clearth.utils.csv.writers.ClearThCsvWriter;
import com.exactprosystems.clearth.utils.csv.writers.ClearThCsvWriterConfig;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import static com.exactprosystems.clearth.utils.csv.readers.ClearThCsvReaderConfig.withFirstLineAsHeader;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class StepTest
{
	private static final String DATA_CHECK_WRITER = "Step1,Default,,End of previous step,0,,0,0,1,",
								DATA_CHECK_READER = "Global step,Step kind,Start at,Start at type,Wait next day," +
										"Parameter,Ask for continue,Ask if failed,Execute,Comment\n" + DATA_CHECK_WRITER;

	@Test
	public void testCreateStepFromCsvFile() throws IOException
	{
		try (ClearThCsvReader reader = createReader())
		{
			assertTrue(reader.hasNext());
			Step actualStep = new DefaultStep(reader.getRecord()),
				expectedStep = createExpectedStep();
			assertThat(actualStep).usingRecursiveComparison().isEqualTo(expectedStep);
		}
	}

	@Test
	public void testSaveStepWithCsvWriter() throws IOException
	{
		Step step = createExpectedStep();
		StringWriter stringWriter = new StringWriter();

		try (ClearThCsvWriter writer = new ClearThCsvWriter(stringWriter, createCsvWriterConfig()))
		{
			step.save(writer);
			assertEquals(stringWriter.toString(), DATA_CHECK_WRITER);
		}
	}

	@Test
	public void testCreateStepFromStepFactory() throws IOException
	{
		StepFactory stepFactory = new DefaultStepFactory();
		try (ClearThCsvReader reader = createReader())
		{
			assertTrue(reader.hasNext());
			Step step = stepFactory.createStep(reader.getRecord());
			assertThat(step).usingRecursiveComparison().isEqualTo(createExpectedStep());
		}
	}

	private ClearThCsvWriterConfig createCsvWriterConfig()
	{
		ClearThCsvWriterConfig config = new ClearThCsvWriterConfig();
		config.setWithTrim(false);
		config.setDelimiter(',');
		return config;
	}

	private ClearThCsvReader createReader() throws IOException
	{
		return new ClearThCsvReader(new StringReader(DATA_CHECK_READER), withFirstLineAsHeader());
	}

	private Step createExpectedStep()
	{
		return new DefaultStep("Step1","Default", "",
				StartAtType.END_STEP, false, "",
				false, false, true, "");
	}
}