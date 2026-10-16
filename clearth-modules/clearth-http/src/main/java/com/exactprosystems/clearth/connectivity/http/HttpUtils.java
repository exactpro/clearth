/******************************************************************************
 * Copyright 2009-2025 Exactpro Systems Limited
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

import com.exactprosystems.clearth.automation.actions.http.HttpRequestSettings;
import com.exactprosystems.clearth.utils.CommaBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.exactprosystems.clearth.connectivity.http.ClearThHttpClient.METHOD_POST;
import static com.exactprosystems.clearth.connectivity.http.ClearThHttpClient.METHOD_PUT;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.collections4.MapUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

public class HttpUtils
{
	public static String buildUrl(String parentUrl, String url, String urlParameter)
	{
		StringBuilder completeUrl = new StringBuilder();
		if (parentUrl != null)
			completeUrl.append(parentUrl);
		completeUrl.append(url);
		if (urlParameter != null)
			completeUrl.append(urlParameter);
		return completeUrl.toString();
	}
	
	public static String buildUrl(HttpRequestSettings settings) throws URISyntaxException
	{
		Map<String, String> params = settings.getParameters();
		String method = settings.getMethod();
		//POST and PUT will use parameters in their contents, not in URL
		if (isEmpty(params) || (METHOD_POST.equalsIgnoreCase(method)) || (METHOD_PUT.equalsIgnoreCase(method)))
			return settings.getUrl();

		URIBuilder uriBuilder = new URIBuilder(URI.create(settings.getUrl()));
		addParameters(uriBuilder, settings);
		return uriBuilder.build().toString();
	}

	public static boolean allowBody(String method)
	{
		return method.equals(HttpPost.METHOD_NAME)
				|| method.equals(HttpPut.METHOD_NAME)
				|| method.equals(HttpPatch.METHOD_NAME)
				|| method.equals(HttpDelete.METHOD_NAME);
	}
	
	private static void addParameters(URIBuilder uriBuilder, HttpRequestSettings settings)
	{
		List<String> pathParametersNames = settings.getPathParameterNames();
		Map<String, String> params = settings.getParameters();
		if (settings.isNamedParameters())
		{
			// path + query parameters
			if (isNotEmpty(pathParametersNames))
			{
				addPathParameters(uriBuilder, pathParametersNames, params);
				addQueryParameters(uriBuilder, pathParametersNames, params);
			}
			else // query parameters
			{
				addQueryParameters(uriBuilder, null, params);
			}
		}
		else // path parameters
		{
			// explicitly specified or all
			Collection<String> names = isNotEmpty(pathParametersNames) ? pathParametersNames : params.keySet();
			addPathParameters(uriBuilder, names, params);
		}
	}

	public static void fillParamsFromAbsoluteUrl(URIBuilder builder, String absoluteUrl)
	{
		if (StringUtils.isBlank(absoluteUrl))
			throw new IllegalArgumentException("Url is not provided");
		int indexOfSchemaAndHostSeparator = absoluteUrl.indexOf("://");
		if (indexOfSchemaAndHostSeparator == -1)
			throw new IllegalArgumentException(String.format("Schema not found in absolute url '%s'", absoluteUrl));
		int indexOfHostAndPathSeparator = absoluteUrl.indexOf("/", indexOfSchemaAndHostSeparator + 3);
		boolean hasPath = indexOfHostAndPathSeparator != -1;
		
		String scheme = absoluteUrl.substring(0, indexOfSchemaAndHostSeparator);
		String host = hasPath ?
				absoluteUrl.substring(indexOfSchemaAndHostSeparator + 3, indexOfHostAndPathSeparator)
				: absoluteUrl.substring(indexOfSchemaAndHostSeparator + 3);
		String path = hasPath ? absoluteUrl.substring(indexOfHostAndPathSeparator + 1) : null;
		
		builder.setScheme(scheme);
		builder.setHost(host);
		builder.setPath(path);
	}
	
	private static void addPathParameters(URIBuilder uriBuilder, 
	                                      Collection<String> names,
	                                      Map<String, String> params)
	{
		CommaBuilder pathBuilder = new CommaBuilder("/");
		for (String name : names)
		{
			pathBuilder.append(params.get(name));
		}
		String path = pathBuilder.toString();		
		if (isNotEmpty(path))
		{
			String staticPart = uriBuilder.getPath();
			String fullPath = isNotEmpty(staticPart) ? staticPart + '/' + path : path;
			uriBuilder.setPath(fullPath);
		}
	}
	
	private static void addQueryParameters(URIBuilder uriBuilder,
	                                       Collection<String> toExclude,
	                                       Map<String, String> params)
	{
		for (Map.Entry<String, String> p : params.entrySet())
		{
			String name = p.getKey();
			if ((toExclude != null) && toExclude.contains(name))
				continue;
			String value = p.getValue();
			uriBuilder.addParameter(name, value);
		}
	}
}
