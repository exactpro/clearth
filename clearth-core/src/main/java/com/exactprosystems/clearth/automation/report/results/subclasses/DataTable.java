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

import com.exactprosystems.clearth.utils.LineBuilder;

import java.util.ArrayList;
import java.util.List;

public class DataTable implements RichTextElement
{
	private String title;
	private String comment;
	private List<String> headers;
	private List<List<String>> records;

	public DataTable() {}

	public DataTable(String title)
	{
		this.title = title;
		this.records = new ArrayList<List<String>>();
	}


	public DataTable(String title, List<String> headers)
	{
		this.title = title;
		this.headers = headers;
		this.records = new ArrayList<List<String>>();
	}

	public DataTable(String title, List<String> headers, List<List<String>> records)
	{
		this.title = title;
		this.headers = headers;
		this.records = records;
	}

	public void addRecord(List<String> record)
	{
		records.add(record);
	}

	public List<List<String>> getRecords()
	{
		return records;
	}

	public void setHeaders(List<String> headers)
	{
		this.headers = headers;
	}

	public List<String> getHeaders()
	{
		return headers;
	}

	public String getTitle()
	{
		return title;
	}

	public void setTitle(String title)
	{
		this.title = title;
	}

	public String getComment()
	{
		return comment;
	}

	public void setComment(String comment)
	{
		this.comment = comment;
	}

	@Override
	public String toString()
	{
		return toLineBuilder(new LineBuilder(), "").toString();
	}

	@Override
	public LineBuilder toLineBuilder(LineBuilder builder, String prefix)
	{
		int cols = headers == null? 0 : headers.size();
		int rows = records == null? 0 : records.size();
		builder.add(prefix).add(getClass().getSimpleName()).add(":").eol();
		builder.add(prefix).add(" Title: ").add(title).eol();
		if (comment != null)
			builder.add(prefix).add(" Comment: ").add(comment).eol();
		builder.add(prefix).add(" Data: {Columns: ").add(cols).add(", Rows: ").add(rows).add("}").eol();
		return builder;
	}

}
