/******************************************************************************
 * Copyright 2009-2022 Exactpro Systems Limited
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

package com.exactprosystems.clearth.web.beans.tools;

import static com.exactprosystems.clearth.ClearThCore.comparisonUtils;

import java.text.MessageFormat;
import java.util.List;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.MatrixFunctions;
import com.exactprosystems.clearth.automation.Scheduler;
import com.exactprosystems.clearth.automation.exceptions.ParametersException;
import com.exactprosystems.clearth.tools.ExpressionCalculatorTool;
import com.exactprosystems.clearth.tools.calculator.CalculatorVariable;
import com.exactprosystems.clearth.tools.calculator.HistoryRow;
import com.exactprosystems.clearth.web.beans.ClearThBean;
import com.exactprosystems.clearth.web.misc.MessageUtils;
import com.exactprosystems.clearth.web.misc.UserInfoUtils;

/**
 * Created by alexander.magomedov on 11/11/16.
 */
public class ExpressionCalculatorToolBean extends ClearThBean
{
	
	public static final int MAX_VARS_COUNT = 10;
	public static final String NEW_VAR_ERROR = "Could not add variable",
			MAX_VARS_ERROR = String.format("Too many variables, only %d are available in this tool", MAX_VARS_COUNT);
	private static final String GEN_VAR_NAME_PATTERN = "id.param{0}";
	
	private final ExpressionCalculatorTool calculator;
	
	private String expression = "",
			toCompare = null,
			selectedScheduler = null;
	
	private int genVarNameSuffix = 0;
	private CalculatorVariable selectedVar;
	private HistoryRow selectedHistoryRow;
	
	private String result = null,
			comparisonResult = null;
	
	public ExpressionCalculatorToolBean()
	{
		calculator = ClearThCore.getInstance().getToolsFactory().createExpressionCalculatorTool();
	}

	public void calculateAndCompare()
	{
		try
		{
			clearResults();
			calculate();
			compare();
		}
		catch (Exception e)
		{
			getLogger().error("Error while calculating expression", e);
			result = "failed - "+MatrixFunctions.errorToText(e);
		}
	}

	protected void clearResults()
	{
		result = null;
		comparisonResult = null;
	}

	protected void calculate() throws Exception
	{
		Scheduler scheduler = ClearThCore.getInstance().getSchedulersManager().getSchedulerByName(selectedScheduler, UserInfoUtils.getUserName());
		getLogger().info("Selected scheduler: " + ((scheduler != null) ? scheduler.getName() : "[null]"));
		result = calculator.calculate(expression, scheduler);
	}

	protected void compare() throws ParametersException
	{
		if ((toCompare != null) && (!toCompare.isEmpty()))
			comparisonResult = comparisonUtils().compareValues(result, toCompare) ? "Passed" : "Failed";
		else
			comparisonResult = null;
	}

	
	public void clearInput()
	{
		expression = "";
		selectedScheduler = null;
		toCompare = null;
	}

	public void clearResult()
	{
		result = null;
	}

	public boolean isHasResults()
	{
		return result == null ? false : true;
	}

	
	public List<CalculatorVariable> getVars()
	{
		return calculator.getVariables();
	}
	
	public void generateNewVar()
	{
		try
		{
			addVar(generateVarName(), null);
		}
		catch (ParametersException e)
		{
			MessageUtils.addErrorMessage(NEW_VAR_ERROR, e.getMessage());
		}
		verifyVars();
	}

	public void generateNewVar(int count)
	{
		try
		{
			for (int i = 0; i < count; i++)
				addVar(generateVarName(), null);
		}
		catch (ParametersException e)
		{
			MessageUtils.addErrorMessage(NEW_VAR_ERROR, e.getMessage());
		}
		verifyVars();
	}

	protected String generateVarName()
	{
		return MessageFormat.format(GEN_VAR_NAME_PATTERN, genVarNameSuffix++);
	}

	protected void addVar(String name, String value) throws ParametersException
	{
		if (calculator.getVariables().size() >= MAX_VARS_COUNT)
			throw new ParametersException(MAX_VARS_ERROR);
		calculator.getVariables().add(new CalculatorVariable(name, value));
	}
	
	public void verifyVars()
	{
		calculator.verifyVariables();
	}

	
	public void deleteSelectedVar()
	{
		calculator.getVariables().remove(selectedVar);
		selectedVar = null;
		verifyVars();
	}

	public void undefineSelectedVar()
	{
		selectedVar.setValue(null);
	}

	public void clearVars()
	{
		calculator.getVariables().clear();
	}
	
	
	public List<HistoryRow> getHistory()
	{
		return calculator.getHistory();
	}
	
	
	public void restoreFromHistory()
	{
		if (selectedHistoryRow == null)
			return;
		
		expression = selectedHistoryRow.getExpression();
	}
	
	public void copyUsage(String usg)
	{
		expression = expression + usg;
	}

	public ExpressionCalculatorTool getCalculator()
	{
		return calculator;
	}

	public String getResult()
	{
		return result;
	}

	public String getComparisonResult()
	{
		return comparisonResult;
	}

	public String getExpression()
	{
		return expression;
	}

	public void setExpression(String expression)
	{
		this.expression = expression;
	}

	public String getToCompare()
	{
		return toCompare;
	}

	public void setToCompare(String toCompare)
	{
		this.toCompare = toCompare;
	}

	public String getSelectedScheduler()
	{
		return selectedScheduler;
	}

	public void setSelectedScheduler(String selectedScheduler)
	{
		this.selectedScheduler = selectedScheduler;
	}

	public CalculatorVariable getSelectedVar()
	{
		return selectedVar;
	}

	public void setSelectedVar(CalculatorVariable selectedVar)
	{
		this.selectedVar = selectedVar;
	}

	public HistoryRow getSelectedHistoryRow()
	{
		return selectedHistoryRow;
	}

	public void setSelectedHistoryRow(HistoryRow selectedHistoryRow)
	{
		this.selectedHistoryRow = selectedHistoryRow;
	}
}
