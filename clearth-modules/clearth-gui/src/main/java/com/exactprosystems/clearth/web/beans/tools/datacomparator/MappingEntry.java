/******************************************************************************
 * Copyright 2009-2023 Exactpro Systems Limited
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

package com.exactprosystems.clearth.web.beans.tools.datacomparator;

import java.math.BigDecimal;

import org.apache.commons.lang3.StringUtils;

import com.exactprosystems.clearth.utils.tabledata.comparison.mappings.descs.FieldDesc;
import com.exactprosystems.clearth.utils.tabledata.comparison.mappings.descs.SourceColumnDesc;

public class MappingEntry
{
	private String name,
			expectedName,
			actualName;
	private boolean key,
			numeric,
			ignore,
			info;
	private BigDecimal precision;
	
	public static MappingEntry fromFieldDesc(FieldDesc fieldDesc)
	{
		MappingEntry result = new MappingEntry();
		result.setName(fieldDesc.getLocalName());
		if (fieldDesc.getExpected() != null)
			result.setExpectedName(fieldDesc.getExpected().getName());
		if (fieldDesc.getActual() != null)
			result.setActualName(fieldDesc.getActual().getName());
		result.setKey(fieldDesc.isKey());
		result.setNumeric(fieldDesc.isNumeric());
		result.setIgnore(fieldDesc.isIgnore());
		result.setInfo(fieldDesc.isInfo());
		result.setPrecision(fieldDesc.getPrecision());
		return result;
	}
	
	
	public FieldDesc toFieldDesc()
	{
		FieldDesc result = new FieldDesc();
		result.setLocalName(getName());
		
		if (!StringUtils.isEmpty(getExpectedName()))
		{
			SourceColumnDesc scd = new SourceColumnDesc();
			scd.setName(getExpectedName());
			result.setExpected(scd);
		}
		
		if (!StringUtils.isEmpty(getActualName()))
		{
			SourceColumnDesc scd = new SourceColumnDesc();
			scd.setName(getActualName());
			result.setActual(scd);
		}
		
		result.setKey(isKey());
		result.setNumeric(isNumeric());
		result.setIgnore(isIgnore());
		result.setInfo(isInfo());
		result.setPrecision(getPrecision());
		return result;
	}
	
	
	public String getName()
	{
		return name;
	}
	
	public void setName(String name)
	{
		this.name = name;
	}
	
	
	public String getExpectedName()
	{
		return expectedName;
	}
	
	public void setExpectedName(String expectedName)
	{
		this.expectedName = expectedName;
	}
	
	
	public String getActualName()
	{
		return actualName;
	}
	
	public void setActualName(String actualName)
	{
		this.actualName = actualName;
	}
	
	
	public boolean isKey()
	{
		return key;
	}
	
	public void setKey(boolean key)
	{
		this.key = key;
	}
	
	
	public boolean isNumeric()
	{
		return numeric;
	}
	
	public void setNumeric(boolean numeric)
	{
		this.numeric = numeric;
	}
	
	
	public boolean isIgnore()
	{
		return ignore;
	}
	
	public void setIgnore(boolean ignore)
	{
		this.ignore = ignore;
	}
	
	
	public boolean isInfo()
	{
		return info;
	}
	
	public void setInfo(boolean info)
	{
		this.info = info;
	}
	
	
	public BigDecimal getPrecision()
	{
		return precision;
	}
	
	public void setPrecision(BigDecimal precision)
	{
		this.precision = precision;
	}
}
