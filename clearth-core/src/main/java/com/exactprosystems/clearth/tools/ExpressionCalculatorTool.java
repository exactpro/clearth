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

package com.exactprosystems.clearth.tools;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.ExecutorFactory;
import com.exactprosystems.clearth.automation.MatrixFunctions;
import com.exactprosystems.clearth.automation.Scheduler;
import com.exactprosystems.clearth.tools.calculator.CalculatorVariable;
import com.exactprosystems.clearth.tools.calculator.HistoryRow;
import com.exactprosystems.clearth.utils.ObjectWrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by alexander.magomedov on 11/16/16.
 */
public class ExpressionCalculatorTool
{
	private static final Logger logger = LoggerFactory.getLogger(ExpressionCalculatorTool.class);
	
	private int maxHistorySize = 10;
	private final List<CalculatorVariable> variables = new ArrayList<CalculatorVariable>();
	private final LinkedList<HistoryRow> history = new LinkedList<HistoryRow>();
	
	
	public String calculate(String expression, Scheduler scheduler) throws Exception
	{
		MatrixFunctions matrixFunctions = initMatrixFunctions(scheduler);
		Map<String, Object> mvelVars = makeMvelVars();
		String result = calculate(expression, matrixFunctions, mvelVars);
		storeResult(expression, result);
		return result;
	}
	
	public void verifyVariables()
	{
		resetVariablesStatus();
		checkForDuplicates();
		checkVariables();
	}
	
	public List<CalculatorVariable> getVariables()
	{
		return variables;
	}
	
	public LinkedList<HistoryRow> getHistory()
	{
		return history;
	}
	
	
	public int getMaxHistorySize()
	{
		return maxHistorySize;
	}
	
	public void setMaxHistorySize(int maxHistorySize)
	{
		this.maxHistorySize = maxHistorySize;
	}
	
	
	protected MatrixFunctions initMatrixFunctions(Scheduler scheduler)
	{
		ClearThCore coreInstance = ClearThCore.getInstance();
		if (scheduler != null)
			return coreInstance.createMatrixFunctions(scheduler);
		return coreInstance.createMatrixFunctions(new LinkedHashMap<String, Boolean>(), new Date(), null, false);
	}
	
	
	@SuppressWarnings("unchecked")
	private void addVarToMap(String name, String value, Map<String, Object> map)
	{
		int dotIndex = name.indexOf(".");
		
		//if name doesn't look like "container.variable", just put it as is
		if (dotIndex < 0)
			map.put(name, value);
		else
		{
			//variable is expected to be nested in a container
			String containerName = name.substring(0, dotIndex),
					varName = name.substring(dotIndex+1);
			
			Object containerObj = map.get(containerName);
			if ((containerObj == null) || !(containerObj instanceof Map))
			{
				containerObj = new HashMap<String, Object>();
				map.put(containerName, containerObj);
			}
			addVarToMap(varName, value, (Map<String, Object>)containerObj);
		}
	}
	
	protected Map<String, Object> makeMvelVars()
	{
		if (variables.isEmpty())
			return null;
		
		Map<String, Object> result = new HashMap<String, Object>();
		for (CalculatorVariable var : variables)
		{
			if (var.isValid())
				addVarToMap(var.getName(), var.getValue(), result);
		}
		return result;
	}
	
	
	protected String wrapExpression(String exp)
	{
		if (!exp.startsWith(MatrixFunctions.FORMULA_START) && !exp.endsWith(MatrixFunctions.FORMULA_END))
			return MatrixFunctions.FORMULA_START + exp + MatrixFunctions.FORMULA_END;
		return exp;
	}
	
	protected String calculate(String expression, MatrixFunctions matrixFunctions, Map<String, Object> vars) throws Exception
	{
		expression = wrapExpression(expression);
		logger.debug("Calculating expression '" + expression + "'");
		ObjectWrapper objectWrapper = new ObjectWrapper(0);
		return matrixFunctions.calculateExpression(expression, null, vars, null, null, objectWrapper).toString();
	}
	
	
	protected void storeResult(String expression, String result)
	{
		history.add(0, new HistoryRow(expression, result));
		while (history.size() > maxHistorySize)
			history.removeLast();
	}
	
	
	protected void resetVariablesStatus()
	{
		for (CalculatorVariable var : variables)
			var.setStatusCode(CalculatorVariable.UNCHECKED);
	}
	
	
	protected void checkForDuplicates(CalculatorVariable var)
	{
		if (var.isChecked())
			return;
		
		String varName = var.getName();
		for (CalculatorVariable anotherVar : variables) {
			if (anotherVar.isChecked())
				continue;
			
			if ((var != anotherVar) && (varName.equals(anotherVar.getName())))
			{
				var.setStatusCode(CalculatorVariable.DUPLICATED_NAME);
				anotherVar.setStatusCode(CalculatorVariable.DUPLICATED_NAME);
			}
		}
	}
	
	protected void checkForDuplicates()
	{
		for (CalculatorVariable var : variables)
			checkForDuplicates(var);
	}
	
	protected boolean checkVariableName(CalculatorVariable var, String name)
	{
		if (!CalculatorVariable.validateId(name))
		{
			var.setStatusCode(CalculatorVariable.WRONG_FORMAT);
			var.setStatusDetails("Variable name should start with non-digit character. Only letters, digits, '$' and '_' characters are allowed in name.");
			return false;
		}
		return true;
	}
	
	protected void checkVariable(CalculatorVariable var, String name)
	{
		if (var.isChecked())
			return;
		
		if (name == null || name.trim().isEmpty())
		{
			var.setStatusCode(CalculatorVariable.NAME_NOT_DEFINED);
			return;
		}
		
		if (name.startsWith(".") || name.endsWith("."))
		{
			var.setStatusCode(CalculatorVariable.WRONG_FORMAT);
			var.setStatusDetails("Dot can be only in the middle of variable name");
			return;
		}
		
		String[] splittedParts = name.split("\\.");
		if (splittedParts.length > 1)
		{
			if (checkVariableName(var, splittedParts[0]));
				checkVariable(var, name.substring(name.indexOf(".")+1));
			return;
		}
		
		if (checkVariableName(var, name))
			var.setStatusCode(CalculatorVariable.PASSED);
	}
	
	protected void checkVariables()
	{
		for (CalculatorVariable var : variables)
			checkVariable(var, var.getName());
	}
}
