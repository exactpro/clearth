/******************************************************************************
 * Copyright 2009-2024 Exactpro Systems Limited
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

package com.exactprosystems.clearth.connectivity;

import com.exactprosystems.clearth.connectivity.connections.ConnectionTypeInfo;
import com.exactprosystems.clearth.data.DefaultDataHandlersFactory;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.testng.Assert.assertEquals;

public class BasicClearThConnectionTest
{
	private static final Path OUTPUT_ROOT = Paths.get("testOutput")
			.resolve(BasicClearThConnectionTest.class.getSimpleName()).toAbsolutePath();
	
	@Test
	public void checkCloseConnection() throws Exception
	{
		TestMessageConnection con = new TestMessageConnection(c -> new CountingClearThClient(c));
		con.setTypeInfo(new ConnectionTypeInfo("TestTypeInfo",
				TestMessageConnection.class,
				OUTPUT_ROOT));
		
		con.setName("TestCon");
		con.setDataHandlersFactory(new DefaultDataHandlersFactory());
		
		assertEquals(CountingClearThClient.value, 15);
		try
		{
			//CountingClearThClient throws error from connect() method, thus,
			//connection won't start and must call countingClearThClient.closeConnections(), updating CountingClearThClient.value
			con.start();
		}
		catch (Exception e)
		{
			assertEquals(e.getCause().getMessage(), "value = 50");
		}
		assertEquals(CountingClearThClient.value, 30);
	}
}
