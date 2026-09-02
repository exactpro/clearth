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

package com.exactprosystems.clearth.woodpecker.utils;

import org.slf4j.Logger;

import static java.lang.String.format;

/**
 * 12 October 2018
 */
public class WoodpeckerExceptionUtils
{
	/**
	 * Helper method to prevent situation when we catch an exception but forget to pass it to logger method.
	 * @param logger slf4j logger
	 * @param error exception to log
	 * @param messageTemplate template for String.format
	 * @param templateArgs template arguments for String.format
	 */
	public static void logError(Logger logger,
	                            Throwable error,
	                            String messageTemplate,
	                            Object... templateArgs)
	{
		logger.error(format(messageTemplate, templateArgs), error);
	}
}
