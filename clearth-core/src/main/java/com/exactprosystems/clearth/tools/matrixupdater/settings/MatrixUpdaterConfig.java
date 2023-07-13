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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "MatrixUpdaterConfig")
public class MatrixUpdaterConfig
{
	@XmlElement(name = "Update")
	private List<Update> updates;

	public MatrixUpdaterConfig()
	{
		updates = new ArrayList<>();
	}

	public Update addUpdate(String name, UpdateType type)
	{
		Update update = new Update(name, type);
		updates.add(update);
		return update;
	}

	public void removeUpdate(Update update)
	{
		updates.remove(update);
	}

	public List<Update> getUpdates()
	{
		return updates;
	}

	public Update getUpdate(String name)
	{
		for (Update update : updates)
		{
			if (update.getName().equals(name))
				return update;
		}

		return null;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		MatrixUpdaterConfig that = (MatrixUpdaterConfig) o;
		return updates.equals(that.updates);
	}

	@Override
	public int hashCode()
	{
		return updates.hashCode();
	}
}
