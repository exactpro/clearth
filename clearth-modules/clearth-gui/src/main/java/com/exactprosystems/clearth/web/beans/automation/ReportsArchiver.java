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

package com.exactprosystems.clearth.web.beans.automation;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.Matrix;
import com.exactprosystems.clearth.automation.ReportsInfo;
import com.exactprosystems.clearth.utils.ExceptionUtils;
import com.exactprosystems.clearth.utils.FileOperationUtils;
import com.exactprosystems.clearth.web.misc.MessageUtils;
import com.exactprosystems.clearth.web.misc.UserInfoUtils;
import com.exactprosystems.clearth.xmldata.XmlMatrixInfo;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.MimetypesFileTypeMap;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReportsArchiver {

	private static final Logger logger = LoggerFactory.getLogger(ReportsArchiver.class);

	private static final char[] ILLEGAL_CHARACTERS = { '/', '\n', '\r', '\t', '\0', '\f', '`', '?', '*', '\\', '<', '>', '|', '\"', ':' };
	private static final Pattern titlePattern = Pattern.compile("<title>(.*)</title>");

	protected List<Matrix> filteredRTMatrices;
	protected List<XmlMatrixInfo> filteredReportsInfo;

	public ReportsArchiver setFilteredData(List<Matrix> filteredRTMatrices, List<XmlMatrixInfo> filteredReportsInfo) {
		this.filteredRTMatrices = filteredRTMatrices;
		this.filteredReportsInfo = filteredReportsInfo;
		return this;
	}

	protected void getFilesForZipReports(List<File> files, List<String> names, List<String> filteredReports, ReportsInfo reportInfoPath) throws IOException {

		// for paused scheduler
		File dir = new File(ClearThCore.appRootRelative(reportInfoPath.getPath()));

		if (!dir.exists())
		{
			//for completed scheduler
			dir = new File(ClearThCore.appRootRelative(ClearThCore.getInstance().getReportsPath() +
					reportInfoPath.getPath()));
		}

		if(!dir.exists())
		{
			MessageUtils.addErrorMessage("Could not download reports", "These reports do not exist");
			return;
		}
		if(dir.isDirectory())
		{
			File[] reports = dir.listFiles(file -> {
				if (!file.isDirectory())
					return false;
				return new File(file, "report.html").isFile();
			});
			if(reports == null || reports.length==0)
				return;

			//1. Take the first directory and take all files except HTML
			File[] aux = reports[0].listFiles((dir1, name) -> !name.toLowerCase().endsWith(".html"));
			if (aux == null)
				return;

			//They will be in the beginning of our ZIP file
			files.addAll(Arrays.asList(aux));
			names.addAll(Arrays.asList(new String[aux.length]));

			//2. For each directory, we need to open its HTML report
			//and extract contents of <title> tag
			for (File repDir : reports)
			{
				//2.1 Open a file
				File report = new File(repDir, "report.html");
				if (!report.isFile())
				{
					names.add(null);
					continue;
				}

				if (filteredReports != null && !filteredReports.isEmpty()) {
					boolean breakFlag = true;
					for (String filteredReport : filteredReports) {
						if (repDir.getName().equals(filteredReport)) {
							breakFlag = false;
							break;
						}
					}
					if (breakFlag)
						continue;
				}

				String line, title = null;
				try (BufferedReader reader = new BufferedReader(new FileReader(report))) {
					int i = 0;
					//2.2 Scan the file. We'll check first 50 lines
					while (title == null && (line = reader.readLine()) != null && i < 50) {
						Matcher m = titlePattern.matcher(line);
						if (m.find())
							title = m.group(1);
						i++;
					}
				}


				if (title == null || title.trim().isEmpty())
				{
					IOException e = new IOException("Unable to extract the title for "+report.getPath());
					logger.error(e.getMessage(), e);
					throw e;
				}

				//2.3 Replace illegal characters with a dot
				for (char ic : ILLEGAL_CHARACTERS)
				{
					title = title.replace(ic, '.');
				}

				title += ".html";
				names.add(title);
				files.add(report);
			}
		}

	}

	private List<String> getFilteredReports(boolean realtimeSnapshot)
	{
		List<String> filteredReports = new ArrayList<>();
		if (realtimeSnapshot && filteredRTMatrices != null)
			for (Matrix record : filteredRTMatrices)
				filteredReports.add(record.getFileName());
		if (!realtimeSnapshot && filteredReportsInfo != null)
			for (XmlMatrixInfo record : filteredReportsInfo)
				filteredReports.add(record.getFileName());
		return filteredReports;
	}

	protected void zipFiles(File result, List<File> filesToZip, List<String> names) throws IOException
	{
		//Utils.zipDirectories(result, filesToZip);
		FileOperationUtils.zipFiles(result, filesToZip, names);
	}

	public StreamedContent getZipSelectedReports(boolean realtimeSnapshot, ReportsInfo reportInfoPath)
	{
		try
		{
			List<File> filesToZip = new ArrayList<>();
			List<String> names = new ArrayList<>();

			getFilesForZipReports(filesToZip, names, getFilteredReports(realtimeSnapshot), reportInfoPath);

			if (filesToZip.size() == 0) {
				return null;
			}

			File result = new File(ClearThCore.tempPath() + UserInfoUtils.getUserName() + "_reports.zip");
			zipFiles(result, filesToZip, names);

			return new DefaultStreamedContent(new FileInputStream(result), new MimetypesFileTypeMap().getContentType(result), "reports.zip");

		} catch (IOException e)
		{
			MessageUtils.addErrorMessage("Could not download reports", ExceptionUtils.getDetailedMessage(e));
			logger.debug("Could not download reports", e);
			return null;
		}
	}

	public StreamedContent getZipSelectedReportsWithLogs(File shortLog, ReportsInfo reportPath) throws IOException {
		List<File> filesToZip = new ArrayList<>();
		List<String> names = new ArrayList<>();

			
		filesToZip.add(shortLog);
		names.add(null);
		
		getFilesForZipReports(filesToZip, names, getFilteredReports(false), reportPath);
		File result = new File(ClearThCore.tempPath() + UserInfoUtils.getUserName() + "_reports_logs.zip");
		zipFiles(result, filesToZip, names);

		return new DefaultStreamedContent(new FileInputStream(result), new MimetypesFileTypeMap().getContentType(result), "reports_logs.zip");
		
	}
	
	
}
