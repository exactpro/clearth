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

import static org.apache.commons.lang.time.DurationFormatUtils.formatDurationHMS;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.exec.environment.EnvironmentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.utils.Utils;

public class ScriptUtils extends Utils
{
	private static final Logger logger = LoggerFactory.getLogger(ScriptUtils.class);
	
	public static ScriptResult executeScript(String commandLineString, String[] args, int[] exitValues) throws IOException
	{
		logger.debug("Command line to execute: {}. Parameters: {} ", commandLineString, args);

		CommandLine commandLine = CommandLine.parse(commandLineString, EnvironmentUtils.getProcEnvironment());
		if (args != null)
			commandLine.addArguments(args, false);

		Executor executor = new DefaultExecutor();
		executor.setExitValues(exitValues);

		ByteArrayOutputStream outWriter = new ByteArrayOutputStream();
		ByteArrayOutputStream errWriter = new ByteArrayOutputStream();
		executor.setStreamHandler(new PumpStreamHandler(new PrintStream(outWriter), new PrintStream(errWriter)));

		long startTime = System.currentTimeMillis();
		int result = executor.execute(commandLine);
		logger.debug("Script execution duration: {}", formatDurationHMS(System.currentTimeMillis() - startTime));

		return new ScriptResult(result, outWriter.toString(), errWriter.toString());
	}
	
	public static ScriptResult executeScript(String commandLineString, int[] exitValues) throws IOException
	{
		return executeScript(commandLineString, null, exitValues);
	}
	
	public static ScriptResult executeScript(String commandLineString) throws IOException
	{
		return executeScript(commandLineString, new int[]{0});
	}
	

	public static void executeScriptAsync(String commandLineString, String[] args, int[] exitValues, String messageComplete, String messageFail) throws IOException
	{
		logger.debug("Command line to execute: {}. Parameters: {} ", commandLineString, args);

		CommandLine commandLine = CommandLine.parse(commandLineString, EnvironmentUtils.getProcEnvironment());
		if (args != null)
			commandLine.addArguments(args, false);

		Executor executor = new DefaultExecutor();
		executor.setExitValues(exitValues);

		ByteArrayOutputStream outWriter = new ByteArrayOutputStream();
		ByteArrayOutputStream errWriter = new ByteArrayOutputStream();
		executor.setStreamHandler(new PumpStreamHandler(new PrintStream(outWriter), new PrintStream(errWriter)));

		ScriptResultHandler scriptResultHandler = new ScriptResultHandler(outWriter, errWriter);
		scriptResultHandler.setMessageComplete(messageComplete);
		scriptResultHandler.setMessageFail(messageFail);

		executor.execute(commandLine, scriptResultHandler);
	}
	
	public static void executeScriptAsync(String commandLineString, int[] exitValues, String messageComplete, String messageFail) throws IOException
	{
		executeScriptAsync(commandLineString, null, exitValues, messageComplete, messageFail);
	}
	
	public static void executeScriptAsync(String commandLineString, String messageComplete, String messageFail) throws IOException
	{
		executeScriptAsync(commandLineString, new int[]{0}, messageComplete, messageFail);
	}
}
