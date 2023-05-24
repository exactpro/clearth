/******************************************************************************
 * Copyright 2009-2023 Exactpro Systems Limited
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

package com.exactprosystems.clearth.web.misc;

import com.exactprosystems.clearth.ClearThCore;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLDecoder;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;
import org.apache.commons.io.FileUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportDownloadServlet extends HttpServlet
{
	private static final long serialVersionUID = -1151238303525772634L;
	private static final Logger logger = LoggerFactory.getLogger(ReportDownloadServlet.class);
	private static final String REPORT_PATH = "automation/reports/",
		JSON_RESPONSE_COMMAND = "application/json json";
	private static final int DEFAULT_BUFFER_SIZE = 4 * 1024; //4Kb - standard page size
	
	protected final File reportFile;
	
	protected final MimetypesFileTypeMap fileTypeMap;
	
	public ReportDownloadServlet()
	{
		fileTypeMap = getServletMimetypesFileTypeMap();
		reportFile = new File(getReportPath());
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
		throws ServletException, IOException
	{
		String filePath = req.getPathInfo();

		//In case if /clearth/reports/ is direclty invoked
		if (filePath == null || filePath.length() < 1) {
			resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		File downloadFile = new File(reportFile, URLDecoder.decode(filePath, "UTF-8"));

		if (!verifyFileAccess(downloadFile))
		{
			resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		if (!downloadFile.isFile())
		{
			resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		
		int bufferSize = getBufferSize();
		long responseSize = downloadFile.length();
		
		resp.setContentType(fileTypeMap.getContentType(downloadFile));
		resp.setStatus(HttpServletResponse.SC_OK);
		resp.setBufferSize(bufferSize);
		resp.setContentLengthLong(responseSize);
		try (BufferedOutputStream outStream = new BufferedOutputStream(resp.getOutputStream());
			BufferedInputStream in = new BufferedInputStream(new FileInputStream(downloadFile), bufferSize)) 
		{
			byte[] byteBuffer = new byte[bufferSize];
			int length = 0;
			while ((length = in.read(byteBuffer)) != -1)
			{
				outStream.write(byteBuffer, 0, length);
			}
		}
	}

	protected MimetypesFileTypeMap getServletMimetypesFileTypeMap()
	{
		MimetypesFileTypeMap result = new MimetypesFileTypeMap();
		result.addMimeTypes(JSON_RESPONSE_COMMAND);
		return result;
	}

	protected boolean verifyFileAccess(File downloadFile) throws IOException
	{
		//Can happen if someone requests files from report directory directly before any matrix was run
		if (!reportFile.exists())
			return false;

		return FileUtils.directoryContains(reportFile, downloadFile);
	}

	protected int getBufferSize()
	{
		String value = getInitParameter("BUFFER_SIZE");
		try
		{
			if (value != null)
				return Integer.parseInt(value);
		}
		catch (NumberFormatException e)
		{
			logger.error("Buffer size conversion from settings failed for value " + value, e);
		}
		return DEFAULT_BUFFER_SIZE;
	}

	protected String getReportPath()
	{
		return ClearThCore.rootRelative(REPORT_PATH);
	}
}