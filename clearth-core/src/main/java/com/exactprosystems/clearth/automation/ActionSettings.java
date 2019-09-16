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

import java.util.*;

import com.exactprosystems.clearth.automation.async.WaitAsyncEnd;

import static com.exactprosystems.clearth.ClearThCore.comparisonUtils;

public class ActionSettings
{
	private String actionId = null, actionName = null, comment = null;
	private boolean executable = true, inverted = false, suspendIfFailed = false;
	protected String formulaExecutable = null, formulaInverted = null, formulaComment = null, formulaTimeout = null, 
			formulaAsync = null, formulaAsyncGroup = null, formulaWaitAsyncEnd = null;
	private long timeout = 0;
	
	protected boolean async;
	protected String asyncGroup;
	protected WaitAsyncEnd waitAsyncEnd = WaitAsyncEnd.NO;
	
	private LinkedHashMap<String, String> params = new LinkedHashMap<String, String>();
	private Set<String> matrixInputParams = null;
	private List<String> duplicateParams = null;
	private HashMap<String, String> formulas = null;
	
	private Matrix matrix = null;
	private Step step = null;
	
	
	public String getActionId()
	{
		return actionId;
	}
	
	public void setActionId(String actionId)
	{
		this.actionId = actionId;
	}
	
	
	public Step getStep()
	{
		return step;
	}
	
	public void setStep(Step step)
	{
		this.step = step;
	}

	public String getActionName()
	{
		return actionName;
	}
	
	public void setActionName(String actionName)
	{
		this.actionName = actionName;
	}
	
	
	public String getComment()
	{
		return comment;
	}
	
	public void setComment(String comment)
	{
		this.comment = comment;
	}
	
	public String getFormulaComment()
	{
		return formulaComment;
	}

	public void setFormulaComment(String formulaComment)
	{
		this.formulaComment = formulaComment;
	}
	
	
	public boolean isExecutable()
	{
		return executable;
	}
	
	public void setExecutable(boolean executable)
	{
		this.executable = executable;
	}
	
	public String getFormulaExecutable()
	{
		return formulaExecutable;
	}

	public void setFormulaExecutable(String formulaExecutable)
	{
		this.formulaExecutable = formulaExecutable;
	}
	
	
	public boolean isInverted()
	{
		return inverted;
	}
	
	public void setInverted(boolean inverted)
	{
		this.inverted = inverted;
	}
	
	public String getFormulaInverted()
	{
		return formulaInverted;
	}

	public void setFormulaInverted(String formulaInverted)
	{
		this.formulaInverted = formulaInverted;
	}
	
	
	public long getTimeout()
	{
		return timeout;
	}
	
	public void setTimeout(long timeout)
	{
		this.timeout = timeout;
	}
	
	public String getFormulaTimeout()
	{
		return formulaTimeout;
	}

	public void setFormulaTimeout(String formulaTimeout)
	{
		this.formulaTimeout = formulaTimeout;
	}
	
	
	public boolean isAsync()
	{
		return async;
	}

	public void setAsync(boolean async)
	{
		this.async = async;
	}
	
	public String getFormulaAsync()
	{
		return formulaAsync;
	}

	public void setFormulaAsync(String formulaAsync)
	{
		this.formulaAsync = formulaAsync;
	}

	
	public String getAsyncGroup()
	{
		return asyncGroup;
	}

	public void setAsyncGroup(String asyncGroup)
	{
		this.asyncGroup = asyncGroup;
	}
	
	public String getFormulaAsyncGroup()
	{
		return formulaAsyncGroup;
	}

	public void setFormulaAsyncGroup(String formulaAsyncGroup)
	{
		this.formulaAsyncGroup = formulaAsyncGroup;
	}

	
	public WaitAsyncEnd getWaitAsyncEnd()
	{
		return waitAsyncEnd;
	}

	public void setWaitAsyncEnd(WaitAsyncEnd waitAsyncEnd)
	{
		this.waitAsyncEnd = waitAsyncEnd;
	}
	
	public String getFormulaWaitAsyncEnd()
	{
		return formulaWaitAsyncEnd;
	}

	public void setFormulaWaitAsyncEnd(String formulaWaitAsyncEnd)
	{
		this.formulaWaitAsyncEnd = formulaWaitAsyncEnd;
	}


	public LinkedHashMap<String, String> getParams()
	{
		return params;
	}
	
	public void setParams(LinkedHashMap<String, String> params)
	{
		this.params = params;
	}
	
	public void addParam(String name, String value)
	{
		params.put(name, value);
		if (value.contains(MatrixFunctions.FORMULA_START) && !comparisonUtils().isSpecialValue(value))
		{
			if (formulas==null)
				formulas = new HashMap<String, String>();
			formulas.put(name, value);
		}
	}
	
	
	public HashMap<String, String> getFormulas()
	{
		return formulas;
	}

	public Set<String> getMatrixInputParams()
	{
		return matrixInputParams;
	}

	public void setMatrixInputParams(Set<String> matrixInputParams)
	{
		this.matrixInputParams = matrixInputParams;
	}
	
	public List<String> getDuplicateParams()
	{
		return duplicateParams;
	}

	public void addDuplicateParam(String name)
	{
		if (duplicateParams==null)
			duplicateParams = new ArrayList<String>();
		duplicateParams.add(name);
	}
	
	
	public Matrix getMatrix()
	{
		return matrix;
	}
	
	public void setMatrix(Matrix matrix)
	{
		this.matrix = matrix;
	}

	public boolean isSuspendIfFailed() {
		return suspendIfFailed;
	}

	public void setSuspendIfFailed(boolean suspendIfFailed) {
		this.suspendIfFailed = suspendIfFailed;
	}
}
