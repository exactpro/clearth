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

package com.exactprosystems.clearth.automation.persistence.db;

import java.util.HashMap;
import java.util.Map;

import com.exactprosystems.clearth.automation.persistence.ExecutorStateException;
import com.exactprosystems.clearth.automation.persistence.ExecutorStateContext;

public class DbStateContext implements ExecutorStateContext
{
	private final int infoId;
	private final Map<String, Integer> stepIds = new HashMap<>(),
			matrixIds = new HashMap<>();
	private final Map<Integer, String> stepNames = new HashMap<>(),
			matrixNames = new HashMap<>();
	private final Map<String, Map<String, Integer>> matrixActionsIds = new HashMap<>();
	private final Map<MatrixStepReference, Integer> stepContextIds = new HashMap<>(),
			stepSuccessIds = new HashMap<>(),
			stepStatusCommentsIds = new HashMap<>();
	
	public DbStateContext(int infoId)
	{
		this.infoId = infoId;
	}
	
	
	public int getInfoId()
	{
		return infoId;
	}
	
	
	public int getActionId(ActionReference actionRef) throws ExecutorStateException
	{
		Map<String, Integer> actionsIds = matrixActionsIds.get(actionRef.getMatrixName());
		if (actionsIds == null)
			throw actionIdNotFound(actionRef);
		
		Integer id = actionsIds.get(actionRef.getId());
		if (id == null)
			throw actionIdNotFound(actionRef);
		return id;
	}
	
	public void setActionId(ActionReference actionRef, int id)
	{
		matrixActionsIds.computeIfAbsent(actionRef.getMatrixName(), name -> new HashMap<>())
				.put(actionRef.getId(), id);
	}
	
	public void removeActionIds(String matrixName)
	{
		matrixActionsIds.remove(matrixName);
	}
	
	
	public int getStepId(String stepName) throws ExecutorStateException
	{
		Integer id = stepIds.get(stepName);
		if (id == null)
			throw new ExecutorStateException("No ID stored for step '"+stepName+"'");
		return id;
	}
	
	public String getStepName(int stepId) throws ExecutorStateException
	{
		String name = stepNames.get(stepId);
		if (name == null)
			throw new ExecutorStateException("No name stored for step "+stepId);
		return name;
	}
	
	public void setStepId(String stepName, int id)
	{
		stepIds.put(stepName, id);
		stepNames.put(id, stepName);
	}
	
	
	public int getMatrixId(String matrixName) throws ExecutorStateException
	{
		Integer id = matrixIds.get(matrixName);
		if (id == null)
			throw new ExecutorStateException("No ID stored for matrix '"+matrixName+"'");
		return id;
	}
	
	public String getMatrixName(int matrixId) throws ExecutorStateException
	{
		String name = matrixNames.get(matrixId);
		if (name == null)
			throw new ExecutorStateException("No name stored for matrix "+matrixId);
		return name;
	}
	
	public void setMatrixId(String matrixName, int id)
	{
		matrixIds.put(matrixName, id);
		matrixNames.put(id, matrixName);
	}
	
	
	public Integer getStepContextIdOrNull(int stepId, int matrixId)
	{
		return stepContextIds.get(new MatrixStepReference(matrixId, stepId));
	}
	
	public int getStepContextId(int stepId, int matrixId) throws ExecutorStateException
	{
		Integer id = getStepContextIdOrNull(stepId, matrixId);
		if (id == null)
			throw new ExecutorStateException("No ID stored for step context of step "+stepId+" and matrix "+matrixId);
		return id;
	}
	
	public void setStepContextId(int stepId, int matrixId, int id)
	{
		stepContextIds.put(new MatrixStepReference(matrixId, stepId), id);
	}
	
	
	public Integer getStepSuccessIdOrNull(int matrixId, int stepId)
	{
		return stepSuccessIds.get(new MatrixStepReference(matrixId, stepId));
	}
	
	public int getStepSuccessId(int matrixId, int stepId) throws ExecutorStateException
	{
		Integer id = getStepSuccessIdOrNull(matrixId, stepId);
		if (id == null)
			throw new ExecutorStateException("No ID stored for step success of matrix "+matrixId+" and step "+stepId);
		return id;
	}
	
	public void setStepSuccessId(int matrixId, int stepId, int id)
	{
		stepSuccessIds.put(new MatrixStepReference(matrixId, stepId), id);
	}
	
	
	public Integer getStepStatusCommentsIdOrNull(int matrixId, int stepId)
	{
		return stepStatusCommentsIds.get(new MatrixStepReference(matrixId, stepId));
	}
	
	public int getStepStatusCommentsId(int matrixId, int stepId) throws ExecutorStateException
	{
		Integer id = getStepStatusCommentsIdOrNull(matrixId, stepId);
		if (id == null)
			throw new ExecutorStateException("No ID stored for step status comments of matrix "+matrixId+" and step "+stepId);
		return id;
	}
	
	public void setStepStatusCommentsId(int matrixId, int stepId, int id)
	{
		stepStatusCommentsIds.put(new MatrixStepReference(matrixId, stepId), id);
	}
	
	
	private ExecutorStateException actionIdNotFound(ActionReference actionRef)
	{
		return new ExecutorStateException("No ID stored for action '"+actionRef.getId()+"' from matrix '"+actionRef.getMatrixName()+"'");
	}
}
