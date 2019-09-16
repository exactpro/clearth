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

package com.exactprosystems.clearth.automation.actions;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.GlobalContext;
import com.exactprosystems.clearth.automation.MatrixContext;
import com.exactprosystems.clearth.automation.StepContext;
import com.exactprosystems.clearth.automation.exceptions.FailoverException;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.results.DefaultResult;

public class SetBaseTime extends Action
{
	public static final String PARAM_TIME = "Time";
	protected static final DateFormat formatterShort = new SimpleDateFormat("HH:mm"),
			formatterLong = new SimpleDateFormat("HH:mm:ss"),
			formatterLongest = new SimpleDateFormat("HH:mm:ss.SSS");
	
	@Override
	protected Result run(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext) throws ResultException, FailoverException
	{
		String time = getInputParam(PARAM_TIME, "");
		
		Date bt = null;
		if (!time.isEmpty())
		{
			for (DateFormat df : getFormatters())
			{
				try
				{
					bt = df.parse(time);
				}
				catch (ParseException e)
				{
					//Skip exception and try next formatter
				}
			}
			
			if (bt == null)
				return DefaultResult.failedWithComment("Could not parse value from '"+PARAM_TIME+"' parameter as time.");
		}
		
		globalContext.getMatrixFunctions().setBaseTime(bt);
		logger.warn(String.format("Action '%s' from matrix '%s' has set scheduler base time to '%s'", idInMatrix, getMatrix().getName(), time));
		return null;
	}
	
	
	protected DateFormat[] getFormatters()
	{
		return new DateFormat[]{formatterLongest, formatterLong, formatterShort};
	}
}
