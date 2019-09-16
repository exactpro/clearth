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

package com.exactprosystems.clearth.utils;

import com.exactprosystems.clearth.connectivity.ConnectivityException;

@SuppressWarnings("serial")
public class DictionaryLoadException extends ConnectivityException 
{
	public DictionaryLoadException(String message)
	{
		super(message);
	}

	public DictionaryLoadException(Throwable cause)
	{
		super(cause);
	}

	public DictionaryLoadException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
