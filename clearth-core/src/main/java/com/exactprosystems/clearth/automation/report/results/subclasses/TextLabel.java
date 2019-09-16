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

public class TextLabel implements RichTextElement
{
	private String text;

	public TextLabel() {}

	public TextLabel(String text)
	{
		this.text = text;
	}

	public String getText()
	{
		return text;
	}

	public void setText(String text)
	{
		this.text = text;
	}

	@Override
	public String toString()
	{
		return toLineBuilder(new LineBuilder(), "").toString();
	}

	@Override
	public LineBuilder toLineBuilder(LineBuilder builder, String prefix)
	{
		builder.add(prefix).add(getClass().getSimpleName()).add(": ").add(text).eol();
		return builder;
	}
}
