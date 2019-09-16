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

package com.exactprosystems.clearth.web.beans;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.web.misc.UserInfoUtils;

/**
 * @author daria.plotnikova
 *
 */
public class ClearThBean
{
	private Logger logger;
	
	public ClearThBean()
	{
		initLogger();
	}
	
	public Logger getLogger()
	{
		return logger;
	}
	
	protected void initLogger()
	{
		String loggerName = this.getClass().getName();
		if (UserInfoUtils.isLoggedIn())
			loggerName += " User: "+UserInfoUtils.getUserName();

		setLogger(loggerName);

	}
	
	protected void setLogger(String name)
	{
		logger = LoggerFactory.getLogger(name);
	}
}
