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
import com.exactprosystems.clearth.utils.FileOperationUtils;
import com.exactprosystems.clearth.web.misc.UserInfoUtils;
import com.exactprosystems.clearth.xmldata.XmlMatrixInfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.exactprosystems.clearth.automation.report.ActionReportWriter.*;

public class ReportsArchiver {

	private static final char[] ILLEGAL_CHARACTERS = { '/', '\n', '\r', '\t', '\0', '\f', '`', '?', '*', '\\', '<', '>', '|', '\"', ':' };
	private static final Pattern HTML_TITLE_PATTERN = Pattern.compile("<title>(.*)</title>"),
			JSON_NAME_PATTERN = Pattern.compile("\"matrixName\" : \"(.*)\","),
			JSON_START_PATTERN = Pattern.compile("\"executionStart\" : (.*),"),
			JSON_RESULT_PATTERN = Pattern.compile("\"result\" : (.*),");
	
	private final String reportsPath,
			tempPath;
	private final Function<String, String> pathResolver;
	private final String userName;
	
	protected List<Matrix> filteredRTMatrices;
	protected List<XmlMatrixInfo> filteredReportsInfo;
	
	public ReportsArchiver()
	{
		ClearThCore app = ClearThCore.getInstance();
		reportsPath = app.getReportsPath();
		tempPath = app.getTempDirPath();
		pathResolver = p -> app.getAppRootRelative(p);
		userName = UserInfoUtils.getUserName();
	}
	
	public ReportsArchiver(String reportsPath, String tempPath, Function<String, String> pathResolver, String userName)
	{
		this.reportsPath = reportsPath;
		this.tempPath = tempPath;
		this.pathResolver = pathResolver;
		this.userName = userName;
	}
	
	
	public ReportsArchiver setFilteredData(List<Matrix> filteredRTMatrices, List<XmlMatrixInfo> filteredReportsInfo)
	{
		this.filteredRTMatrices = filteredRTMatrices;
		this.filteredReportsInfo = filteredReportsInfo;
		return this;
	}
	
	protected void getFilesForZipReports(List<File> files, List<String> names, Set<String> filteredReports,
			ReportsInfo reportInfoPath) throws IOException
	{
		// for paused scheduler
		Path dir = Path.of(pathResolver.apply(reportInfoPath.getPath()));
		if (!Files.exists(dir))
		{
			//for completed scheduler
			dir = Path.of(pathResolver.apply(reportsPath + reportInfoPath.getPath()));
			if(!Files.exists(dir))
				throw new FileNotFoundException("These reports do not exist");
		}
		
		List<Path> reports = getReportsDirectories(dir);
		if (reports == null || reports.isEmpty())
			return;
		
		Set<String> addedFiles = new HashSet<>();
		
		for (Path repDir : reports)
		{
			if (!checkFilter(repDir, filteredReports))
				continue;
			
			Path htmlRep = getReportPathIfExists(repDir, HTML_REPORT_NAME),
					htmlFailedRep = getReportPathIfExists(repDir, HTML_FAILED_REPORT_NAME),
					jsonRep = getReportPathIfExists(repDir, JSON_REPORT_NAME);
			String title = sanitizeTitle(extractTitle(htmlRep, htmlFailedRep, jsonRep), repDir.toFile());
			
			// Takes all files and directories except for HTML reports, JSON reports and identical files of these reports in the 'repDir' that were taken earlier.
			try (Stream<Path> stream = Files.list(repDir))
			{
				Iterator<Path> pathIterator = stream.iterator();
				while (pathIterator.hasNext())
				{
					Path repFile = pathIterator.next();
					if (repFile.equals(htmlRep))
					{
						addFile(htmlRep.toFile(), String.format("%s%s", title, HTML_SUFFIX), files, names);
						continue;
					}
					if (repFile.equals(htmlFailedRep))
					{
						addFile(htmlFailedRep.toFile(), String.format("%s failed report%s", title, HTML_SUFFIX), files, names);
						continue;
					}
					if (repFile.equals(jsonRep))
					{
						addFile(jsonRep.toFile(), String.format("%s%s", title, JSON_SUFFIX), files, names);
						continue;
					}
					
					if (Files.isDirectory(repFile) || addedFiles.add(repFile.getFileName().toString()))
					{
						files.add(repFile.toFile());
						names.add(null);
					}
				}
			}
		}
	}
	
	private List<Path> getReportsDirectories(Path dir) throws IOException
	{
		try (Stream<Path> stream = Files.list(dir))
		{
			return stream.filter(file -> Files.isDirectory(file)
							&& (getReportPathIfExists(file, HTML_REPORT_NAME) != null
									|| getReportPathIfExists(file, HTML_FAILED_REPORT_NAME) != null
									|| getReportPathIfExists(file, JSON_REPORT_NAME) != null))
					.collect(Collectors.toList());
		}
	}
	
	private Path getReportPathIfExists(Path path, String fileName)
	{
		Path report = path.resolve(fileName);
		if (Files.isRegularFile(report))
			return report;
		return null;
	}
	
	private boolean checkFilter(Path reportDir, Set<String> filteredReports)
	{
		if (filteredReports != null)
			return filteredReports.contains(reportDir.getFileName().toString());
		return true;
	}
	
	private String extractTitle(Path htmlRep, Path htmlFailedRep, Path jsonRep) throws IOException
	{
		if (htmlRep != null)
			return extractTitleFromHtml(htmlRep.toFile());
		
		if (htmlFailedRep != null)
			return extractTitleFromHtml(htmlFailedRep.toFile());
		
		if (jsonRep != null)
			return extractTitleFromJson(jsonRep.toFile());
		
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
		FileOperationUtils.zipFiles(result, filesToZip, names);
	}

	public File getZipSelectedReports(boolean realtimeSnapshot, ReportsInfo reportsInfo) throws IOException
	{
		return getZipReports(realtimeSnapshot, reportsInfo, Collections.emptyList(), Collections.emptyList(), "_reports");
	}

	public File getZipSelectedReportsWithLogs(File shortLog, ReportsInfo reportsInfo) throws IOException
	{
		return getZipReports(false, reportsInfo, Collections.singletonList(shortLog), Collections.singletonList(null), "_reports_logs");
	}
	
	
	private File getZipReports(boolean realtimeSnapshot, ReportsInfo reportsInfo, List<File> filesToAdd, List<String> filesToAddNames, String fileName) throws IOException
	{
		List<File> filesToZip = new ArrayList<>(filesToAdd);
		List<String> names = new ArrayList<>(filesToAddNames);
		getFilesForZipReports(filesToZip, names, getFilteredReports(realtimeSnapshot), reportsInfo);
		return getZipFile(fileName, filesToZip, names);
	}
	
	public File getZipSelectedMatrixReports(XmlMatrixInfo info, String selReportsPath) throws IOException
	{
		Path path = Path.of(this.reportsPath, selReportsPath, info.getFileName());
		if(!Files.isDirectory(path))
			throw new FileNotFoundException("Reports directory does not exist");
		
		List<File> filesToZip;
		try (Stream<Path> stream = Files.list(path))
		{
			filesToZip = stream.map(Path::toFile).collect(Collectors.toList());
		}
		return getZipFile(info.getFileName(), filesToZip, Collections.emptyList());
	}
	
	private File getZipFile(String fileName, List<File> filesToZip, List<String> names) throws IOException
	{
		if (filesToZip.isEmpty())
			return null;
		
		fileName = String.format("%s_%s_%s.zip", userName, fileName, System.currentTimeMillis());
		File zipFile = new File(tempPath, fileName);
		Files.createDirectories(zipFile.getParentFile().toPath());
		zipFiles(zipFile, filesToZip, names);
		return zipFile;
	}
}
