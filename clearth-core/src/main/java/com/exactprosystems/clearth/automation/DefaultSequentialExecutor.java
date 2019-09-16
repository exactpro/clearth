/******************************************************************************
 * Copyright 2009-2019 Exactpro Systems Limited
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

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultSequentialExecutor extends SequentialExecutor
{
	private static final Logger logger = LoggerFactory.getLogger(DefaultSequentialExecutor.class);
	
	public DefaultSequentialExecutor(ExecutorFactory executorFactory, Scheduler scheduler, String startedByUser, Map<String, Preparable> preparableActions)
	{
		super(executorFactory, scheduler, startedByUser, preparableActions);
	}
	
	@Override
	protected Logger getLogger()
	{
		return logger;
	}
	
	@Override
	protected void initExecutor(Executor executor)
	{
	}
}
