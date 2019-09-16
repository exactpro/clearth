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

package com.exactprosystems.clearth.automation.actions;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.GlobalContext;
import com.exactprosystems.clearth.automation.MatrixContext;
import com.exactprosystems.clearth.automation.StepContext;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.ResultDetail;
import com.exactprosystems.clearth.automation.report.results.DetailedResult;
import com.exactprosystems.clearth.utils.ComparisonPair;
import com.exactprosystems.clearth.utils.ComparisonUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class CompareMultipleValues extends Action
{
	private static final Logger log = LoggerFactory.getLogger(CompareMultipleValues.class);
	
	@Override
	protected Result run(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext)
			throws ResultException
	{
		List<ComparisonPair<String>> comparisonPairs = collectPairs();
		return comparePairs(comparisonPairs);
	}
	
	protected void parseParameter(String name, String value, Map<String, String> expected, Map<String, String> actual)
	{
		String[] dotSplitParam = name.split("\\.");

		if (dotSplitParam.length == 2) {
			String side = dotSplitParam[0],
					param = dotSplitParam[1];

			if ("Expected".equals(side)) {
				expected.put(param, value);
			} else if ("Actual".equals(side)) {
				actual.put(param, value);
			} else {
				log.warn(String.format("Parameter '%s' has unexpected name. Expected syntax: Expected.<param>, Actual.<param>", name));
			}
		}
	}
	
	protected List<ComparisonPair<String>> mergePairs(Map<String, String> expected, Map<String, String> actual)
	{
		Collection<String> params = CollectionUtils.intersection(expected.keySet(), actual.keySet());
		List<ComparisonPair<String>> result = new ArrayList<ComparisonPair<String>>(params.size());
		for (String param : params)
			result.add(new ComparisonPair<String>(param, expected.get(param), actual.get(param)));
		return result;
	}

	protected List<ComparisonPair<String>> collectPairs()
	{
		Map<String, String> expectedParams = new LinkedHashMap<String, String>(),
				actualParams = new LinkedHashMap<String, String>();
		for (Map.Entry<String,String> entry : inputParams.entrySet())
			parseParameter(entry.getKey(), entry.getValue(), expectedParams, actualParams);
		
		List<ComparisonPair<String>> result = mergePairs(expectedParams, actualParams);
		return result;
	}
	
	
	protected ResultDetail compare(ComparisonPair<String> pair, ComparisonUtils cu)
	{
		return cu.createResultDetail(pair.getName(), pair.getExpectedValue(), pair.getActualValue());
	}
	
	protected Result comparePairs(List<ComparisonPair<String>> pairs)
	{
		DetailedResult result = new DetailedResult();
		ComparisonUtils cu = ClearThCore.comparisonUtils();
		for (ComparisonPair<String> comparisonPair : pairs)
		{
			ResultDetail resultDetail = compare(comparisonPair, cu);
			result.addResultDetail(resultDetail);
		}
		return result;
	}
}
