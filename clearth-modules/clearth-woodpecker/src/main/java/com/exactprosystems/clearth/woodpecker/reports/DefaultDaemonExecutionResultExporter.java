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

package com.exactprosystems.clearth.woodpecker.reports;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.woodpecker.configuration.settings.DaemonSettings;
import com.exactprosystems.clearth.woodpecker.daemons.WoodpeckerDaemon;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerDaemonException;
import com.exactprosystems.clearth.woodpecker.statistics.LoadingStatistics;
import com.exactprosystems.clearth.woodpecker.utils.charts.ChartExporter;
import com.exactprosystems.clearth.woodpecker.utils.charts.LoadingChart;
import com.exactprosystems.clearth.woodpecker.utils.charts.LoadingChartBuilder;

import static com.exactprosystems.clearth.ClearThCore.rootRelative;
import static com.exactprosystems.clearth.woodpecker.configuration.WoodpeckerPaths.STATISTICS_DIR;
import static java.lang.String.format;
import static java.nio.file.Files.createDirectories;
import static java.time.format.DateTimeFormatter.ofPattern;
import static org.apache.commons.lang3.StringUtils.deleteWhitespace;

public class DefaultDaemonExecutionResultExporter implements DaemonExecutionResultExporter
{
	private static final Logger log = LoggerFactory.getLogger(DefaultDaemonExecutionResultExporter.class);

	private static final DateTimeFormatter FILE_NAME_TIMESTAMP_FORMATTER = ofPattern("ddMMyy_HHmmss_SSS");
	
	
	@Override
	public void export(WoodpeckerDaemon daemon) throws WoodpeckerDaemonException
	{
		Path targetDir = prepareDirectory(daemon);
		
		DaemonExecutionReportWriter executionReportWriter = daemon.getFactory().createDaemonExecutionReportWriter();
		executionReportWriter.writeReport(daemon, targetDir);
		
		exportChart(daemon, targetDir);
		
		log.info("Execution result of daemon '{}' has been successfully saved to '{}'", 
				daemon.getSettings().getFullName(), targetDir);
	}
	
	
	private Path prepareDirectory(WoodpeckerDaemon daemon) throws WoodpeckerDaemonException
	{
		Path path = Paths.get(rootRelative(STATISTICS_DIR), deleteWhitespace(daemon.getSettings().getDaemonType()));
		try
		{
			createDirectories(path);
		}
		catch (IOException e)
		{
			throw new WoodpeckerDaemonException(e, "Unable to create directory '%s' to export execution result.", path);
		}
		return path;
	}
	
	private void exportChart(WoodpeckerDaemon daemon, Path targetDir) throws WoodpeckerDaemonException
	{
		DaemonSettings settings = daemon.getSettings();
		LoadingStatistics statistics = daemon.getLoadingStatistics();
		if (statistics == null)
		{
			log.warn("Unable to generate loading chart for daemon '{}': statistics is null.", settings.getFullName());
			return;
		}

		LoadingChartBuilder chartBuilder = daemon.getFactory().createLoadingChartBuilder();
		LoadingChart chart = chartBuilder.createChart(daemon);
		chartBuilder.updateChart(daemon, chart);

		String timestamp = FILE_NAME_TIMESTAMP_FORMATTER.format(daemon.getStartTime());
		String fileName = format("%s_chart_%s.html", deleteWhitespace(settings.getDaemonName()), timestamp);
		
		ChartExporter exporter = new ChartExporter();
		exporter.exportChartToFile(chart, targetDir.resolve(fileName).toFile());
	}
}
