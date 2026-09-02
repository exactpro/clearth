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

package com.exactprosystems.clearth.woodpecker.daemons.message;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.*;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.exactprosystems.clearth.woodpecker.daemons.WoodpeckerDaemon;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerDaemonException;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerException;
import com.exactprosystems.clearth.woodpecker.statistics.LoadingStatistics;

import static com.exactprosystems.clearth.utils.FileOperationUtils.resourceToAbsoluteFilePath;
import static com.exactprosystems.clearth.woodpecker.configuration.settings.ExecutionMode.Scheduled;
import static com.exactprosystems.clearth.woodpecker.configuration.settings.ExecutionMode.Simple;
import static com.exactprosystems.clearth.woodpecker.configuration.settings.loading.LoadingSchedule.parse;
import static com.exactprosystems.clearth.woodpecker.configuration.settings.loading.PriorityMode.Loading;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.exists;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.io.FileUtils.cleanDirectory;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class MessageDaemonExecutionReportWriterTest
{
	private static final String REPORTS_DIR = "DefaultDaemonExecutionReportWriter";
	private static final Path OUTPUT_DIR = Paths.get("testOutput", REPORTS_DIR);
	
	
	@BeforeClass
	public void init() throws IOException
	{
		createDirectories(OUTPUT_DIR);
	}
	
	@AfterClass
	public void clear() throws IOException
	{
		cleanDirectory(OUTPUT_DIR.toFile());
	}
	
	
	@Test
	public void checkReportForSimpleExecution() throws WoodpeckerDaemonException, IOException
	{
		WoodpeckerDaemon daemon = mock(WoodpeckerDaemon.class);

		LocalDateTime startTime = LocalDateTime.of(2020, Month.AUGUST, 17, 12, 34, 56, 789_000_000);
		when(daemon.getStartTime()).thenReturn(startTime);
		
		LocalDateTime endTime = LocalDateTime.of(2020, Month.AUGUST, 17, 12, 39, 50, 789_000_000);
		when(daemon.getEndTime()).thenReturn(endTime);

		MessageDaemonSettings settings = new MessageDaemonSettings();
		settings.setDaemonName("Simple Execution");
		settings.setExecutionMode(Simple);
		settings.setRateUnit(SECONDS);
		settings.setRate(600);
		settings.setMinBatchSize(5);
		settings.setMaxBatchSize(10);
		settings.setUseStrictMinBatchSize(true);
		when(daemon.getSettings()).thenReturn(settings);

		Map<String, Long> sentOpsByOpName = new LinkedHashMap<>();
		sentOpsByOpName.put("SendA", 8009L);
		sentOpsByOpName.put("SendB", 12901L);
		sentOpsByOpName.put("SendC", 34046L);

		Map<String, Long> sentMsgsByOpName = new LinkedHashMap<>();
		sentMsgsByOpName.put("SendA", 8009L);
		sentMsgsByOpName.put("SendB", 24182L);
		sentMsgsByOpName.put("SendC", 34046L);
		
		LoadingStatistics statistics = mockStatistics(54956, 66237, 
				sentOpsByOpName, sentMsgsByOpName);
		when(daemon.getLoadingStatistics()).thenReturn(statistics);
		
		MessageDaemonExecutionReportWriter reportWriter = new MessageDaemonExecutionReportWriter();
		reportWriter.writeReport(daemon, OUTPUT_DIR);
		
		checkReport("SimpleExecution_170820_123456_789.CSV");
	}
	
	@Test
	public void checkReportForScheduledExecution() throws WoodpeckerException, IOException
	{
		WoodpeckerDaemon daemon = mock(WoodpeckerDaemon.class);

		LocalDateTime startTime = LocalDateTime.of(2020, Month.AUGUST, 17, 12, 40, 9, 888_000_000);
		when(daemon.getStartTime()).thenReturn(startTime);

		LocalDateTime endTime = LocalDateTime.of(2020, Month.AUGUST, 17, 12, 45, 50, 888_000_000);
		when(daemon.getEndTime()).thenReturn(endTime);

		MessageDaemonSettings settings = new MessageDaemonSettings();
		settings.setDaemonName("Scheduled Execution");
		settings.setExecutionMode(Scheduled);
		settings.setRateUnit(MINUTES);
		settings.setSchedule(parse("600/1 900/1 1200/1 1500/1 1800/1"));
		settings.setPriorityMode(Loading);
		settings.setMinBatchSize(1);
		settings.setMaxBatchSize(1);
		settings.setUseStrictMinBatchSize(false);
		when(daemon.getSettings()).thenReturn(settings);

		Map<String, Long> sentOpsByOpName = new LinkedHashMap<>();
		sentOpsByOpName.put("SendA", 1000L);
		sentOpsByOpName.put("SendB", 3000L);
		sentOpsByOpName.put("SendC", 2000L);

		Map<String, Long> sentMsgsByOpName = new LinkedHashMap<>();
		sentMsgsByOpName.put("SendA", 1000L);
		sentMsgsByOpName.put("SendB", 9000L);
		sentMsgsByOpName.put("SendC", 4000L);

		LoadingStatistics statistics = mockStatistics(6000, 14000,
				sentOpsByOpName, sentMsgsByOpName);
		when(daemon.getLoadingStatistics()).thenReturn(statistics);

		MessageDaemonExecutionReportWriter reportWriter = new MessageDaemonExecutionReportWriter();
		reportWriter.writeReport(daemon, OUTPUT_DIR);

		checkReport("ScheduledExecution_170820_124009_888.CSV");
	}

	
	@SuppressWarnings("SuspiciousMethodCalls")
	private LoadingStatistics mockStatistics(long totalSentOps, long totalSentMsgs,
	                                         Map<String, Long> sentOpsByOpName, Map<String, Long> sentMsgsByOpName)
	{
		MessageLoadingStatistics statistics = mock(MessageLoadingStatistics.class);
		
		when(statistics.getTotalSentOperations()).thenReturn(totalSentOps);
		when(statistics.getTotalSentMessages()).thenReturn(totalSentMsgs);

		List<String> operationNames = new ArrayList<>(sentMsgsByOpName.keySet());
		when(statistics.getOperationNames()).thenReturn(operationNames);
		
		when(statistics.getTotalSentOperationsByName(anyString()))
				.thenAnswer(i -> sentOpsByOpName.get(i.getArguments()[0]));
		when(statistics.getTotalSentMessagesByOperationName(anyString()))
				.thenAnswer(i -> sentMsgsByOpName.get(i.getArguments()[0]));
		
		return statistics;
	}
	
	private void checkReport(String fileName) throws IOException
	{
		Path expectedReportPath = Paths.get(resourceToAbsoluteFilePath(REPORTS_DIR), fileName);
		String expectedReportText = loadReport(expectedReportPath);
		
		Path actualReportPath = OUTPUT_DIR.resolve(fileName);
		assertTrue(exists(actualReportPath), format("File '%s' not found.", actualReportPath));
		String actualReportText = loadReport(actualReportPath);
		
		assertEquals(actualReportText, expectedReportText);
	}
	
	private String loadReport(Path path) throws IOException
	{
		String reportText = readFileToString(path.toFile(), UTF_8);
		reportText = reportText.trim();
		return reportText.replace("\r", "");
	}
}