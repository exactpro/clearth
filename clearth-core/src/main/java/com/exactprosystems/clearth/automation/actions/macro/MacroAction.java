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

import com.exactprosystems.clearth.automation.*;
import com.exactprosystems.clearth.automation.exceptions.FailoverException;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.results.DefaultResult;
import com.exactprosystems.clearth.utils.inputparams.InputParamsUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MacroAction extends Action implements Preparable
{
	public static final String PARAM_MACRO_FILENAME = "MacroFileName";
	
	protected SchedulerStatus schedulerStatus; // For actions that should be prepared
	protected StepContext stepContext;
	protected GlobalContext globalContext;
	protected NestedActionGenerator naGenerator;
	
	private String nestedActionsReportFilePath;
	private NestedActionsExecutionProgress executionProgress;
	
	@Override
	public void prepare(GlobalContext globalContext, SchedulerStatus status) throws Exception
	{
		this.schedulerStatus = status;
	}
	
	@Override
	protected Result run(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext)
			throws ResultException, FailoverException
	{
		initMacroAction(stepContext, globalContext);
		try
		{
			naGenerator.generateNestedActions();
			// Prepare necessary actions
			for (Preparable preparableAction : naGenerator.getPreparableActions())
				preparableAction.prepare(globalContext, schedulerStatus);
			return executeNestedActions();
		}
		catch (Exception e)
		{
			return DefaultResult.failed("Error occurred while working with nested actions.", e);
		}
	}
	
	
	protected void initMacroAction(StepContext stepContext, GlobalContext globalContext)
	{
		this.stepContext = stepContext;
		this.globalContext = globalContext;
		
		File macroMatrixFile = new File(InputParamsUtils.getRequiredFilePath(inputParams, PARAM_MACRO_FILENAME));
		Step nestedStep = new DefaultStep(buildNestedStepName(macroMatrixFile), CoreStepKind.Default.getLabel(), null,
				StartAtType.DEFAULT, false, null, false, false, true, null);
		naGenerator = createActionGenerator(macroMatrixFile, copyInputParams(), nestedStep);
		nestedActionsReportFilePath = Paths.get(getStep().getActionsReportsDir(), getMatrix().getShortFileName(), nestedStep.getSafeName()).toString();
		executionProgress = new NestedActionsExecutionProgress();
	}
	
	protected Result executeNestedActions() throws IOException
	{
		try (NestedActionExecutor actionExec = createActionExecutor())
		{
			actionExec.reset(null, executionProgress); // actionsReportsDir isn't used in NestedActionReportWriter class
			for (NestedAction nestedAction : naGenerator.getNestedActions())
			{
				Action actionToExecute = nestedAction.getAction();
				// Need to "execute" action anyway to calculate necessary MVEL values which could be used in other actions
				actionExec.executeAction(actionToExecute, stepContext, nestedAction.isShowInReport());
				if (actionToExecute.isExecutable())
				{
					if (actionToExecute.isPassed())
					{
						if (!nestedAction.isShowInReport())
							executionProgress.incrementHidden();
					}
					else if (!nestedAction.isContinueIfFailed())
						return DefaultResult.failed("Nested action '" + actionToExecute.getIdInMatrix() + "' (" + actionToExecute.getName() + ") failed. Macro execution has been interrupted.");
				}
			}
			
			int executedActionsCount = executionProgress.getDone(), passedActionsCount = executionProgress.getSuccessful();
			if (executedActionsCount == passedActionsCount)
				return DefaultResult.passed(executedActionsCount > 0 ? "Nested actions executed successfully." : "No one executable nested action found in macro file.");
			else
				return DefaultResult.failed("Nested actions have been executed, but " + (executedActionsCount - passedActionsCount) + " of them failed.");
		}
	}
	
	
	protected String buildNestedStepName(File macroMatrixFile)
	{
		return String.format("%s_%s(%s)_%s_%d", getStep().getSafeName(), getIdInMatrix(), getName(), macroMatrixFile.getName(), System.currentTimeMillis());
	}
	
	protected NestedActionGenerator createActionGenerator(File macroMatrixFile, Map<String, String> macroParams, Step nestedStep)
	{
		return NestedActionGenerator.create(macroMatrixFile, macroParams, nestedStep);
	}
	
	protected NestedActionExecutor createActionExecutor()
	{
		ActionParamsCalculator paramsCalculator = new ActionParamsCalculator(globalContext.getMatrixFunctions());
		return new NestedActionExecutor(globalContext, paramsCalculator, getNestedActionsReportFilePath());
	}
	
	
	public List<NestedAction> getNestedActions()
	{
		return naGenerator.getNestedActions();
	}
	
	public String getNestedActionsReportFilePath()
	{
		return nestedActionsReportFilePath;
	}
	
	public ActionsExecutionProgress getExecutionProgress()
	{
		return executionProgress;
	}
}
