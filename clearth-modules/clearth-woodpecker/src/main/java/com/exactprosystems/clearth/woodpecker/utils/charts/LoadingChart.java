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

package com.exactprosystems.clearth.woodpecker.utils.charts;

import static com.exactprosystems.clearth.woodpecker.utils.charts.DatasetType.*;
import static com.exactprosystems.clearth.woodpecker.utils.charts.TimeLabelFormat.*;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class LoadingChart
{
	private final String name;

	private final long startTime;
	private final TimeUnit periodUnit;
	
	private int currentDelta;
	private int previousDelta;
	
	private TimeLabelFormat labelFormat = RELATIVE;
	
	private List<Dataset> datasets = new ArrayList<>();
	private List<String> labels = new ArrayList<>();
	
	private boolean completed;
	private boolean updated;
	

	public LoadingChart(String name, long startTime, TimeUnit periodUnit)
	{
		this.name = name;
		this.startTime = startTime;
		this.periodUnit = periodUnit;
	}


	public Dataset getDataset(String datasetName)
	{
		for (Dataset d : datasets)
		{
			if (d.getName().equals(datasetName))
				return d;
		}
		return null;
	}

	public boolean isCompleted()
	{
		return completed;
	}

	public void setCompleted(boolean completed)
	{
		this.completed = completed;
	}

	public boolean isUpdated()
	{
		return updated;
	}

	public void setUpdated(boolean updated)
	{
		this.updated = updated;
	}

	public long getStartTime()
	{
		return startTime;
	}

	public List<Dataset> getDatasets()
	{
		return datasets;
	}

	public List<String> getLabels()
	{
		return labels;
	}

	public void addDatasetByPoints(String name, String displayName, List<Point> data, DatasetType type)
	{
		datasets.add(new Dataset(name, displayName, type, data));

		this.labels = autoGenerateLabels();
	}
	
	public void addDataset(Dataset dataset)
	{
		datasets.add(dataset);
	}

	public List<String> generateAbsoluteLabels(List<Dataset> datasets)
	{
		List<Long> deltaLabels = generateDeltaLabels(datasets);
		List<String> res = new ArrayList<>();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

		long periodMs = periodUnit.toMillis(1);
		for (int i = 0; i < deltaLabels.size(); i++)
		{
			LocalTime lt = Instant.ofEpochMilli(startTime + periodMs * i)
					.atZone(ZoneId.systemDefault())
					.toLocalTime();
			res.add(lt.format(formatter));
		}
		return res;
	}
	
	public List<String> generateRelativeLabels(List<Dataset> datasets)
	{
		List<Long> deltaLabels = generateDeltaLabels(datasets);
		List<String> res = new ArrayList<>();

		long periodMs = periodUnit.toMillis(1);
		for (Long deltaLabel : deltaLabels)
		{
			res.add(formatTime(deltaLabel * periodMs));
		}
		return res;
	}

	public List<Long> generateDeltaLabels(List<Dataset> datasets)
	{
		List<Long> res = new ArrayList<>();

		long max = 0;
		for (Dataset d : datasets)
		{
			if (d.getDatasetType() == DYNAMIC)
				max = Math.max(max, d.size());
		}

		for (long i = 0; i <= max; i++)
		{
			res.add(i);
		}
		return res;
	}

	public LoadingChart getChartCroppedByDelta(int startIndex, int endIndex, boolean dynamicOnly)
	{
		LoadingChart res = new LoadingChart(name, 0, periodUnit);
		for (Dataset d : datasets)
		{
			if (d.getDatasetType() == DYNAMIC)
			{
				res.addDatasetByPoints(d.getName(), d.getDisplayName(),
						getDatasetCroppedByDelta(d, startIndex, endIndex).getPoints(), d.getDatasetType());
			}
			else if (!dynamicOnly && d.getDatasetType() == STATIC)
			{
				res.addDatasetByPoints(d.getName(), d.getDisplayName(), d.getPoints(), d.getDatasetType());
			}
		}
		res.setLabelFormat(this.labelFormat);
		res.labels = autoGenerateLabels();
		return res;
	}

	private Dataset getDatasetCroppedByDelta(Dataset d, int startIndex, int endIndex)
	{
		if (startIndex > endIndex)
			throw new IllegalArgumentException("Start index should be less than end index.");

		return new Dataset(d.getName(), d.getDisplayName(), d.getDatasetType(),
				d.getPoints().subList(startIndex, endIndex));
	}

	private String formatTime(Long time)
	{
		long second = (time / 1000) % 60;
		long minute = (time / (1000 * 60)) % 60;
		long hour = (time / (1000 * 60 * 60)) % 24;

		return String.format("%02d:%02d:%02d", hour, minute, second);
	}

	public void addAllValuesToDataset(String datasetName, List<Integer> values)
	{
		Dataset d = getDataset(datasetName);
		if (d == null)
			return;

		for (Integer value : values)
		{
			if (!d.getPoints().isEmpty())
			{
				Point lastPoint = d.getPoints().get(d.getPoints().size() - 1);
				d.addPoint(lastPoint.getX() + 1, value);
			}
			else
				d.addPoint(1, value);
		}

		this.labels = autoGenerateLabels();
	}
	
	private List<String> autoGenerateLabels()
	{
		switch (labelFormat)
		{
			case RELATIVE:
				return generateRelativeLabels(datasets);
			case ABSOLUTE:
				return generateAbsoluteLabels(datasets);
			default:
				return null;
		}
	}

	@SuppressWarnings("unused")
	public TimeLabelFormat getLabelFormat()
	{
		return labelFormat;
	}

	public void setLabelFormat(TimeLabelFormat labelFormat)
	{
		this.labelFormat = labelFormat;

		this.labels = autoGenerateLabels();
	}

	public int getCurrentDelta()
	{
		return currentDelta;
	}

	public int getPreviousDelta()
	{
		return previousDelta;
	}

	public void setCurrentDelta(int currentDelta)
	{
		this.currentDelta = currentDelta;
	}

	public void setPreviousDelta(int previousDelta)
	{
		this.previousDelta = previousDelta;
	}

	public String getDaemonName()
	{
		return name;
	}

	public String getDaemonNameWOSpaces()
	{
		return name.replaceAll("\\s+","");
	}

	public TimeUnit getPeriodUnit()
	{
		return periodUnit;
	}
}
