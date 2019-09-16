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
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use=JsonTypeInfo.Id.MINIMAL_CLASS)
public abstract class Comparison
{
	protected String comment;
	protected boolean passed;
	
	public Comparison()
	{
		comment = "";
		passed = true;
	}
	
	public Comparison(String comment, boolean passed)
	{
		this.comment = comment;
		this.passed = passed;
	}
		
	
	public String getComment()
	{
		return comment;
	}
	
	public void setComment(String comment)
	{
		this.comment = comment;
	}
	
	
	public boolean isPassed()
	{
		return passed;
	}
	
	public void setPassed(boolean passed)
	{
		this.passed = passed;
	}

	@Override
	public String toString()
	{
		return toLineBuilder(new LineBuilder(), "").toString();
	}

	public LineBuilder toLineBuilder(LineBuilder builder, String prefix) 
	{
		builder.add(prefix).add("Passed: ").add(passed).eol();
		builder.add(prefix).add("Comment: ").add(comment).eol();
		return builder;
	}
}
