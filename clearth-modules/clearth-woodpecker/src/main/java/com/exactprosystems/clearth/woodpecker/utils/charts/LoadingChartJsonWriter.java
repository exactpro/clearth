/******************************************************************************
 * Copyright 2009-2022 Exactpro Systems Limited
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

import java.util.Collection;

public class LoadingChartJsonWriter
{
	public String createJson(Collection<LoadingChart> loadingCharts, boolean cropCharts)
	{
		StringBuilder builder = new StringBuilder("[");
		for (LoadingChart chart : loadingCharts)
			builder.append(toJson(cropCharts ? getCroppedChart(chart) : chart)).append(",");
		builder.setLength(builder.length() - 1);
		return builder.append("]").toString();
	}
	
	
	private LoadingChart getCroppedChart(LoadingChart chart)
	{
		LoadingChart croppedChart;
		if (!chart.isUpdated())
			croppedChart = chart.getChartCroppedByDelta(chart.getPreviousDelta(), chart.getCurrentDelta(), true);
		else
			croppedChart = chart.getChartCroppedByDelta(0, 0, true);
		
		chart.setUpdated(chart.isCompleted());
		return croppedChart;
	}
	
	private String toJson(LoadingChart chart)
	{
		StringBuilder builder = new StringBuilder("{");
		builder.append("name:\"").append(chart.getDaemonNameWOSpaces()).append("\",");
		
		builder.append("datasets:{");
		for (Dataset dataset : chart.getDatasets())
		{
			builder.append(dataset.getName()).append(":{");
			builder.append("displayName:\"").append(dataset.getDisplayName()).append("\",");
			builder.append("points:").append(dataset.getPoints()).append("},");
		}
		builder.setLength(builder.length() - 1);
		builder.append("},");
		
		builder.append("labels:[");
		for (String label : chart.getLabels())
			builder.append("\"").append(label).append("\",");
		builder.setLength(builder.length() - 1);
		builder.append("]");
		
		return builder.append("}").toString();
	}
}
