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

package com.exactprosystems.clearth.utils.csv;

public enum ClearThQuoteMode
{
	/** Encloses each value in double quotes. */
	ALL,

	/** Encloses each value in double quotes, except for null-fields. */
	ALL_NON_NULL,

	/** Encloses in double quotes the values that contain special characters.
	 * For example, a delimiter, double quote or any other special character specified in the configuration. */
	MINIMAL,

	/** Encloses all non-numeric values in double quotes. */
	NON_NUMERIC,

	/** No values will be enclosed in double quotes.
	 * If there is a delimiter in the value, the writer will add an escape character to it.
	 * In this case, the escape character is mandatory to be set in configuration, else an exception will be thrown. */
	NONE
}
