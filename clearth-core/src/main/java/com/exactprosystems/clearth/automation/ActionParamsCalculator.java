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

package com.exactprosystems.clearth.automation;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.automation.async.WaitAsyncEnd;
import com.exactprosystems.clearth.utils.ObjectWrapper;
import com.exactprosystems.clearth.utils.inputparams.InputParamsUtils;

public class ActionParamsCalculator
{
	private static final Logger logger = LoggerFactory.getLogger(ActionParamsCalculator.class);
	public static final String PARAMETER_ERROR_FORMAT = "'%s' - formula '%s' returned value '%s'";
	
	private final MatrixFunctions matrixFunctions;
	private Action action;
	private Map<String, String> fixedIds;
	private List<String> errors;
	private boolean warnOnError;
	
	public ActionParamsCalculator(MatrixFunctions matrixFunctions)
	{
		this.matrixFunctions = matrixFunctions;
	}
	
	
	public void init(List<Matrix> matrices)
	{
		//Saving all action parameters in mvelVars, so references to future actions can be solved during step execution
		for (Matrix matrix : matrices)
		{
			getLogger().trace("Saving action parameters for matrix '"+matrix.getShortFileName()+"'");
			Map<String, Object> mvelVars = matrix.getMvelVars();
			for (Action action : matrix.getActions())
			{
				String actionId = action.getIdInMatrix(), id;
				//Do we need to fix action ID so that MVEL could resolve reference to it?
				if ((!actionId.isEmpty()) && ((Character.isDigit(actionId.charAt(0))) || (actionId.charAt(0)=='_')))
				{
					id = "MVELFIXED_"+actionId;
					if (fixedIds == null)
						fixedIds = new HashMap<String, String>();
					fixedIds.put(actionId, id);
				}
				else
					id = actionId;
				
				mvelVars.put(id, makeInputParamsMap(action.getInputParams()));
			}
			getLogger().trace("Finished saving action parameters for matrix '"+matrix.getShortFileName()+"'");
		}
	}
	
	
	protected Logger getLogger()
	{
		return logger;
	}
	
	protected MatrixFunctions getMatrixFunctions()
	{
		return matrixFunctions;
	}
	
	protected Action getAction()
	{
		return action;
	}

	protected Map<String, String> getFixedIds()
	{
		return fixedIds;
	}
	
	public List<String> getErrors()
	{
		return errors;
	}

	public boolean isWarnOnError()
	{
		return warnOnError;
	}


	public List<String> calculateParameters(Action action, boolean warnOnError)
	{
		calculateMainParameters(action, new ArrayList<String>(), warnOnError);
		
		Map<String, String> params = action.getInputParams();
		matrixFunctions.setCurrentTime(Calendar.getInstance());
		for (Entry<String, String> param : params.entrySet())
		{
			String value = calculateParameter(param.getValue(), param.getKey());
			if (value != null)
				params.put(param.getKey(), value);
		}
		matrixFunctions.setCurrentTime(null);
		
		return errors;
	}
	
	public void calculateMainParameters(Action action, List<String> errorsOutput, boolean warnOnError)
	{
		this.action = action;
		this.errors = errorsOutput;
		this.warnOnError = warnOnError;
		
		calculateExecute();
		calculateComment();
		calculateTimeout(); 
		calculateInverted();
		calculateAsync();
		calculateAsyncGroup();
		calculateWaitAsyncEnd();
	}
	
	public String buildParameterError(String name, String expression, String value)
	{
		return String.format(PARAMETER_ERROR_FORMAT, name, expression, value);
	}
	
	
	@SuppressWarnings("unchecked")
	protected String calculateParameter(String valueExpression, String parameterName)
	{
		if (valueExpression == null)
			return null;
		
		Map<String, Object> mvelVars = action.getMatrix().getMvelVars();
		Map<String, String> fixedIds = getFixedIds();
		try
		{
			String value = matrixFunctions.calculateExpression(valueExpression, parameterName, mvelVars, fixedIds, getAction(),
					new ObjectWrapper(0)).toString();
			
			String id = MatrixFunctions.resolveFixedId(action.getIdInMatrix(), fixedIds);
			((Map<String, Object>)mvelVars.get(id)).put(parameterName, value);
			return value;
		}
		catch (Exception e)
		{
			if (warnOnError && action.isExecutable())
				getLogger().warn("Error while calculating parameter '"+parameterName+"'", e);
			
			if (errors != null)
			{
				String error = "'"+parameterName+"' - "+MatrixFunctions.errorToText(e);
				errors.add(error);
			}
			return null;
		}
	}
	
	protected void calculateExecute()
	{
		String formula = action.getFormulaExecutable(),
				execute = calculateParameter(formula, ActionGenerator.COLUMN_EXECUTE);
		if (execute == null)
			return;
		
		if (InputParamsUtils.YES.contains(execute.toLowerCase()))
			action.setExecutable(true);
		else if (InputParamsUtils.NO.contains(execute.toLowerCase()))
		{
			action.setExecutable(false);
			action.getMatrix().getNonExecutableActions().add(action.getIdInMatrix());
		}
		else
		{
			action.setExecutable(true);
			if (errors != null)
				errors.add(buildParameterError(ActionGenerator.COLUMN_EXECUTE, formula, execute));
		}
	}
	
	protected void calculateComment()
	{
		String formula = action.getFormulaComment(),
				comment = calculateParameter(formula, ActionGenerator.COLUMN_COMMENT);
		if (comment == null)
		{
			if (formula != null)
				action.setComment(formula);
		}
		else
			action.setComment(comment);
	}
	
	protected void calculateTimeout()
	{
		String formula = action.getFormulaTimeout(),
				timeout = calculateParameter(formula, ActionGenerator.COLUMN_TIMEOUT);
		if (timeout == null)
		{
			if (formula != null)
				action.setTimeOut(0);
			return;
		}
		
		try
		{
			action.setTimeOut(Integer.parseInt(timeout));
		}
		catch (Exception e)
		{
			action.setTimeOut(0);
			if (errors != null)
				errors.add(buildParameterError(ActionGenerator.COLUMN_TIMEOUT, formula, timeout));
		}
	}
	
	protected void calculateInverted()
	{
		String formula = action.getFormulaInverted(),
				inverted = calculateParameter(formula, ActionGenerator.COLUMN_INVERT);
		if (inverted == null)
			return;
		
		if (InputParamsUtils.YES.contains(inverted.toLowerCase()))
			action.setInverted(true);
		else if (InputParamsUtils.NO.contains(inverted.toLowerCase()))
			action.setInverted(false);
		else
		{
			if (errors != null)
				errors.add(buildParameterError(ActionGenerator.COLUMN_INVERT, formula, inverted));
		}
	}
	
	protected void calculateAsync()
	{
		String formula = action.getFormulaAsync(),
				async = calculateParameter(formula, ActionGenerator.COLUMN_ASYNC);
		if (async == null)
			return;
		
		if (InputParamsUtils.YES.contains(async.toLowerCase()))
			action.setAsync(true);
		else if (InputParamsUtils.NO.contains(async.toLowerCase()))
			action.setAsync(false);
		else
		{
			if (errors != null)
				errors.add(buildParameterError(ActionGenerator.COLUMN_ASYNC, formula, async));
		}
	}
	
	protected void calculateAsyncGroup()
	{
		String formula = action.getFormulaAsyncGroup(),
				group = calculateParameter(formula, ActionGenerator.COLUMN_ASYNCGROUP);
		if (group != null)
			action.setAsyncGroup(group);
	}
	
	protected void calculateWaitAsyncEnd()
	{
		String formula = action.getFormulaWaitAsyncEnd(),
				wait = calculateParameter(formula, ActionGenerator.COLUMN_WAITASYNCEND);
		if (wait != null)
			action.setWaitAsyncEnd(WaitAsyncEnd.byLabel(wait));
	}
	
	
	protected Map<String, Object> makeInputParamsMap(Map<String, String> inputParams)
	{
		Map<String, Object> commonParams = new LinkedHashMap<String, Object>(inputParams),
				justInput = new LinkedHashMap<String, Object>(inputParams);
		commonParams.put(ActionExecutor.PARAMS_IN, justInput);
		return commonParams;
	}
}
