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

package com.exactprosystems.clearth.connectivity.remotehand.data;

public enum RhResponseCode
{
	SUCCESS(0),
	COMPILE_ERROR(1),
	EXECUTION_ERROR(2),
	TOOL_BUSY(3),
	INCORRECT_REQUEST(4),
	RH_ERROR(5),
	UNKNOWN(-1);
	
	private final int code;
	
	RhResponseCode(int code)
	{
		this.code = code;
	}
	
	public int getCode()
	{
		return code;
	}
	
	public static RhResponseCode byCode(int code)
	{
		for (RhResponseCode error : values())
		{
			if (code == error.code)
				return error;
		}
		return UNKNOWN;
	}
}
