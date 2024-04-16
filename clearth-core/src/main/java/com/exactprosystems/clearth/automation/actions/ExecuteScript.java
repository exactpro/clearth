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

package com.exactprosystems.clearth.automation.actions;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import com.exactprosystems.clearth.automation.report.FailReason;
import com.exactprosystems.clearth.automation.report.results.AttachedFilesResult;
import com.exactprosystems.clearth.utils.FileOperationUtils;
import com.exactprosystems.clearth.utils.inputparams.InputParamsHandler;
import com.exactprosystems.clearth.utils.scripts.ProcessedScriptResult;
import org.apache.commons.exec.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.GlobalContext;
import com.exactprosystems.clearth.automation.MatrixContext;
import com.exactprosystems.clearth.automation.StepContext;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.results.DefaultResult;
import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.utils.inputparams.InputParamsUtils;
import com.exactprosystems.clearth.utils.scripts.ScriptResult;
import com.exactprosystems.clearth.utils.scripts.ScriptUtils;

public class ExecuteScript extends Action {
	
	public static final String SCRIPT_FILE_NAME = "ScriptName",
			PARAMETERS = "Parameters", //this is used only to pass complete string with script parameters
			PARAMS_FROM_ACTION = "ParamsFromAction", //this is used to pass action params as script params
			SCRIPT_DIRECTORY = "ScriptDirectory",
			OUTPUT = "Output",
			FAIL_ON_ERROR_OUTPUT = "FailOnErrorOutput",
			SUCCESS_RESULT_CODES = "SuccessResultCodes",
			SCRIPT_TEXT_PARAM = "ScriptText",
			EXECUTABLE_NAME_PARAM = "ExecutableName",
			SHELL_OPTION_PARAM = "ShellOption",
			WORKING_DIRECTORY = "WorkingDirectory",
			STDOUT_REDIRECT_PARAMETER = "OutRedirect",
			STDERR_REDIRECT_PARAMETER = "ErrRedirect",
			ERROR_OUT = "ErrOutput",
			PARAMS_DELIMITER = "ParamsDelimiter",
			COMPRESS_SCRIPT_OUTPUT = "CompressScriptOutput";

	protected String command, executableName, shellOption;

	protected File workingDir;
	protected Map<String, String> envVars;

	protected String tempFileName;
	protected boolean isOutRedirected, isErrRedirected, isCompressScriptResult;

	protected String[] additionalCLineParams;

	@Override
	protected Result run(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext) throws ResultException
	{
		if (inputParams.isEmpty())
			return noParametersResult();

		initParams(stepContext, matrixContext, globalContext);

		return executeScript(command);
	}

	protected void initParams(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext)
	{
		String workingDirPath = InputParamsUtils.getStringOrDefault(inputParams, WORKING_DIRECTORY, getDefaultWorkingDir());
		workingDir = new File(ClearThCore.rootRelative(workingDirPath));
		envVars = createEnvironmentVars(stepContext, matrixContext, globalContext);

		command = buildCommand(getInputParams());
		additionalCLineParams = getAdditionalParameters();

		isOutRedirected = InputParamsUtils.getBooleanOrDefault(inputParams, STDOUT_REDIRECT_PARAMETER, false);
		isErrRedirected = InputParamsUtils.getBooleanOrDefault(inputParams, STDERR_REDIRECT_PARAMETER, false);

		isCompressScriptResult = InputParamsUtils.getBooleanOrDefault(inputParams, COMPRESS_SCRIPT_OUTPUT, false);

		logger.debug("Script: {}. Parameters: {}. Working dir: {}.", command, convertToString(additionalCLineParams), workingDir);
	}

	protected Result noParametersResult()
	{
		return DefaultResult.failed(
				String.format("No parameters specified. Required parameter: %s. Optional parameters: %s, %s, %s.",
				SCRIPT_FILE_NAME, SCRIPT_DIRECTORY, WORKING_DIRECTORY, PARAMETERS));
	}
	
	protected String buildCommand(Map<String, String> parameters) throws ResultException
	{
		String scriptTextCommand = InputParamsUtils.getStringOrDefault(parameters, SCRIPT_TEXT_PARAM, null);
		if (scriptTextCommand != null)
		{
			executableName = InputParamsUtils.getRequiredString(parameters, EXECUTABLE_NAME_PARAM);
			shellOption = InputParamsUtils.getStringOrDefault(parameters, SHELL_OPTION_PARAM, "-c");
			return scriptTextCommand;
		}
		String scriptName = InputParamsUtils.getRequiredString(parameters, SCRIPT_FILE_NAME),
				scriptDir = InputParamsUtils.getStringOrDefault(parameters, SCRIPT_DIRECTORY, null);

		tempFileName = FilenameUtils.removeExtension(FilenameUtils.getName(scriptName)) + "_";

		File scriptFile;
		if (scriptDir != null)
		{
			File dir = new File(ClearThCore.rootRelative(scriptDir));
			if (!dir.isDirectory())
				throw new ResultException("'"+dir.getAbsolutePath()+"' does not exist or is not a directory");
			
			scriptFile  = new File (dir, scriptName);
		}
		else
			scriptFile = new File(ClearThCore.rootRelative(scriptName));
		
		if (!scriptFile.isFile())
			throw new ResultException("'"+scriptFile.getAbsolutePath()+"' does not exist or is a directory");
		return scriptFile.getAbsolutePath();
	}
	
	protected String buildScriptParameters(Map<String, String> parameters) throws ResultException
	{
		return parameters.get(PARAMETERS);
	}

	protected String[] getAdditionalParameters() throws ResultException
	{
		InputParamsHandler handler = new InputParamsHandler(inputParams);
		String parametersString = buildScriptParameters(inputParams);
		String delimiter = handler.getString(PARAMS_DELIMITER, ";");
		Set<String> paramsFromAction = handler.getSet(PARAMS_FROM_ACTION, delimiter);

		if (StringUtils.isNotEmpty(parametersString))
		{
			if (!paramsFromAction.isEmpty())
				logger.info("The script parameters were obtained from the '{}' action parameter", PARAMETERS);
			return new String[]{parametersString.trim()};
		}

		if (!paramsFromAction.isEmpty())
		{
			try
			{
				String[] params = paramsFromAction.toArray(new String[0]);

				for (int i = 0; i < params.length; i++)
				{
					String paramName = StringUtils.trim(params[i]);

					if (paramName.equals(PARAMETERS))
						params[i] = "";
					else
						params[i] = handler.getRequiredString(paramName);
				}

				logger.info("The script parameters were obtained from the '{}' action parameter", PARAMS_FROM_ACTION);
				return params;
			}
			finally
			{
				handler.check();
			}
		}

		logger.info("The script will be executed without parameters");
		return new String[]{};
	}
	
	protected ScriptResult doExecuteScript(String command)
	{
		ScriptResult res;
		try
		{
			res = ScriptUtils.executeScript(command, additionalCLineParams, null, workingDir, envVars);
		}
		catch (ExecuteException e)
		{
			throw ResultException.failed(String.format("Error while executing script '%s' with parameters %s", command, convertToString(additionalCLineParams)), e);
		}
		catch (IOException e)
		{
			throw ResultException.failed(String.format("Script '%s' with parameters %s was not launched", command, convertToString(additionalCLineParams)), e);
		}

		logger.debug("Script {} with parameters {} executed. {}", command, convertToString(additionalCLineParams), res);

		return res;
	}
	
	protected ProcessedScriptResult processScriptResult(ScriptResult res)
	{
		String resultString,
				outStr = res.outStr,
				errStr = res.errStr;

		ProcessedScriptResult processedScriptResult = new ProcessedScriptResult();
		if (isOutRedirected)
		{
			Path outFilePath = saveScriptResult(outStr, "_out.txt");
			processedScriptResult.setOutFilePath(outFilePath);
			addOutputParam(OUTPUT, outFilePath.getFileName().toString());
		}
		else
		{
			if (StringUtils.endsWith(outStr, Utils.EOL))
				resultString = StringUtils.left(outStr, outStr.length() - Utils.EOL.length());
			else if (StringUtils.endsWith(outStr,"\n"))
				resultString = StringUtils.left(outStr, outStr.length() - 1);
			else
				resultString = outStr;

			addOutputParam(OUTPUT, resultString);
		}

		if (isErrRedirected)
		{
			Path errFilePath = saveScriptResult(errStr, "_err.txt");
			processedScriptResult.setErrFilePath(errFilePath);
			addOutputParam(ERROR_OUT, errFilePath.getFileName().toString());
		}
		else
			addOutputParam(ERROR_OUT, errStr);

		return processedScriptResult;
	}
	
	protected boolean isFailOnErrorOutput() {
		return InputParamsUtils.getBooleanOrDefault(getInputParams(), FAIL_ON_ERROR_OUTPUT, false);
	}

	protected Result buildActionResult(ScriptResult res, ProcessedScriptResult processedScriptResult)
	{
		InputParamsHandler handler = new InputParamsHandler(getInputParams());
		boolean failOnErrorOutput = isFailOnErrorOutput();
		Set<Integer> resultCodes = convertToInt(handler.getSet(SUCCESS_RESULT_CODES, ","));
		if (handler.getString(SUCCESS_RESULT_CODES) == null)
				resultCodes.add(0);

		Result result;
		if (isOutRedirected || isErrRedirected)
		{
			AttachedFilesResult attachedFilesResult = new AttachedFilesResult();
			if (isOutRedirected)
				attachedFilesResult.attach(OUTPUT, processedScriptResult.getOutFilePath());
			if (isErrRedirected)
				attachedFilesResult.attach(ERROR_OUT, processedScriptResult.getErrFilePath());
			result = attachedFilesResult;
		}
		else
			result = new DefaultResult();

		result.setComment(String.format("Script finished. Code: %d." + Utils.EOL
				+ " Output: %s"+ Utils.EOL
				+ " Error: %s", res.result, isOutRedirected ? processedScriptResult.getOutFilePath().getFileName().toFile() : res.outStr,
				isErrRedirected ? processedScriptResult.getErrFilePath().getFileName().toString() : res.errStr));

		boolean success = (res.errStr != null && res.errStr.isEmpty()) || !failOnErrorOutput;
		result.setSuccess(success);
		if (success && !resultCodes.isEmpty())
			result.setSuccess(resultCodes.contains(res.result));
		
		if (result.isSuccess())
			result.setFailReason(null);
		else if (result.getFailReason() == null)
			result.setFailReason(FailReason.FAILED);
		
		return result;
	}

	private Set<Integer> convertToInt(Set<String> set)
	{
		Set<Integer> result = new LinkedHashSet<>();
		for (String s : set)
			result.add(Integer.parseInt(s));
		return result;
	}

	private String convertToString(String[] additionalCLineParams)
	{
		return Arrays.toString(additionalCLineParams);
	}

	private Path saveScriptResult(String result, String fileSuffix)
	{
		try
		{
			Path tempDir = Paths.get(ClearThCore.getInstance().getTempDirPath());
			if (!Files.exists(tempDir))
				Files.createDirectories(tempDir);

			Path tempFilePath = Files.createTempFile(tempDir, tempFileName, fileSuffix);
			FileUtils.writeStringToFile(tempFilePath.toFile(), result, StandardCharsets.UTF_8);

			return isCompressScriptResult ? compressScriptResult(tempFilePath.toFile()) : tempFilePath;
		}
		catch (IOException e)
		{
			String msg = "Error on save script out data to file";
			logger.warn(msg, e);
			throw new ResultException(msg, e);
		}
	}

	private Path compressScriptResult(File tempFile) throws IOException
	{
		Path zipFile = Paths.get(tempFile + ".zip");
		FileOperationUtils.zipFiles(zipFile.toFile(), new File[]{tempFile});
		FileUtils.deleteQuietly(tempFile);
		return zipFile;
	}

	protected Result executeScript(String command) throws ResultException
	{
		ScriptResult res;
		if (executableName == null)
		{
			res = doExecuteScript(command);
		}
		else 
		{
			try
			{
				res = ScriptUtils.executeScript(command, executableName, shellOption, additionalCLineParams, null, workingDir, envVars);
			}
			catch (IOException e)
			{
				throw ResultException.failed(String.format("Script '%s' with parameters '%s' was not launched", command, convertToString(additionalCLineParams)), e);
			}
		}
		return buildActionResult(res, processScriptResult(res));
	}

	protected Map<String, String> createEnvironmentVars(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext)
	{
		return null;
	}

	protected String getDefaultWorkingDir()
	{
		return ClearThCore.filesRoot();
	}
}
