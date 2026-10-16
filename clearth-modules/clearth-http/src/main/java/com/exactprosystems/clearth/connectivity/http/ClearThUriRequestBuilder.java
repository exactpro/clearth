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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import com.exactprosystems.clearth.automation.MatrixFunctions;
import com.exactprosystems.clearth.utils.LineBuilder;
import org.apache.commons.collections.CollectionUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.mvel2.templates.CompiledTemplate;
import org.mvel2.templates.TemplateCompiler;
import org.mvel2.templates.TemplateRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.MatrixContext;
import com.exactprosystems.clearth.connectivity.ConnectivityException;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.ICodec;
import com.exactprosystems.clearth.connectivity.iface.SimpleClearThMessageBuilder;
import com.exactprosystems.clearth.messages.MessageBuilder;
import com.exactprosystems.clearth.utils.CommaBuilder;
import com.exactprosystems.clearth.utils.Utils;

import static com.exactprosystems.clearth.utils.CollectionUtils.join;
import static com.exactprosystems.clearth.utils.ExceptionUtils.getDetailedMessage;
import static com.exactprosystems.clearth.utils.Utils.nvl;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyMap;
import static org.apache.commons.lang3.StringUtils.*;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;

public class ClearThUriRequestBuilder 
{	
	private static final Logger logger = LoggerFactory.getLogger(ClearThUriRequestBuilder.class);
	
	static final String NULL_FUNCTIONS_ERROR_MESSAGE = 
			"Parameters descriptions contain expressions, but functions aren't specified.";
	
	private final HttpRequestDesc requestDesc;
	private final Set<String> httpParametersNames;
	private final ICodec codec;
	
	private List<NameValuePair> pathParameters;
	private List<NameValuePair> queryParameters;
	private List<NameValuePair> bodyParameters;
	private String encodedBody;
	private MessageBuilder messageBuilder;
	
	private Set<String> parametersToIgnore;
	private String absoluteUrl;
	private String rootUrl = "";
	private Map<String, String> requestFields;	
	// Reference to missed variable causes Exception in MVEl. 
	// To calculate expressions like [required = "@{OtherParam == null}"] we need to put null values 
	// for all parameters defined in the dictionary but missed in requestFields to MVEL variables map.
	private Map<String, String> requestFieldsForMvel;
	private MatrixContext matrixContext;
	private Action action;
	private List<ClearThMessage> subMessages;
	private boolean allowBlankBody = false;
	private MatrixFunctions matrixFunctions;
	
	private List<String> missedRequiredParameters;
	private List<String> errorsInParameters;
	private List<String> missedRequiredHeaders;
	
	
	public ClearThUriRequestBuilder(HttpDictionary dictionary, String requestType) 
			throws ConnectivityException 
	{
		requestDesc = dictionary.findRequestDesc(requestType);
		if (requestDesc == null)
			throw new ConnectivityException(format("Could not find definition for the specified request type '%s'.", requestType));
		codec = dictionary.getCodec(requestDesc);
		httpParametersNames = dictionary.getHttpParametersNames(requestType);
	}
	
	public ClearThUriRequestBuilder rootUrl(String rootUrl) throws ConnectivityException 
	{
		if(isBlank(rootUrl))
			throw new ConnectivityException("If setting root URL, it should be non-empty string");
		this.rootUrl = rootUrl;
		return this;
	}

	public ClearThUriRequestBuilder absoluteUrl(String absoluteUrl) throws ConnectivityException
	{
		if(isBlank(absoluteUrl))
			throw new ConnectivityException("If setting absolute URL, it should be non-empty string");
		this.absoluteUrl = absoluteUrl;
		return this;
	}

	public ClearThUriRequestBuilder requestFields(Map<String,String> requestFields) 
	{
		this.requestFields = requestFields;
		if (requestFields != null)
			addFieldsForMvel(requestFields);
		return this;
	}
	
	public ClearThUriRequestBuilder rgs(MatrixContext matrixContext, Action action) 
	{
		this.subMessages = null;
		
		this.matrixContext = matrixContext;
		this.action = action;
		return this;
	}
	
	public ClearThUriRequestBuilder rgs(List<ClearThMessage> subMessages) 
	{
		this.matrixContext = null;
		this.action = null;
		
		this.subMessages = subMessages;
		return this;
	}
	
	public ClearThUriRequestBuilder parametersToIgnore(Set<String> parametersToIgnore)
	{
		this.parametersToIgnore = parametersToIgnore;
		return this;
	}
	
	public ClearThUriRequestBuilder encodedBody(String encodedBody) throws ConnectivityException 
	{
		if(encodedBody == null || (isBlank(encodedBody) && (!allowBlankBody)))
			throw new ConnectivityException("If setting encoded body, it should be non-empty string");
		this.encodedBody = encodedBody;
		return this;
	}
	
	public ClearThUriRequestBuilder allowBlankBody(boolean allowBlankBody) 
	{
		this.allowBlankBody = allowBlankBody;
		return this;
	}
	
	public ClearThUriRequestBuilder messageBuilder(MessageBuilder messageBuilder) 
			throws ConnectivityException 
	{
		if(messageBuilder == null)
			throw new ConnectivityException("When setting Message Builder, it should not be a null reference");
		this.messageBuilder = messageBuilder;
		return this;
	}

	public ClearThUriRequestBuilder usingFunctionsForExpressions(MatrixFunctions matrixFunctions)
	{
		this.matrixFunctions = matrixFunctions;
		return this;
	}
	
	
	public final HttpUriRequest build() throws ConnectivityException
	{
		resolveParameters();
		checkForErrorsInParameters();
		
		if(encodedBody == null)
			encodeBody();
		
		URIBuilder builder = new URIBuilder();
		String completeUrl = absoluteUrl != null ? absoluteUrl : rootUrl + requestDesc.getUrl();
		HttpUtils.fillParamsFromAbsoluteUrl(builder, completeUrl);
		
		setPathParameters(builder);
		if(queryParameters != null && !queryParameters.isEmpty())
			builder.addParameters(queryParameters);
		
		HttpUriRequest request;
		try 
		{
			request = initByMethod(builder.build());
		} 
		catch (URISyntaxException e)
		{
			throw new ConnectivityException("Could not build URL", e);
		}		
		setRequestHeaders(request);
		checkForErrorsInHeaders();
		setRequestBody(request);
		
		reset();
		return request;
	}
	
	private void setRequestBody(HttpUriRequest request)
	{
		if(encodedBody == null || (isBlank(encodedBody) && (!allowBlankBody)) || 
				(!(request instanceof HttpEntityEnclosingRequest)))
			return;
		
		((HttpEntityEnclosingRequest)request).setEntity(new StringEntity(encodedBody, UTF_8));
		logger.trace("Body:{}{}", Utils.EOL, encodedBody);
	}

	private void setRequestHeaders(HttpUriRequest request)
	{
		Set<String> headersNames = setRequestHeadersFromFields(request);

		if (headersNames.isEmpty() || !headersNames.contains(CONTENT_TYPE))
		{
			Header genH = generateContentTypeHeader();
			if (genH != null)
				request.setHeader(genH);
		}

		if (logger.isTraceEnabled())
		{
			for (Header addedHeader : request.getAllHeaders())
			{
				logger.trace("Header: '{}={}'", addedHeader.getName(), addedHeader.getValue());
			}
		}
	}

	private Set<String> setRequestHeadersFromFields(HttpUriRequest request)
	{
		Set<String> headersNames = new HashSet<>();
		if (requestDesc.getHeadersBlock() == null)
			return headersNames;

		for (HttpHeader h : requestDesc.getHeadersBlock().getHeaders())
		{
			String name = h.getName();
			String defaultValue = h.getValue();
			String requestName = h.getRequestName();
			boolean required = h.isRequired();

			if (isBlank(requestName))
				requestName = name;

			String value = (requestFields != null) ? requestFields.get(name) : null;
			if (isBlank(value))
			{
				if (isNotBlank(defaultValue))
					value = defaultValue;
				else 
				{
					if (required)
						addMissedRequiredHeader(name);
					continue;
				}
			}
			request.setHeader(requestName, value);
			headersNames.add(requestName);
		}
		return headersNames;
	}

	private void setPathParameters(URIBuilder uriBuilder)
	{
		if (CollectionUtils.isEmpty(pathParameters))
			return;
		
		CommaBuilder pathBuilder = new CommaBuilder("/");
		for (NameValuePair param : pathParameters)
		{
			pathBuilder.append(param.getValue());
		}
		String path = pathBuilder.toString();
		if (isNotEmpty(path))
		{
			String staticPart = uriBuilder.getPath();
			String fullPath = isNotEmpty(staticPart) ? staticPart + '/' + path : path;
			uriBuilder.setPath(fullPath);
		}
	}
	
	private HttpUriRequest initByMethod(URI url) throws ConnectivityException 
	{
		HttpUriRequest request;
		switch(requestDesc.getMethod())
		{
			case GET: 
				request = new HttpGet(url);
				break;
			case POST:
				request = new HttpPost(url);
				break;
			case PUT:
				request = new HttpPut(url);
				break;
			case DELETE:
				request = new HttpDeleteWithBody(url);
				break;
			default:
				throw new ConnectivityException(format("Unsupported HTTP method: %s", 
						requestDesc.getMethod()));
		}		
		logger.trace("Request: {}", request);
		return request;
	}

	private void resolveParameters() throws ConnectivityException
	{
		if (requestDesc.getPathParametersBlock() != null)
			pathParameters = resolveParametersFromDesc(requestDesc.getPathParametersBlock().getParameters(), true);

		if (requestDesc.getQueryParametersBlock() != null)
			queryParameters = resolveParametersFromDesc(requestDesc.getQueryParametersBlock().getParameters(), false);

		if(requestDesc.getBodyParametersBlock() != null)
			bodyParameters = resolveParametersFromDesc(requestDesc.getBodyParametersBlock().getParameters(), false);
	}

	private List<NameValuePair> resolveParametersFromDesc(List<HttpParameter> paramsDesc, boolean areAllParamsRequired) throws ConnectivityException
	{
		List<NameValuePair> parameters = new ArrayList<>();

		if (paramsDesc == null)
			return parameters;

		for (HttpParameter param : paramsDesc)
		{
			String name = param.getName();
			boolean isRequired;

			if (areAllParamsRequired)
				isRequired = true;
			else
				isRequired = isRequired(name, param.getRequiredExpression());

			String delimiter = param.getDelimiter();
			String value = resolveParameter(name, isRequired, param.getDefaultValue());
			if (isNotBlank(value))
			{
				if (isNotBlank(delimiter))
				{
					String[] valueParts = value.split(delimiter);
					for (String valuePart : valueParts)
					{
						addParameter(parameters, param, valuePart);
					}
				}
				else
				{
					addParameter(parameters, param, value);
				}
			}
		}

		return parameters;
	}

	private void addParameter(List<NameValuePair> parameters, HttpParameter param, String value)
	{
		String errorMessage = new HttpParameterValidator(param).check(value);
		if (errorMessage != null)
			addErrorInParameter(errorMessage);
		else
			parameters.add(new BasicNameValuePair(getParamRequestName(param), value));
	}

	private String resolveParameter(String name, boolean isRequired, String defaultValue) throws ConnectivityException
	{
		String value = (requestFields != null) ? requestFields.get(name) : null;
		if (isNotBlank(value))
			return value;

		if (isNotBlank(defaultValue))
			return calculateDefaultValue(name, defaultValue);
		
		if (isRequired)
			addMissedRequiredParameter(name);
		
		return null;
	}

	private String calculateDefaultValue(String paramName, String defaultValue) throws ConnectivityException
	{
		if (!containsExpression(defaultValue))
		{
			addFieldForMvel(paramName, defaultValue);
			return defaultValue;
		}
		
		checkIfFunctionsNotNull();
		
		Object resultObject;
		try
		{
			resultObject = calculateExpression(defaultValue);
		}
		catch (Exception e)
		{
			logger.warn("[{}] Calculation of defaultValue = {} for parameter '{}' failed.", 
					requestDesc.getType(), defaultValue, paramName, e);
			addErrorInParameter("Calculation of defaultValue = %s for parameter '%s' failed: %s",
					defaultValue, paramName, getDetailedMessage(e));
			return null;
		}
		
		if (resultObject != null)
		{
			String result = String.valueOf(resultObject);
			addFieldForMvel(paramName, result);
			return result;
		}
		else
		{
			addErrorInParameter("defaultValue = %s for parameter '%s' evaluated to NULL.",
					defaultValue, paramName);
			return null;
		}
	}

	private boolean isRequired(String paramName, String requiredExpression) throws ConnectivityException
	{
		if (isBooleanValue(requiredExpression))
			return Boolean.parseBoolean(requiredExpression);
		
		if (!containsExpression(requiredExpression))
		{
			addErrorInParameter("Invalid value in required expression = '%s' for parameter '%s': " +
					"must be a boolean value or expression.", requiredExpression, paramName);
			return false;
		}

		checkIfFunctionsNotNull();

		Object value;
		try
		{
			value = calculateExpression(requiredExpression);
		}
		catch (Exception e)
		{
			logger.warn("[{}] Calculation of required expression = {} for parameter '{}' failed.",
					requestDesc.getType(), requiredExpression, paramName, e);
			addErrorInParameter("Calculation of required expression = %s for parameter '%s' failed: " +
					"%s", requiredExpression, paramName, getDetailedMessage(e));
			return false;
		}

		if (value instanceof Boolean)
		{
			return (Boolean) value;
		}
		else
		{
			addErrorInParameter("Required expression = %s for parameter '%s' evaluated to '%s', " +
					"but boolean value is expected.", requiredExpression, paramName, value);
			return false;
		}
	}

	private boolean isBooleanValue(String value)
	{
		return TRUE.toString().equalsIgnoreCase(value) || FALSE.toString().equalsIgnoreCase(value);
	}

	private boolean containsExpression(String value)
	{
		return contains(value, MatrixFunctions.FORMULA_START);
	}
	
	private Object calculateExpression(String expression)
	{
		CompiledTemplate template = TemplateCompiler.compileTemplate(expression);
		return TemplateRuntime.execute(template, matrixFunctions, nvl(requestFieldsForMvel, emptyMap()));
	}
	
	private void checkIfFunctionsNotNull() throws ConnectivityException
	{
		if (matrixFunctions == null)
			throw new ConnectivityException(NULL_FUNCTIONS_ERROR_MESSAGE);
	}
	
	
	private void addFieldsForMvel(Map<String, String> requestFields)
	{
		HttpRequestDesc.PathParameters pathParamsBlock = requestDesc.getPathParametersBlock();
		if (pathParamsBlock != null)
		{
			addFieldsForMvelFromList(requestFields, pathParamsBlock.getParameters());
		}
		HttpQueryParametersBlock queryParamsBlock = requestDesc.getQueryParametersBlock();
		if (queryParamsBlock != null)
		{
			addFieldsForMvelFromList(requestFields, queryParamsBlock.getParameters());
		}
		HttpBodyParametersBlock bodyParamsBlock = requestDesc.getBodyParametersBlock();
		if (bodyParamsBlock != null)
		{
			addFieldsForMvelFromList(requestFields, bodyParamsBlock.getParameters());
		}
	}

	private void addFieldsForMvelFromList(Map<String, String> requestFields, List<HttpParameter> parameters)
	{
		for (HttpParameter param : parameters)
		{
			addFieldForMvel(param.getName(), requestFields.get(param.getName()));
		}
	}
	
	private void addFieldForMvel(String name, String value)
	{
		if (requestFieldsForMvel == null)
			requestFieldsForMvel = new HashMap<>();
		requestFieldsForMvel.put(name, value);
	}
	
	
	private void addMissedRequiredParameter(String paramName)
	{
		if (missedRequiredParameters == null)
			missedRequiredParameters = new ArrayList<>();
		missedRequiredParameters.add(paramName);
	}
	
	private void addErrorInParameter(String errorMessageTemplate, Object... templateArgs)
	{
		if (errorsInParameters == null)
			errorsInParameters = new ArrayList<>();
		errorsInParameters.add(format(errorMessageTemplate, templateArgs));
	}

	private void addMissedRequiredHeader(String headerName)
	{
		if (missedRequiredHeaders == null)
			missedRequiredHeaders = new ArrayList<>();
		missedRequiredHeaders.add(headerName);
	}
	
	private void checkForErrorsInParameters() throws ConnectivityException
	{
		if ((missedRequiredParameters == null) && (errorsInParameters == null))
			return;

		LineBuilder errorBuilder = new LineBuilder();
		errorBuilder.append("Unable to set parameters to HTTP request.");
		
		if (missedRequiredParameters != null)
			errorBuilder.append(format("The following required parameters are missing: %s.", 
					join(missedRequiredParameters)));
		
		if (errorsInParameters != null)
		{
			for (String error : errorsInParameters)
			{
				errorBuilder.append(error);
			}
		}
		throw new ConnectivityException(errorBuilder.toString().trim());
	}

	private void checkForErrorsInHeaders() throws ConnectivityException
	{
		if ((missedRequiredHeaders == null))
			return;

		LineBuilder errorBuilder = new LineBuilder();
		errorBuilder.append("Unable to set headers to HTTP request.");

		errorBuilder.add(format("The following required headers are missing: %s.",
				join(missedRequiredHeaders)));

		throw new ConnectivityException(errorBuilder.toString());
	}

	private String getParamRequestName(HttpParameter param)
	{
		return (isNotBlank(param.getRequestName())) ? param.getRequestName() : param.getName();
	}

	private void encodeBody() throws ConnectivityException
	{
		if (requestDesc.getBody() != null && requestDesc.getBodyParametersBlock() != null)
		{
			throw new ConnectivityException("The HTTP dictionary has body and bodyParameters tags for request, please" +
					" delete one of them in the dictionary.");
		}
		else if (requestDesc.getBody() != null)
		{
			encodeBodyByCodec();
		}
		else if(requestDesc.getBodyParametersBlock() != null)
		{
			encodeBodyByParameters();
		}
	}
	
	private void encodeBodyByCodec() throws ConnectivityException
	{
		if(messageBuilder == null)
			messageBuilder = new SimpleClearThMessageBuilder();
		
		if(requestFields != null)
		{
			Map<String, String> bodyFields = new HashMap<String, String>(requestFields);
			bodyFields.keySet().removeAll(httpParametersNames);
			if(parametersToIgnore != null)
				bodyFields.keySet().removeAll(parametersToIgnore);
			messageBuilder.fields(bodyFields);
		}
		
		if(matrixContext != null && action != null) 
			messageBuilder.rgs(matrixContext, action);
		else if(CollectionUtils.isNotEmpty(subMessages))
			messageBuilder.rgs(subMessages);
		
		String msgType;
		if(isBlank(msgType = requestDesc.getBody().getMsgType()))
			throw new ConnectivityException(format("Body message type is not set in the HTTP dictionary for request '%s'.", 
					requestDesc.getType()));
		
		ClearThMessage msg = messageBuilder
				.type(msgType)
				.build();
		if(codec == null)
			throw new ConnectivityException(format("Codec to encode body is not set in the HTTP dictionary for request '%s'.",
					requestDesc.getType()));
		encodedBody = codec.encode(msg);
	}

	private void encodeBodyByParameters()
	{
		if (bodyParameters != null)
			encodedBody = URLEncodedUtils.format(bodyParameters, UTF_8);
	}
	
	private Header generateContentTypeHeader() 
	{
		if(requestDesc.getBodyParametersBlock() != null)
			return new BasicHeader(CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.getMimeType());

		BodyType bodyType;
		if(requestDesc.getBody() == null || 
				(bodyType = requestDesc.getBody().getType()) == null)
			return null;
		
		String contentType = null;
		switch(bodyType)
		{
			case JSON: 
				contentType = ContentType.APPLICATION_JSON.getMimeType();
				break;
			case SWIFT:
				contentType = ContentType.TEXT_PLAIN.getMimeType();
				break;
			case XML:
				contentType = ContentType.APPLICATION_XML.getMimeType();
				break;
			default:
				break;
		}
		return (contentType != null) ? new BasicHeader(CONTENT_TYPE, contentType) : null;
	}
	
	
	private void reset()
	{
		pathParameters = null;
		queryParameters = null;
		bodyParameters = null;
		encodedBody = null;
		messageBuilder = null;
		parametersToIgnore = null;
		absoluteUrl = null;
		rootUrl = "";
		requestFields = null;
		requestFieldsForMvel = null;
		matrixContext = null;
		action = null;
		subMessages = null;
		allowBlankBody = false;
		matrixFunctions = null;
		missedRequiredParameters = null;
		errorsInParameters = null;
		missedRequiredHeaders = null;
	}
}
