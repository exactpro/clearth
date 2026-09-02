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
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.exactprosystems.clearth.woodpecker.configuration.daemonsDictionary.DaemonDesc;
import com.exactprosystems.clearth.woodpecker.configuration.settings.DaemonSettings;
import com.exactprosystems.clearth.woodpecker.configuration.settings.loading.LoadingSchedule;
import com.exactprosystems.clearth.woodpecker.daemons.WoodpeckerDaemon;
import com.exactprosystems.clearth.woodpecker.statistics.LoadingStatistics;

import static com.exactprosystems.clearth.woodpecker.configuration.settings.ExecutionMode.Scheduled;
import static com.exactprosystems.clearth.woodpecker.utils.charts.DatasetType.STATIC;
import static java.util.concurrent.TimeUnit.SECONDS;

public abstract class BaseLoadingChartBuilder implements LoadingChartBuilder
{
	private static final String SCHEDULE = "LoadingSchedule";
	
	protected abstract List<Dataset> createDatasets(WoodpeckerDaemon daemon);
	
	protected abstract String createScheduleLineName(DaemonDesc daemonDesc);
	
	protected abstract Map<String, List<Integer>> getDistributionsByLineName(WoodpeckerDaemon daemon,
	                                                                         TimeUnit periodUnit);


	@Override
	public final LoadingChart createChart(WoodpeckerDaemon daemon)
	{
		LoadingStatistics statistics = daemon.getLoadingStatistics();
		DaemonSettings settings = daemon.getSettings();
		
		LoadingChart chart = new LoadingChart(settings.getFullName(), statistics.getStartTimestamp(), SECONDS);

		List<Dataset> datasets = createDatasets(daemon);
		for (Dataset dataset : datasets)
		{
			chart.addDataset(dataset);
		}

		if ((settings.getExecutionMode() == Scheduled) && (settings.getRateUnit() == SECONDS))
		{
			List<Point> schedule = createLoadingScheduleDataSet(settings.getSchedule());

			String scheduleDisplayName = createScheduleLineName(daemon.getDescription());
			chart.addDatasetByPoints(SCHEDULE, scheduleDisplayName, schedule, STATIC);
		}
		
		return chart;
	}

	@Override
	public final void updateChart(WoodpeckerDaemon daemon, LoadingChart chart)
	{
		LoadingStatistics statistics = daemon.getLoadingStatistics();

		Map<String, List<Integer>> distributionsByName = getDistributionsByLineName(daemon, chart.getPeriodUnit());

		updateDataSets(chart, distributionsByName);

		if (statistics.isCompleted())
			chart.setCompleted(true);
	}


	private void updateDataSets(LoadingChart chart, Map<String, List<Integer>> distributionsByName)
	{
		int previousDelta = chart.getCurrentDelta();

		int currentDelta = distributionsByName.values().stream()
				.mapToInt(List::size)
				.min()
				.orElse(previousDelta);

		if (currentDelta == previousDelta)
			return;

		for (Map.Entry<String, List<Integer>> e : distributionsByName.entrySet())
		{
			String dataSetName = e.getKey();
			List<Integer> distribution = e.getValue();

			chart.addAllValuesToDataset(dataSetName, distribution.subList(previousDelta, currentDelta));
		}

		chart.setPreviousDelta(previousDelta);
		chart.setCurrentDelta(currentDelta);
	}
	
	private List<Point> createLoadingScheduleDataSet(LoadingSchedule loadingSchedule)
	{
		List<Point> schedule = new ArrayList<>();
		int x = 0;
		for (LoadingSchedule.Period period : loadingSchedule)
		{
			int d = period.getDuration();
			double y = period.getRate();
			int x1 = x + 1;
			int x2 = x + d;
			schedule.add(new Point(x1, y));
			if (x2 != x1)
				schedule.add(new Point(x2, y));
			x += d;
		}
		return schedule;
	}
}
