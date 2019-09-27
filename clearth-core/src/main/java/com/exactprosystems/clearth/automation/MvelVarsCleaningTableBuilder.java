/******************************************************************************
 * Copyright (c) 2009-2019, Exactpro Systems LLC
 * www.exactpro.com
 * Build Software to Test Software
 *
 * All rights reserved.
 * This is unpublished, licensed software, confidential and proprietary 
 * information which is the property of Exactpro Systems LLC or its licensors.
 ******************************************************************************/
package com.exactprosystems.clearth.automation;

import com.exactprosystems.clearth.automation.expressions.ActionReferenceFinder;
import com.exactprosystems.clearth.utils.Utils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.exactprosystems.clearth.automation.ActionExecutor.PARAMS_PREV_ACTION;
import static com.exactprosystems.clearth.automation.ActionExecutor.PARAMS_THIS_ACTION;
import static com.exactprosystems.clearth.automation.MatrixFunctions.FORMULA_START;
import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.contains;

public class MvelVarsCleaningTableBuilder
{
	private static final Logger log = LoggerFactory.getLogger(MvelVarsCleaningTableBuilder.class);

	private static final Set<String> KEYWORDS_TO_IGNORE = new HashSet<>();
	static
	{
		KEYWORDS_TO_IGNORE.add(PARAMS_PREV_ACTION);
		KEYWORDS_TO_IGNORE.add(PARAMS_THIS_ACTION);
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
				throw new IllegalStateException(format("Reference to step isn't set for action with #id='%s'.",
						action.getIdInMatrix()));

			actionsByStepName.put(step.getName(), action);
		}
		return actionsByStepName;
	}


	private Map<String, String> getActionIdToLastReferringIdMap(List<Action> actions)
	{
		Map<String, String> result = new HashMap<>(actions.size());
		for (Action action : actions)
		{
			String currentId = action.getIdInMatrix();
			result.put(currentId, currentId);

			processInputParams(action.getInputParams(), currentId, result);
		}
		return result;
	}

	private void processInputParams(Map<String, String> inputParams, String currentId,
	                                Map<String, String> actionIdToLastReferringIdMap)
	{
		for (String paramValue : inputParams.values())
		{
			if (contains(paramValue, FORMULA_START))
				processExpression(paramValue, currentId, actionIdToLastReferringIdMap);
		}
	}

	private void processExpression(String expression, String currentId, Map<String, String> actionIdToLastReferringIdMap)
	{
		ActionReferenceFinder refFinder = new ActionReferenceFinder(expression);
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
}
