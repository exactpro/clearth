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

package com.exactprosystems.clearth.connectivity.th2;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.ssl.SSLContextBuilder;

import com.exactprosystems.clearth.connectivity.ConnectivityException;
import com.exactprosystems.clearth.connectivity.connections.BasicClearThConnection;
import com.exactprosystems.clearth.connectivity.connections.ClearThCheckableConnection;
import com.exactprosystems.clearth.connectivity.connections.SettingsClass;
import com.exactprosystems.clearth.utils.SettingsException;

@XmlRootElement(name="Th2LoaderConnection")
@XmlAccessorType(XmlAccessType.NONE)
@SettingsClass(Th2LoaderConnectionSettings.class)
public class Th2LoaderConnection extends BasicClearThConnection implements ClearThCheckableConnection
{
	public static final String TYPE_TH2_LOADER = "th2 loader";
	
	@Override
	public Th2LoaderConnectionSettings getSettings()
	{
		return (Th2LoaderConnectionSettings)super.getSettings();
	}
	
	@Override
	public void check() throws SettingsException, ConnectivityException
	{
		try (CloseableHttpClient client = createClient())
		{
			HttpUriRequest request = createCheckRequest();
			client.execute(request);
		}
		catch (Exception e)
		{
			throw new ConnectivityException("Could not connect to th2 loader", e);
		}
	}
	
	public CloseableHttpClient createClient() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException
	{
		HttpClientBuilder builder = HttpClients.custom();
		builder.useSystemProperties();
		builder.setRedirectStrategy(new LaxRedirectStrategy());
		builder.setRetryHandler(new DefaultHttpRequestRetryHandler(3, true));
		
		//For HTTPS
		SSLContext sslContext = SSLContextBuilder.create().loadTrustMaterial(new TrustSelfSignedStrategy()).build();
		HostnameVerifier verifier = new DefaultHostnameVerifier();
		SSLConnectionSocketFactory sslConnectionFactory = new SSLConnectionSocketFactory(sslContext, verifier);
		builder.setSSLSocketFactory(sslConnectionFactory);
		
		//Default request configuration
		builder.setDefaultRequestConfig(RequestConfig.custom()
				.setCookieSpec(CookieSpecs.DEFAULT)
				.setCircularRedirectsAllowed(true)
				.setRedirectsEnabled(true)
				.build());
		
		return builder.build();
	}
	
	
	protected HttpUriRequest createCheckRequest()
	{
		return new HttpGet(getSettings().getUrl());
	}
}
