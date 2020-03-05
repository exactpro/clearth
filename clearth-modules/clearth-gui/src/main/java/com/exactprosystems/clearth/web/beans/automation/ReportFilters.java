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

package com.exactprosystems.clearth.web.beans.automation;

import com.exactprosystems.clearth.automation.Matrix;
import com.exactprosystems.clearth.automation.Step;
import com.exactprosystems.clearth.xmldata.XmlMatrixInfo;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class ReportFilters {

	protected List<Step> rtSelectedSteps;

	public void setRtSelectedSteps(List<Step> rtSelectedSteps) {
		this.rtSelectedSteps = rtSelectedSteps;
	}

	public boolean filterRTStepByRegEx(Object value, Object filter, Locale locale)
	{
		String filterRegEx = (filter == null) ? null : filter.toString().trim();
		if (StringUtils.isEmpty(filterRegEx))
			return true;
		if (value == null)
			return false;
		try
		{
			Pattern pattern = Pattern.compile(filterRegEx);
			Matcher matcher = pattern.matcher(value.toString());
			return matcher.find();
		}
		catch (PatternSyntaxException e)
		{
			return false;
		}
	}

	public boolean filterReportsInfoByState(Object value, Object filter, Locale locale)
	{
		if (filter == null)
			return true;
		if (value == null)
			return false;
		Integer filterValue = (Integer) filter;
		XmlMatrixInfo matrixInfo = (XmlMatrixInfo) value;
		switch (filterValue)
		{
			case 1:
				return matrixInfo.getActionsDone() > 0;
			case 2:
				return !matrixInfo.isSuccessful();
			case 3:
				return matrixInfo.getActionsDone() > 0 && !matrixInfo.isSuccessful();
			default:
				return true;
		}
	}

	public boolean filterReportsInfoByRegEx(Object value, Object filter, Locale locale) {
		String filterRegEx = (filter == null) ? null : filter.toString().trim();
		if (StringUtils.isEmpty(filterRegEx))
			return true;
		if (value == null)
			return false;

		try
		{
			Pattern pattern = Pattern.compile(filterRegEx);
			Matcher matcher = pattern.matcher(value.toString());
			return matcher.find();
		}
		catch (PatternSyntaxException e)
		{
			return false;
		}
	}

	public boolean filterRTMatricesByState(Object value, Object filter, Locale locale) {
		if(filter == null) {
			return true;
		} else if(value == null) {
			return false;
		} else {
			Integer filterValue = (Integer)filter;
			Matrix matrix = (Matrix)value;
			switch(filterValue) {
				case 1: //Passed
					if(rtSelectedSteps.isEmpty())
					{
						return matrix.isSuccessful();
					}
					else
					{
						return !hasFailedSelectedSteps(rtSelectedSteps, matrix);
					}
				case 2: //Started
					return matrix.getActionsDone() > 0;
				case 3: //Failed
					if(rtSelectedSteps.isEmpty())
					{
						return !matrix.isSuccessful();
					}
					else
					{
						return hasFailedSelectedSteps(rtSelectedSteps, matrix);
					}
				case 4: //Started and failed
					return matrix.getActionsDone() > 0 && !matrix.isSuccessful();
				case 5: //Finished
					int done = matrix.getActionsDone();
					return done > 0 && (matrix.getActions().size() - matrix.getNonExecutableActions().size()) == done;
				default:
					return true;
			}
		}
	}

	private boolean hasFailedSelectedSteps(List<Step> selectedSteps, Matrix matrix)
	{
		boolean hasFailedSelectedSteps = false;
		for (Step s : selectedSteps)
		{
			hasFailedSelectedSteps = hasFailedSelectedSteps || !matrix.isStepSuccessful(s.getName());
		}
		return hasFailedSelectedSteps;
	}
	
}
