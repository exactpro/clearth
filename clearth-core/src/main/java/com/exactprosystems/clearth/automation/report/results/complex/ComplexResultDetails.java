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

import java.util.LinkedHashMap;
import java.util.Map;

public class ComplexResultDetails
{
	private String comment;
	private boolean passed = true;
	private SimpleComparison mainComparison = new SimpleComparison();
	private Map<String, ComplexComparison> subComparisons = null;
	
	
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
	
	
	public SimpleComparison getMainComparison()
	{
		return mainComparison;
	}
	
	public void setMainComparison(SimpleComparison mainComparison)
	{
		this.mainComparison = mainComparison;
	}
	
	
	public Map<String, ComplexComparison> getSubComparisons()
	{
		return subComparisons;
	}

	public void setSubComparisons(Map<String, ComplexComparison> subComparisons)
	{
		this.subComparisons = subComparisons;
	}
	
	public void addSubComparison(String actionId, ComplexComparison subComparison)
	{
		if (subComparisons==null)
			subComparisons = new LinkedHashMap<String, ComplexComparison>();
		subComparisons.put(actionId, subComparison);
	}
	
	public ComplexComparison getSubComparison(String actionId, boolean createIfNeeded)
	{
		ComplexComparison cc;
		if (subComparisons != null)
			cc = subComparisons.get(actionId);
		else
			cc = null;
		
		if (cc != null)
			return cc;
		
		if (!createIfNeeded)
			return null;
			
		cc = new ComplexComparison();
		addSubComparison(actionId, cc);
		return cc;
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
		builder.add(prefix).add("MainComparison: ").eol();
		mainComparison.toLineBuilder(builder, prefix + " ");
		builder.add(prefix).add("SubComparisons: ").eol();
		for (Map.Entry<String, ComplexComparison> sub: subComparisons.entrySet())
		{
			builder.append(prefix).add(" ").add(sub.getKey()).add(":").eol();
			sub.getValue().toLineBuilder(builder, prefix + " ");
		}
		return builder;
	}
}
