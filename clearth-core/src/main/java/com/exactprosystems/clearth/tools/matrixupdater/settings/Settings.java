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

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Settings")
public class Settings
{
	@XmlElement(name = "Condition", required = true)
	private List<Condition> conditions;

	@XmlElement(name = "Change", required = true)
	private Change change;

	public Settings()
	{
		conditions = new ArrayList<>();
		change = new Change();
	}

	public Settings(List<Condition> conditions, Change change)
	{
		this.conditions = conditions;
		this.change = change;
	}

	public void addCondition(String name)
	{
		conditions.add(new Condition(name));
	}

	public void removeCondition(Condition condition)
	{
		conditions.remove(condition);
	}

	/** GETTER */

	public List<Condition> getConditions()
	{
		return conditions;
	}

	public Change getChange()
	{
		return change;
	}

	/** SETTER */

	public void setConditions(List<Condition> conditions)
	{
		this.conditions = conditions;
	}

	public void setChange(Change change)
	{
		this.change = change;
	}


	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Settings settings = (Settings) o;
		return Objects.equals(conditions, settings.conditions) &&
			Objects.equals(change, settings.change);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(conditions, change);
	}
}
