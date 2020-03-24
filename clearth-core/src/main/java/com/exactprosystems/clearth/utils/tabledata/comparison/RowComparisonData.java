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

package com.exactprosystems.clearth.utils.tabledata.comparison;

import com.exactprosystems.clearth.automation.report.ResultDetail;
import com.exactprosystems.clearth.automation.report.results.DetailedResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Storage of table rows comparison details.
 * @param <A> class of columns objects.
 * @param <B> class of expected and actual values.
 */
public class RowComparisonData<A, B>
{
	private List<RowComparisonDetail<A, B>> compDetails;
	private List<String> errors;
	private RowComparisonResultType resultType = RowComparisonResultType.PASSED;
	
	public RowComparisonData()
	{
		compDetails = new ArrayList<>();
		errors = new ArrayList<>();
	}
	
	public void addComparisonDetail(A column, B expectedValue, B actualValue, boolean identical)
	{
		if (resultType != RowComparisonResultType.FAILED)
		{
			if (expectedValue == null || actualValue == null)
			{
				if (expectedValue == null)
				{
					if (resultType == RowComparisonResultType.NOT_FOUND)
						resultType = RowComparisonResultType.FAILED;
					else
						resultType = RowComparisonResultType.EXTRA;
				}
				
				if (actualValue == null)
				{
					if (resultType == RowComparisonResultType.EXTRA)
						resultType = RowComparisonResultType.FAILED;
					else
						resultType = RowComparisonResultType.NOT_FOUND;
				}
			}
			else if (!identical)
				resultType = RowComparisonResultType.FAILED;
		}
		compDetails.add(new RowComparisonDetail<>(column, expectedValue, actualValue, false, identical));
	}
	
	public void addInfoComparisonDetail(A column, B expectedValue, B actualValue)
	{
		compDetails.add(new RowComparisonDetail<>(column, expectedValue, actualValue, true, true));
	}
	
	public void addErrorMsg(String errorMsg)
	{
		errors.add(errorMsg);
	}
	
	
	public boolean isSuccess()
	{
		return compDetails.stream().allMatch(RowComparisonDetail::isIdentical);
	}
	
	public List<RowComparisonDetail<A, B>> getCompDetails()
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
	 * Converts this {@code RowComparisonData} instance to {@code DetailedResult} one.
	 * @return result of conversion.
	 */
	public DetailedResult toDetailedResult()
	{
		DetailedResult result = new DetailedResult();
		for (RowComparisonDetail<A, B> compDetail : compDetails)
		{
			ResultDetail detail = new ResultDetail();
			detail.setParam(compDetail.getColumn().toString());
			B expectedValue = compDetail.getExpectedValue(), actualValue = compDetail.getActualValue();
			detail.setExpected(expectedValue != null ? expectedValue.toString() : null);
			detail.setActual(actualValue != null ? actualValue.toString() : null);
			detail.setInfo(compDetail.isInfo());
			detail.setIdentical(compDetail.isIdentical());
			result.addResultDetail(detail);
		}
		return result;
	}
}
