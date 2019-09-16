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

package com.exactprosystems.clearth.automation.report.results.complex;

import com.exactprosystems.clearth.utils.LineBuilder;

import java.util.ArrayList;
import java.util.List;

public class ComparisonBlock
{
	private String comment;
	private List<ComparisonRow> rows = null;
	
	public ComparisonBlock()
	{
		comment = "";
	}
	
	public ComparisonBlock(String comment)
	{
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
	

	public List<ComparisonRow> getRows()
	{
		return rows;
	}

	public void setRows(List<ComparisonRow> rows)
	{
		this.rows = rows;
	}
	
	public void addRow(ComparisonRow row)
	{
		if (rows==null)
			rows = new ArrayList<ComparisonRow>();
		rows.add(row);
	}

	@Override
	public String toString()
	{
		return toLineBuilder(new LineBuilder(), "").toString();
	}

	public LineBuilder toLineBuilder(LineBuilder builder, String prefix)
	{
		builder.add(prefix).add("Comment: ").add(comment).eol();
		builder.add(prefix).add("Rows (Identical / Exp. / Act.)").eol();
		if (rows != null)
			for (ComparisonRow row: rows)
				row.toLineBuilder(builder, prefix + " ");
		return builder;
	}
}
