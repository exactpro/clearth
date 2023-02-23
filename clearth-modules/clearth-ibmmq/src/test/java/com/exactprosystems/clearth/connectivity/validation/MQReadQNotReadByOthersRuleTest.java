/*******************************************************************************
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

package com.exactprosystems.clearth.connectivity.validation;

import com.exactprosystems.clearth.BasicIbmMqTest;
import com.exactprosystems.clearth.connectivity.ibmmq.IbmMqConnection;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.*;

public class MQReadQNotReadByOthersRuleTest extends BasicIbmMqTest
{
	private IbmMqReadQNotReadByOthersRule rule = new IbmMqReadQNotReadByOthersRule();
	@DataProvider(name = "invalidData")
	public Object[][] createInvalidData() {
		return new Object[][]
				{
						{
								createConnection("MQConnToCheck",
								                 "1.2.3.4",
								                 1234,
								                 "MI_MANAGER",
								                 "MI_QUEUE",
								                 true),
								"Can't start connection 'MQConnToCheck' that reads the same queue " +
										"as 'MQConn' (receiveQueue = 'MI_QUEUE')."
						}
				};
	}

	@DataProvider(name = "validData")
	public Object[][] createValidData() {
		return new Object[][]
				{
						{
								createConnection("MQConnToCheck",
								                 "1.2.3.4",
								                 1234,
								                 "MI_MANAGER",
								                 "SOME_QUEUE",
								                 true)
						},
						{
								createConnection("MQConnToCheck",
								                 "1.2.3.4",
								                 1234,
								                 "SOME_MANAGER",
								                 "MI_QUEUE",
								                 true)
						},
						{
								createConnection("MQConnToCheck",
								                 "5.6.7.8",
								                 1234,
								                 "MI_MANAGER",
								                 "MI_QUEUE",
								                 true)
						},
						{
								createConnection("MQConnToCheck",
								                 "1.2.3.4",
								                 12345,
								                 "MI_MANAGER",
								                 "MI_QUEUE",
								                 true)
						},
						{
								createConnection("MQConnToCheck",
								                 "1.2.3.4",
								                 1234,
								                 "MI_MANAGER",
								                 "MI_QUEUE",
								                 false)
						}
				};
	}

	@Test(dataProvider = "invalidData")
	public void checkConnectionWithConflicts(IbmMqConnection connection, String expectedErrorMessage)
	{
		assertTrue(rule.isConnectionSuitable(connection));
		assertEquals(rule.check(connection), expectedErrorMessage);
	}

	@Test(dataProvider = "validData")
	public void checkValidConnection(IbmMqConnection connection)
	{
		assertTrue(rule.isConnectionSuitable(connection));
		assertNull(rule.check(connection));
	}
}
