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

package com.exactprosystems.clearth.utils.tabledata.comparison;

import com.exactprosystems.clearth.automation.report.ResultDetail;
import com.exactprosystems.clearth.automation.report.results.DetailedResult;

import java.util.ArrayList;
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
	private RowComparisonResult result = RowComparisonResult.PASSED;
	
	public RowComparisonData()
	{
		compDetails = new ArrayList<>();
		errors = new ArrayList<>();
	}
	
	public void addComparisonDetail(A column, B expectedValue, B actualValue, boolean identical)
	{
		if (result != RowComparisonResult.FAILED)
		{
			if (expectedValue == null || actualValue == null)
			{
				if (expectedValue == null)
				{
					if (result == RowComparisonResult.NOT_FOUND)
						result = RowComparisonResult.FAILED;
					else
						result = RowComparisonResult.EXTRA;
				}
				
				if (actualValue == null)
				{
					if (result == RowComparisonResult.EXTRA)
						result = RowComparisonResult.FAILED;
					else
						result = RowComparisonResult.NOT_FOUND;
				}
			}
			else if (!identical)
				result = RowComparisonResult.FAILED;
		}
		compDetails.add(new RowComparisonDetail<>(column, expectedValue, actualValue, false, identical));
	}
	
	public void addInfoComparisonDetail(A column, B actualValue)
	{
		compDetails.add(new RowComparisonDetail<>(column, null, actualValue, true, true));
	}
	
	public void addErrorMsg(String errorMsg)
	{
		errors.add(errorMsg);
	}
	
	public boolean isSuccess()
	{
		return compDetails.stream().allMatch(RowComparisonDetail::isIdentical);
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

	public List<RowComparisonDetail<A, B>> getCompDetails()
	{
		return compDetails;
	}

	public List<String> getErrors()
	{
		return errors;
	}
	
	public RowComparisonResult getResult()
	{
		return result;
	}
}
