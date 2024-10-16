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

package com.exactprosystems.clearth.automation;

import com.exactprosystems.clearth.automation.report.FailReason;
import com.exactprosystems.clearth.automation.report.Result;
import org.apache.commons.collections4.MultiValuedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.exactprosystems.clearth.automation.ActionExecutor.*;
import static com.exactprosystems.clearth.automation.Matrix.MATRIX;
import static com.exactprosystems.clearth.automation.expressions.MvelExpressionUtils.fixActionIdForMvel;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

public class MvelVariables
{
	private static final Logger log = LoggerFactory.getLogger(MvelVariables.class);

	public static final String PASSED_PARAM = "passed";
	public static final String FAIL_REASON_PARAM = "failReason";

	public static final Map<String, Object> PASSED_ACTION_PARAMS = new HashMap<>();
	static
	{
		PASSED_ACTION_PARAMS.put(PASSED_PARAM, true);
		PASSED_ACTION_PARAMS.put(FAIL_REASON_PARAM, FailReason.NO);
	}
	public static final Map<FailReason, Map<String, Object>> FAILED_ACTION_PARAMS = new EnumMap<>(FailReason.class);
	static
	{
		for (FailReason failReason : FailReason.values())
		{
			if (failReason.equals(FailReason.NO))
				continue;
			Map<String, Object> params = new HashMap<>();
			params.put(PASSED_PARAM, false);
			params.put(FAIL_REASON_PARAM, failReason.name());
			FAILED_ACTION_PARAMS.put(failReason, params);
		}
	}

	private Map<String, Object> variables = new HashMap<>();
	private Map<String, String> actionIdInMatrixToIdForMvel = new HashMap<>();

	// Key: Action ID. Values: IDs of actions last referenced by this action.
	private MultiValuedMap<String, String> cleaningTable;


	public void setCleaningTable(MultiValuedMap<String, String> cleaningTable)
	{
		this.cleaningTable = cleaningTable;
	}
	

	public void put(String name, Object value)
	{
		variables.put(name, value);
	}

	public Object get(String name)
	{
		return variables.get(name);
	}
	
	public void putAll(Map<String, ?> variablesToPut)
	{
		variables.putAll(variablesToPut);
	}


	@SuppressWarnings("unchecked")
	public void saveCalculatedParameter(String actionId, String parameterName, String value)
	{
		String idForMvel = actionIdInMatrixToIdForMvel.getOrDefault(actionId, actionId);
		Map<String, Object> actionParams = (Map<String, Object>) variables.computeIfAbsent(idForMvel, k -> new HashMap<>());
		actionParams.put(parameterName, value);
		
		Map<String, Object> inParams =
				(Map<String, Object>) actionParams.computeIfAbsent(PARAMS_IN, k -> new HashMap<>());
		inParams.put(parameterName, value);
	}


	public void saveInputParams(Action action)
	{
		String idInMatrix = action.getIdInMatrix();
		String idForMvel = fixActionIdForMvel(idInMatrix);
		//noinspection StringEquality
		if (idForMvel != idInMatrix)
			actionIdInMatrixToIdForMvel.put(idInMatrix, idForMvel);

		variables.merge(idForMvel, makeInputParamsMap(action.getInputParams()), this::mergeActionsParams);

		variables.put(PARAMS_THIS_ACTION, variables.get(idForMvel));
	}


	public void saveActionResult(Action action)
	{
		saveOutputParams(action);

		LinkedHashMap<String, LinkedHashMap<String, String>> subParams = action.getSubOutputParams();
		LinkedHashMap<String, SubActionData> subData = action.getSubActionData();
		//Need to put sub-output parameters to mvelVars to make them available by reference. 
		//Even if there is no sub-output parameters, action result should be available for sub-actions as well
		if ((subParams == null) && (subData == null))
			return;

		Map<String, Object> execResultParams = getExecutionResultParams(action);
		if (subParams != null)
			addSubParams(subParams, execResultParams);
		else
			addSubData(subData, execResultParams);
	}

	public void saveOutputParams(Action action)
	{
		String id = actionIdInMatrixToIdForMvel.getOrDefault(action.getIdInMatrix(), action.getIdInMatrix());
		//noinspection unchecked
		Map<String, Object> actionVars = (Map<String, Object>) variables.get(id);
		if (actionVars == null)
			return; // This may happen if result of async action is no longer needed.

		if (action.getOutputParams() != null)
			addOutputParams(actionVars, action.getOutputParams());

		variables.put(PARAMS_PREV_ACTION, actionVars);

		actionVars.put(VARKEY_ACTION, getExecutionResultParams(action));
	}

	private void addSubParams(LinkedHashMap<String, LinkedHashMap<String, String>> subParams,
	                          Map<String, Object> execResultParams)
	{
		for (Map.Entry<String, LinkedHashMap<String, String>> entry : subParams.entrySet())
		{
			String subActionId = entry.getKey();
			if (subActionId == null)  //This may happen if sub-output parameters are based on some generated message, not on matrix actions
				continue;

			String subId = actionIdInMatrixToIdForMvel.getOrDefault(subActionId, subActionId);
			//noinspection unchecked
			Map<String, Object> subVars = (Map<String, Object>) variables.get(subId);
			if (subVars == null)  //Checking if reference is valid just in case
				continue;

			LinkedHashMap<String, String> subOutParams = entry.getValue();
			subVars.putAll(subOutParams);
			subVars.put(PARAMS_OUT, subOutParams);
			subVars.put(VARKEY_ACTION, execResultParams);
		}
	}

	private void addSubData(LinkedHashMap<String, SubActionData> subData, Map<String, Object> execResultParams)
	{
		for (String subActionId : subData.keySet())
		{
			String subId = actionIdInMatrixToIdForMvel.getOrDefault(subActionId, subActionId);
			//noinspection unchecked
			Map<String, Object> subVars = (Map<String, Object>) variables.get(subId);
			if (subVars != null)
				subVars.put(VARKEY_ACTION, execResultParams);
		}
	}


	public void cleanAfterAction(Action action)
	{
		String actionId = action.getIdInMatrix();

		Collection<String> idsToClean = cleaningTable.remove(actionId);
		if (isEmpty(idsToClean))
			return;
		
		//todo: Change level to trace after few months of usage.
		log.debug("Cleaning MVEL variables after action '{}'. IDs to clean: {}", actionId, idsToClean);

		for (String idInMatrix : idsToClean)
		{
			String idForMvel = actionIdInMatrixToIdForMvel.getOrDefault(idInMatrix, idInMatrix);
			variables.remove(idForMvel);
		}
	}


	public void saveMatrixInfo(String name, String value)
	{
		//noinspection unchecked
		Map<String, String> info = (Map<String, String>) variables.computeIfAbsent(MATRIX, k -> new HashMap<>());
		info.put(name, value);
	}


	private Map<String, Object> makeInputParamsMap(Map<String, String> inputParams)
	{
		Map<String, Object> commonParams = new LinkedHashMap<>(inputParams);
		Map<String, Object> justInput = new LinkedHashMap<>(inputParams);
		commonParams.put(PARAMS_IN, justInput);
		return commonParams;
	}

	private void addOutputParams(Map<String, Object> destination, Map<String, String> outputParams)
	{
		Map<String, Object> commonParams = new LinkedHashMap<>(outputParams);
		Map<String, Object> justOutput = new LinkedHashMap<>(outputParams);
		commonParams.put(PARAMS_OUT, justOutput);
		destination.putAll(commonParams);
	}

	private Map<String, Object> getExecutionResultParams(Action action)
	{
		if (action.isPassed())
			return PASSED_ACTION_PARAMS;
		else
		{
			Result result = action.getResult();
			FailReason failReason = (result != null) ? result.getFailReason() : FailReason.FAILED;
			return FAILED_ACTION_PARAMS.get(failReason);
		}
	}


	/*
	 * Remapping function for merge of parameters of actions with duplicated IDs.
	 */
	private Map<String, String> mergeActionsParams(Object oldValue, Object newValue)
	{
		//noinspection unchecked
		Map<String, String> newParams = (Map<String, String>) newValue;
		if (oldValue instanceof Map)
		{
			//noinspection unchecked
			Map<String, String> mergedParams = new LinkedHashMap<>((Map<String, String>) oldValue);
			mergedParams.putAll(newParams);
			return mergedParams;
		}
		else 
			return newParams;
	}


	public Map<String, Object> getVariables()
	{
		return variables;
	}

	public Map<String, String> getFixedIds()
	{
		return actionIdInMatrixToIdForMvel;
	}
}
