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

package com.exactprosystems.clearth.utils;

/**
 *         07 December 2016
 */
public enum SpecialValue
{
	EMPTY("@{empty}", ""),
	SPACE("@{space}", " "),
	UNKNOWN("", ""),
	NULL("@{null}","");
	
	private final String matrixName;
	private final String value;

	SpecialValue(String matrixName, String value)
	{
		this.matrixName = matrixName;
		this.value = value;
	}
	
	public String matrixName()
	{
		return matrixName;
	}
	
	public String value()
	{
		return value;
	}
	
	public static SpecialValue byMatrixName(String name)
	{
		for (SpecialValue sv : values())
		{
			if (sv.matrixName.equals(name))
				return sv;
		}
		return UNKNOWN;
	}
	
	public static boolean isSpecialValue(String value)
	{
		return byMatrixName(value) != UNKNOWN;
	}
	
	public static String convert(String matrixName)
	{
		SpecialValue sv = byMatrixName(matrixName);
		return (sv != UNKNOWN) ? sv.value() : matrixName;
	}
}
