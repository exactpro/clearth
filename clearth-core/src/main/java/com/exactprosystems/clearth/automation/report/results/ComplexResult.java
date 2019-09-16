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

package com.exactprosystems.clearth.automation.report.results;

import java.util.ArrayList;
import java.util.List;

import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.results.complex.ComplexResultDetails;
import com.exactprosystems.clearth.utils.LineBuilder;

public class ComplexResult extends Result
{
	private static final long serialVersionUID = -1889650938013789140L;

	private List<ComplexResultDetails> resultDetails = new ArrayList<ComplexResultDetails>();

	public List<ComplexResultDetails> getResultDetails()
	{
		return this.resultDetails;
	}

	/**
	 * Add result detail to result
	 * @param resultDetail instance of resultDetail
	 */
	public void addResultDetail(ComplexResultDetails resultDetail)
	{
		this.resultDetails.add(resultDetail);
	}

	@Override
	public void clearDetails()
	{
		resultDetails.clear();
	}

	@Override
	public LineBuilder toLineBuilder(LineBuilder builder, String prefix)
	{
		super.toLineBuilder(builder, prefix);
		builder.add(prefix).add("ResultDetails:").eol();
		for (ComplexResultDetails detail: resultDetails)
			detail.toLineBuilder(builder, prefix + " ");
		return builder;
	}
}
