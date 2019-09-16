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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.ActionGenerator;
import com.exactprosystems.clearth.automation.ActionGeneratorMessage;
import com.exactprosystems.clearth.automation.ActionGeneratorMessageType;
import com.exactprosystems.clearth.automation.ActionSettings;
import com.exactprosystems.clearth.automation.Matrix;
import com.exactprosystems.clearth.automation.MatrixData;
import com.exactprosystems.clearth.automation.Preparable;
import com.exactprosystems.clearth.automation.Step;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.utils.LineBuilder;
import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.utils.inputparams.InputParamsUtils;

public class NestedActionGenerator extends ActionGenerator
{	
	private static final Logger logger = LoggerFactory.getLogger(NestedActionGenerator.class);
	
	public static final String SHOW_IN_REPORT = "showinreport",
			CONTINUE_IF_FAILED = "continueiffailed";
	
	protected File macroMatrixFile;
	protected Map<String, String> macroParams;
	protected Step macroStep;
	
	protected List<Matrix> matrices;
	protected Matrix generatedMacroMatrix;
	protected List<NestedAction> nestedActions;
	protected Map<String, Preparable> preparableActions;
	
	private NestedActionGenerator()
	{
		super(new HashMap<String, Step>(), new ArrayList<Matrix>(), null);
	}
	
	private NestedActionGenerator(File macroMatrixFile, Map<String, String> macroParams, Step macroStep,
			List<Matrix> matrices, Map<String, Preparable> preparableActions)
	{
		super(new HashMap<String, Step>(), matrices, preparableActions);
		this.macroMatrixFile = macroMatrixFile;
		this.macroParams = macroParams;
		this.macroStep = macroStep;
		this.matrices = matrices;
		this.preparableActions = preparableActions;
	}
	
	public static NestedActionGenerator create(File macroMatrixFile, Map<String, String> macroParams, Step macroStep)
	{
		return new NestedActionGenerator(macroMatrixFile, macroParams, macroStep, new ArrayList<Matrix>(), new HashMap<String, Preparable>());
	}
	
	
	public void generateNestedActions() throws Exception
	{
		MatrixData matrixData = buildMatrixData();
		boolean allSuccess = this.build(matrixData, false);
		generatedMacroMatrix = matrices.get(0);
		if (!allSuccess)
			checkGenerationMessages(generatedMacroMatrix.getGeneratorMessages()); // If any error occurred on macro compilation, exception will be thrown
		if (CollectionUtils.isEmpty(generatedMacroMatrix.getActions()))
			throw ResultException.failed("No actions found in macro file '" + macroMatrixFile.getAbsolutePath() + "'.");
		
		// Adding MVEL parameters to be able to use them inside nested actions
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
		return new ArrayList<Preparable>(preparableActions.values());
	}
	
	
	protected MatrixData buildMatrixData()
	{
		MatrixData data = ClearThCore.getInstance().getMatrixDataFactory().createMatrixData();
		data.setName(macroMatrixFile.getName());
		data.setFile(macroMatrixFile);
		data.setExecute(true);
		data.setTrim(true);
		return data;
	}
	
	protected void checkGenerationMessages(List<ActionGeneratorMessage> generatorMsgs) throws Exception
	{
		if (CollectionUtils.isNotEmpty(generatorMsgs))
		{
			LineBuilder msgBuilder = new LineBuilder();
			for (ActionGeneratorMessage message : generatorMsgs)
			{
				if (message.type != ActionGeneratorMessageType.INFO)
					msgBuilder.add("* ").append(message.message);
			}
			throw ResultException.failed("The following error(s) occurred while compiling macro:" + Utils.EOL + msgBuilder.toString());
		}
	}
	
	protected List<NestedAction> wrapActions(List<Action> actions)
	{
		List<NestedAction> wrappedActions = new ArrayList<NestedAction>();
		List<String> settingsParamsNames = getSettingsParamsNames();
		for (Action action : actions)
		{
			NestedAction nestedAction = createNestedAction(action);
			setNestedActionParams(action, nestedAction);
			// Remove settings parameters from all action input params
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
		List<String> paramsNames = new ArrayList<String>();
		paramsNames.add(SHOW_IN_REPORT);
		paramsNames.add(CONTINUE_IF_FAILED);
		return paramsNames;
	}
	
	
	@Override
	protected Logger getLogger()
	{
		return logger;
	}

	@Override
	protected boolean customSetting(String name, String value, ActionSettings settings, int headerLineNumber, int lineNumber)
	{
		if (name.equals(SHOW_IN_REPORT) || name.equals(CONTINUE_IF_FAILED))
		{
			// Need to add to settings this parameters with lower case key
			settings.addParam(name, value);
			return true;
		}
		return false;
	}
	
	@Override
	protected int initAction(Action action, ActionSettings settings, int headerLineNumber, int lineNumber)
	{
		action.init(settings);
		if (!checkAction(action, headerLineNumber, lineNumber))
			return CHECKING_ERROR;
		return NO_ERROR;
	}
	
	@Override
	protected boolean checkAction(Action action, int headerLineNumber, int lineNumber)
	{
		return true;
	}

	@Override
	protected boolean initActionSettings(ActionSettings actionSettings,
									   Matrix matrix,
									   int lineNumber,
									   List<String> header,
									   List<String> values,
									   int headerLineNumber,
									   boolean missingValues)
	{
		actionSettings.setStep(macroStep);
		// Nested actions should be executed sequentially
		actionSettings.setAsync(false);
		actionSettings.setFormulaAsync(null);

		return super.initActionSettings(actionSettings, matrix, lineNumber, header, values, headerLineNumber, missingValues);
	}
}
