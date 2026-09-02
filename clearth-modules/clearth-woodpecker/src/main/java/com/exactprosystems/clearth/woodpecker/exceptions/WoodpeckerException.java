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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.NoSuchFileException;

import static java.lang.String.format;

public class WoodpeckerException extends Exception
{
	public WoodpeckerException()
	{
		super();
	}

	public WoodpeckerException(String message)
	{
		super(message);
	}

	public WoodpeckerException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public WoodpeckerException(Throwable cause)
	{
		super(cause);
	}

	protected static String messageFromIOException(IOException e, String messageTemplate, Object ... templateParams)
	{
		String message = format(messageTemplate, templateParams);
		if ((e instanceof FileNotFoundException) || (e instanceof NoSuchFileException))
		{
			String fileName = e.getMessage();
			if (message.contains(fileName))
				message += ": file not found";
			else
				message += format(": file '%s' not found", fileName);
		}
		else if (e.getMessage() != null)
			message += ": " + e.getMessage();
		return message;
	}
}
