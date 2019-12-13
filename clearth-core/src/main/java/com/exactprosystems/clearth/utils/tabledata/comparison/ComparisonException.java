/******************************************************************************
 * Copyright 2009-2020 Exactpro Systems Limited
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

package com.exactprosystems.clearth.utils.tabledata.comparison;

public class ComparisonException extends Exception
{
	private static final long serialVersionUID = 6943493622785056997L;
	
	public ComparisonException() { }

	public ComparisonException(String message)
	{
		super(message);
	}

	public ComparisonException(Throwable cause)
	{
		super(cause);
	}

	public ComparisonException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
