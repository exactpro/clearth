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

package com.exactprosystems.clearth.automation.actions;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.exactprosystems.clearth.utils.inputparams.InputParamsHandler;
import org.apache.commons.exec.ExecuteException;
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
			PARAMETERS = "Parameters",
			SCRIPT_DIRECTORY = "ScriptDirectory",
			ASYNC = "Async",
			OUTPUT = "Output",
			FAIL_ON_ERROR_OUTPUT = "FailOnErrorOutput",
			SUCCESS_RESULT_CODES = "SuccessResultCodes";

	@Override
	protected Result run(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext) throws ResultException
	{
		if (inputParams.size() == 0)
			return noParametersResult();

		String command = buildScriptPath(getInputParams());
		String parameters = buildScriptParameters(getInputParams());
		if (logger.isDebugEnabled())
			logger.debug("Script location: " + command + ". Parameters: " + parameters);
		
		if (parameters != null && !StringUtils.isEmpty(parameters.trim()))
			command += " " + parameters.trim();
		
		return executeScript(command);
	}
	
	
	protected Result noParametersResult()
	{
		return DefaultResult.failed(String.format("No parameters specified. "
				+ "Required parameter: %s. Optional parameters: %s, %s, %s.", SCRIPT_FILE_NAME, SCRIPT_DIRECTORY, PARAMETERS, ASYNC));
	}
	
	protected String buildScriptPath(Map<String, String> parameters) throws ResultException
	{
		String scriptName = InputParamsUtils.getRequiredString(parameters, SCRIPT_FILE_NAME),
				scriptDir = InputParamsUtils.getStringOrDefault(parameters, SCRIPT_DIRECTORY, null);
		
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
	
	
	protected ScriptResult doExecuteScript(String command)
	{
		ScriptResult res;
		try
		{
			res = ScriptUtils.executeScript(command, null);
		}
		catch (ExecuteException e)
		{
			throw ResultException.failed(String.format("Error while executing script '%s'", command), e);
		}
		catch (IOException e)
		{
			throw ResultException.failed(String.format("Script '%s' was not launched", command), e);
		}

		logger.debug("Script {} executed. {}", command, res);

		return res;
	}
	
	protected void processScriptResult(ScriptResult res)
	{
		String resultString, outStr = res.outStr;
		
		if (StringUtils.endsWith(outStr, Utils.EOL))
			resultString = StringUtils.left(outStr, outStr.length() - Utils.EOL.length());
		else if (StringUtils.endsWith(outStr,"\n"))
			resultString = StringUtils.left(outStr, outStr.length() - 1);
		else
			resultString = outStr;

		addOutputParam(OUTPUT, resultString);
	}
	
	protected boolean isFailOnErrorOutput() {
		return InputParamsUtils.getBooleanOrDefault(getInputParams(), FAIL_ON_ERROR_OUTPUT, false);
	}
	
	protected Result buildActionResult(ScriptResult res)
	{
		InputParamsHandler handler = new InputParamsHandler(getInputParams());
		boolean failOnErrorOutput = isFailOnErrorOutput();
		Set<Integer> resultCodes = convertToInt(handler.getSet(SUCCESS_RESULT_CODES, ","));
		if (handler.getString(SUCCESS_RESULT_CODES) == null)
				resultCodes.add(0);

		Result result = new DefaultResult();
		result.setComment(String.format("Script finished. Code: %d." + Utils.EOL
				+ " Output: %s"+ Utils.EOL
				+ " Error: %s", res.result, res.outStr, res.errStr));
		boolean success = res.errStr.isEmpty() || !failOnErrorOutput;
		result.setSuccess(success);
		if (success && !resultCodes.isEmpty())
			result.setSuccess(resultCodes.contains(res.result));
		return result;
	}

	private Set<Integer> convertToInt(Set<String> set)
	{
		Set<Integer> result = new LinkedHashSet<>();
		for (String s : set)
			result.add(Integer.parseInt(s));
		return result;
	}
	
	protected Result executeScriptSync(String command)
	{
		ScriptResult res = doExecuteScript(command);
		processScriptResult(res);
		return buildActionResult(res);
	}
	
	protected Result executeScriptAsync(final String command)
	{
		String messageComplete = String.format("Script '%s' executed asynchronously, triggered by action '%s' from matrix '%s'.",
		                                       command, getIdInMatrix(), getMatrix().getName());
		String messageFail = String.format("Error while executing script '%s' asynchronously, triggered by action '%s' from matrix '%s'.",
		                                   command, getIdInMatrix(), getMatrix().getName());
		try
		{
			ScriptUtils.executeScriptAsync(command, null, messageComplete, messageFail);
		}
		catch (IOException e)
		{
			logger.error("Script '{}' was not launched, triggered by action '{}' from matrix '{}'.", 
					new Object[] {command, getIdInMatrix(), getMatrix().getName(), e});
		}

		return DefaultResult.passed(String.format("Execution of script '%s' started asynchronously", command));
	}
	
	protected Result executeScript(String command)
	{
		if (!InputParamsUtils.getBooleanOrDefault(getInputParams(), ASYNC, false))
			return executeScriptSync(command);
		return executeScriptAsync(command);
	}
}
