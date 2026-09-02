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

package com.exactprosystems.clearth.woodpecker.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static java.lang.String.format;

public class WoodpeckerDaemonException extends WoodpeckerException
{
	private static final Logger log = LoggerFactory.getLogger(WoodpeckerDaemonException.class);

	public WoodpeckerDaemonException(String message)
	{
		super(message);
	}

	public WoodpeckerDaemonException(String message, Throwable cause)
	{
		super(message, cause);
	}
	
	public WoodpeckerDaemonException(String messageTemplate, Object... templateParams)
	{
		super(format(messageTemplate, templateParams));
	}
	
	public WoodpeckerDaemonException(Throwable cause, String messageTemplate, Object... templateParams)
	{
		super(format(messageTemplate, templateParams), cause);
	}
	
	public static WoodpeckerDaemonException fromIOException(IOException e, String messageTemplate, Object ... templateParams)
	{
		return new WoodpeckerDaemonException(messageFromIOException(e, messageTemplate, templateParams), e);
	}
	
	public static WoodpeckerDaemonException fromOtherException(Throwable cause, String messageTemplate,
	                                                           Object... templateParams)
	{
		String message = format(messageTemplate, templateParams);
		if (cause instanceof RuntimeException)
		{
			log.error(message, cause); // Let's log it here as well to avoid missing of stacktrace
			return new WoodpeckerDaemonException(format("%s: Unexpected error (%s)", 
					message, cause.getClass().getName()), cause);
		}
		else 
			return new WoodpeckerDaemonException(message, cause);
	}
}
