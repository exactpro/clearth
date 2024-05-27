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
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.*;
import com.exactprosystems.clearth.automation.exceptions.AutomationException;
import com.exactprosystems.clearth.automation.report.Result;
import com.thoughtworks.xstream.annotations.XStreamAlias;

import org.apache.commons.lang.StringUtils;

@XStreamAlias("ActionState")
public class ActionState
{
	private Class<?> actionClass = null;

	private Set<String> matrixInputParams = null;
	private Map<String, String> inputParams = null, 
			specialParams = null;
	private LinkedHashMap<String, SubActionData> subActionsData = null;
	//outputParams, subOutputParams, duplicateParams, formulas are cleaned after action end, no need to store them

	/**
	 * {@link #idInTemplate} field can be used in some template or matrix generator tool if you have one.
	 * Can be helpful to debug matrix.
	 */
	private String idInMatrix, comment, name, stepName, idInTemplate;
	private boolean executable = true, inverted = false, done = false, passed = true, suspendIfFailed = false;
	/**
	 * {@link #formulaIdInTemplate} same as {@link #idInTemplate} but set with formula.
	 */
	private String formulaExecutable, formulaInverted, formulaComment, formulaTimeout, formulaIdInTemplate;
	
	private long timeout = 0;
	private ResultState result = null;
	
	private Date started = null, finished = null;
	
	public ActionState()
	{
	}
	
	public ActionState(Action action)
	{
		this.actionClass = action.getClass();

		this.matrixInputParams = action.getMatrixInputParams();
		this.inputParams = action.getInputParams();
		this.specialParams = action.getSpecialParams();
		this.subActionsData = action.getSubActionData();
		
		this.idInMatrix = action.getIdInMatrix();
		this.comment = action.getComment();
		this.formulaComment = action.getFormulaComment();
		this.name = action.getName();
		this.stepName = action.getStepName();
		this.executable = action.isExecutable();
		this.formulaExecutable = action.getFormulaExecutable();
		this.inverted = action.isInverted();
		this.formulaInverted = action.getFormulaInverted();
		this.done = action.isDone();
		this.passed = action.isPassed();
		this.suspendIfFailed = action.isSuspendIfFailed();
		
		this.timeout = action.getTimeOut();
		this.formulaTimeout = action.getFormulaTimeout();
		
		this.result = action.getResult()!=null ? createResultState(action.getResult()) : null;
		
		this.started = action.getStarted();
		this.finished = action.getFinished();

		idInTemplate = action.getIdInTemplate();
		formulaIdInTemplate = action.getFormulaIdInTemplate();
	}
	
	public Action actionFromState(List<Step> steps) throws IllegalArgumentException, SecurityException, InstantiationException, 
			IllegalAccessException, InvocationTargetException, NoSuchMethodException, AutomationException
	{
		ActionFactory factory = ClearThCore.getInstance().getActionFactory();
		Action result = factory.createAction(name);
		ActionSettings settings = factory.createActionSettings();
		
		settings.setParams(inputParams);
		settings.setMatrixInputParams(matrixInputParams);
		settings.setSpecialParams(specialParams);
		settings.setActionId(idInMatrix);
		settings.setComment(comment);
		settings.setFormulaComment(formulaComment);
		settings.setActionName(name);
		for(Step step : steps)
		{
			if(StringUtils.equals(step.getName(),stepName))
			{
				settings.setStep(step);
				break;
			}
		}
		settings.setExecutable(executable);
		settings.setFormulaExecutable(formulaExecutable);
		settings.setInverted(inverted);
		settings.setFormulaInverted(formulaInverted);
		settings.setTimeout(timeout);
		settings.setFormulaTimeout(formulaTimeout);
		settings.setSuspendIfFailed(suspendIfFailed);
		settings.setIdInTemplate(idInTemplate);
		settings.setFormulaIdInTemplate(formulaIdInTemplate);
		
		initActionSettings(settings);
		
		result.init(settings);
		
		result.setSubActionsData(this.subActionsData);
		
		result.setDone(this.done);
		result.setPassed(this.passed);
		
		if (this.result!=null)
			result.setResult(this.result.resultFromState());
		
		result.setStarted(this.started);
		result.setFinished(this.finished);
		
		return result;
	}
	
	
	protected ResultState createResultState(Result result)
	{
		return new ResultState(result);
	}
	
	protected void initActionSettings(ActionSettings settings)
	{
	}
	
	
	public Class<?> getActionClass()
	{
		return actionClass;
	}
	
	public void setActionClass(Class<?> actionClass)
	{
		this.actionClass = actionClass;
	}
	
	
	public Map<String, String> getInputParams()
	{
		return inputParams;
	}
	
	public void setInputParams(Map<String, String> inputParams)
	{
		this.inputParams = inputParams;
	}

	public Map<String, String> getSpecialParams()
	{
		return specialParams;
	}

	public void setSpecialParams(Map<String, String> specialParams)
	{
		this.specialParams = specialParams;
	}

	public LinkedHashMap<String,SubActionData> getSubActionsData()
	{
		return subActionsData;
	}
	
	public void setSubActionsData( LinkedHashMap<String,SubActionData> subActionsData)
	{
		this.subActionsData = subActionsData;
	}
	
	
	public String getIdInMatrix()
	{
		return idInMatrix;
	}
	
	public void setIdInMatrix(String idInMatrix)
	{
		this.idInMatrix = idInMatrix;
	}
	
	
	public String getComment()
	{
		return comment;
	}
	
	public void setComment(String comment)
	{
		this.comment = comment;
	}
	
	
	public String getName()
	{
		return name;
	}
	
	public void setName(String name)
	{
		this.name = name;
	}

	public String getStepName()
	{
		return stepName;
	}

	public void setStepName(String stepName)
	{
		this.stepName = stepName;
	}

	public boolean isExecutable()
	{
		return executable;
	}
	
	public void setExecutable(boolean executable)
	{
		this.executable = executable;
	}
	
	
	public boolean isInverted()
	{
		return inverted;
	}
	
	public void setInverted(boolean inverted)
	{
		this.inverted = inverted;
	}
	
	
	public boolean isDone()
	{
		return done;
	}
	
	public void setDone(boolean done)
	{
		this.done = done;
	}
	
	
	public boolean isPassed()
	{
		return passed;
	}
	
	public void setPassed(boolean passed)
	{
		this.passed = passed;
	}
	
	
	public long getTimeout()
	{
		return timeout;
	}
	
	public void setTimeout(long timeout)
	{
		this.timeout = timeout;
	}
	
	
	public ResultState getResult()
	{
		return result;
	}
	
	public void setResult(ResultState result)
	{
		this.result = result;
	}
	
	
	public Date getStarted()
	{
		return started;
	}
	
	public void setStarted(Date started)
	{
		this.started = started;
	}
	
	
	public Date getFinished()
	{
		return finished;
	}
	
	public void setFinished(Date finished)
	{
		this.finished = finished;
	}


	public String getIdInTemplate()
	{
		return idInTemplate;
	}

	public void setIdInTemplate(String idInTemplate)
	{
		this.idInTemplate = idInTemplate;
	}


	public String getFormulaIdInTemplate()
	{
		return formulaIdInTemplate;
	}

	public void setFormulaIdInTemplate(String formulaIdInTemplate)
	{
		this.formulaIdInTemplate = formulaIdInTemplate;
	}
}
