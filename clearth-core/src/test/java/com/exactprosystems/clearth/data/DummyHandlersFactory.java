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

package com.exactprosystems.clearth.data;

import java.util.HashMap;
import java.util.Map;

public class DummyHandlersFactory implements DataHandlersFactory
{
	private final Map<String, DummyMessageHandler> messageHandlersByConName = new HashMap<>();
	private final Map<String, DummyTestExecutionHandler> testHandlersByScheduler = new HashMap<>();
	private boolean createActiveHandlers = true;
	
	@Override
	public void close() throws Exception
	{
	}
	
	@Override
	public MessageHandler createMessageHandler(String connectionName) throws DataHandlingException
	{
		DummyMessageHandler result = new DummyMessageHandler(createActiveHandlers);
		messageHandlersByConName.put(connectionName, result);
		return result;
	}
	
	@Override
	public TestExecutionHandler createTestExecutionHandler(String schedulerName) throws DataHandlingException
	{
		DummyTestExecutionHandler result = new DummyTestExecutionHandler(createActiveHandlers);
		testHandlersByScheduler.put(schedulerName, result);
		return result;
	}
	
	
	public DummyMessageHandler getMessageHandler(String connectionName)
	{
		return messageHandlersByConName.get(connectionName);
	}
	
	public DummyTestExecutionHandler getTestExecutionHandler(String schedulerName)
	{
		return testHandlersByScheduler.get(schedulerName);
	}
	
	
	public boolean isCreateActiveHandlers()
	{
		return createActiveHandlers;
	}
	
	public void setCreateActiveHandlers(boolean createActiveHandlers)
	{
		this.createActiveHandlers = createActiveHandlers;
	}
}
