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
import org.apache.commons.lang.StringUtils;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Change")
public class Change
{
	@XmlAttribute
	private Boolean before;

	@XmlAttribute
	private String addition;

	@XmlAttribute
	private Boolean updateIDs;

	@XmlElement(name = "Cell", required = true)
	private List<Cell> cells;

	public Change()
	{
		cells = new ArrayList<>();
	}

	public Change(Boolean before, String addition, Boolean updateIDs, List<Cell> cells)
	{
		this.before = before;
		setAddition(addition);
		this.updateIDs = updateIDs;
		this.cells = cells;
	}

	/** GETTER */

	public void addCell (Cell cell)
	{
		cells.add(cell);
	}

	public Boolean isBefore()
	{
		return before == null || before;
	}

	public Boolean isUpdateIDs()
	{
		return updateIDs != null && updateIDs;
	}

	public List<Cell> getCells()
	{
		return cells;
	}

	public String getAddition()
	{
		return addition;
	}

	/** SETTER */
	public void setBefore(Boolean before)
	{
		this.before = before;
	}

	public void setUpdateIDs(Boolean updateIDs)
	{
		this.updateIDs = updateIDs;
	}

	public void setAddition(String addition)
	{
		parseAddition(addition);
	}

	public void setCells(List<Cell> cells)
	{
		this.cells = cells;
	}

	private void parseAddition(String addition)
	{
		if (StringUtils.isNotBlank(addition) && (addition.contains("/") || addition.contains("\\")))
		{
			File file = new File(addition);
			this.addition = file.getName();
		}
		else
			this.addition = addition;
	}

	private void afterUnmarshal(Unmarshaller unmarshaller, Object parent)
	{
		parseAddition(addition);
	}

}
