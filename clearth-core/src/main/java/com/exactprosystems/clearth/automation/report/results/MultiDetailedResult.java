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
import com.exactprosystems.clearth.automation.report.ResultDetail;
import com.exactprosystems.clearth.automation.report.results.subclasses.DetailsBlock;
import com.exactprosystems.clearth.utils.LineBuilder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

//Result with few comparison tables, each with optional comment
@JsonIgnoreProperties({"maxWidth", "currentBlock"})
public class MultiDetailedResult extends Result
{
	private static final long serialVersionUID = -231684978831215582L;

	protected List<DetailsBlock> details = new ArrayList<DetailsBlock>();
	protected boolean maxWidth = true;

	public MultiDetailedResult()
	{
	}

	@Override
	public void clearDetails()
	{
		details.clear();
	}

	public List<DetailsBlock> getDetails()
	{
		return details;
	}

	public DetailsBlock getCurrentBlock()
	{
		if (details.isEmpty())
			startNewBlock(null);
		return details.get(details.size() - 1);
	}

	public void startNewBlock(String comment)
	{
		details.add(new DetailsBlock(this, comment));
	}

	public String getBlockComment()
	{
		return getCurrentBlock().getComment();
	}

	public void setBlockComment(String comment)
	{
		getCurrentBlock().setComment(comment);
	}

	public boolean isBlockSuccess()
	{
		return getCurrentBlock().isSuccess();
	}

	public void setBlockSuccess(boolean success)
	{
		getCurrentBlock().setSuccess(success);
	}

	public void addResultDetail(ResultDetail rd)
	{
		getCurrentBlock().addDetail(rd);
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
	public LineBuilder toLineBuilder(LineBuilder builder, String prefix)
	{
		super.toLineBuilder(builder, prefix);
		builder.add(prefix).add("Details (Identical / Exp. / Act.)").add((details == null ? " null" : "")).eol();
		if (details != null) 
			for (DetailsBlock detail : details)
				detail.toLineBuilder(builder, prefix + " ");
		return builder;
	}
}
