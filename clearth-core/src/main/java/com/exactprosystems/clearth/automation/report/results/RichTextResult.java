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

import com.exactprosystems.clearth.automation.report.results.subclasses.RichTextElement;
import com.exactprosystems.clearth.utils.LineBuilder;

import java.util.ArrayList;
import java.util.List;

public class RichTextResult extends DetailedResult
{
	private static final long serialVersionUID = 4963104517115924293L;

	private String title;
	private List<RichTextElement> richTextElements = new ArrayList<RichTextElement>();

	public RichTextResult()
	{
		super();
	}

	/**
	 * @param title
	 * = null for write this element without expander
	 */
	public RichTextResult(String title)
	{
		super();
		this.title = title;
	}

	public void addElement(RichTextElement element)
	{
		richTextElements.add(element);
	}

	public List<RichTextElement> getElements()
	{
		return richTextElements;
	}

	public String getTitle()
	{
		return title;
	}

	public void setTitle(String title)
	{
		this.title = title;
	}

	public void clearElements()
	{
		this.richTextElements.clear();
	}

	@Override
	public LineBuilder toLineBuilder(LineBuilder builder, String prefix)
	{
		super.toLineBuilder(builder, prefix);
		builder.add(prefix).add("RichTextElements:").eol();
		for (RichTextElement element : richTextElements)
			element.toLineBuilder(builder, prefix + " ");
		return builder;
	}
}
