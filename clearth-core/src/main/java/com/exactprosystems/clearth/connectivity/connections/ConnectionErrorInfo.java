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

package com.exactprosystems.clearth.connectivity.connections;

import java.time.Instant;
import java.util.Date;

public class ConnectionErrorInfo
{
	private final String connectionName,
			errorText;
	private final Instant occurred;

	public ConnectionErrorInfo(String connectionName, String errorText, Instant occurred)
	{
		this.connectionName = connectionName;
		this.errorText = errorText;
		this.occurred = occurred;
	}
	
	public String getConnectionName()
	{
		return connectionName;
	}
	
	public String getErrorText()
	{
		return errorText;
	}

	public Instant getOccurred()
	{
		return occurred;
	}
	
	public Date getOccurredDate()
	{
		return new Date(occurred.toEpochMilli());
	}
}
