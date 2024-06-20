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

package com.exactprosystems.clearth.automation.persistence;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.*;
import com.exactprosystems.clearth.automation.exceptions.AutomationException;
import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("MatrixState")
public class MatrixState
{
	private String fileName = null,
			name = null,
			description = null;
	
	private transient List<ActionState> actions = new ArrayList<ActionState>();
	private transient MvelVariables mvelVars;
	private Map<String, Boolean> stepSuccess = null;
	private Map<String, List<String>> stepStatusComments = null;
	private Map<String, String> constants;
	
	private Date started;
	private int actionsDone = 0;
	private boolean successful = true;
	
	private MatrixContext context = null;
	private MatrixData matrixData;
	
	
	public MatrixState()
	{
	}
	
	public MatrixState(Matrix matrix, Function<Action, ActionState> actionToState)
	{
		this.fileName = matrix.getFileName();
		this.name = matrix.getName();
		this.description = matrix.getDescription();
		
		for (Action action : matrix.getActions())
			this.actions.add(actionToState.apply(action));
		this.mvelVars = matrix.getMvelVars();
		this.stepSuccess = matrix.getStepSuccess();
		this.stepStatusComments = matrix.getStepStatusComments();
		this.constants = matrix.getConstants();
		
		this.started = matrix.getStarted();
		this.actionsDone = matrix.getActionsDone();
		this.successful = matrix.isSuccessful();
		
		this.context = matrix.getContext();
		this.matrixData = matrix.getMatrixData();
	}
	
	public Matrix matrixFromState(List<Step> steps) throws IllegalArgumentException, SecurityException, InstantiationException, 
			IllegalAccessException, InvocationTargetException, NoSuchMethodException, AutomationException
	{
		Matrix result = new Matrix(ClearThCore.getInstance().getMvelVariablesFactory());
		
		result.setFileName(this.fileName);
		result.setName(this.name);
		result.setDescription(this.description);
		
		if (this.actions!=null)
			for (ActionState actionState : this.actions)
			{
				Action a = actionState.actionFromState(steps);
				a.setMatrix(result);
				result.getActions().add(a);
			}
		result.setMvelVars(this.mvelVars);
		result.setStepSuccess(this.stepSuccess);
		result.setStepStatusComments(this.stepStatusComments);
		if (this.constants != null)
			result.getConstants().putAll(this.constants);
		
		result.setStarted(this.started);
		result.setActionsDone(this.actionsDone);
		result.setSuccessful(this.successful);
		
		result.setContext(context);
		result.setMatrixData(matrixData);
		
		return result;
	}
	
	
	public String getFileName()
	{
		return fileName;
	}
	
	public void setFileName(String fileName)
	{
		this.fileName = fileName;
	}
	
	
	public List<ActionState> getActions()
	{
		return actions;
	}
	
	public void setActions(List<ActionState> actions)
	{
		this.actions = actions;
	}
	
	
	public MvelVariables getMvelVars()
	{
		return mvelVars;
	}
	
	public void setMvelVars(MvelVariables mvelVars)
	{
		this.mvelVars = mvelVars;
	}
	
	
	public Map<String, Boolean> getStepSuccess()
	{
		return stepSuccess;
	}
	
	public void setStepSuccess(Map<String, Boolean> stepSuccess)
	{
		this.stepSuccess = stepSuccess;
	}
	
	
	public Map<String, List<String>> getStepStatusComments()
	{
		return stepStatusComments;
	}
	
	public void setStepStatusComments(Map<String, List<String>> stepStatusComments)
	{
		this.stepStatusComments = stepStatusComments;
	}
	
	
	public Date getStarted()
	{
		return started;
	}
	
	public void setStarted(Date started)
	{
		this.started = started;
	}
	
	
	public int getActionsDone()
	{
		return actionsDone;
	}
	
	public void setActionsDone(int actionsDone)
	{
		this.actionsDone = actionsDone;
	}
	
	
	public boolean isSuccessful()
	{
		return successful;
	}
	
	public void setSuccessful(boolean successful)
	{
		this.successful = successful;
	}
	
	
	public MatrixContext getContext()
	{
		return context;
	}
	
	public void setContext(MatrixContext context)
	{
		this.context = context;
	}
	
	
	public MatrixData getMatrixData()
	{
		return matrixData;
	}
	
	public void setMatrixData(MatrixData matrixData)
	{
		this.matrixData = matrixData;
	}
	
	
	public String getName()
	{
		return name;
	}
	
	public void setName(String name)
	{
		this.name = name;
	}
	
	
	public String getDescription()
	{
		return description;
	}
	
	public void setDescription(String description)
	{
		this.description = description;
	}
	
	
	public Map<String, String> getConstants()
	{
		return constants;
	}
	
	public void setConstants(Map<String, String> constants)
	{
		this.constants = constants;
	}
}
