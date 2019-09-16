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

package com.exactprosystems.clearth.connectivity.xml;

import com.exactprosystems.clearth.connectivity.iface.MessageValidatorCondition;

import java.util.regex.Pattern;

import static java.lang.String.format;

/**
 * 11 March 2019
 */
public class RootTagCondition implements MessageValidatorCondition
{
	private static final String REGEX_TEMPLATE = "\\A(<\\?(?i)xml(?-i).*>\\s*)?<rootTag.*>";
	
	private final String rootTag;
	private final Pattern pattern;

	public RootTagCondition(String rootTag)
	{
		this.rootTag = rootTag;
		pattern = Pattern.compile(REGEX_TEMPLATE.replace("rootTag", rootTag));
	}

	@Override
	public boolean check(String message)
	{
		return pattern.matcher(message).find();
	}

	@Override
	public String buildErrorMessage()
	{
		return format("Root tag of message isn't equal to '%s'.", rootTag);
	}
}
