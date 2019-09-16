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

package com.exactprosystems.clearth.automation.report.results.subclasses;

import java.util.ArrayList;
import java.util.List;

import com.exactprosystems.clearth.automation.report.FailReason;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.ResultDetail;
import com.exactprosystems.clearth.utils.LineBuilder;

public class DetailsBlock
{
	private Result parent;
	private String comment;
	private boolean success = true;
	private List<ResultDetail> details = new ArrayList<ResultDetail>();

	public DetailsBlock() {}

	public DetailsBlock(Result parent, String comment)
	{
		this.parent = parent;
		this.comment = comment;
	}
	
	public String getComment()
	{
		return comment;
	}
	
	public void setComment(String comment)
	{
		this.comment = comment;
	}
	
	public boolean isSuccess()
	{
		return success;
	}
	
	public void setSuccess(boolean success)
	{
		this.success = success;
		if ((!success) && (parent.isSuccess()))
		{
			parent.setSuccess(false);
			parent.setFailReason(FailReason.COMPARISON);
		}
	}
	
	public List<ResultDetail> getDetails()
	{
		return details;
	}
	
	public void addDetail(ResultDetail detail)
	{
		details.add(detail);
		if ((isSuccess()) && (!detail.isIdentical()))
			setSuccess(false);
	}
	
	@Override
	public String toString() 
	{
		return toLineBuilder(new LineBuilder(), "").toString();
	}

	public LineBuilder toLineBuilder(LineBuilder builder, String prefix)
	{
		builder.add(prefix).add(getClass().getSimpleName()).eol();
		builder.add(prefix).add("Success: ").add(success).eol();
		if (comment != null)
			builder.add(prefix).add("Comment: ").add(comment).eol();
		if (details != null) 
			for (ResultDetail detail : details)
				detail.toLineBuilder(builder, prefix + " ");
		return builder;
	}
}
