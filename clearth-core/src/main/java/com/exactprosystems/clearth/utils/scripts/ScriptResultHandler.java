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

package com.exactprosystems.clearth.utils.scripts;

import static org.apache.commons.lang.time.DurationFormatUtils.*;

import java.io.ByteArrayOutputStream;

import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteResultHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.utils.LineBuilder;

/**
 * Implementation of 'ExecuteResultHandler' used for asynchronous
 * process handling.
 *
 */

public class ScriptResultHandler implements ExecuteResultHandler
{

	protected static final Logger logger = LoggerFactory.getLogger(ScriptResultHandler.class);

	/** Time when handler was created */
	private final long startTime;

	/** Output of script execution*/
	private final ByteArrayOutputStream outWriter;

	/** Error of script execution */
	private final ByteArrayOutputStream errWriter;

	/** Message for completed process*/
	private String messageComplete;

	/** Message for failed process*/
	private String messageFail;

	/**
	 * Constructor.
	 */
	public ScriptResultHandler(ByteArrayOutputStream outWriter, ByteArrayOutputStream errWriter)
	{
		this.outWriter = outWriter;
		this.errWriter = errWriter;
		this.startTime = System.currentTimeMillis();
		this.messageComplete = "Script executed asynchronously.";
		this.messageFail = "Error while executing script asynchronously.";
	}

	/**
	 * @see ExecuteResultHandler#onProcessComplete(int)
	 */
	@Override
	public void onProcessComplete(final int exitValue)
	{
		if(exitValue == 0)
			logger.debug(buildMessage(messageComplete, exitValue));
		else
			logger.warn(buildMessage(messageComplete, exitValue));
	}

	/**
	 * @see ExecuteResultHandler#onProcessFailed(ExecuteException)
	 */
	@Override
	public void onProcessFailed(final ExecuteException e)
	{
		logger.error(buildMessage(messageFail, e.getExitValue()), e);
	}

	public void setMessageComplete(String message)
	{
		this.messageComplete = message;
	}

	public void setMessageFail(String message)
	{
		this.messageFail = message;
	}

	private String buildMessage(String message, final int exitValue)
	{
		String scriptResult = String.format("Result code=%d, Output=%s, Error string=%s", exitValue, outWriter.toString(), errWriter.toString());
		LineBuilder lineBuilder = new LineBuilder();
		lineBuilder.append(message).append(scriptResult).add("Script execution duration: ").add(formatDurationHMS(System.currentTimeMillis() - startTime));
		return lineBuilder.toString();
	}
}
