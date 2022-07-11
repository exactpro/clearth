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
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;

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

	public static ScriptResult executeScript(String commandLineString, String[] args, int[] exitValues)
			throws IOException
	{
		return executeScript(commandLineString, args, exitValues, null);
	}

	public static ScriptResult executeScript(String commandLineString, String[] args, int[] exitValues, File workingDir,
			Map<String, String> envVars) throws IOException
	{
		logger.debug("Command line to execute: {}. Parameters: {} ", commandLineString, args);

		CommandLine commandLine = CommandLine.parse(commandLineString, EnvironmentUtils.getProcEnvironment());

		return execute(commandLine, args, exitValues, workingDir, envVars);
	}

	public static ScriptResult executeScript(String commandLineString, String[] args, File workingDir,
											 Map<String, String> envVars) throws IOException
	{
		return executeScript(commandLineString, args, new int[]{0}, workingDir, envVars);
	}

	public static ScriptResult executeScript(String commandLineString, String[] args, int[] exitValues, File workingDir)
			throws IOException
	{
		return executeScript(commandLineString, args, exitValues, workingDir, null);
	}

	public static ScriptResult executeScript(String commandLineString, int[] exitValues) throws IOException
	{
		return executeScript(commandLineString, null, exitValues);
	}

	public static ScriptResult executeScript(String commandLineString, int[] exitValues, File workingDir,
			Map<String, String> envVars) throws IOException
	{
		return executeScript(commandLineString, null, exitValues, workingDir, envVars);
	}

	public static ScriptResult executeScript(String commandLineString, int[] exitValues, File workingDir) throws IOException
	{
		return executeScript(commandLineString, null, exitValues, workingDir);
	}

	public static ScriptResult executeScript(String commandLineString, File workingDir, Map<String, String> envVars) throws IOException
	{
		return executeScript(commandLineString, new int[]{0}, workingDir, envVars);
	}

	public static ScriptResult executeScript(String commandLineString, File workingDir) throws IOException
	{
		return executeScript(commandLineString, new int[]{0}, workingDir);
	}

	public static ScriptResult executeScript(String commandLineString) throws IOException
	{
		return executeScript(commandLineString, new int[]{0});
	}

	public static ScriptResult executeScript(String command, String executableName, String shellOption, String[] args,
	                                         int[] exitValues) throws IOException
	{
		return executeScript(command, executableName, shellOption, args, exitValues, null);
	}

	public static ScriptResult executeScript(String command, String executableName, String shellOption, String[] args,
	                                         int[] exitValues, File workingDir, Map<String, String> envVars) throws IOException
	{
		CommandLine commandLine =
				CommandLine.parse(executableName, EnvironmentUtils.getProcEnvironment())
						.addArgument(shellOption).addArgument(command, false);
		return execute(commandLine, args, exitValues, workingDir, envVars);
	}

	public static ScriptResult executeScript(String command, String executableName, String shellOption, String[] args,
	                                         int[] exitValues, File workingDir) throws IOException
	{
		return executeScript(command, executableName, shellOption, args, exitValues, workingDir, null);
	}


	public static void executeScriptAsync(String commandLineString, String[] args, int[] exitValues,
	                                      String messageComplete, String messageFail) throws IOException
	{
		executeScriptAsync(commandLineString, args, exitValues, messageComplete, messageFail, null);
	}

	public static void executeScriptAsync(String commandLineString, String[] args, int[] exitValues,
			String messageComplete, String messageFail, File workingDir, Map<String, String> envVars) throws IOException
	{
		logger.debug("Command line to execute: {}. Parameters: {} ", commandLineString, args);

		CommandLine commandLine = CommandLine.parse(commandLineString, EnvironmentUtils.getProcEnvironment());
		if (args != null)
			commandLine.addArguments(args, false);

		executeAsync(commandLine, exitValues, messageComplete, messageFail, workingDir, envVars);
	}

	public static void executeScriptAsync(String commandLineString, String[] args, int[] exitValues,
			String messageComplete, String messageFail, File workingDir) throws IOException
	{
		executeScriptAsync(commandLineString, args, exitValues, messageComplete, messageFail, workingDir, null);
	}

	public static void executeScriptAsync(String commandLineString, int[] exitValues, String messageComplete,
	                                      String messageFail) throws IOException
	{
		executeScriptAsync(commandLineString, null, exitValues, messageComplete, messageFail);
	}

	public static void executeScriptAsync(String commandLineString, int[] exitValues, String messageComplete,
			String messageFail, File workingDir, Map<String, String> envVars) throws IOException
	{
		executeScriptAsync(commandLineString, null, exitValues, messageComplete, messageFail, workingDir, envVars);
	}

	public static void executeScriptAsync(String commandLineString, int[] exitValues, String messageComplete,
			String messageFail, File workingDir) throws IOException
	{
		executeScriptAsync(commandLineString, null, exitValues, messageComplete, messageFail, workingDir, null);
	}

	public static void executeScriptAsync(String commandLineString, String messageComplete, String messageFail,
	                                      File workingDir) throws IOException
	{
		executeScriptAsync(commandLineString, new int[]{0}, messageComplete, messageFail, workingDir);
	}

	public static void executeScriptAsync(String commandLineString, String messageComplete, String messageFail)
			throws IOException
	{
		executeScriptAsync(commandLineString, new int[]{0}, messageComplete, messageFail);
	}

	public static void executeScriptAsync(String command, String executableName, String shellOption, int[] exitValues,
	                                      String messageComplete, String messageFail) throws IOException
	{
		executeScriptAsync(command, executableName, shellOption, exitValues, messageComplete, messageFail, null);
	}

	public static void executeScriptAsync(String command, String executableName, String shellOption, int[] exitValues,
			String messageComplete, String messageFail, File workingDir, Map<String, String> envVars) throws IOException
	{
		executeScriptAsync(command, null, executableName, shellOption, exitValues, messageComplete, messageFail, workingDir, envVars);
	}

	public static void executeScriptAsync(String command, String[] args, String executableName, String shellOption, int[] exitValues,
										  String messageComplete, String messageFail, File workingDir, Map<String, String> envVars) throws IOException
	{
		CommandLine commandLine =
				CommandLine.parse(executableName, EnvironmentUtils.getProcEnvironment())
						.addArgument(shellOption).addArgument(command, false).addArguments(args, false);
		executeAsync(commandLine, exitValues, messageComplete, messageFail, workingDir, envVars);
	}

	public static void executeScriptAsync(String command, String executableName, String shellOption, int[] exitValues,
			String messageComplete, String messageFail, File workingDir) throws IOException
	{
		executeScriptAsync(command, executableName, shellOption, exitValues, messageComplete, messageFail, workingDir, null);
	}

	protected static ScriptResult execute(CommandLine commandLine, String[] args, int[] exitValues, File workingDir,
			Map<String, String> envVars) throws IOException
	{
		Executor executor = createExecutor(exitValues, workingDir);
		if (args != null)
			commandLine.addArguments(args, false);

		try (ByteArrayOutputStream outWriter = new ByteArrayOutputStream();
		     ByteArrayOutputStream errWriter = new ByteArrayOutputStream())
		{
			executor.setStreamHandler(new PumpStreamHandler(new PrintStream(outWriter), new PrintStream(errWriter)));
			long startTime = System.currentTimeMillis();
			int result = executor.execute(commandLine, envVars);
			logger.debug("Script execution duration: {}", formatDurationHMS(System.currentTimeMillis() - startTime));
			return new ScriptResult(result, outWriter.toString(), errWriter.toString());
		}
	}

	protected static void executeAsync(CommandLine commandLine, int[] exitValues, String messageComplete,
			String messageFail, File workingDir, Map<String, String> envVars) throws IOException
	{
		Executor executor = createExecutor(exitValues, workingDir);

		try (ByteArrayOutputStream outWriter = new ByteArrayOutputStream();
		     ByteArrayOutputStream errWriter = new ByteArrayOutputStream())
		{
			executor.setStreamHandler(new PumpStreamHandler(new PrintStream(outWriter), new PrintStream(errWriter)));

			ScriptResultHandler scriptResultHandler = new ScriptResultHandler(outWriter, errWriter);

			scriptResultHandler.setMessageComplete(messageComplete);
			scriptResultHandler.setMessageFail(messageFail);

			executor.execute(commandLine, envVars, scriptResultHandler);
		}
	}

	protected static void executeAsync(CommandLine commandLine, int[] exitValues, String messageComplete,
	                                   String messageFail, File workingDir) throws IOException
	{
		executeAsync(commandLine, exitValues, messageComplete, messageFail, workingDir, null);
	}

	protected static Executor createExecutor(int[] exitValues, File workingDir)
	{
		Executor executor = new DefaultExecutor();
		executor.setExitValues(exitValues);

		if (workingDir != null)
		{
			executor.setWorkingDirectory(workingDir);
			logger.debug("Script execution directory: {}", executor.getWorkingDirectory());
		}
		return executor;
	}
}
