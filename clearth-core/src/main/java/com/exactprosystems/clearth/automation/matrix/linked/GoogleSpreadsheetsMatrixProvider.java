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

package com.exactprosystems.clearth.automation.matrix.linked;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.SecurityUtils;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GoogleSpreadsheetsMatrixProvider implements MatrixProvider
{
	
	public static final String TYPE = "GoogleSpreadsheets";
//	public static final String SERVICE_ACCOUNT_EMAIL_PROPERTY = "service_account_email";
//	public static final String SERVICE_ACCOUNT_PKCS12_IS_PROPERTY = "service_account_pkcs12_input_stream";
	public static final String KEY_TYPE = "PKCS12";
	public static final String ID_IN_URL_PATTERN = ".*\\/spreadsheets\\/d\\/(.*)\\/.*(gid=.*)";
	protected Drive driveService;
	protected String name;
	protected String link;
	protected InputStream inputStream;
//	protected String serviceAccountEmail;
//	protected InputStream serviceAccountPkcs12;
	protected GoogleSpreadsheetsConfiguration configuration;

	public GoogleSpreadsheetsMatrixProvider(String link, GoogleSpreadsheetsConfiguration configuration){
		this.link = link;
		this.configuration = configuration;
	}

	@Override
	public InputStream getMatrix() throws Exception {
		try
		{
			if (inputStream == null)
			{
				Pattern patt = Pattern.compile(ID_IN_URL_PATTERN);
				Matcher match = patt.matcher(link);
				String fileId, sheetId;
				if (match.find())
				{
					fileId = match.group(1);
					sheetId = match.group(2);
				}
				else
				{
					throw new IllegalArgumentException("Incorrect link");
				}
				if (driveService == null)
				{
					initService();
				}
				File file = driveService.files().get(fileId).execute();
				String downloadUrl = file.getExportLinks().get("text/csv") + '&' + sheetId;
				inputStream = downloadFile(driveService, downloadUrl);
				name = file.getTitle();
			}
			return inputStream;
		}
		catch (GoogleJsonResponseException e) 
		{
			throw new IOException(e.getDetails().getMessage(), e);
		}
	}

	@Override
	public String getName() throws Exception
	{
		if(name == null){
			getMatrix();
		}
		return name;
	}
	
	protected GoogleSpreadsheetKeyLoader createKeyLoader() {
		return new GoogleSpreadsheetKeyLoader();
	}

	protected void initService() throws Exception {
		HttpTransport httpTransport = new NetHttpTransport();
		JacksonFactory jsonFactory = new JacksonFactory();
		KeyStore keyStore = KeyStore.getInstance(KEY_TYPE);
		
		PrivateKey privateKey = SecurityUtils.loadPrivateKeyFromKeyStore(keyStore,
				this.createKeyLoader().getDecryptedFilePath(this.configuration.getP12FileName()),
				this.configuration.getKeyStorePass(),
				this.configuration.getAlias(),
				this.configuration.getKeyPass());
		
		Credential credential = new GoogleCredential.Builder()
				.setTransport(httpTransport)
				.setJsonFactory(jsonFactory)
				.setServiceAccountId(this.configuration.getServiceEmail())
				.setServiceAccountScopes(Arrays.asList(DriveScopes.DRIVE))
				.setServiceAccountPrivateKey(privateKey)
				.build();
		driveService = new Drive.Builder(httpTransport, jsonFactory, null)
				.setApplicationName("ClearTh")
				.setHttpRequestInitializer(credential).build();
	}

	private InputStream downloadFile(Drive service, String file) throws IOException
	{
		HttpResponse resp = service.getRequestFactory().buildGetRequest(new GenericUrl(file))
				.execute();
		return resp.getContent();
	}
}
