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

package com.exactprosystems.clearth.newproject;

import java.util.Collections;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.utils.ClearThException;

public class Application extends ClearThCore
{
	private static final Logger logger = LoggerFactory.getLogger(Application.class);
	
	public Application() throws ClearThException
	{
		super();
	}

	@Override
	protected Logger getLogger()
	{
		return logger;
	}
	
	@Override
	protected void createDirs() throws Exception
	{
	}

	@Override
	protected void initOtherEntities(Object... otherEntities) throws Exception
	{
	}

	@Override
	public Map<String, Object> getAdditionalTemplateParams()
	{
		return Collections.EMPTY_MAP;
	}
}
