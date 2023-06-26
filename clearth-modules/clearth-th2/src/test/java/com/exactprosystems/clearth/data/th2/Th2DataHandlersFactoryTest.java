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

package com.exactprosystems.clearth.data.th2;

import java.nio.file.Paths;

import org.testng.annotations.Test;

import com.exactprosystems.clearth.data.DataHandlersFactory;
import com.exactprosystems.clearth.utils.ClearThException;

public class Th2DataHandlersFactoryTest
{
	@Test(description = "Tests that factory is instantiated if all required files are in place")
	public void goodConfig() throws Exception
	{
		try (DataHandlersFactory factory = new Th2DataHandlersFactory(Paths.get("src", "test", "resources", "goodConfig")))
		{
		}
	}
	
	@Test(expectedExceptions = ClearThException.class, expectedExceptionsMessageRegExp = ".*storage\\.json.*")
	public void noStorageConfig() throws Exception
	{
		try (DataHandlersFactory factory = new Th2DataHandlersFactory(Paths.get("src", "test", "resources", "noStorageConfig")))
		{
		}
	}
	
	@Test(expectedExceptions = ClearThException.class, expectedExceptionsMessageRegExp = ".*rabbitMQ\\.json.*")
	public void noRabbitConfig() throws Exception
	{
		try (DataHandlersFactory factory = new Th2DataHandlersFactory(Paths.get("src", "test", "resources", "noRabbitConfig")))
		{
		}
	}
}
