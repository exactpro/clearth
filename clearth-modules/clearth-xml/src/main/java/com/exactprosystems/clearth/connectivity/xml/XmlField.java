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

package com.exactprosystems.clearth.connectivity.xml;

import org.apache.commons.lang.StringUtils;

public class XmlField implements Cloneable
{
	protected String value;
	protected boolean numeric;
	
	public XmlField(String value, boolean numeric)
	{
		this.value = value;
		this.numeric = numeric;
	}
	
	@Override
	public String toString()
	{
		return value;
	}
	
	@Override
	public XmlField clone()
	{
		return new XmlField(value, numeric);
	}

	public String getValue()
	{
		return value;
	}

	public void setValue(String value)
	{
		this.value = value;
	}

	public boolean isNumeric()
	{
		return numeric;
	}

	public void setNumeric(boolean numeric)
	{
		this.numeric = numeric;
	}
	
	@Override
	public boolean equals(Object object)
	{
		if (this == object)
			return true;
		if (!(object instanceof  XmlField))
			return false;	
		XmlField xmlField = (XmlField) object;
		return (this.numeric == xmlField.numeric) 
				&& StringUtils.equals(this.value, ((XmlField) object).value);
	}
}
