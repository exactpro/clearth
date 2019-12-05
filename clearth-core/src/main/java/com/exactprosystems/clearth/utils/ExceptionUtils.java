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

package com.exactprosystems.clearth.utils;

import static com.exactprosystems.clearth.utils.Utils.EOL;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.exception.ExceptionUtils.getThrowableList;

import java.util.List;

public class ExceptionUtils
{
	private static String getRuntimeExceptionMessage(Throwable t)
	{
		String message = t.getClass().getName();
		
		StackTraceElement[] stackTrace = t.getStackTrace();
		if ((stackTrace != null) && (stackTrace.length != 0))
			message += " at " + stackTrace[0];
		
		if (t.getMessage() != null)
			message += ": " + t.getMessage();
		
		return message;
	}
	
	private static String getMessage(Throwable t)
	{
		if (t instanceof RuntimeException)
			return getRuntimeExceptionMessage(t);
		else 
			return (t.getMessage() != null) ? t.getMessage() : "";
	}
	
	private static String joinStackTraceMessages(Throwable cause)
	{
		if (cause == null)
			return "";

		@SuppressWarnings("unchecked") List<Throwable> throwables = getThrowableList(cause);
		if (throwables.size() == 1)
			return getMessage(cause);

		StringBuilder msgBuilder = new StringBuilder();
		for (Throwable t : throwables)
		{
			String msg = getMessage(t);
			if (isEmpty(msg))
				continue;

			if (msgBuilder.indexOf(msg) != -1)
				continue;

			if (msgBuilder.length() > 0)
				msgBuilder.append("Cause: ");
			msgBuilder.append(msg).append(EOL);
		}

		return msgBuilder.toString();
	}
	
	/**
	 * Returns detailed information about given error as one string.
	 * @param e error to get information about
	 * @return messages related to given error and its causes.
	 */
	public static String getDetailedMessage(Throwable e)
	{
		return joinStackTraceMessages(e);
	}
	
	/**
	 * Use {@link #getDetailedMessage(Throwable e) getDetailedMessage} instead
	 */
	@Deprecated
	public static String getExceptionMessage(Exception e)
	{
		return getDetailedMessage(e);
	}
	
	public static String getExceptionMessageDeep(Exception e)
	{
		Throwable current = e;
		while (current != null && current.getMessage() == null)
			current = current.getCause();
		return current == null ? null : current.getMessage();
	}
	
	@SuppressWarnings("rawtypes")
	public static boolean causedBy(Throwable t, Class cause)
	{
		if (cause.isInstance(t))
			return true;
		
		if (t.getCause() == null)
			return false;
		
		return causedBy(t.getCause(), cause);
	}
}
