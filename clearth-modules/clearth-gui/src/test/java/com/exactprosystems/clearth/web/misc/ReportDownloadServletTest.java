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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

public class ReportDownloadServletTest
{
	private Path reportsDir;
	
	@BeforeClass
	public void init() throws IOException
	{
		reportsDir = Paths.get("src", "test", "resources", "reports");
	}
	
	
	@DataProvider(name = "files")
	public String[][] filesProvider()
	{
		return new String[][]
				{
					{"dummy.html", "text/html"},
					{"dummy.json", "application/json"}
				};
	}
	
	
	@Test
	public void outsideOfDirectory() throws ServletException, IOException
	{
		String fileName = "../outside/file.txt";
		Path filePath = reportsDir.resolve(fileName);
		try
		{
			Files.createDirectories(filePath.getParent());
			Files.createFile(filePath);
			
			HttpServletRequest request = mockGetRequest(fileName);
			HttpServletResponse response = new HttpResponseMinimalImpl();
			new CustomPathServlet().service(request, response);
			
			Assert.assertEquals(response.getStatus(), HttpServletResponse.SC_FORBIDDEN, "Download status");
		}
		finally
		{
			Files.deleteIfExists(filePath);
		}
	}
	
	@Test
	public void absentFile() throws ServletException, IOException
	{
		String filePath = "nofile.txt";
		
		HttpServletRequest request = mockGetRequest(filePath);
		HttpServletResponse response = new HttpResponseMinimalImpl();
		new CustomPathServlet().service(request, response);
		
		Assert.assertEquals(response.getStatus(), HttpServletResponse.SC_FORBIDDEN, "Download status");
	}
	
	@Test
	public void emptyQuery() throws ServletException, IOException
	{
		HttpServletRequest request = mockGetRequest("");
		HttpServletResponse response = new HttpResponseMinimalImpl();
		new CustomPathServlet().service(request, response);
		
		Assert.assertEquals(response.getStatus(), HttpServletResponse.SC_NOT_FOUND, "Download status");
	}
	
	@Test
	public void directoryDownload() throws ServletException, IOException
	{
		HttpServletRequest request = mockGetRequest("sub");
		HttpServletResponse response = new HttpResponseMinimalImpl();
		new CustomPathServlet().service(request, response);
		
		Assert.assertEquals(response.getStatus(), HttpServletResponse.SC_NOT_FOUND, "Download status");
	}
	
	@Test(dataProvider = "files")
	public void fileDownload(String filePath, String contentType) throws ServletException, IOException
	{
		HttpServletRequest request = mockGetRequest(filePath);
		HttpResponseMinimalImpl response = new HttpResponseMinimalImpl();
		new CustomPathServlet().service(request, response);
		
		Path file = reportsDir.resolve(filePath);
		
		SoftAssert soft = new SoftAssert();
		soft.assertEquals(response.getStatus(), HttpServletResponse.SC_OK, "Download status");
		soft.assertEquals(response.getContentLength(), Files.size(file), "File size");
		soft.assertEquals(response.getContentType(), contentType, "Content type");
		soft.assertEquals(response.getUsedOutputStream().getBuffer().array(), FileUtils.readFileToByteArray(file.toFile()), "File content");
		soft.assertAll();
	}
	
	
	private HttpServletRequest mockGetRequest(String path)
	{
		HttpServletRequest result = Mockito.mock(HttpServletRequest.class);
		Mockito.when(result.getMethod()).thenReturn("GET");
		Mockito.when(result.getPathInfo()).thenReturn(path);
		return result;
	}
	
	
	private class CustomPathServlet extends ReportDownloadServlet
	{
		private static final long serialVersionUID = 8062689312936558146L;
		
		public CustomPathServlet()
		{
			super();
		}
		
		@Override
		protected String getReportPath()
		{
			return reportsDir.toString();
		}
		
		@Override
		public String getInitParameter(String name)
		{
			return null;
		}
		
		@Override
		public Enumeration<String> getInitParameterNames()
		{
			return Collections.emptyEnumeration();
		}
	}
}
