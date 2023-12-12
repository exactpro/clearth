/******************************************************************************
 * Copyright 2009-2023 Exactpro Systems Limited
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
	private MvelVariables mvelVars;
	private List<String> errors;
	private boolean warnOnError;
	
	public ActionParamsCalculator(MatrixFunctions matrixFunctions)
	{
		this.matrixFunctions = matrixFunctions;
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
		this.action = action;
		this.mvelVars = action.getMatrix().getMvelVars();
		this.errors = new ArrayList<>();
		this.warnOnError = warnOnError;
		
		mvelVars.saveInputParams(action);
		
		calculateMainParameters();
		calculateSpecialParameters();
		
		Map<String, String> params = action.getInputParams();
		matrixFunctions.setCurrentTime(Calendar.getInstance());
		for (Entry<String, String> param : params.entrySet())
		{
			String value = calculateAndSaveParameter(param.getValue(), param.getKey(), null);
			if (value != null)
				params.put(param.getKey(), value);
		}
		matrixFunctions.setCurrentTime(null);
		
		return errors;
	}

	private void calculateSpecialParameters()
	{
		Map<String, String> formulas = action.getSpecialParamsFormulas();
		if (formulas == null)
			return;
		
		Map<String, String> specialParams = action.getSpecialParams();
		for (String param : action.getSpecialParamsNames())
		{
			String value = calculateAndSaveParameter(formulas.get(param), param, specialParams.get(param));
			if (value != null)
				specialParams.put(param, value);
		}
	}
	
	private void calculateMainParameters()
	{
		calculateExecute();
		calculateComment();
		calculateTimeout(); 
		calculateInverted();
		calculateAsync();
		calculateAsyncGroup();
		calculateWaitAsyncEnd();
		calculateIdInTemplate();
	}
	
	public String buildParameterError(String name, String expression, String value)
	{
		return String.format(PARAMETER_ERROR_FORMAT, name, expression, value);
	}
	
	
	protected String calculateParameter(String valueExpression, String parameterName)
	{
		if (valueExpression == null)
			return null;

		try
		{
			Object valueObj = matrixFunctions.calculateExpression(valueExpression, parameterName,
					mvelVars.getVariables(), mvelVars.getFixedIds(), getAction(), new ObjectWrapper(0));
			
			return (valueObj != null) ? valueObj.toString() : null;
		}
		catch (Exception e)
		{
			if (warnOnError && action.isExecutable())
				getLogger().warn("Error while calculating parameter '{}'", parameterName, e);
			
			if (errors != null)
			{
				String error = "'"+parameterName+"' - "+MatrixFunctions.errorToText(e);
				errors.add(error);
			}
			return null;
		}
	}
	
	protected String calculateAndSaveParameter(String valueExpression, String parameterName, String defaultToSave)
	{
		String result = calculateParameter(valueExpression, parameterName),
				toSave = result != null ? result : defaultToSave;
		if (toSave != null)
			mvelVars.saveCalculatedParameter(action.getIdInMatrix(), parameterName, toSave);
		return result;
	}
	
	protected void calculateExecute()
	{
		String formula = action.getFormulaExecutable(),
				execute = calculateAndSaveParameter(formula, ActionGenerator.COLUMN_EXECUTE, Boolean.toString(action.isExecutable()));
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
				comment = calculateAndSaveParameter(formula, ActionGenerator.COLUMN_COMMENT, action.getComment());
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
				timeout = calculateAndSaveParameter(formula, ActionGenerator.COLUMN_TIMEOUT, Long.toString(action.getTimeOut()));
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
				inverted = calculateAndSaveParameter(formula, ActionGenerator.COLUMN_INVERT, Boolean.toString(action.isInverted()));
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
				async = calculateAndSaveParameter(formula, ActionGenerator.COLUMN_ASYNC, Boolean.toString(action.isAsync()));
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
				group = calculateAndSaveParameter(formula, ActionGenerator.COLUMN_ASYNCGROUP, action.getAsyncGroup());
		if (group != null)
			action.setAsyncGroup(group);
	}
	
	protected void calculateWaitAsyncEnd()
	{
		String formula = action.getFormulaWaitAsyncEnd(),
				wait = calculateAndSaveParameter(formula, ActionGenerator.COLUMN_WAITASYNCEND, 
						action.getWaitAsyncEnd() != null ? action.getWaitAsyncEnd().getLabel() : null);
		if (wait != null)
			action.setWaitAsyncEnd(WaitAsyncEnd.byLabel(wait));
	}
	
	protected void calculateIdInTemplate()
	{
		String formula = action.getFormulaIdInTemplate();
		String idInMatrixGenerator = calculateParameter(formula, ActionGenerator.COLUMN_ID_IN_TEMPLATE);
		if (idInMatrixGenerator != null)
			action.setIdInTemplate(idInMatrixGenerator);
	}
}
