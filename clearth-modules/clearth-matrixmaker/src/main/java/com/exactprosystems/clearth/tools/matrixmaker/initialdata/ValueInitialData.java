/******************************************************************************
 * Copyright 2009-2025 Exactpro Systems Limited
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

package com.exactprosystems.clearth.tools.matrixmaker.initialdata;

import com.exactprosystems.clearth.tools.matrixmaker.GenerateException;
import org.apache.commons.lang3.StringUtils;

public class ValueInitialData implements InitialData
{
	private String name;
	private String value;

	public ValueInitialData(String name, String value)
	{
		this.name = name;
		this.value = value;
	}

	@Override
	public String getField(String[] params) throws GenerateException
	{
		if(params == null || params.length == 0)
			return value;
		
		if(params.length != 1)
			throw new GenerateException(name + " function must have 1 parameter");
		
		if(StringUtils.equals(params[0], "++"))
			next();
		else
			throw new GenerateException(name + " function does not support parameter " + params[0]);
		
		return value;
	}

	@Override
	public void next()
	{
		value = Integer.toString(Integer.parseInt(value)+1);
	}

	@Override
	public boolean hasNext()
	{
		return true;
	}

	@Override
	public String getFormula()
	{
		return null;
	}
	
	public String toString()
	{
		return name+"="+value;
	}
}
