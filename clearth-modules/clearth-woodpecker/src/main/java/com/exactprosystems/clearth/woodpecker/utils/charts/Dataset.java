/******************************************************************************
 * Copyright 2009-2020 Exactpro Systems Limited
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

package com.exactprosystems.clearth.woodpecker.utils.charts;

import java.util.ArrayList;
import java.util.List;

public class Dataset
{
	private final String name;
	private final String displayName;
	private final DatasetType datasetType;
	private final List<Point> points;

	
	public Dataset(String name, String displayName, DatasetType type, List<Point> points)
	{
		this.displayName = displayName;
		this.name = name;
		this.points = points;
		this.datasetType = type;
	}

	public Dataset(String name, String displayName, DatasetType type)
	{
		this(name, displayName, type, new ArrayList<>());
	}
	

	public String getName()
	{
		return name;
	}
	
	public String getDisplayName() 
	{
		return displayName; 
	}

	public DatasetType getDatasetType()
	{
		return datasetType;
	}
	
	
	public List<Point> getPoints()
	{
		return new ArrayList<>(points);
	}

	public void addPoint(long x, double y)
	{
		points.add(new Point(x, y));
	}

	public int size()
	{
		return points.size();
	}
}
