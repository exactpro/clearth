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

package com.exactprosystems.clearth;

import com.exactprosystems.clearth.utils.ClearThException;
import com.exactprosystems.clearth.utils.FileOperationUtils;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;

import static com.exactprosystems.clearth.utils.CollectionUtils.map;
import static org.testng.Assert.assertEquals;

public class GlobalConstantsTest
{
	private Path resDir, res_file;
	@BeforeClass
	public void init() throws FileNotFoundException, ClearThException
	{
		resDir = Path.of(FileOperationUtils.resourceToAbsoluteFilePath(GlobalConstantsTest.class.getSimpleName()));
		res_file = resDir.resolve("global_constants.cfg");
	}
	
	@Test
	public void testGlobalConstants() throws IOException
	{
		GlobalConstants constants = new GlobalConstants(res_file);
		assertEquals(constants.getAll(), map("env_name", "QA_55", "system_reports_dir", "/opt/systemX/reports/"));
		assertEquals(constants.get("env_name"), "QA_55");
	}
}