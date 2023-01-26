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

package com.exactprosystems.clearth.config;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;

import javax.xml.bind.annotation.*;

@XmlType(name = "locations")
public class LocationConfig
{
	protected volatile List<ReplacedPath> location;
	protected Map<String, String> mappedLocations;

	public LocationConfig() {}

	public void setLocation(List<ReplacedPath> location)
	{
		this.location = location;
		updateLocationsMapping();
	}

	public List<ReplacedPath> getLocation()
	{
		if (location == null)
			location = new ArrayList<ReplacedPath>();
		return this.location;
	}

	public Map<String, String> getLocationsMapping()
	{
		if (location == null)
			return null;
		return mappedLocations;
	}

	protected void updateLocationsMapping()
	{
		mappedLocations = new HashMap<String, String>();
		for (ReplacedPath currentLocation : location)
		{
			mappedLocations.put(currentLocation.getOriginalPath(), currentLocation.getNewPath());
		}
	}

	@Override
	public String toString()
	{
		return "Replaced locations: " + this.getLocation().stream().map(x -> x.toString()).collect(Collectors.joining("\n"));
	}
}
