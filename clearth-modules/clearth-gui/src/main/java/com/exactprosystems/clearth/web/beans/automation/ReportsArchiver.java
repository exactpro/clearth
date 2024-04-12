/******************************************************************************
 * Copyright 2009-2024 Exactpro Systems Limited
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

package com.exactprosystems.clearth.web.beans.automation;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.Matrix;
import com.exactprosystems.clearth.automation.ReportsInfo;
import com.exactprosystems.clearth.automation.report.ActionReportWriter;
import com.exactprosystems.clearth.utils.FileOperationUtils;
import com.exactprosystems.clearth.xmldata.XmlMatrixInfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ReportsArchiver {

	private static final char[] ILLEGAL_CHARACTERS = { '/', '\n', '\r', '\t', '\0', '\f', '`', '?', '*', '\\', '<', '>', '|', '\"', ':' };
	private static final Pattern HTML_TITLE_PATTERN = Pattern.compile("<title>(.*)</title>"),
			JSON_NAME_PATTERN = Pattern.compile("\"matrixName\" : \"(.*)\","),
			JSON_START_PATTERN = Pattern.compile("\"executionStart\" : (.*),"),
			JSON_RESULT_PATTERN = Pattern.compile("\"result\" : (.*),");
	
	private final String reportsPath,
			tempPath;
	private final Function<String, String> pathResolver;
	
	protected List<Matrix> filteredRTMatrices;
	protected List<XmlMatrixInfo> filteredReportsInfo;
	
	public ReportsArchiver()
	{
		ClearThCore app = ClearThCore.getInstance();
		reportsPath = app.getReportsPath();
		tempPath = app.getTempDirPath();
		pathResolver = p -> app.getAppRootRelative(p);
	}
	
	public ReportsArchiver(String reportsPath, String tempPath, Function<String, String> pathResolver)
	{
		this.reportsPath = reportsPath;
		this.tempPath = tempPath;
		this.pathResolver = pathResolver;
	}
	
	
	public ReportsArchiver setFilteredData(List<Matrix> filteredRTMatrices, List<XmlMatrixInfo> filteredReportsInfo) {
		this.filteredRTMatrices = filteredRTMatrices;
		this.filteredReportsInfo = filteredReportsInfo;
		return this;
	}

	protected void getFilesForZipReports(List<File> files, List<String> names, Set<String> filteredReports, ReportsInfo reportInfoPath) throws IOException {

		// for paused scheduler
		File dir = new File(pathResolver.apply(reportInfoPath.getPath()));
		if (!dir.exists())
		{
			//for completed scheduler
			dir = new File(pathResolver.apply(reportsPath + reportInfoPath.getPath()));
			if(!dir.exists())
				throw new FileNotFoundException("These reports do not exist");
		}
		
		File[] reports = getReportsDirectories(dir);
		if (reports == null || reports.length==0)
			return;
		
		//1. Take the first directory and take all files except for HTML and JSON report. These are just media stuff identical for all reports.
		File[] aux = reports[0].listFiles((dir1, name) -> !name.toLowerCase().endsWith(ActionReportWriter.HTML_SUFFIX) 
				&& !name.toLowerCase().endsWith(ActionReportWriter.JSON_SUFFIX));
		if (aux == null)
			return;
		
		//They will be in the beginning of our ZIP file
		files.addAll(Arrays.asList(aux));
		names.addAll(Arrays.asList(new String[aux.length]));
		
		//2. For each directory, we need to open its HTML report and extract contents of <title> tag
		//or open JSON report and extract contents of "matrixName" + "executionStart" + "result" and build title from them
		for (File repDir : reports)
		{
			if (!checkFilter(repDir, filteredReports))
				continue;
			
			String title = extractTitle(repDir);
			title = sanitizeTitle(title, repDir);
			
			addFile(new File(repDir, ActionReportWriter.HTML_REPORT_NAME), 
					title+ActionReportWriter.HTML_SUFFIX,
					files, names);
			addFile(new File(repDir, ActionReportWriter.JSON_REPORT_NAME), 
					title+ActionReportWriter.JSON_SUFFIX,
					files, names);
		}
	}
	
	private File[] getReportsDirectories(File dir)
	{
		return dir.listFiles(file -> {
			if (!file.isDirectory())
				return false;
			
			return new File(file, ActionReportWriter.HTML_REPORT_NAME).isFile()
					|| new File(file, ActionReportWriter.JSON_REPORT_NAME).isFile();
		});
	}
	
	private boolean checkFilter(File reportDir, Set<String> filteredReports)
	{
		if (filteredReports != null)
			return filteredReports.contains(reportDir.getName());
		return true;
	}
	
	private String extractTitle(File reportDir) throws IOException
	{
		File report = new File(reportDir, ActionReportWriter.HTML_REPORT_NAME);
		if (report.isFile())
			return extractTitleFromHtml(report);
		
		report = new File(reportDir, ActionReportWriter.JSON_REPORT_NAME);
		if (report.isFile())
			return extractTitleFromJson(report);
		
		return null;
	}
	
	private String extractTitleFromHtml(File report) throws IOException
	{
		String title = null,
				line;
		try (BufferedReader reader = new BufferedReader(new FileReader(report)))
		{
			int i = 0;
			while (title == null && (line = reader.readLine()) != null && i < 50)
			{
				Matcher m = HTML_TITLE_PATTERN.matcher(line);
				if (m.find())
					title = m.group(1);
				i++;
			}
		}
		return title;
	}
	
	private String extractTitleFromJson(File report) throws IOException
	{
		String name = null;
		LocalDateTime start = null;
		Boolean result = null;
		String line;
		try (BufferedReader reader = new BufferedReader(new FileReader(report)))
		{
			int i = 0;
			while ((name == null || start == null || result == null)
					&& (line = reader.readLine()) != null && i < 10)
			{
				i++;
				
				Matcher m = JSON_NAME_PATTERN.matcher(line);
				if (m.find())
				{
					name = m.group(1);
					continue;
				}
				
				m = JSON_START_PATTERN.matcher(line);
				if (m.find())
				{
					long millis = Long.parseLong(m.group(1));
					start = Instant.ofEpochMilli(millis)
							.atZone(ZoneId.systemDefault())
							.toLocalDateTime();
					continue;
				}
				
				m = JSON_RESULT_PATTERN.matcher(line);
				if (m.find())
					result = m.group(1).equalsIgnoreCase("true");
			}
		}
		
		StringBuilder sb = new StringBuilder(name).append(" ");
		if (start != null)
			sb.append(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss").format(start)).append("  ");
		sb.append(result != null && result ? "(PASSED)" : "(FAILED)");
		return sb.toString();
	}
	
	private String sanitizeTitle(String title, File reportDir) throws IOException
	{
		if (title == null)
			throw new IOException("Unable to extract title for "+reportDir.getName());
		
		for (char ic : ILLEGAL_CHARACTERS)
			title = title.replace(ic, '.');
		return title;
	}
	
	private void addFile(File file, String nameToUse, List<File> allFiles, List<String> allNames)
	{
		if (file.isFile())
		{
			allFiles.add(file);
			allNames.add(nameToUse);
		}
	}
	
	private Set<String> getFilteredReports(boolean realtimeSnapshot)
	{
		if (realtimeSnapshot && filteredRTMatrices != null)
			return filteredRTMatrices.stream().map(Matrix::getShortFileName).collect(Collectors.toSet());
		if (!realtimeSnapshot && filteredReportsInfo != null)
			return filteredReportsInfo.stream().map(XmlMatrixInfo::getFileName).collect(Collectors.toSet());
		return null;
	}

	protected void zipFiles(File result, List<File> filesToZip, List<String> names) throws IOException
	{
		//Utils.zipDirectories(result, filesToZip);
		FileOperationUtils.zipFiles(result, filesToZip, names);
	}

	public File getZipSelectedReports(boolean realtimeSnapshot, ReportsInfo reportsInfo, String fileName) throws IOException
	{
		return getZipReports(realtimeSnapshot, reportsInfo, Collections.emptyList(), Collections.emptyList(), fileName);
	}

	public File getZipSelectedReportsWithLogs(File shortLog, ReportsInfo reportsInfo, String fileName) throws IOException
	{
		return getZipReports(false, reportsInfo, Collections.singletonList(shortLog), Collections.singletonList(null), fileName);
	}
	
	
	private File getZipReports(boolean realtimeSnapshot, ReportsInfo reportsInfo, List<File> filesToAdd, List<String> filesToAddNames, String fileName) throws IOException
	{
		List<File> filesToZip = new ArrayList<>(filesToAdd);
		List<String> names = new ArrayList<>(filesToAddNames);
		
		getFilesForZipReports(filesToZip, names, getFilteredReports(realtimeSnapshot), reportsInfo);
		if (filesToZip.size() == 0)
			return null;
		
		File result = new File(tempPath, fileName);
		Files.createDirectories(result.getParentFile().toPath());
		zipFiles(result, filesToZip, names);
		return result;
	}
}
