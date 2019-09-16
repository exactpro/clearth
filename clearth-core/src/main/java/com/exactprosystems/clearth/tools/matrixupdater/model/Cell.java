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

package com.exactprosystems.clearth.tools.matrixupdater.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Cell")
public class Cell
{
	@XmlAttribute
	private String column;
	@XmlAttribute
	private String value;

	public Cell() {}

	public Cell(String column, String value)
	{
		setColumn(column);
		this.value = value;
	}

	public String getColumn()
	{
		return column;
	}

	public String getValue()
	{
		return value;
	}

	public void setColumn(String column)
	{
		column = "#" + column.replaceAll("#", "");


		this.column = column;
	}

	public void setValue(String value)
	{
		this.value = value;
	}
}
