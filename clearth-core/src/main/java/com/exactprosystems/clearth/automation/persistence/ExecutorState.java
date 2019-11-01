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

package com.exactprosystems.clearth.automation.persistence;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.exactprosystems.clearth.automation.exceptions.AutomationException;
import org.apache.commons.io.FileUtils;

import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.Executor;
import com.exactprosystems.clearth.automation.ExecutorFactory;
import com.exactprosystems.clearth.automation.GlobalContext;
import com.exactprosystems.clearth.automation.Matrix;
import com.exactprosystems.clearth.automation.Preparable;
import com.exactprosystems.clearth.automation.ReportsInfo;
import com.exactprosystems.clearth.automation.Scheduler;
import com.exactprosystems.clearth.automation.Step;
import com.exactprosystems.clearth.automation.StepContext;
import com.exactprosystems.clearth.automation.StepFactory;
import com.exactprosystems.clearth.utils.XmlUtils;

public abstract class ExecutorState
{
	public static final String STATEOBJECTS_FILENAME = "stateobjects.xml", STATEINFO_FILENAME = "stateinfo.xml";
	protected static final int MAXACTIONS = 100, MAXVARS = 100;
	protected Map<String, Preparable> preparableActions;
	
	public List<StepState> steps = new ArrayList<StepState>();
	public boolean weekendHoliday = true;
	public Map<String, Boolean> holidays = null;
	public Date businessDay = null;
	public String startedByUser = null;
	public Date started = null, ended = null;
	public ReportsInfo reportsInfo = null;  //Need it to have access to reports from "Saved state" tab of "Automation" page
	
	public List<MatrixState> matrices = new ArrayList<MatrixState>();
	public Map<String, String> fixedIDs = null;
	
	public ExecutorState() 
	{
	}
	
	public ExecutorState(Executor executor, StepFactory stepFactory, ReportsInfo reportsInfo)
	{
		for (Step step : executor.getSteps())
			this.steps.add(stepFactory.createStepState(step));
		this.weekendHoliday = executor.isWeekendHoliday();
		this.holidays = executor.getHolidays();
		this.businessDay = executor.getBusinessDay();
		this.startedByUser = executor.getStartedByUser();
		this.started = executor.getStarted();
		this.ended = executor.getEnded();
		this.reportsInfo = reportsInfo;
		
		for (Matrix matrix : executor.getMatrices())
			this.matrices.add(createMatrixState(matrix));
		this.fixedIDs = executor.getFixedIds();
	}
	
	public ExecutorState(File sourceDir) throws IOException
	{
		load(sourceDir);
	}
	
	
	protected abstract MatrixState createMatrixState(Matrix matrix);
	protected abstract void initExecutor(Executor executor);
	
	protected abstract ExecutorStateInfo createStateInfo();
	protected abstract void initStateInfo(ExecutorStateInfo stateInfo);
	protected abstract void initFromStateInfo(ExecutorStateInfo stateInfo);
	
	protected abstract ExecutorStateObjects createStateObjects();
	protected abstract void initStateObjects(ExecutorStateObjects stateObjects);
	
	protected abstract Class[] getActionStateAnnotations();
	protected abstract Class[] getStateInfoAnnotations();
	protected abstract Class[] getStateObjectsAnnotations();
	
	
	public Executor executorFromState(Scheduler scheduler, ExecutorFactory executorFactory, Date businessDay, Date baseTime, String startedByUser) throws IllegalArgumentException, SecurityException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, AutomationException {
		preparableActions = new HashMap<String, Preparable>();
		List<Step> steps = null;
		Map<Step, Map<String, StepContext>> allStepContexts = null;
		if (this.steps!=null)
		{
			steps = new ArrayList<Step>();
			allStepContexts = new HashMap<Step, Map<String,StepContext>>();
			for (StepState stepState : this.steps)
			{
				Step restored = stepState.stepFromState(scheduler.getStepFactory());
				steps.add(restored);
				allStepContexts.put(restored, stepState.getStepContexts());
			}
		}
		
		List<Matrix> matrices = null;
		if (this.matrices!=null)
		{
			matrices = new ArrayList<Matrix>();
			for (MatrixState matrixState : this.matrices)
			{
				Matrix m = matrixState.matrixFromState(steps);
				matrices.add(m);
				if (steps != null)
				{
					for (Action a : m.getActions())
						for (Step step : steps)
							if (step.getName().equals(a.getStepName()))
							{
								step.addAction(a);
								if (!preparableActions.containsKey(a.getName()) && a.isExecutable() && a instanceof Preparable)
									preparableActions.put(a.getName(), (Preparable)a);
								break;
							}
					
					for (Step step : steps)
					{
						Map<String, StepContext> stepContexts = allStepContexts.get(step);
						if (stepContexts == null)
							continue;
						
						if (stepContexts.containsKey(m.getName()))
						{
							Map<Matrix, StepContext> sc = step.getStepContexts();
							if (sc == null)
							{
								sc = new LinkedHashMap<Matrix, StepContext>();
								step.setStepContexts(sc);
							}
							sc.put(m, stepContexts.get(m.getName()));
						}
					}
				}
			}
		}
		
		GlobalContext globalContext = executorFactory.createGlobalContext(businessDay, baseTime, this.weekendHoliday, this.holidays, startedByUser);
		
		Executor result = executorFactory.createExecutor(scheduler, steps, matrices, globalContext, preparableActions);
		result.setFixedIds(this.fixedIDs);
		result.setStarted(this.started);
		result.setEnded(this.ended);
		initExecutor(result);
		
		return result;
	}
	
	
	protected static String actionsFileName(String matrixShortFileName, int fileIndex)
	{
		return matrixShortFileName+"_actions_"+fileIndex+".xml";
	}
	
	protected static String varsFileName(String matrixShortFileName, int fileIndex)
	{
		return matrixShortFileName+"_vars_"+fileIndex+".xml";
	}
	
	public void save(File destDir) throws IOException
	{
		if (destDir.exists())
			FileUtils.deleteDirectory(destDir);
		destDir.mkdirs();
		
		List<String> matricesNames = new ArrayList<String>();
		if (matrices!=null)
		{
			for (MatrixState matrix : matrices)
			{
				String shortName = new File(matrix.getFileName()).getName();
				matricesNames.add(shortName);
				
				//If there are too many actions, such XML cannot be unmarshalled due to lack of memory. Writing such large list as few portions in separate files
				if (matrix.getActions()!=null)// && (matrix.getActions().size()>MAXACTIONS))
				{
					int index = 1;
					List<ActionState> states = new ArrayList<ActionState>();
					for (ActionState action : matrix.getActions())
					{
						states.add(action);
						if (states.size()>=MAXACTIONS)
						{
							XmlUtils.objectToXmlFile(states, new File(destDir, actionsFileName(shortName, index)), getActionStateAnnotations());
							index++;
							states.clear();
						}
					}
					if (states.size()>0)
						XmlUtils.objectToXmlFile(states, new File(destDir, actionsFileName(shortName, index)), getActionStateAnnotations());
//					matrix.getActions().clear();
				}
				
				if (matrix.getMvelVars()!=null)// && (matrix.getMvelVars().size()>MAXVARS))
				{
					int index = 1;
					Map<String, Object> vars = new LinkedHashMap<String, Object>();
					for (String key : matrix.getMvelVars().keySet())
					{
						vars.put(key, matrix.getMvelVars().get(key));
						if (vars.size()>=MAXVARS)
						{
							XmlUtils.objectToXmlFile(vars, new File(destDir, varsFileName(shortName, index)), null);
							index++;
							vars.clear();
						}
					}
					if (vars.size()>0)
						XmlUtils.objectToXmlFile(vars, new File(destDir, varsFileName(shortName, index)), null);
//					matrix.getMvelVars().clear();
				}
			}
		}
		
		ExecutorStateInfo stateInfo = createStateInfo();
		stateInfo.setSteps(steps);
		stateInfo.setMatrices(matricesNames);
		stateInfo.setWeekendHoliday(weekendHoliday);
		stateInfo.setHolidays(holidays);
		stateInfo.setBusinessDay(businessDay);
		stateInfo.setStartedByUser(startedByUser);
		stateInfo.setStarted(started);
		stateInfo.setEnded(ended);
		stateInfo.setReportsInfo(reportsInfo);
		initStateInfo(stateInfo);
		saveStateInfo(destDir, stateInfo, getStateInfoAnnotations());
		
		ExecutorStateObjects stateObjects = createStateObjects();
		stateObjects.setMatrices(matrices);
		stateObjects.setFixedIDs(fixedIDs);
		initStateObjects(stateObjects);
		XmlUtils.objectToXmlFile(stateObjects, new File(destDir, STATEOBJECTS_FILENAME), getStateObjectsAnnotations());
	}
	
	public void load(File sourceDir) throws IOException
	{
		ExecutorStateInfo stateInfo = loadStateInfo(sourceDir, getStateInfoAnnotations());
		ExecutorStateObjects stateObjects = (ExecutorStateObjects)XmlUtils.xmlFileToObject(new File(sourceDir, STATEOBJECTS_FILENAME), getStateObjectsAnnotations());
		
		steps = stateInfo.getSteps();
		weekendHoliday = stateInfo.isWeekendHoliday();
		holidays = stateInfo.getHolidays();
		businessDay = stateInfo.getBusinessDay();
		startedByUser = stateInfo.getStartedByUser();
		started = stateInfo.getStarted();
		ended = stateInfo.getEnded();
		reportsInfo = stateInfo.getReportsInfo();
		initFromStateInfo(stateInfo);
		
		matrices = stateObjects.getMatrices();
		fixedIDs = stateObjects.getFixedIDs();
		
		if (matrices!=null)
		{
			for (MatrixState matrix : matrices)
			{
				String shortName = new File(matrix.getFileName()).getName();
				int index = 1;
				File actionFile = new File(sourceDir, actionsFileName(shortName, index));
				while (actionFile.isFile())
				{
					List<ActionState> actions = (List<ActionState>)XmlUtils.xmlFileToObject(actionFile, getActionStateAnnotations());
					if (matrix.getActions()==null)
						matrix.setActions(new ArrayList<ActionState>());
					matrix.getActions().addAll(actions);
					
					index++;
					actionFile = new File(sourceDir, actionsFileName(shortName, index));
				}
				
				index = 1;
				File varsFile = new File(sourceDir, varsFileName(shortName, index));
				while (varsFile.isFile())
				{
					Map<String, Object> vars = (Map<String, Object>)XmlUtils.xmlFileToObject(varsFile, null);
					if (matrix.getMvelVars()==null)
						matrix.setMvelVars(new LinkedHashMap<String, Object>());
					matrix.getMvelVars().putAll(vars);
					
					index++;
					varsFile = new File(sourceDir, varsFileName(shortName, index));
				}
			}
		}
	}
	
	
	public static void saveStateInfo(File destDir, ExecutorStateInfo stateInfo, Class[] stateInfoAnnotations) throws IOException
	{
		XmlUtils.objectToXmlFile(stateInfo, new File(destDir, STATEINFO_FILENAME), stateInfoAnnotations);
	}
	
	public static ExecutorStateInfo loadStateInfo(File sourceDir, Class[] stateInfoAnnotations) throws IOException
	{
		return (ExecutorStateInfo)XmlUtils.xmlFileToObject(new File(sourceDir, STATEINFO_FILENAME), stateInfoAnnotations);
	}
}
