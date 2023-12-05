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

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.expressions.ActionReferenceFinder;
import com.exactprosystems.clearth.utils.ObjectWrapper;
import com.exactprosystems.clearth.utils.Utils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static com.exactprosystems.clearth.ClearThCore.rootRelative;
import static com.exactprosystems.clearth.automation.ActionExecutor.*;
import static com.exactprosystems.clearth.automation.MatrixFunctions.*;
import static com.exactprosystems.clearth.utils.Utils.nvl;
import static java.nio.file.Files.exists;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.apache.commons.lang.StringUtils.*;

public class MvelVarsCleaningTableBuilder
{
	private static final Logger log = LoggerFactory.getLogger(MvelVarsCleaningTableBuilder.class);

	private static final Set<String> KEYWORDS_TO_IGNORE = new HashSet<>();
	static
	{
		KEYWORDS_TO_IGNORE.add(PARAMS_PREV_ACTION);
		KEYWORDS_TO_IGNORE.add(PARAMS_THIS_ACTION);
		KEYWORDS_TO_IGNORE.add(ENV_VARS);
		KEYWORDS_TO_IGNORE.add(GLOBAL_CONST);
	}
	
	private final MatrixFunctions functions;
	

	public MvelVarsCleaningTableBuilder()
	{
		this.functions = ClearThCore.getInstance().createMatrixFunctions(emptyMap(),
				null, null, true);
	}
	
	public MvelVarsCleaningTableBuilder(MatrixFunctions functions)
	{
		this.functions = functions;
	}
	

	public MultiValuedMap<String, String> build(Matrix matrix, Collection<String> stepNames)
	{
		List<Action> sortedActions = getActionsSortedBySteps(matrix, stepNames);
		Map<String, String> actionIdToLastReferringIdMap = getActionIdToLastReferringIdMap(sortedActions);
		MultiValuedMap<String, String> table = buildCleaningTable(actionIdToLastReferringIdMap);
		log.trace("MVEL variables cleaning table for matrix '{}':" + Utils.EOL + "{}", matrix.getName(), table);
		return table;
	}


	private List<Action> getActionsSortedBySteps(Matrix matrix, Collection<String> stepNames)
	{
		MultiValuedMap<String, Action> actionsByStepName = getActionsGroupedByStepName(matrix);

		int totalActionsCount = matrix.getActions().size();
		List<Action> sortedActions = new ArrayList<>(totalActionsCount);

		for (String stepName : stepNames)
		{
			Collection<Action> stepActions = actionsByStepName.get(stepName);
			if (stepActions != null)
				sortedActions.addAll(stepActions);
		}

		return sortedActions;
	}

	private MultiValuedMap<String, Action> getActionsGroupedByStepName(Matrix matrix)
	{
		MultiValuedMap<String, Action> actionsByStepName = new ArrayListValuedHashMap<>();
		for (Action action : matrix.getActions())
		{
			Step step = action.getStep();
			if (step == null)
				continue; // Nonexistent step is specified in matrix, action won't be executed

			actionsByStepName.put(step.getName(), action);
		}
		return actionsByStepName;
	}


	private Map<String, String> getActionIdToLastReferringIdMap(List<Action> actions)
	{
		Map<String, String> result = new HashMap<>(actions.size());
		MvelVariables vars = new MvelVariables();
		for (Action action : actions)
		{
			String currentId = action.getIdInMatrix();
			result.put(currentId, currentId);

			processBasicParams(action, result);
			processInputParams(action, result, vars);
		}
		return result;
	}

	private void processInputParams(Action action, Map<String, String> actionIdToLastReferringIdMap,
	                                MvelVariables previousActionsVars)
	{
		previousActionsVars.saveInputParams(action);
		
		String currentId = action.getIdInMatrix();
		Map<String, String> inputParams = action.getInputParams();
		Set<String> inputFileParamNames = nvl(action.getInputFileParamNames(), emptySet());
		
		for (Map.Entry<String, String> e : inputParams.entrySet())
		{
			String paramValue = e.getValue();
			if (isBlank(paramValue))
				continue;
			
			if (contains(paramValue, FORMULA_START))
				processExpression(paramValue, currentId, actionIdToLastReferringIdMap);
			
			String paramName = e.getKey();
			if (inputFileParamNames.contains(paramName))
				processInputFileParam(paramValue, paramName, currentId, previousActionsVars, 
						actionIdToLastReferringIdMap);
		}
	}
	
	private void processBasicParams(Action action, Map<String, String> actionIdToLastReferringIdMap)
	{
		String currentId = action.getIdInMatrix();
		processBasicParamFormula(action.getFormulaExecutable(), currentId, actionIdToLastReferringIdMap);
		processBasicParamFormula(action.getFormulaInverted(), currentId, actionIdToLastReferringIdMap);
		processBasicParamFormula(action.getFormulaComment(), currentId, actionIdToLastReferringIdMap);
		processBasicParamFormula(action.getFormulaTimeout(), currentId, actionIdToLastReferringIdMap);
		processBasicParamFormula(action.getFormulaAsync(), currentId, actionIdToLastReferringIdMap);
		processBasicParamFormula(action.getFormulaAsyncGroup(), currentId, actionIdToLastReferringIdMap);
		processBasicParamFormula(action.getFormulaWaitAsyncEnd(), currentId, actionIdToLastReferringIdMap);
	}
	
	private void processBasicParamFormula(String formula, String currentId, 
	                                      Map<String, String> actionIdToLastReferringIdMap)
	{
		if (formula != null)
			processExpression(formula, currentId, actionIdToLastReferringIdMap);
	}


	private void processExpression(String expression, String currentId, Map<String, String> actionIdToLastReferringIdMap)
	{
		ActionReferenceFinder refFinder = new ActionReferenceFinder(expression);
		List<String> refFinderWarnings = refFinder.checkExpressionCorrectness();
		for (String message : refFinderWarnings)
		{
			log.warn("Warning for expression '{}': {}", expression, message);
		}
		while (refFinder.findNext())
		{
			String refActionId = refFinder.nextActionId();
			if (!KEYWORDS_TO_IGNORE.contains(refActionId))
				actionIdToLastReferringIdMap.put(refActionId, currentId);
		}
	}


	private MultiValuedMap<String, String> buildCleaningTable(Map<String, String> actionIdToLastReferringIdMap)
	{
		MultiValuedMap<String, String> cleaningTable = new HashSetValuedHashMap<>();
		for (Map.Entry<String, String> e : actionIdToLastReferringIdMap.entrySet())
		{
			String actionId = e.getKey();
			String lastRefActionId = e.getValue();

			cleaningTable.put(lastRefActionId, actionId);
		}
		return cleaningTable;
	}


	// Processing of parameters containing links to input files with expressions (f.e. SQL queries)

	private void processInputFileParam(String paramValue, String paramName, String currentId,
	                                   MvelVariables previousActionsVars,
	                                   Map<String, String> actionIdToLastReferringIdMap)
	{
		Path filePath = resolveFilePath(paramValue, paramName, currentId, previousActionsVars);
		if (filePath == null)
			return;

		try
		{
			String fileContent = readFileToString(filePath.toFile(), StandardCharsets.UTF_8);

			processExpression(fileContent, currentId, actionIdToLastReferringIdMap);
		}
		catch (IOException e)
		{
			log.warn("Error while loading content of file #{}='{}' from action '{}'.",
					paramName, filePath, currentId, e);
		}
	}

	private Path resolveFilePath(String paramValue, String paramName, String currentId,
	                             MvelVariables previousActionsVars)
	{
		if (contains(paramValue, FORMULA_START))
		{
			paramValue = calculateFilePath(paramValue, paramName, currentId, previousActionsVars);
			if (paramValue == null)
				return null;
		}

		Path filePath = Paths.get(rootRelative(paramValue));
		if (exists(filePath))
			return filePath;
		else
		{
			log.warn("File #{}='{}' from action '{}' not found.", paramName, filePath, currentId);
			return null;
		}
	}

	private String calculateFilePath(String expression, String paramName, String currentId,
	                                 MvelVariables previousActionsVars)
	{
		try
		{
			Object objValue = functions.calculateExpression(expression, paramName,
					previousActionsVars.getVariables(), previousActionsVars.getFixedIds(),
					null, new ObjectWrapper(0));

			if (objValue != null)
				return objValue.toString();
			else
			{
				log.warn("Expression in input file parameter #{}='{}' from action '{}' evaluated to null.",
						paramName, expression, currentId);
				return null;
			}
		}
		catch (Exception e)
		{
			log.warn("Evaluation of expression in input file parameter #{}='{}' from action '{}' failed.",
					paramName, expression, currentId, e);
			return null;
		}
	}
}
