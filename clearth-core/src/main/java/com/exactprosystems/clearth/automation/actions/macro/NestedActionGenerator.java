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

package com.exactprosystems.clearth.automation.actions.macro;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.*;
import com.exactprosystems.clearth.utils.LineBuilder;
import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.utils.inputparams.InputParamsUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class NestedActionGenerator extends ActionGenerator
{	
	private static final Logger logger = LoggerFactory.getLogger(NestedActionGenerator.class);
	
	public static final String SHOW_IN_REPORT = "showinreport", CONTINUE_IF_FAILED = "continueiffailed";
	
	protected File macroMatrixFile;
	protected Map<String, String> macroParams;
	protected Step macroStep;
	
	protected Matrix generatedMacroMatrix;
	protected List<NestedAction> nestedActions;
	// For compatibility with super-class
	protected List<Matrix> matrices;
	protected Map<String, Preparable> preparableActions;
	
	private NestedActionGenerator(File macroMatrixFile, Map<String, String> macroParams, Step macroStep,
			List<Matrix> matrices, Map<String, Preparable> preparableActions, SpecialActionParams specialActionParams)
	{
		super(new HashMap<>(), matrices, preparableActions, 
				new ActionGeneratorResources(specialActionParams, 
						ClearThCore.getInstance().getActionFactory(),
						ClearThCore.getInstance().getMvelVariablesFactory(),
						ClearThCore.getInstance().createMatrixFunctions(Collections.emptyMap(), null, null, true),
						ClearThCore.getInstance().getConfig().getAutomation().getMatrixFatalErrors()));
		this.macroMatrixFile = macroMatrixFile;
		this.macroParams = macroParams;
		this.macroStep = macroStep;
		this.matrices = matrices;
		this.preparableActions = preparableActions;
	}
	
	public static NestedActionGenerator create(File macroMatrixFile, Map<String, String> macroParams, Step macroStep, SpecialActionParams specialActionParams)
	{
		return new NestedActionGenerator(macroMatrixFile, macroParams, macroStep, new ArrayList<>(), new HashMap<>(), specialActionParams);
	}
	
	
	public void generateNestedActions() throws IOException, NestedActionGenerationException
	{
		MatrixData matrixData = buildMatrixData();
		boolean allSuccess = this.build(matrixData, false);
		generatedMacroMatrix = matrices.get(0);
		if (!allSuccess)
			checkGeneratorMessages(generatedMacroMatrix.getGeneratorMessages()); // If any error occurred on macro compilation, exception will be thrown here
		if (CollectionUtils.isEmpty(generatedMacroMatrix.getActions()))
			throw new NestedActionGenerationException("No actions found in macro file '" + macroMatrixFile.getAbsolutePath() + "'.");
		
		// Add MVEL parameters to be able to use them inside nested actions
		generatedMacroMatrix.getMvelVars().put("macro", macroParams);
		this.nestedActions = wrapActions(generatedMacroMatrix.getActions());
	}
	
	public Matrix getMacroMatrix()
	{
		return generatedMacroMatrix;
	}
	
	public List<NestedAction> getNestedActions()
	{
		return nestedActions;
	}
	
	public List<Preparable> getPreparableActions()
	{
		return new ArrayList<>(preparableActions.values());
	}
	
	
	protected MatrixData buildMatrixData()
	{
		MatrixData mData = ClearThCore.getInstance().getMatrixDataFactory().createMatrixData();
		mData.setName(macroMatrixFile.getName());
		mData.setFile(macroMatrixFile);
		mData.setExecute(true);
		mData.setTrim(true);
		return mData;
	}
	
	protected void checkGeneratorMessages(List<ActionGeneratorMessage> generatorMsgs) throws NestedActionGenerationException
	{
		if (CollectionUtils.isNotEmpty(generatorMsgs))
		{
			LineBuilder msgBuilder = new LineBuilder();
			generatorMsgs.stream().filter(msg -> msg.type != ActionGeneratorMessageType.INFO)
					.map(msg -> msg.message).forEach(msg -> msgBuilder.add("* ").append(msg));
			throw new NestedActionGenerationException("The following error(s) occurred while compiling macro:" + Utils.EOL + msgBuilder.toString());
		}
	}
	
	protected List<NestedAction> wrapActions(List<Action> actions)
	{
		List<NestedAction> wrappedActions = new ArrayList<>();
		List<String> settingsParamsNames = getSettingsParamsNames();
		for (Action action : actions)
		{
			NestedAction nestedAction = createNestedAction(action);
			setNestedActionParams(action, nestedAction);
			// Remove settings parameters from all action's input ones
			for (String paramName : settingsParamsNames)
			{
				action.getInputParams().remove(paramName);
				action.getMatrixInputParams().remove(paramName);
			}
			wrappedActions.add(nestedAction);
		}
		return wrappedActions;
	}
	
	protected NestedAction createNestedAction(Action action)
	{
		return new NestedAction(action);
	}
	
	protected void setNestedActionParams(Action sourceAction, NestedAction targetAction)
	{
		targetAction.setShowInReport(InputParamsUtils.getBooleanOrDefault(sourceAction.getInputParams(), SHOW_IN_REPORT, true));
		targetAction.setContinueIfFailed(InputParamsUtils.getBooleanOrDefault(sourceAction.getInputParams(), CONTINUE_IF_FAILED, false));
	}
	
	protected List<String> getSettingsParamsNames()
	{
		return Arrays.asList(SHOW_IN_REPORT, CONTINUE_IF_FAILED);
	}
	
	
	@Override
	protected boolean customSetting(String name, String value, ActionSettings settings, int headerLineNumber, int lineNumber)
	{
		if (name.equals(SHOW_IN_REPORT) || name.equals(CONTINUE_IF_FAILED))
		{
			// Adding special parameters for nested action in lower case key
			settings.addParam(name, value);
			return true;
		}
		return false;
	}

	@Override
	protected boolean initActionSettings(ActionSettings actionSettings, Matrix matrix, int lineNumber,
	                                     List<String> header, List<String> values, int headerLineNumber, 
	                                     boolean missingValues, boolean onlyCheck)
	{
		actionSettings.setStep(macroStep);
		// Nested actions could be executed only sequentially (just preventing non-sequential execution if async=true is specified in matrix)
		actionSettings.setAsync(false);
		actionSettings.setFormulaAsync(null);

		return super.initActionSettings(actionSettings, matrix, lineNumber, header, values, headerLineNumber, 
				missingValues, onlyCheck);
	}
	
	@Override
	protected int initAction(Action action, ActionSettings settings, int headerLineNumber, int lineNumber)
	{
		action.init(settings);
		if (!checkAction(action, headerLineNumber, lineNumber))
			return CHECKING_ERROR;
		return NO_ERROR;
	}
	

	protected boolean checkAction(Action action, int headerLineNumber, int lineNumber)
	{
		return true;
	}
	
	
	@Override
	protected Logger getLogger()
	{
		return logger;
	}
}
