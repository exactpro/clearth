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

package com.exactprosystems.clearth.automation.actions.macro;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.ActionParamsCalculator;
import com.exactprosystems.clearth.automation.CoreStepKind;
import com.exactprosystems.clearth.automation.DefaultStep;
import com.exactprosystems.clearth.automation.GlobalContext;
import com.exactprosystems.clearth.automation.Matrix;
import com.exactprosystems.clearth.automation.MatrixContext;
import com.exactprosystems.clearth.automation.Preparable;
import com.exactprosystems.clearth.automation.SchedulerStatus;
import com.exactprosystems.clearth.automation.StartAtType;
import com.exactprosystems.clearth.automation.Step;
import com.exactprosystems.clearth.automation.StepContext;
import com.exactprosystems.clearth.automation.exceptions.FailoverException;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.results.DefaultResult;
import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.utils.inputparams.InputParamsUtils;


public class MacroAction extends Action implements Preparable
{
	public static final String PARAM_MACRO_FILENAME = "MacroFileName";
	
	protected SchedulerStatus schedulerStatus; // For actions that should be prepared
	protected StepContext stepContext;
	protected GlobalContext globalContext;
	
	private int executedActionsCount, passedActionsCount, hiddenActionsCount;
	protected String nestedActionsReportsPath;
	
	@Override
	public void prepare(GlobalContext globalContext, SchedulerStatus status) throws Exception
	{
		this.schedulerStatus = status;
	}
	
	@Override
	protected Result run(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext)
			throws ResultException, FailoverException
	{
		this.stepContext = stepContext;
		this.globalContext = globalContext;
		
		File macroMatrixFile = new File(InputParamsUtils.getRequiredFilePath(inputParams, PARAM_MACRO_FILENAME));
		Step macroStep = new DefaultStep(buildNestedStepName(macroMatrixFile), CoreStepKind.Default.getLabel(), null, StartAtType.DEFAULT, false, null, false, false, true, null);
		NestedActionGenerator naGenerator = createActionGenerator(macroMatrixFile, copyInputParams(), macroStep);
		nestedActionsReportsPath = getStep().getActionsReportsDir() + File.separator + getMatrix().getShortFileName() + File.separator + macroStep.getSafeName();
		try
		{
			naGenerator.generateNestedActions();
			for (Preparable preparableAction : naGenerator.getPreparableActions())
				preparableAction.prepare(globalContext, schedulerStatus);
			
			return executeNestedActions(createActionExecutor(naGenerator.getMacroMatrix()), naGenerator.getNestedActions());
		}
		catch (Exception e)
		{
			if (e instanceof ResultException)
				throw (ResultException)e;
			throw ResultException.failed("Error occurred while working with nested actions.", e);
		}
	}
	
	protected Result executeNestedActions(NestedActionExecutor actionExec, List<NestedAction> nestedActions) throws Exception
	{
		try
		{
			int failedActionsCount = 0;
			for (NestedAction nestedAction : nestedActions)
			{
				Action actionToExecute = nestedAction.getAction();
				// Need to "execute" action anyway to calculate necessary MVEL values which could be used in other actions
				actionExec.executeAction(actionToExecute, stepContext, nestedAction.isShowInReport());
				if (actionToExecute.isExecutable())
				{
					executedActionsCount++;
					if (actionToExecute.isPassed())
					{
						passedActionsCount++;
						if (!nestedAction.isShowInReport())
							hiddenActionsCount++;
					}
					else if (!nestedAction.isContinueIfFailed())
						return DefaultResult.failed("Nested action '" + actionToExecute.getIdInMatrix() + "' (" + actionToExecute.getName() + ") failed. Macro execution has been interrupted.");
					else
						failedActionsCount++;
				}
			}
			
			if (failedActionsCount == 0)
				return DefaultResult.passed(executedActionsCount != 0 ? "Nested actions executed successfully." : "No one executable nested action found in macro file.");
			else
				return DefaultResult.failed("Nested actions have been executed, but " + failedActionsCount + " of them were failed.");
		}
		finally
		{
			Utils.closeResource(actionExec);
		}
	}
	
	protected String buildNestedStepName(File macroMatrixFile)
	{
		return String.format("%s_%s(%s)_%s_%d", getStep().getSafeName(), getIdInMatrix(), getName(), macroMatrixFile.getName(), System.currentTimeMillis());
	}
	
	protected NestedActionGenerator createActionGenerator(File macroMatrixFile, Map<String, String> macroParams, Step macroStep)
	{
		return NestedActionGenerator.create(macroMatrixFile, macroParams, macroStep);
	}
	
	protected NestedActionExecutor createActionExecutor(Matrix macroMatrix)
	{
		ActionParamsCalculator paramsCalculator = new ActionParamsCalculator(globalContext.getMatrixFunctions());
		return new NestedActionExecutor(globalContext, paramsCalculator, getNestedActionsReportsPath());
	}

	public int getExecutedActionsCount()
	{
		return executedActionsCount;
	}

	public int getPassedActionsCount()
	{
		return passedActionsCount;
	}

	public int getHiddenActionsCount()
	{
		return hiddenActionsCount;
	}

	public String getNestedActionsReportsPath()
	{
		return nestedActionsReportsPath;
	}
}
