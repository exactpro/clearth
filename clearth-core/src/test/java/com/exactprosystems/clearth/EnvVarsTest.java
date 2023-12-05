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
import org.testng.annotations.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import static com.exactprosystems.clearth.utils.CollectionUtils.map;
import static org.testng.Assert.assertEquals;

public class EnvVarsTest
{
	private Path res_dir, res_file;
	private static final String PATH = "PATH";
	
	@BeforeClass
	public void init() throws ClearThException, IOException
	{
		res_dir = Paths.get(FileOperationUtils.resourceToAbsoluteFilePath(EnvVarsTest.class.getSimpleName()));
		res_file = res_dir.resolve("env_variables.cfg");
	}
	
	@Test
	public void testGetVars() throws IOException
	{
		EnvVars envVars = new EnvVars(res_file);
		//SYSTEM_DIR is present in file, but is absent in environment variables, so it won't be added to envVars
		assertEquals(envVars.getMap(), map(PATH, System.getenv(PATH)));
		assertEquals(envVars.getNames(), Set.of(PATH));
	}
}