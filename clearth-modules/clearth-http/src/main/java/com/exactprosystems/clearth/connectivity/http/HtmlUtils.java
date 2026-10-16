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

package com.exactprosystems.clearth.connectivity.http;

public class HtmlUtils
{
	public static String getInputTag(String inputType, String inputName, String body)
	{
		int start = -1;
		do
		{
			start = body.indexOf("<input", start);
			if (start < 0)
				return null;

			int end = body.indexOf("/>", start);
			if (end < 0)
				return null;

			String inputTag = body.substring(start, end + 2);
			if ((inputTag.indexOf(" type=\"" + inputType + "\"") < 0) || (inputTag.indexOf(" name=\"" + inputName + "\"") < 0))
			{
				start = end;
				continue;
			}

			return inputTag;
		}
		while (start < body.length());

		return null;
	}

	public static String getInputTagProperty(String propertyName, String inputTag)
	{
		int start = inputTag.indexOf(" " + propertyName + "=");
		if (start < 0)
			return null;

		start = inputTag.indexOf("\"", start);
		start++;
		int end = inputTag.indexOf("\"", start);
		return inputTag.substring(start, end);
	}

	public static String getInputTagProperty(String inputType, String inputName, String propertyName, String body)
	{
		String inputTag = getInputTag(inputType, inputName, body);
		if (inputTag == null)
			return null;

		return getInputTagProperty(propertyName, inputTag);
	}

	public static String getHiddenValue(String inputName, String body)
	{
		String inputTag = getInputTag("hidden", inputName, body);
		if (inputTag == null)
			return null;

		return getInputTagProperty("value", inputTag);
	}

	public static String getValueFromURI(String uri, String paramName)
	{
		int start = uri.indexOf("&" + paramName + "=");
		if (start < 0)
		{
			start = uri.indexOf("?" + paramName + "=");
			if (start < 0)
				return "";
		}
		start += paramName.length() + 2; // 2 = "?".length()+"=".length()
		int end = uri.indexOf("&", start);
		if (end < 0)
			end = uri.length();
		return uri.substring(start, end);
	}
}
