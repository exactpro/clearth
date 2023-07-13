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

package com.exactprosystems.clearth.tools.matrixupdater.settings;

import com.exactprosystems.clearth.tools.matrixupdater.model.Cell;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Condition")
public class Condition
{
	@XmlAttribute
	private String name;

	@XmlElement(name = "Cell")
	private List<Cell> cells;

	public Condition()
	{
		cells = new ArrayList<>();
	}

	public Condition(String name)
	{
		this();
		this.name = name;
	}

	public Condition(String name, List<Cell> cells)
	{
		this.name = name;
		this.cells = cells;
	}

	public void addCell(Cell cell)
	{
		cells.add(cell);
	}

	public List<Cell> getCells()
	{
		return cells;
	}

	public String getName()
	{
		return name;
	}

	public void setCells(List<Cell> cells)
	{
		this.cells = cells;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Condition condition = (Condition) o;
		return Objects.equals(name, condition.name) &&
			Objects.equals(cells, condition.cells);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(name, cells);
	}
}
