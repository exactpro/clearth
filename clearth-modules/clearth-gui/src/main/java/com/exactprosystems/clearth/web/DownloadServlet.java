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

package com.exactprosystems.clearth.web;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;

import javax.activation.FileDataSource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.ClearThCore;

@SuppressWarnings("serial")
public class DownloadServlet extends HttpServlet
{
	private static final int BUFFER_SIZE = 8192;
	
	private final static Logger logger = LoggerFactory.getLogger(DownloadServlet.class);

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String fileName = request.getParameter("file"),
				fileNameToShow = request.getParameter("fileName");
		logger.trace("Downloading file: '"+fileName+"'");
		if (fileName==null)
		{
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		File root = new File(ClearThCore.filesRoot());
		FileDataSource file = new FileDataSource(new File(root, fileName));
		//Checking if file is inside application folder, no external files should be downloadable!
		if ((!file.getFile().exists()) || (!file.getFile().getCanonicalPath().startsWith(root.getCanonicalPath())))
		{
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		
		response.reset();
		response.setBufferSize(BUFFER_SIZE);
		if (request.getParameter("inline")==null)
		{
			response.setContentType(file.getContentType());
			response.setHeader("Content-Disposition", "attachment; filename=\""+(fileNameToShow==null ? file.getName() : fileNameToShow)+"\"");
		}
		else
			response.setHeader("Content-Disposition", "inline");
		response.setHeader("Content-Length", String.valueOf(file.getFile().length()));

		BufferedInputStream input = null;
		BufferedOutputStream output = null;
		try
		{
			input = new BufferedInputStream(file.getInputStream(), BUFFER_SIZE);
			output = new BufferedOutputStream(response.getOutputStream(), BUFFER_SIZE);

			byte[] buffer = new byte[BUFFER_SIZE];
			int length;
			while ((length = input.read(buffer)) > 0)
				output.write(buffer, 0, length);
		}
		finally
		{
			if (output!=null)
				output.close();
			if (input!=null)
				input.close();
		}
	}
}
