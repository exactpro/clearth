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

import com.exactprosystems.clearth.BasicTestNgTest;
import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.MatrixFunctions;
import com.exactprosystems.clearth.connectivity.ConnectivityException;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.ICodec;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.client.methods.HttpUriRequest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.exactprosystems.clearth.utils.CollectionUtils.map;
import static com.exactprosystems.clearth.utils.FileOperationUtils.resourceToAbsoluteFilePath;
import static com.exactprosystems.clearth.utils.Utils.EOL;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class ClearThUriRequestBuilderTest extends BasicTestNgTest
{
	private static final String HTTP_DICTIONARY_PATH = "cfg/dicts/http.xml";
	private static final String ROOT_URL = "http://localhost:9090/";
	private static final String UUID = "123-4567-890";
	
	private HttpDictionary dictionary;
	private MatrixFunctions functions;


	@Override
	protected void mockOtherApplicationFields(ClearThCore application) throws Exception
	{
		ICodec codec = mock(ICodec.class);
		when(codec.encode(any(ClearThMessage.class))).thenReturn("encodedMessage");
		doReturn(codec).when(application).createCodec(anyString());
	}

	@Override
	protected void mockOtherBeforeClass() throws Exception
	{
		dictionary = new HttpDictionary(resourceToAbsoluteFilePath(HTTP_DICTIONARY_PATH));
		MatrixFunctions realFunctions = new MatrixFunctions(emptyMap(), null, null, true, null);
		functions = spy(realFunctions);
		when(functions.uuid()).thenReturn(UUID);
	}
	
	@DataProvider(name = "forCheckBuildUrl")
	public Object[][] createParamsToCheckBuildUrl()
	{
		return new Object[][]
				{
						// requestType, parameters, expectedUrl
						{
								"DefaultPathParamsRequest",
								singletonMap("InputParam", "0001"),
								"http://localhost:9090/application/0001/EUR/"+UUID+"/0001/EUR"
						},
						{
								"DefaultQueryParamsRequest",
								singletonMap("InputParam", "0001"),
								"http://localhost:9090/some-request?" +
										"input=0001&simple=EUR&function="+UUID+"&input-ref=0001&def-ref=EUR"
						},
						{
								"RequiredExpressionsRequest",
								map("RequiredParam", "123", "ConditionallyRequiredParam", "003"),
								"http://localhost:9090/some-request?" +
										"required=123&conditionally-required=003"
						},
						{
								"TextParametersValidationRequest",
								map("TextPathParam", "any_text", "PossibleValuePathParam", "yes",
										"TextQueryParam", "AnyText", "PossibleValueQueryParam", "NIL"),
								"http://localhost:9090/some-request/any_text/yes?" +
										"text-param=AnyText&possible-value-param=NIL"
						},
						{
								"IntegerParametersValidationRequest",
								map("SimpleParam", "1234", "MinValueParam", "100",
										"MaxValueParam", "9", "InRangeParam", "-10"),
								"http://localhost:9090/some-request/1234/100?" +
										"max-value-param=9&in-range-param=-10"
						},
						{
								"IntegerParametersValidationRequest",
								map("SimpleParam", "1582829001517", "MinValueParam", "45",
										"MaxValueParam", "900", "InRangeParam", "-100"),
								"http://localhost:9090/some-request/1582829001517/45?" +
										"max-value-param=900&in-range-param=-100"
						},
						{
								"DecimalParametersValidationRequest",
								map("SimpleParam", "12.89", "PrecisionParam", "0.25", "InRangeParam", "6.45"),
								"http://localhost:9090/some-request/12.89?" +
										"precision-param=0.25&in-range-param=6.45"
						},
						{
								"DecimalParametersValidationRequest",
								map("SimpleParam", "12345", "PrecisionParam", "0.125", "InRangeParam", "999.99"),
								"http://localhost:9090/some-request/12345?" +
										"precision-param=0.125&in-range-param=999.99"
						},
						{
								"TimestampParametersValidationRequest",
								map("DateParam", "20-03-09", "TimeParam", "12:34:56"),
								"http://localhost:9090/some-request/20-03-09?" +
										"time-param=12%3A34%3A56"
						},
						{
								"DuplicateQueryParamsRequest",
								singletonMap("InputParam", "0001,0002"),
								"http://localhost:9090/some-request?input=0001&input=0002&input-ref=0001&input-ref=0002"
						}
				};
	}
	
	@DataProvider(name = "forCheckErrors")
	public Object[][] createParamsToCheckErrors()
	{
		return new Object[][]
				{
						//requestType, parameters, expectedErrorMessage
						{
								"DefaultValueErrorsRequest",
								emptyMap(),
								"Unable to set parameters to HTTP request." + EOL +
								"The following required parameters are missing: 'MissedPathParam', 'MissedRequiredQueryParam'." + EOL +
								"defaultValue = @{MissedPathParam} for parameter 'NullPathParam' evaluated to NULL." + EOL +
								"defaultValue = @{MissedOptionalQueryParam} for parameter 'NullQueryParam' evaluated to NULL."
						},
						{
								"MissedRequiredParamsRequest",
								singletonMap("InputParam", "yes"),
								"Unable to set parameters to HTTP request." + EOL +
								"The following required parameters are missing: 'RequiredParam', 'ConditionallyRequiredParam'."
						},
						{
								"RequiredNotBooleanErrorRequest",
								emptyMap(),
								"Unable to set parameters to HTTP request." + EOL +
								"Invalid value in required expression = 'no' for parameter 'RequiredNotBoolean': must be " +
								"a boolean value or expression."
						},
						{
								"RequiredExpNotBooleanErrorRequest",
								singletonMap("InputParam", "yes"),
								"Unable to set parameters to HTTP request." + EOL +
								"Required expression = @{InputParam} for parameter 'RequiredNotBoolean' " +
								"evaluated to 'yes', but boolean value is expected."
						},
						{
								"TextParametersValidationRequest",
								map("TextPathParam", "any_text", "PossibleValuePathParam", "ja",
										"TextQueryParam", "AnyText", "PossibleValueQueryParam", "DWP"),
								"Unable to set parameters to HTTP request." + EOL +
								"'PossibleValuePathParam' = 'ja' isn't in list [yes, no]." + EOL +
								"'PossibleValueQueryParam' = 'DWP' isn't in list [RCO, PCO, NIL]."
						},
						{
								"IntegerParametersValidationRequest",
								map("SimpleParam", "123.45", "MinValueParam", "43",
										"MaxValueParam", "9000", "InRangeParam", "101"),
								"Unable to set parameters to HTTP request." + EOL +
								"'SimpleParam' = '123.45' isn't valid integer number." + EOL +
								"'MinValueParam' = '43' is out of range [45, +Infinity]." + EOL +
								"'MaxValueParam' = '9000' is out of range [-Infinity, 900]." + EOL +
								"'InRangeParam' = '101' is out of range [-100, 100]."
						},
						{
								"DecimalParametersValidationRequest",
								map("SimpleParam", "123,45", "PrecisionParam", "45.0125",
										"InRangeParam", "1000.01"),
								"Unable to set parameters to HTTP request." + EOL +
								"'SimpleParam' = '123,45' isn't valid decimal number." + EOL +
								"'PrecisionParam' = '45.0125' has precision more than 3." + EOL +
								"'InRangeParam' = '1000.01' is out of range [0.0, 1000.0]."
						},
						{
								"TimestampParametersValidationRequest",
								map("DateParam", "20-20-20",
										"TimeParam", "99:99:99"),
								"Unable to set parameters to HTTP request." + EOL +
								"'DateParam' = '20-20-20' doesn't fit to format 'yy-MM-dd'." + EOL +
								"'TimeParam' = '99:99:99' doesn't fit to format 'HH:mm:ss'."
						},
						{
								"TimestampParametersValidationRequest",
								map("DateParam", "20-03-09 12:34:56",
										"TimeParam", "20-03-09 12:34:56"),
								"Unable to set parameters to HTTP request." + EOL +
								"'DateParam' = '20-03-09 12:34:56' doesn't fit to format 'yy-MM-dd'." + EOL +
								"'TimeParam' = '20-03-09 12:34:56' doesn't fit to format 'HH:mm:ss'."
						},
						{
								"MissedRequiredHeaderRequest",
								emptyMap(),
								"Unable to set headers to HTTP request." + EOL +
										"The following required headers are missing: 'MissedRequiredHeader'."
						},
						{
								"BodyAndBodyParametersRequest",
								map("RequestId", "123"),
								"The HTTP dictionary has body and bodyParameters tags for request, " +
										"please delete one of them in the dictionary."
						},
						{
							"MissedRequiredBodyParameterRequest",
								emptyMap(),
								"Unable to set parameters to HTTP request." + EOL +
										"The following required parameters are missing: 'MissedRequiredBodyParameter'."
						}
				};
	}

	@DataProvider(name = "forCheckExpressionErrors")
	public Object[][] createParamsToCheckExpressionErrors()
	{
		return new Object[][]
				{
						// requestType, expectedErrorSubstring
						{
								"ErrorInDefaultValueExpressionRequest",
								"Calculation of defaultValue = @{format(time(0), 'abcdefg')} " +
										"for parameter 'ErrorParam' failed:"
						},
						{
								"ErrorInRequiredExpressionRequest",
								"Calculation of required expression = @{format(time(0), 'abcdefg')} " +
										"for parameter 'ErrorParam' failed:"
						}
				};
	}

	@DataProvider(name = "forCheckHeaders")
	public Object[][] createParamsToCheckHeaders()
	{
		return new Object[][]
				{
						//requestType, parameters, expected headers parameters
						{
								"CheckHeadersRequest",
								map("OptionalHeader", "123", "HeaderWithRequestName", "ABC",
										"ContentTypeHeader", "application/json", "RequiredHeader", "001"),
								map("OptionalHeader", "123", "Request-Name", "ABC",
										"Content-Type", "application/json", "RequiredHeader", "001")
						},
						{
								"DefaultValueRequiredHeaderRequest",
								emptyMap(),
								map("DefaultValueHeader", "123", "Default Value Required Header", "ABC")
						},
						{
								"DefaultValueRequiredHeaderRequest",
								null,
								map("DefaultValueHeader", "123", "Default Value Required Header", "ABC")
						},
						{
								"DefaultValueRequiredHeaderRequest",
								map("DefaultValueHeader", "456", "DefaultValueRequiredHeader", "DEF"),
								map("DefaultValueHeader", "456", "Default Value Required Header", "DEF")
						},
						{
								"CheckBodyParametersRequest",
								map("RequiredParameter", "123"),
								map("Content-Type", "application/x-www-form-urlencoded")
						},
						{
								"CheckXmlBodyRequest",
								emptyMap(),
								map("Content-Type", "application/xml")
						},
						{
								"NullRequestParametersRequest",
								null,
								map("Content-Type", "application/xml")
						}
				};
	}

	@DataProvider(name = "forCheckBodyEncoding")
	public Object[][] createParamsToCheckBodyEncoding()
	{
		return new Object[][]
				{
						//requestType, parameters, expected encoded message
						{
								"DifferentValueTypesBodyParametersRequest",
								map("RequestId", "123", "StartTime", "12:34:56", "EndTime", "12:34:59",
										"Instrument", "instrument"),
								"request-id=123&start-time=12%3A34%3A56&end-time=12%3A34%3A59&instrument=instrument"
						},
						{
								"CheckXmlBodyRequest",
								emptyMap(),
								"encodedMessage"
						},
						{
								"CheckBodyParametersRequest",
								map("RequiredParameter", "123", "ParameterWithRequestName", "ABC",
										"OptionalParameter", "456"),
								"RequiredParameter=123&Request-name=ABC&OptionalParameter=456"
						},
						{
								"DefaultValueRequiredBodyParameterRequest",
								emptyMap(),
								"DefaultValueParameter=123&Default-Value-Required-Body-Parameter=ABC"
						},
						{
								"DefaultValueRequiredBodyParameterRequest",
								map("DefaultValueParameter", "789", "DefaultValueRequiredParameter", "DEF"),
								"DefaultValueParameter=789&Default-Value-Required-Body-Parameter=DEF"
						},
				};
	}
	
	@Test(dataProvider = "forCheckBuildUrl")
	public void checkBuildUrl(String requestType, Map<String, String> parameters, String expectedUrl) 
			throws ConnectivityException
	{
		ClearThUriRequestBuilder builder = new ClearThUriRequestBuilder(dictionary, requestType);
		HttpUriRequest httpUriRequest = builder.rootUrl(ROOT_URL)
				.requestFields(parameters)
				.usingFunctionsForExpressions(functions)
				.build();
		assertEquals(httpUriRequest.getRequestLine().getUri(), expectedUrl);
	}
	
	@Test(dataProvider = "forCheckErrors")
	public void checkErrors(String requestType, Map<String, String> parameters, String expectedErrorMessage)
			throws ConnectivityException
	{
		ClearThUriRequestBuilder builder = new ClearThUriRequestBuilder(dictionary, requestType);
		builder.rootUrl(ROOT_URL)
				.requestFields(parameters)
				.usingFunctionsForExpressions(functions);
		
		ConnectivityException exception = expectThrows(ConnectivityException.class, builder::build);
		assertEquals(exception.getMessage(), expectedErrorMessage);
	}

	@Test(dataProvider = "forCheckExpressionErrors")
	public void checkExpressionErrors(String requestType, String expectedErrorSubstring) throws ConnectivityException
	{
		ClearThUriRequestBuilder builder = new ClearThUriRequestBuilder(dictionary, requestType);
		builder.rootUrl(ROOT_URL)
				.usingFunctionsForExpressions(functions);

		ConnectivityException exception = expectThrows(ConnectivityException.class, builder::build);
		String actualMessage = exception.getMessage();
		assertNotNull(actualMessage);
		// We don't check full error text, because MVEL error message is too long.
		assertTrue(actualMessage.contains(expectedErrorSubstring), "Actual error message: " + actualMessage);
	}
	
	@Test
	public void checkErrorIfFunctionsIsNull() throws ConnectivityException
	{
		ClearThUriRequestBuilder builder = new ClearThUriRequestBuilder(dictionary, "DefaultQueryParamsRequest");
		builder.rootUrl(ROOT_URL);
		
		ConnectivityException exception = expectThrows(ConnectivityException.class, builder::build);
		assertEquals(exception.getMessage(), ClearThUriRequestBuilder.NULL_FUNCTIONS_ERROR_MESSAGE);
	}

	@Test(dataProvider = "forCheckHeaders")
	public void checkHeaders(String requestType, Map<String, String> parameters,
	                         Map<String, String> expectedHeaders)
			throws ConnectivityException
	{
		ClearThUriRequestBuilder builder = new ClearThUriRequestBuilder(dictionary, requestType);
		HttpUriRequest httpUriRequest = builder.rootUrl(ROOT_URL)
				.requestFields(parameters)
				.usingFunctionsForExpressions(functions)
				.build();

		Map<String, String> actualHeaders = new LinkedHashMap<>();
		for (Header header : httpUriRequest.getAllHeaders())
		{
			actualHeaders.put(header.getName(), header.getValue());
		}

		assertEquals(actualHeaders, expectedHeaders);
	}

	@Test(dataProvider = "forCheckBodyEncoding")
	public void checkBodyEncoding(String requestType, Map<String, String> parameters,
	                         String expectedEncodedBody)
			throws ConnectivityException, IOException
	{
		ClearThUriRequestBuilder builder = new ClearThUriRequestBuilder(dictionary, requestType);
		HttpUriRequest httpUriRequest = builder.rootUrl(ROOT_URL)
				.requestFields(parameters)
				.usingFunctionsForExpressions(functions)
				.build();

		String actualEncodedBody =
				(IOUtils.toString(((HttpEntityEnclosingRequest) httpUriRequest).getEntity().getContent(), UTF_8));

		assertEquals(actualEncodedBody, expectedEncodedBody);
	}
}
