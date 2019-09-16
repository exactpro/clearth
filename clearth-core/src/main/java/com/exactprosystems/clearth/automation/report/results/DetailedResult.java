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
import java.util.Objects;

import com.exactprosystems.clearth.automation.report.FailReason;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.ResultDetail;
import com.exactprosystems.clearth.utils.LineBuilder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties({"maxWidth"})
public class DetailedResult extends Result
{
	private static final long serialVersionUID = 5893835088217062776L;

	protected List<ResultDetail> details = new ArrayList<ResultDetail>();
	private boolean maxWidth = true;
	
	@Override
	public void clearDetails()
	{
		details.clear();
	}

	public List<ResultDetail> getResultDetails()
	{
		return details;
	}

	public void addResultDetail(ResultDetail rd)
	{
		details.add(rd);
		if ((isSuccess()) && (!rd.isIdentical()))
		{
			setSuccess(false);
			setFailReason(FailReason.COMPARISON);
		}
	}

	@Override
	public LineBuilder toLineBuilder(LineBuilder builder, String prefix)
	{
		super.toLineBuilder(builder, prefix);
		builder.add(prefix).add("Details (Identical / Exp. / Act.)").add((details == null ? " null" : "")).eol();
		if (details != null) 
			for (ResultDetail detail : details)
				detail.toLineBuilder(builder, prefix + " ");
		return builder;
	}

	public boolean isMaxWidth()
	{
		return maxWidth;
	}

	public void setMaxWidth(boolean isMaxWidth)
	{
		this.maxWidth = isMaxWidth;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (!(o instanceof DetailedResult)) return false;
		if (!super.equals(o)) return false;
		DetailedResult result = (DetailedResult) o;
		return maxWidth == result.maxWidth &&
				Objects.equals(details, result.details);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(super.hashCode(), details, maxWidth);
	}
}
