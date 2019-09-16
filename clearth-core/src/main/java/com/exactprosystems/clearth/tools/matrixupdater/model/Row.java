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

package com.exactprosystems.clearth.tools.matrixupdater.model;

import com.exactprosystems.clearth.utils.tabledata.TableHeader;

import java.util.List;

public class Row
{
	private TableHeader<String> header;
	private List<String> values;

	public Row(List<String> values)
	{
		this.values = values;
	}

	public Row(TableHeader<String> header, List<String> values)
	{
		this.header = header;
		this.values = values;

		regulateValues();
	}

	public void addValue(String value)
	{
		values.add(value);
	}

	private void regulateValues()
	{
		while (values.size() < header.size())
			values.add("");
	}

	public List<String> getValues()
	{
		return values;
	}

	public TableHeader<String> getHeader()
	{
		return header;
	}

	public void setValues(List<String> values)
	{
		this.values = values;
	}

	public void setHeader(TableHeader<String> header)
	{
		this.header = header;
		regulateValues();
	}
}
