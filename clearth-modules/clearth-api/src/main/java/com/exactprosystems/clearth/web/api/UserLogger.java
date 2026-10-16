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

package com.exactprosystems.clearth.web.api;

import org.slf4j.Logger;

public class UserLogger
{
	public static final String USER_LINE = "User: %s - ";
	private final Logger logger;
	
	public UserLogger(Logger logger)
	{
		this.logger = logger;
	}
	
	public Logger getLogger()
	{
		return logger;
	}
	
	public void trace(String userName, String message, Object... args)
	{
		if (logger.isTraceEnabled())
			logger.trace(createMessage(userName, message), args);
	}
	
	public void debug(String userName, String message, Object... args)
	{
		if (logger.isDebugEnabled())
			logger.debug(createMessage(userName, message), args);
	}
	
	public void info(String userName, String message, Object... args)
	{
		if (logger.isInfoEnabled())
			logger.info(createMessage(userName, message), args);
	}
	
	public void warn(String userName, String message, Object... args)
	{
		if (logger.isWarnEnabled())
			logger.warn(createMessage(userName, message), args);
	}
	
	public void error(String userName, String message, Object... args)
	{
		if (logger.isErrorEnabled())
			logger.error(createMessage(userName, message), args);
	}
	
	
	private String createMessage(String userName, String message)
	{
		return String.format(USER_LINE, userName) + message;
	}
}
