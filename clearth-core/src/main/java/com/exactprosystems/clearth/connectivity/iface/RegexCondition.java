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

package com.exactprosystems.clearth.connectivity.iface;

import java.util.regex.Pattern;

import static java.lang.String.format;

/**
 * 11 March 2019
 */
public class RegexCondition implements MessageValidatorCondition
{
	public static final String ERROR_FOR_CONTAINS = "Message does not contain text to match regexp: '%s'",
			ERROR_FOR_NOT_CONTAINS = "Message unexpectedly contains text that matches regexp: '%s'";
	
	private final Pattern pattern;
	private final boolean invert;

	public RegexCondition(String regex, boolean invert)
	{
		this(Pattern.compile(regex), invert);
	}

	public RegexCondition(Pattern pattern, boolean invert)
	{
		this.pattern = pattern;
		this.invert = invert;
	}

	@Override
	public boolean check(String message)
	{
		boolean found = pattern.matcher(message).find();
		return found != invert;
	}

	@Override
	public String buildErrorMessage()
	{
		String errorTemplate = invert ? ERROR_FOR_NOT_CONTAINS : ERROR_FOR_CONTAINS;
		return format(errorTemplate, pattern.pattern());
	}
}
