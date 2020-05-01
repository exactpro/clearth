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

package com.exactprosystems.clearth.utils.exception;

import com.exactprosystems.clearth.utils.ExceptionUtils;

public class MatrixUploadHandlerException extends Exception
{
	private String details = null;


	public MatrixUploadHandlerException(String message)
	{
		super(message);
	}

	public MatrixUploadHandlerException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public MatrixUploadHandlerException(String message, String details)
	{
		super(message);
		this.details = details;
	}

	public String getDetails()
	{
		return details == null ? ExceptionUtils.getDetailedMessage(getCause()) : details;
	}
}
