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

package com.exactprosystems.clearth.connectivity.remotehand.http;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import com.exactprosystems.clearth.connectivity.remotehand.RhClient;
import com.exactprosystems.clearth.connectivity.remotehand.RhException;
import com.exactprosystems.clearth.connectivity.remotehand.RhResponse;
import com.exactprosystems.clearth.connectivity.remotehand.RhScriptProcessor;

/**
 * Implementation of RhClient to send requests via HTTP
 */
public class HttpRhClient extends RhClient
{
	private static final String HTTP_PATH_DELIMITER = "/";
	
	protected final CloseableHttpClient httpClient;
	protected String sessionUrl;
	protected final String baseUrl;
	protected final Set<String> byte_types = new HashSet<>(Arrays.asList("image/png",
			"application/octet-stream"));
	
	public HttpRhClient(RhScriptProcessor processor, CloseableHttpClient httpClient, String host)
	{
		super(processor);
		this.httpClient = httpClient;
		this.baseUrl = host.endsWith(HTTP_PATH_DELIMITER) ? host : host + HTTP_PATH_DELIMITER;
	}
	
	public HttpRhClient(CloseableHttpClient httpClient, String host)
	{
		this.httpClient = httpClient;
		this.baseUrl = host.endsWith(HTTP_PATH_DELIMITER) ? host : host + HTTP_PATH_DELIMITER;
	}
	
	public String getBaseUrl()
	{
		return baseUrl;
	}
	
	public String getSessionUrl()
	{
		return sessionUrl;
	}
	
	public CloseableHttpClient getHttpClient()
	{
		return httpClient;
	}
	
	
	@Override
	protected RhResponse sendLogon() throws IOException
	{
		return executeRequest(new HttpGet(baseUrl + "login"));
	}
	
	@Override
	protected RhResponse sendScript(String script) throws IOException
	{
		HttpPost post = new HttpPost(sessionUrl);
		post.setEntity(new StringEntity(script, StandardCharsets.UTF_8));
		
		return executeRequest(post);
	}
	
	@Override
	protected RhResponse queryStatus() throws IOException
	{
		return executeRequest(new HttpGet(sessionUrl));
	}
	
	@Override
	protected RhResponse downloadFile(String type, String id) throws IOException
	{
		try
		{
			URIBuilder builder = new URIBuilder(baseUrl + "download")
					.addParameter("type", type)
					.addParameter("id", id);
			return executeRequest(new HttpGet(builder.build()));
		}
		catch (URISyntaxException e)
		{
			throw new IllegalArgumentException("Could not build request with given parameters", e);
		}
	}
	
	@Override
	protected RhResponse sendLogout() throws IOException
	{
		return convertResponse(httpClient.execute(new HttpDelete(sessionUrl)));
	}
	
	@Override
	protected void disposeResources() throws Exception
	{
		httpClient.close();
	}
	
	@Override
	public void logon() throws IOException, RhException
	{
		super.logon();
		this.sessionUrl = baseUrl + sessionId;
	}
	
	@Override
	public RhResponse sendFile(File f, String path) throws IOException
	{
		HttpPost post = new HttpPost(sessionUrl);
		post.addHeader("Transfer-filename", path);
		post.setEntity(new FileEntity(f, ContentType.APPLICATION_OCTET_STREAM));
		return executeRequest(post);
	}
	
	
	private RhResponse convertResponse(HttpResponse response) throws ParseException, IOException
	{
		Header[] headers = response.getAllHeaders();
		boolean byteArrayContent = false;
		for (Header h : headers)
		{
			if ("Content-Type".equalsIgnoreCase(h.getName()) && byte_types.contains(h.getValue().toLowerCase()))
			{
				byteArrayContent = true;
				break;
			}
		}
		
		if (byteArrayContent)
		{
			HttpEntity entity = response.getEntity();
			byte[] bytes = EntityUtils.toByteArray(entity);
			EntityUtils.consume(entity);
			return new RhResponse(response.getStatusLine().getStatusCode(), bytes);
		}
		else
		{
			HttpEntity entity = response.getEntity();
			String result = EntityUtils.toString(entity);
			EntityUtils.consume(entity);
			return new RhResponse(response.getStatusLine().getStatusCode(), result);
		}
	}
	
	private RhResponse executeRequest(HttpUriRequest request) throws IOException
	{
		return convertResponse(httpClient.execute(request));
	}
}
