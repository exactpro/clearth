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

package com.exactprosystems.clearth.connectivity.http;

import com.exactprosystems.clearth.BasicConnectionTest;
import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.ICodec;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpStatus;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import static com.exactprosystems.clearth.utils.FileOperationUtils.resourceToAbsoluteFilePath;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class FileDownloaderTest extends BasicConnectionTest
{
	private static final String USER_DIR = System.getProperty("user.dir");
	private static final String TEST_OUTPUT = USER_DIR + "/testOutput";
	private static final String OUTPUT_FILE_PATH = TEST_OUTPUT + "/test.txt";

	@BeforeClass
	public void createDirs() throws IOException
	{
		Files.createDirectories(Paths.get(TEST_OUTPUT));
	}

	@AfterClass
	public void clean() throws IOException
	{
		File file = new File(TEST_OUTPUT);
		FileUtils.deleteDirectory(file);
	}

	@Override
	protected void mockOtherApplicationFields(ClearThCore application) throws Exception
	{
		ICodec codec = mock(ICodec.class);
		when(codec.encode(any(ClearThMessage.class))).thenReturn("encodedMessage");
		doReturn(codec).when(application).createCodec(anyString());
	}

	@DataProvider(name = "downloadFileDataWithMockFile")
	public Object[][] createParametersForDownloadFile()
	{
		return new Object[][]
				{
						{
								"/some-request",
								"DownloadRequest",
								"http://localhost:" + wireMock.port() + "/",
								emptyMap(),
								"DownloadTest.txt"
						}
				};
	}

	@DataProvider(name = "downloadFileDataWithoutMockFile")
	public Object[][] createParametersForDownload()
	{
		return new Object[][]
				{
						{
								"/some-request",
								"DownloadRequest",
								"http://localhost:" + wireMock.port() + "/",
								emptyMap()
						}
				};
	}


	@Test(dataProvider = "downloadFileDataWithMockFile")
	public void testDownloadFile(String expectedUrl, String httpRequestType, String parentUrl,
	                             Map<String, String> requestParams, String mockFileName)
			throws Exception
	{
		wireMock.stubFor(get(urlEqualTo(expectedUrl))
				.willReturn(aResponse()
						.withStatus(HttpStatus.SC_OK)
						.withHeader("Content-Type", "application/octet-stream")
						.withBodyFile(mockFileName)));

		try (ClearThHttpClient client = createClient())
		{
			FileDownloader fileDownloader = new FileDownloader(OUTPUT_FILE_PATH, httpRequestType, parentUrl,
					client, createHttpDictionary(), null, requestParams, createSettings());

			try(RequestSenderResult result = fileDownloader.downloadFile())
			{
				assertNotNull(result);
				assertNull(result.getException());
				assertNull(result.getErrorMessage());
				assertNotNull(result.getResponse());
				String actualFileContent = FileUtils.readFileToString(new File(resourceToAbsoluteFilePath(
						WIREMOCK_RESOURCES_FILES + mockFileName)), UTF_8);
				String downloadedFileContent = FileUtils.readFileToString(new File(OUTPUT_FILE_PATH), UTF_8);
				assertEquals(downloadedFileContent, actualFileContent);
			}
		}
	}

	@Test(dataProvider = "downloadFileDataWithoutMockFile")
	public void testDownloadFileBody(String expectedUrl, String httpRequestType, String parentUrl,
	                                 Map<String, String> requestParams)
			throws Exception
	{
		wireMock.stubFor(get(urlEqualTo(expectedUrl))
				.willReturn(aResponse()
						.withStatus(HttpStatus.SC_OK)
						.withHeader("Content-Type", "application/json")
						.withBody("body")));

		try(ClearThHttpClient client = createClient())
		{
			FileDownloader fileDownloader = new FileDownloader(OUTPUT_FILE_PATH, httpRequestType, parentUrl,
					client, createHttpDictionary(), null, requestParams, createSettings());
			try(RequestSenderResult result = fileDownloader.downloadFile())
			{
				assertNotNull(result);
				assertNull(result.getException());
				assertNull(result.getErrorMessage());
				assertNotNull(result.getResponse());
				String actualFileContent = "body";
				String downloadedFileContent = FileUtils.readFileToString(new File(OUTPUT_FILE_PATH), UTF_8);
				assertEquals(downloadedFileContent, actualFileContent);
			}
		}
	}

	@Test(dataProvider = "downloadFileDataWithoutMockFile")
	public void testDownloadWithError(String expectedUrl, String httpRequestType, String parentUrl,
	                                  Map<String, String> requestParams)
			throws Exception
	{
		wireMock.stubFor(get(urlEqualTo(expectedUrl))
				.willReturn(aResponse()
						.withStatus(HttpStatus.SC_NOT_FOUND)));

		try(ClearThHttpClient client = createClient())
		{
			FileDownloader fileDownloader = new FileDownloader(OUTPUT_FILE_PATH, httpRequestType, parentUrl,
					client, createHttpDictionary(), null, requestParams, createSettings());

			try (RequestSenderResult result = fileDownloader.downloadFile())
			{
				assertNotNull(result);
				assertFalse(Files.exists(Paths.get(OUTPUT_FILE_PATH)));
				assertEquals(result.getErrorMessage(), "Downloaded file is empty.");
			}
		}
	}

}