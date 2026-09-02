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

import java.io.IOException;

import static java.lang.String.format;

public class WoodpeckerBlackListException extends WoodpeckerException
{
	public WoodpeckerBlackListException(String message)
	{
		super(message);
	}
	
	public WoodpeckerBlackListException(String messageTemplate, Object... templateArgs)
	{
		this(format(messageTemplate, templateArgs));
	}

	public WoodpeckerBlackListException(String message, Throwable cause)
	{
		super(message, cause);
	}
	
	public WoodpeckerBlackListException(Throwable cause, String messageTemplate, Object... templateArgs)
	{
		super(format(messageTemplate, templateArgs), cause);
	}
	

	public static WoodpeckerBlackListException fromIOException(IOException cause, 
	                                                           String messageTemplate,
	                                                           Object... templateArgs)
	{
		return new WoodpeckerBlackListException(messageFromIOException(cause, messageTemplate, templateArgs), cause);
	}
}
