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

import static com.exactprosystems.clearth.ClearThCore.comparisonUtils;

import java.io.File;
import java.util.*;

public class Matrix
{
	public static final String MATRIX = "matrix";
	public static final String DESCRIPTION = "desc";

	private String name;
	private String fileName, shortFileName, description;

	private List<Action> actions = new ArrayList<Action>();
	private MvelVariables mvelVars;
	private Map<String, Boolean> stepSuccessMap = new HashMap<>();
	private Map<String, List<String>> stepStatusComments = new HashMap<String, List<String>>();
	private Map<String, String> constants = new LinkedHashMap<String, String>();
	private Map<String, String> formulas = null;
	private Set<String> nonExecutableActions = new HashSet<String>();
	
	private Date started = null;
	private int actionsDone = 0;

	private int actionsSuccess = 0;
	private boolean successful = true;
	
	private MatrixContext context = new MatrixContext();
	
	private MatrixData matrixData;

	private List<ActionGeneratorMessage> generatorMessages = null;
	
	
	public Matrix(MvelVariablesFactory mvelVariablesFactory)
	{
		mvelVars = mvelVariablesFactory.create();
	}
	

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getFileName()
	{
		return fileName;
	}

	public void setFileName(String fileName)
	{
		this.fileName = fileName;
		this.shortFileName = new File(fileName).getName();
	}
	
	public String getShortFileName()
	{
		return shortFileName;
	}

	
	public Map<String, String> getFormulas()
	{
		return formulas;
	}
	

	public void addConstant(String name, String value)
	{
		constants.put(name, value);
		saveMatrixInfoToMvel(name, value);
		if (value.contains(MatrixFunctions.FORMULA_START) && !comparisonUtils().isSpecialValue(value))
		{
			if (formulas == null)
				formulas = new HashMap<String, String>();
			formulas.put(name, value);
		}
	}

	public Map<String, String> getConstants()
	{
		return constants;
	}
	
	
	public void setDescription(String description)
	{
		this.description = description;
		saveMatrixInfoToMvel(DESCRIPTION, description);
	}
	
	public String getDescription()
	{
		return description;
	}

	
	protected void saveMatrixInfoToMvel(String name, String value)
	{
		mvelVars.saveMatrixInfo(name, value);
	}

	
	
	public List<Action> getActions()
	{
		return actions;
	}
	
	public void setActions(List<Action> actions)
	{
		this.actions = actions;
	}
	
	
	public MvelVariables getMvelVars()
	{
		return mvelVars;
	}
	
	public void setMvelVars(MvelVariables variables)
	{
		this.mvelVars = variables;
	}
	
	
	public void setStepSuccessful(String stepName, boolean success)
	{
		// We are not able to make Step or Matrix successful if they are already not.
		Boolean stepSuccess = stepSuccessMap.get(stepName);
		if (stepSuccess != null && !stepSuccess)
			return;

		stepSuccessMap.put(stepName, success);

		if (!success && isSuccessful())
			 setSuccessful(false);
	}
	
	public boolean isStepSuccessful(String stepName)
	{
		Boolean success = stepSuccessMap.get(stepName);
		return success == null ? true : success;
	}
	
	/* Method for MatrixState*/
	public Map<String, Boolean> getStepSuccess()
	{
		return stepSuccessMap;
	}
	
	/* Method for MatrixState */
	public void setStepSuccess(Map<String, Boolean> stepSuccessMap)
	{
		this.stepSuccessMap = stepSuccessMap;
	}
	
	
	public void addStepStatusComment(String stepName, String comment)
	{
		List<String> stepComments;
		if (!stepStatusComments.containsKey(stepName))
		{
			stepComments = new ArrayList<String>();
			stepStatusComments.put(stepName, stepComments);
		}
		else
			stepComments = stepStatusComments.get(stepName);
		if (!stepComments.contains(comment))
			stepComments.add(comment);
	}
	
	public List<String> getStepStatusComments(String stepName)
	{
		return stepStatusComments.get(stepName);
	}
	
	/* Method for MatrixState */
	public Map<String, List<String>> getStepStatusComments()
	{
		return stepStatusComments;
	}
	
	/* Method for MatrixState */
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
	
	public void incActionsDone()
	{
		actionsDone++;
	}
	
	public void incActionsSuccess()
	{
		actionsSuccess++;
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
	
	/* Method for MatrixState */
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

	public void addGeneratorMessage(ActionGeneratorMessageType type, ActionGeneratorMessageKind kind, String message)
	{
		if (generatorMessages==null)
			generatorMessages = new ArrayList<ActionGeneratorMessage>();
		generatorMessages.add(new ActionGeneratorMessage(type, kind, message));
	}
	
	public List<ActionGeneratorMessage> getGeneratorMessages()
	{
		return generatorMessages;
	}

	public Set<String> getNonExecutableActions()
	{
		return nonExecutableActions;
	}

	public int getActionsSuccess()
	{
		return actionsSuccess;
	}

	public void setActionsSuccess(int actionsSuccess)
	{
		this.actionsSuccess = actionsSuccess;
	}
}
