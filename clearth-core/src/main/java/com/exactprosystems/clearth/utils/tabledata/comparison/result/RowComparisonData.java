/******************************************************************************
 * Copyright 2009-2020 Exactpro Systems Limited
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

package com.exactprosystems.clearth.utils.tabledata.comparison.result;

import com.exactprosystems.clearth.automation.report.FailReason;
import com.exactprosystems.clearth.automation.report.ResultDetail;
import com.exactprosystems.clearth.automation.report.results.DetailedResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Storage of comparison details for two processed table rows.
 * @param <A> class of columns objects.
 * @param <B> class of expected and actual values.
 */
public class RowComparisonData<A, B>
{
	private final List<ColumnComparisonDetail<A, B>> compDetails;
	private final List<String> errors;
	private RowComparisonResultType resultType = RowComparisonResultType.PASSED;
	
	public RowComparisonData()
	{
		compDetails = new ArrayList<>();
		errors = new ArrayList<>();
	}
	
	public void addComparisonDetail(ColumnComparisonDetail<A, B> compDetail)
	{
		compDetails.add(compDetail);
	}
	
	public void addComparisonDetail(A column, B expectedValue, B actualValue, boolean identical)
	{
		addComparisonDetail(new ColumnComparisonDetail<>(column, expectedValue, actualValue, identical, false));
	}
	
	public void addInfoComparisonDetail(A column, B expectedValue, B actualValue)
	{
		addComparisonDetail(new ColumnComparisonDetail<>(column, expectedValue, actualValue, true, true));
	}
	
	public void addErrorMsg(String errorMsg)
	{
		errors.add(errorMsg);
	}
	
	
	public boolean isSuccess()
	{
		return compDetails.stream().allMatch(ColumnComparisonDetail::isIdentical);
	}
	
	public List<ColumnComparisonDetail<A, B>> getCompDetails()
	{
		return Collections.unmodifiableList(compDetails);
	}
	
	public List<String> getErrors()
	{
		return Collections.unmodifiableList(errors);
	}
	
	public RowComparisonResultType getResultType()
	{
		return resultType;
	}

	/**
	 * Converts this {@link RowComparisonData} instance to {@link DetailedResult} one.
	 * @return result of conversion.
	 */
	public DetailedResult toDetailedResult()
	{
		DetailedResult result = new DetailedResult();
		for (ColumnComparisonDetail<A, B> compDetail : compDetails)
		{
			ResultDetail detail = new ResultDetail();
			detail.setParam(compDetail.getColumn().toString());
			B expectedValue = compDetail.getExpectedValue(), actualValue = compDetail.getActualValue();
			detail.setExpected(expectedValue != null ? expectedValue.toString() : null);
			detail.setActual(actualValue != null ? actualValue.toString() : null);
			detail.setIdentical(compDetail.isIdentical());
			detail.setInfo(compDetail.isInfo());
			result.addResultDetail(detail);
		}
		if (resultType == RowComparisonResultType.NOT_FOUND || resultType == RowComparisonResultType.EXTRA)
			result.setFailReason(FailReason.FAILED);
		return result;
	}

	public void completeRow()
	{
		resultType = calculateResultType();
	}

	protected RowComparisonResultType calculateResultType()
	{
		boolean isPassed = true, hasPassed = false;
		boolean isActualNull = true, isExpectedNull = true;
		for(ColumnComparisonDetail<A,B> detail: compDetails)
		{
			if (detail.isIdentical())
			{
				hasPassed = true;
				continue;
			}
			isPassed = false;
			if (detail.getActualValue() != null)
				isActualNull = false;
			if (detail.getExpectedValue() != null)
				isExpectedNull = false;
		}
		if (isPassed)
		{
			return RowComparisonResultType.PASSED;
		}
		if (!hasPassed && isActualNull)
		{
			return RowComparisonResultType.NOT_FOUND;
		}
		if (!hasPassed && isExpectedNull)
		{
			return RowComparisonResultType.EXTRA;
		}
		return RowComparisonResultType.FAILED;
	}
}
