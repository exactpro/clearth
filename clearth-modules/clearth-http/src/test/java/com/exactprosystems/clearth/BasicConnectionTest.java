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
package com.exactprosystems.clearth;

import com.exactprosystems.clearth.automation.actions.http.HttpRequestSettings;
import com.exactprosystems.clearth.connectivity.http.ClearThHttpClient;
import com.exactprosystems.clearth.connectivity.http.HttpDictionary;
import com.exactprosystems.clearth.utils.DictionaryLoadException;
import com.exactprosystems.clearth.utils.SettingsException;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import java.io.FileNotFoundException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import static com.exactprosystems.clearth.utils.FileOperationUtils.resourceToAbsoluteFilePath;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

public abstract class BasicConnectionTest extends BasicTestNgTest
{
	protected static final String HTTP_DICTIONARY_PATH = "cfg/dicts/http.xml";
	protected static final String WIREMOCK_RESOURCES_FILES = "__files/";
	protected static final String USER = "admin";
	protected static final String PASSWORD = "admin";

	public WireMockRule wireMock = new WireMockRule(options()
			.bindAddress("localhost")
			.dynamicPort()
			.disableRequestJournal());

	@BeforeClass
	public void startWireMock()
	{
		wireMock.start();
	}

	@AfterClass
	public void stopWireMock()
	{
		wireMock.stop();
	}

	protected ClearThHttpClient createClient()
			throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException
	{
		return new ClearThHttpClient();
	}

	protected HttpDictionary createHttpDictionary()
			throws SettingsException, DictionaryLoadException, FileNotFoundException
	{
		return new HttpDictionary(resourceToAbsoluteFilePath(HTTP_DICTIONARY_PATH));
	}

	protected HttpRequestSettings createSettings()
	{
		HttpRequestSettings settings = new HttpRequestSettings();

		settings.setUser(USER);
		settings.setPassword(PASSWORD);

		return settings;
	}
}
