/******************************************************************************
 * Copyright 2009-2024 Exactpro Systems Limited
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

import java.io.Closeable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.apache.http.Header;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.*;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.*;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.ConnectionSettings;
import com.exactprosystems.clearth.automation.actions.http.HttpRequestSettings;
import com.exactprosystems.clearth.automation.exceptions.ParametersException;
import com.exactprosystems.clearth.connectivity.ProxySettings;
import com.exactprosystems.clearth.utils.Utils;

import static java.lang.String.format;


public class ClearThHttpClient implements Closeable
{
	private static final Logger logger = LoggerFactory.getLogger(ClearThHttpClient.class);
	public static final String METHOD_GET = "GET",
			METHOD_POST = "POST",
			METHOD_PUT = "PUT",
			METHOD_DELETE = "DELETE",
			
			HOSTNAMEVERIFIER_BROWSER_COMPATIBLE = "BROWSER_COMPATIBLE",
			HOSTNAMEVERIFIER_ALLOW_ALL = "ALLOW_ALL",
			HOSTNAMEVERIFIER_NO = "NO";
	
	private final CloseableHttpClient httpClient;
	private String host;
	private final HttpClientContext localContext;

	public ClearThHttpClient() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
		this(null, null, null);
	}
	
	public ClearThHttpClient(ConnectionSettings conSettings, ProxySettings ps, 
			String hostnameVerifier) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException
	{
		httpClient = createClient(conSettings, ps, hostnameVerifier);
		if (conSettings != null)
			this.host = conSettings.url;
		else
			this.host = "";
		localContext = createLocalContext();
	}
	
	@Override
	public void close()
	{
		Utils.closeResource(httpClient);
	}
	
	
	protected Logger getLogger()
	{
		return logger;
	}

	
	protected TrustStrategy createTrustStrategy()
	{
		return new TrustSelfSignedStrategy();
	}
	
	protected SSLContext createSslContext() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException
	{
		return SSLContextBuilder.create().loadTrustMaterial(createTrustStrategy()).build();
	}
	
	protected HostnameVerifier createHostnameVerifier(String name)
	{
		if (name == null || name.equalsIgnoreCase(HOSTNAMEVERIFIER_BROWSER_COMPATIBLE)) {
			return new DefaultHostnameVerifier();
		} else if (name.equalsIgnoreCase(HOSTNAMEVERIFIER_ALLOW_ALL)) {
			return new NoopHostnameVerifier();
		} else if (name.equalsIgnoreCase(HOSTNAMEVERIFIER_NO)) {
			return null;
		}	else {
			throw new IllegalArgumentException("Unknown verifier type: '" + name + "'");
		}
	}
	
	protected SSLConnectionSocketFactory createSslConnectionSocketFactory(SSLContext sslContext, HostnameVerifier hostnameVerifier)
	{
		return new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
	}
	
	protected void addSslConnectionFactory(String hostnameVerifier, HttpClientBuilder builder) throws KeyManagementException, 
			NoSuchAlgorithmException, KeyStoreException
	{
		SSLContext sslContext = createSslContext();
		HostnameVerifier verifier = createHostnameVerifier(hostnameVerifier);
		SSLConnectionSocketFactory sslConnectionFactory = createSslConnectionSocketFactory(sslContext, verifier);
		if (sslConnectionFactory != null)
			builder.setSSLSocketFactory(sslConnectionFactory);
	}
	
	
	protected void addDefaultRequestConfig(HttpClientBuilder builder)
	{
		builder.setDefaultRequestConfig(RequestConfig.custom().
				setCookieSpec(CookieSpecs.DEFAULT).
				setCircularRedirectsAllowed(true).
				setRedirectsEnabled(true).
				build());
	}
	
	
	protected List<Header> createDefaultHeaders()
	{
		List<Header> result = new LinkedList<Header>();
		result.add(new BasicHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"));
		result.add(new BasicHeader("User-Agent", "Mozilla/5.0 (Windows NT 5.1; rv:16.0) Gecko/20100101 Firefox/16.0"));
		result.add(new BasicHeader("Accept-Encoding", "gzip, deflate"));
		result.add(new BasicHeader("Accept-Language", "ru,en;q=0.7,en-us;q=0.3"));
		result.add(new BasicHeader("DNT", "1"));
		result.add(new BasicHeader("Connection", "keep-alive"));
		result.add(new BasicHeader("Proxy-Connection", "keep-alive"));
		result.add(new BasicHeader("Cache-Control", "max-age=0"));
		return result;
	}
	
	protected void addDefaultHeaders(HttpClientBuilder builder)
	{
		List<Header> defaultHeaders = createDefaultHeaders();
		if (defaultHeaders != null)
			builder.setDefaultHeaders(defaultHeaders);
	}
	
	
	
	protected void addCredentials(String url, int port, String user, String password, CredentialsProvider provider)
	{
		provider.setCredentials(new AuthScope(url, port), new UsernamePasswordCredentials(user, password));
	}
	
	protected void addAuth(ConnectionSettings conSettings, CredentialsProvider provider, HttpClientBuilder builder)
	{
		if (conSettings != null)
			addCredentials(conSettings.url, AuthScope.ANY_PORT, conSettings.user, conSettings.password, provider);
	}
	
	protected void addProxy(ProxySettings proxySettings, CredentialsProvider provider, HttpClientBuilder builder)
	{
		if (proxySettings == null)
			return;
		
		addCredentials(proxySettings.host, proxySettings.port, proxySettings.user, proxySettings.password, provider);
		HttpHost proxy = new HttpHost(proxySettings.host, proxySettings.port);
		builder.setProxy(proxy);
	}
	
	protected void addAuthAndProxy(ConnectionSettings conSettings, ProxySettings proxySettings, HttpClientBuilder builder)
	{
		if ((conSettings == null) && (proxySettings == null))
			return;
		
		CredentialsProvider provider = new BasicCredentialsProvider();
		addAuth(conSettings, provider, builder);
		addProxy(proxySettings, provider, builder);
		builder.setDefaultCredentialsProvider(provider);
	}
	
	
	protected void addOtherSettings(HttpClientBuilder builder)
	{
	}
	
	protected CloseableHttpClient createClient(ConnectionSettings conSettings, ProxySettings proxySettings, 
			String hostnameVerifier) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException
	{
		HttpClientBuilder builder = HttpClients.custom();
		builder.useSystemProperties();
		builder.setRedirectStrategy(new LaxRedirectStrategy());
		builder.setRetryHandler(createHttpRequestRetryHandler());

		addSslConnectionFactory(hostnameVerifier, builder);
		addDefaultRequestConfig(builder);
		addDefaultHeaders(builder);
		addAuthAndProxy(conSettings, proxySettings, builder);
		
		addOtherSettings(builder);
		return builder.build();
	}

	protected HttpRequestRetryHandler createHttpRequestRetryHandler()
	{
		return new DefaultHttpRequestRetryHandler(3, true);
	}

	protected HttpClientContext createLocalContext()
	{
		CookieStore cookieStore = new BasicCookieStore();
		HttpClientContext result = HttpClientContext.create();
		result.setCookieStore(cookieStore);
		return result;
	}
	
	protected HttpClientContext createLocalContext(HttpClientContext baseContext)
	{
		HttpClientContext result = HttpClientContext.create();
		result.setCookieStore(baseContext.getCookieStore());
		return result;
	}
	
	protected HttpClientContext createLocalContext(HttpClientContext baseContext, HttpRequestSettings settings)
	{
		HttpClientContext result = createLocalContext(baseContext);
		CredentialsProvider provider = new BasicCredentialsProvider();
		addCredentials(AuthScope.ANY_HOST, AuthScope.ANY_PORT, settings.getUser(), settings.getPassword(), provider);
		result.setCredentialsProvider(provider);
		return result;
	}
	
	protected HttpClientContext getContextToUse(HttpRequestSettings settings)
	{
		if (settings == null)
			return localContext;
		
		if (settings.getContext() != null)
			return settings.getContext();
		
		String user = settings.getUser();
		if ((user != null) && (!user.isEmpty()))
			return createLocalContext(localContext, settings);
		return localContext;
	}
	
	
	public static String cleanHttpValue(String value)
	{
		return value.replace(" ", "%20");
	}
	
	
	
	public CloseableHttpClient getClient()
	{
		return httpClient;
	}

	
	public String getHost()
	{
		return host;
	}

	public void setHost(String host)
	{
		this.host = host;
	}
	
	
	public HttpContext getLocalContext()
	{
		return this.localContext;
	}
	
	
	/**
	 * Creates context to be used while sending requests. New context is based on local context of this client.
	 * If client is used by multiple threads, each thread must use it own context.
	 * @return new context based on local context of this client. This is not a separate copy. It shares cookie store with the original local context
	 */
	public HttpClientContext createSeparateContext()
	{
		return createLocalContext(localContext);
	}
	
	
	/**
	 * Sends given request using given context.
	 * @param request to send
	 * @param context to use while sending request. For correct multi-threaded use each thread should have its own context
	 * @param consumeResponse if true, response will be consumed to free occupied resources
	 * @return {@link CloseableHttpResponse} with complete response information
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public CloseableHttpResponse send(HttpUriRequest request, HttpClientContext context, boolean consumeResponse) throws ClientProtocolException, IOException
	{
		CloseableHttpResponse response = httpClient.execute(request, context);
		getLogger().trace("Response: {}", response);
		
		if (consumeResponse)
			EntityUtils.consume(response.getEntity());
		return response;
	}
	
	/**
	 * Sends given request using local context of this client. 
	 * This method should not be used with multiple threads because each thread should use it own context.
	 * @param request to send
	 * @param consumeResponse if true, response will be consumed to free occupied resources
	 * @return {@link CloseableHttpResponse} with complete response information
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public CloseableHttpResponse send(HttpUriRequest request, boolean consumeResponse) throws ClientProtocolException, IOException
	{
		return send(request, localContext, consumeResponse);
	}
	
	/**
	 * Sends given request using context from settings, if defined, else using local context of this client. 
	 * If settings does not have separate context object, this method should not be used with multiple threads because each thread should use it own context.
	 * @param request to send
	 * @param settings with context to use to send request
	 * @param consumeResponse if true, response will be consumed to free occupied resources
	 * @return {@link CloseableHttpResponse} with complete response information
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public CloseableHttpResponse send(HttpUriRequest request, HttpRequestSettings settings, boolean consumeResponse) throws ClientProtocolException, IOException
	{
		return send(request, getContextToUse(settings), consumeResponse);
	}
	
	/**
	 * Constructs request from settings. Sends it using context from settings, if defined, else using local context of this client. 
	 * If settings does not have separate context object, this method should not be used with multiple threads because each thread should use it own context.
	 * @param settings with context to use to send request
	 * @param consumeResponse if true, response will be consumed to free occupied resources
	 * @return {@link CloseableHttpResponse} with complete response information
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public CloseableHttpResponse send(HttpRequestSettings settings, boolean consumeResponse) throws ParametersException, IOException
	{
		HttpUriRequest request = createRequest(settings);
		return send(request, settings, consumeResponse);
	}
	
	/**
	 * Sends given request using given context.
	 * @param request to send
	 * @param context to use while sending request. For correct multi-threaded use each thread should have its own context
	 * @return response body
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public String send(HttpUriRequest request, HttpClientContext context) throws ClientProtocolException, IOException
	{
		try
		{
			String response = httpClient.execute(request,
					new BasicResponseHandler(),
					context);

			getLogger().trace("Response body: {}", response);

			return response;
		}
		catch (HttpResponseException e)
		{
			throw new HttpResponseException(e.getStatusCode(),
					format("Request %s %s failed. Code: %d. Reason: %s.",
							request.getMethod(), request.getURI(), 
							e.getStatusCode(), e.getMessage()));
		}
	}
	
	/**
	 * Sends given request using context from settings, if defined, else using local context of this client.
	 * If settings does not have separate context object, this method should not be used with multiple threads because each thread should use it own context.
	 * @param request to send
	 * @param settings with context to use to send request
	 * @return response body
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public String send(HttpUriRequest request, HttpRequestSettings settings) throws ClientProtocolException, IOException
	{
		return send(request, getContextToUse(settings));
	}
	
	/**
	 * Sends given request using local context of this client. 
	 * This method should not be used with multiple threads because each thread should use it own context.
	 * @param request to send
	 * @return response body
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public String send(HttpUriRequest request) throws ClientProtocolException, IOException
	{
		return send(request, localContext);
	}
	
	/**
	 * Constructs request from settings. Sends it using context from settings, if defined, else using local context of this client. 
	 * If settings does not have separate context object, this method should not be used with multiple threads because each thread should use it own context.
	 * @param settings with context to use to send request
	 * @return response body
	 * @throws ParametersException
	 * @throws IOException
	 */
	public String send(HttpRequestSettings settings) throws ParametersException, IOException
	{
		HttpUriRequest request = createRequest(settings);
		return send(request, settings);
	}
	
	public String findLocationHeader(Header[] headers)
	{
		for (Header h : headers)
		{
			if ("Location".equalsIgnoreCase(h.getName()))
				return h.getValue();
		}
		return null;
	}
	
	
	public HttpUriRequest createRequest(HttpRequestSettings settings) throws ParametersException, IOException
	{
		String method = settings.getMethod(),
				url = buildUrl(settings);
		
		HttpUriRequest request;
		if (METHOD_GET.equalsIgnoreCase(method))
			request = new HttpGet(url);
		else if (METHOD_POST.equalsIgnoreCase(method))
			request = new HttpPost(url);
		else if (METHOD_PUT.equalsIgnoreCase(method))
			request = new HttpPut(url);
		else if (METHOD_DELETE.equalsIgnoreCase(method))
			request = new HttpDeleteWithBody(url);
		else
			throw new ParametersException("Unknown HTTP method: '"+method+"'");
		getLogger().trace("Request: {}", request);
		
		setRequestHeaders(request, settings);
		setRequestParameters(request, settings);
		setRequestBody(request, settings);
		return request;
	}
	
	
	public String buildUrl(HttpRequestSettings settings) throws ParametersException
	{
		try
		{
			return HttpUtils.buildUrl(settings);
		}
		catch (URISyntaxException e)
		{
			throw new ParametersException("Unable to build valid URL from the specified settings: " + e.getMessage(), e);
		}
	}
	
	public void setRequestHeaders(HttpUriRequest request, HttpRequestSettings settings)
	{
		Map<String, String> headers = settings.getHeaders();
		if ((headers == null) || (headers.isEmpty()))
			return;
		
		for (Entry<String, String> h : headers.entrySet())
		{
			String name = h.getKey();
			String value = h.getValue();
			request.setHeader(h.getKey(), h.getValue());
			getLogger().trace("Header: '{}={}'", name, value);
		}
	}
	
	public void setRequestParameters(HttpUriRequest request, HttpRequestSettings settings) throws UnsupportedEncodingException
	{
		Map<String, String> params = settings.getParameters();
		//If body is defined, it will replace request parameters. GET and DELETE already have parameters applied in their URL
		if ((params == null) || (params.isEmpty()) || (settings.getBody() != null) || (!(request instanceof HttpEntityEnclosingRequest)))
			return;
		
		List<NameValuePair> reqParams = new ArrayList<NameValuePair>();
		for (Entry<String, String> p : params.entrySet())
		{
			String name = p.getKey();
			String value = p.getValue();
			reqParams.add(new BasicNameValuePair(p.getKey(), p.getValue()));
			getLogger().trace("Parameter: '{}={}'", name, value);
		}
		((HttpEntityEnclosingRequest)request).setEntity(new UrlEncodedFormEntity(reqParams, "UTF-8"));
	}
	
	public void setRequestBody(HttpUriRequest request, HttpRequestSettings settings) throws UnsupportedEncodingException
	{
		//GET doesn't support request body
		if ((settings.getBody() == null) || (!(request instanceof HttpEntityEnclosingRequest)))
			return;
		
		((HttpEntityEnclosingRequest)request).setEntity(new ByteArrayEntity(settings.getBody().getBytes("UTF-8")));
		
		getLogger().trace("Body:{}{}", Utils.EOL, settings.getBody());
	}
}
