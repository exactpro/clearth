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

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.woodpecker.configuration.settings.DaemonSettings;
import com.exactprosystems.clearth.woodpecker.configuration.settings.ExecutionMode;
import com.exactprosystems.clearth.woodpecker.daemons.WoodpeckerDaemon;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerDaemonException;
import com.exactprosystems.clearth.woodpecker.statistics.LoadingStatistics;

import static com.exactprosystems.clearth.woodpecker.configuration.settings.ExecutionMode.Simple;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.newBufferedWriter;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.time.format.DateTimeFormatter.ofPattern;
import static org.apache.commons.lang3.StringUtils.deleteWhitespace;

public abstract class BaseDaemonExecutionReportWriter  implements DaemonExecutionReportWriter
{
	private static final Logger log = LoggerFactory.getLogger(BaseDaemonExecutionReportWriter.class);

	private static final DateTimeFormatter FILE_NAME_TIMESTAMP_FORMATTER = ofPattern("ddMMyy_HHmmss_SSS");
	private static final DateTimeFormatter REPORT_TIMESTAMP_FORMATTER = ofPattern("dd/MM/yyyy HH:mm:ss");
	
	
	@SuppressWarnings({"unused", "RedundantThrows"})
	protected void writeAdditionalSettings(CSVPrinter csvPrinter, DaemonSettings settings) throws IOException
	{ /* Nothing to do by default */ }
	
	protected abstract void writeStatistics(CSVPrinter csvPrinter, LoadingStatistics statistics) throws IOException;


	@Override
	public void writeReport(WoodpeckerDaemon daemon, Path targetDir) throws WoodpeckerDaemonException
	{
		DaemonSettings settings = daemon.getSettings();
		if (settings == null)
		{
			log.warn("Unable to write daemon report. Settings is null.");
			return;
		}
		LoadingStatistics statistics = daemon.getLoadingStatistics();
		if (statistics == null)
		{
			log.warn("Unable to write daemon report. Settings is null.");
			return;
		}

		Path reportPath = targetDir.resolve(createFileName(daemon));
		try (BufferedWriter writer = newBufferedWriter(reportPath, UTF_8, CREATE, TRUNCATE_EXISTING);
		     CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT))
		{
			writeRow(csvPrinter, "Start time", REPORT_TIMESTAMP_FORMATTER.format(daemon.getStartTime()));
			writeRow(csvPrinter, "End time", REPORT_TIMESTAMP_FORMATTER.format(daemon.getEndTime()));

			writeSettings(csvPrinter, settings);
			writeStatistics(csvPrinter, statistics);
		}
		catch (IOException e)
		{
			throw new WoodpeckerDaemonException("Unable to write execution report.", e);
		}
	}


	private String createFileName(WoodpeckerDaemon daemon)
	{
		String time = FILE_NAME_TIMESTAMP_FORMATTER.format(daemon.getStartTime());
		String daemonName = deleteWhitespace(daemon.getSettings().getDaemonName());
		return format("%s_%s.CSV", daemonName, time);
	}


	private void writeSettings(CSVPrinter csvPrinter, DaemonSettings settings) throws IOException
	{
		writeCommonSettings(csvPrinter, settings);
		writeAdditionalSettings(csvPrinter, settings);
	}	
	
	private void writeCommonSettings(CSVPrinter csvPrinter, DaemonSettings settings) throws IOException
	{
		ExecutionMode executionMode = settings.getExecutionMode();
		writeRow(csvPrinter, "Execution mode", executionMode);

		writeRow(csvPrinter, ((executionMode == Simple) ? "Rate unit" : "Rate/duration unit"), settings.getRateUnit());

		if (executionMode == Simple)
			writeRow(csvPrinter, "Rate", settings.getRate());
		else
		{
			writeRow(csvPrinter, "Schedule", settings.getSchedule());
			writeRow(csvPrinter, "Priority mode", settings.getPriorityMode());
		}
	}
	
	
	protected void writeRow(CSVPrinter csvPrinter, String paramName, Object value) throws IOException
	{
		csvPrinter.print(paramName);
		csvPrinter.print(value);
		csvPrinter.println();
	}
}
