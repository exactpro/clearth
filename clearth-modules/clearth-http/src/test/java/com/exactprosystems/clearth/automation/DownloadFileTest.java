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

package com.exactprosystems.clearth.automation;

import com.exactprosystems.clearth.BasicConnectionTest;
import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.actions.http.DownloadFile;
import com.exactprosystems.clearth.automation.exceptions.FailoverException;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.connectivity.http.ClearThHttpClient;
import com.exactprosystems.clearth.connectivity.http.HttpDictionary;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.ICodec;
import com.exactprosystems.clearth.data.DefaultTestExecutionHandler;
import com.exactprosystems.clearth.utils.DictionaryLoadException;
import com.exactprosystems.clearth.utils.SettingsException;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpStatus;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static com.exactprosystems.clearth.automation.actions.http.DownloadFile.*;
import static com.exactprosystems.clearth.utils.CollectionUtils.map;
import static com.exactprosystems.clearth.utils.FileOperationUtils.resourceToAbsoluteFilePath;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doReturn;
import static org.testng.Assert.*;

public class DownloadFileTest extends BasicConnectionTest
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
								map(PARAM_HTTP_REQUEST_TYPE, "DownloadRequest",
										PARAM_USER, USER,
										PARAM_PASSWORD, PASSWORD,
										PARAM_ROOT_URL, "http://localhost:" + wireMock.port() + "/"),
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
								map(PARAM_HTTP_REQUEST_TYPE, "DownloadRequest",
										PARAM_USER, USER,
										PARAM_PASSWORD, PASSWORD,
										PARAM_ROOT_URL, "http://localhost:" + wireMock.port() + "/")
						}
				};
	}


	@Test(dataProvider = "downloadFileDataWithMockFile")
	public void testDownloadFile(String expectedUrl, Map<String, String> actionParams, String mockFileName)
			throws FailoverException, IOException
	{
		wireMock.stubFor(get(urlEqualTo(expectedUrl))
				.willReturn(aResponse()
						.withStatus(HttpStatus.SC_OK)
						.withHeader("Content-Type", "application/octet-stream")
						.withBodyFile(mockFileName)));

		Action action = createAction(actionParams);
		Result result = action.execute(null, new MatrixContext(), 
				new GlobalContext(null, false, null, null, null, new DefaultTestExecutionHandler()));

		assertTrue(result.isSuccess(), result.getComment());
		assertNull(result.getError());
		String actualFileContent = FileUtils.readFileToString(new File(resourceToAbsoluteFilePath(
				WIREMOCK_RESOURCES_FILES + mockFileName)), UTF_8);

		String downloadedFileContent = FileUtils.readFileToString(new File(OUTPUT_FILE_PATH), UTF_8);
		assertEquals(downloadedFileContent, actualFileContent);
	}

	@Test(dataProvider = "downloadFileDataWithoutMockFile")
	public void testDownloadFileEmpty(String expectedUrl, Map<String, String> actionParams)
			throws FailoverException
	{
		wireMock.stubFor(get(urlEqualTo(expectedUrl))
				.willReturn(aResponse()
						.withStatus(HttpStatus.SC_OK)));

		Action action = createAction(actionParams);
		Result result = action.execute(null, new MatrixContext(),
				new GlobalContext(null, false, null, null, null, new DefaultTestExecutionHandler()));

		assertFalse(result.isSuccess(), result.getComment());
		assertNull(result.getError());
		
		assertFalse(Files.exists(Paths.get(OUTPUT_FILE_PATH)));
	}

	@Test(dataProvider = "downloadFileDataWithoutMockFile")
	public void testDownloadFileBody(String expectedUrl, Map<String, String> actionParams)
			throws FailoverException, IOException
	{
		wireMock.stubFor(get(urlEqualTo(expectedUrl))
				.willReturn(aResponse()
						.withStatus(HttpStatus.SC_OK)
						.withHeader("Content-Type", "application/json")
						.withBody("body")));

		Action action = createAction(actionParams);
		Result result = action.execute(null, new MatrixContext(),
				new GlobalContext(null, false, null, null, null, new DefaultTestExecutionHandler()));

		assertTrue(result.isSuccess(), result.getComment());
		assertNull(result.getError());
		String actualFileContent = "body";

		String downloadedFileContent = FileUtils.readFileToString(new File(OUTPUT_FILE_PATH), UTF_8);
		assertEquals(downloadedFileContent, actualFileContent);
	}

	@Test(dataProvider = "downloadFileDataWithoutMockFile")
	public void testDownloadWithError(String expectedUrl, Map<String, String> actionParams)
			throws FailoverException
	{
		wireMock.stubFor(get(urlEqualTo(expectedUrl))
				.willReturn(aResponse()
						.withStatus(HttpStatus.SC_NOT_FOUND)));

		Action action = createAction(actionParams);
		Result result = action.execute(null, new MatrixContext(),
				new GlobalContext(null, false, null, null, null, new DefaultTestExecutionHandler()));

		assertFalse(result.isSuccess(), result.getComment());
		assertNull(result.getError());

		assertFalse(Files.exists(Paths.get(OUTPUT_FILE_PATH)));
	}

	public Action createAction(Map<String, String> params)
	{
		Action action = new TestDownloadFile(wireMock.port());
		action.preInit(null,DownloadFile.class.getSimpleName(), new HashMap<>());
		ActionSettings settings = new ActionSettings();
		settings.setParams(params);
		action.init(settings);
		return action;
	}
	
	public static class TestDownloadFile extends DownloadFile
	{
		private final int port;

		public TestDownloadFile(int port)
		{
			this.port = port;
		}

		@Override
		protected String getDefaultRootUrl()
		{
			return "http://localhost:" + port + "/";
		}

		@Override
		protected String getDefaultUser()
		{
			return USER;
		}

		@Override
		protected String getDefaultPassword()
		{
			return PASSWORD;
		}

		@Override
		protected String getDefaultOutputPath()
		{
			return OUTPUT_FILE_PATH;
		}

		@Override
		protected HttpDictionary getHttpDictionary()
		{
			try
			{
				String path = resourceToAbsoluteFilePath(HTTP_DICTIONARY_PATH);
				return new HttpDictionary(path);
			}
			catch (DictionaryLoadException | SettingsException | FileNotFoundException ex)
			{
				throw new ResultException(String.format("Error while loading HTTP Requests Dictionary from file '%s'.",
						HTTP_DICTIONARY_PATH), ex);
			}
		}

		@Override
		protected ClearThHttpClient getHttpClient() throws ResultException
		{
			try
			{
				return new ClearThHttpClient();
			}
			catch (Exception e)
			{
				throw ResultException.failed(e);
			}
		}

	}

}