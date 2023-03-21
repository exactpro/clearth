/*******************************************************************************
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

package com.exactprosystems.clearth.utils.tabledata.comparison.mappings.descs;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.math.BigDecimal;

@XmlAccessorType(XmlAccessType.FIELD)
public class FieldDesc
{
	@XmlAttribute(name = "name", required = true)
	private String localName;
	@XmlElement
	private SourceColumnDesc expected;
	@XmlElement
	private SourceColumnDesc actual;
	@XmlAttribute
	private boolean ignore = false;
	@XmlAttribute
	private boolean info = false;
	@XmlAttribute
	private boolean numeric = false;
	@XmlAttribute
	private boolean key = false;
	@XmlAttribute
	private BigDecimal precision = BigDecimal.ZERO;

	public FieldDesc()
	{
	}

	public String getLocalName()
	{
		return localName;
	}

	public void setLocalName(String localName)
	{
		this.localName = localName;
	}

	public String getExpectedName()
	{
		return expected == null ? localName : expected.getName();
	}

	public String getActualName()
	{
		return actual == null ? localName : actual.getName();
	}

	public SourceColumnDesc getExpected()
	{
		return expected;
	}

	public void setExpected(SourceColumnDesc expected)
	{
		this.expected = expected;
	}

	public SourceColumnDesc getActual()
	{
		return actual;
	}

	public void setActual(SourceColumnDesc actual)
	{
		this.actual = actual;
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

	public boolean isNumeric()
	{
		return numeric;
	}

	public void setNumeric(boolean numeric)
	{
		this.numeric = numeric;
	}

	public boolean isKey()
	{
		return key;
	}

	public void setKey(boolean key)
	{
		this.key = key;
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
