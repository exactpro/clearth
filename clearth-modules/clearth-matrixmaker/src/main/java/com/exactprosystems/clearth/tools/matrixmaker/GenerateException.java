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

package com.exactprosystems.clearth.tools.matrixmaker;

/**
 * Created by victor.akimov on 2/20/17.
 */
public class GenerateException extends Exception
{
	public GenerateException(String message)
	{
		super(message);
	}

	public GenerateException(Throwable cause)
	{
		super(cause);
	}

	public GenerateException(String message, Throwable cause)
	{
		super(message, cause);
	}
	
	public static String createTemplateExceptionMessage(String templateName, Integer rowNumber, Integer columnNumber, Exception e)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(templateName);
		if (rowNumber != null)
		{
			sb.append(" (row: " + rowNumber);
			if (columnNumber != null)
				sb.append(", column: " + columnNumber);
			sb.append(")");
		}
		if (e!= null && e.getMessage() != null)
			sb.append(": ").append(e.getMessage());
		return (sb.toString());
	}
}
