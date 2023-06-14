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

package com.exactprosystems.clearth.connectivity.remotehand;

import java.io.UnsupportedEncodingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.utils.Utils;

public class RhResponse
{
	private static final Logger logger = LoggerFactory.getLogger(RhResponse.class);
	
	private final int code;
	private final byte[] data;
	private String dataString = null;
	
	public RhResponse(int code, byte[] data)
	{
		this.code = code;
		this.data = data;
	}
	
	public RhResponse(int code, String data)
	{
		byte[] bytes;
		try
		{
			bytes = data.getBytes(Utils.UTF8);
		}
		catch (UnsupportedEncodingException e)
		{
			logger.error("Could not convert string to bytes", e);
			bytes = new byte[0];
		}
		
		this.code = code;
		this.data = bytes;
		this.dataString = data;
	}
	
	
	public int getCode()
	{
		return code;
	}
	
	public byte[] getData()
	{
		return data;
	}
	
	public String getDataString()
	{
		if (dataString == null)
		{
			try
			{
				dataString = new String(data, Utils.UTF8);
			}
			catch (UnsupportedEncodingException e)
			{
				logger.error("Could not convert data to string", e);
				dataString = "";
			}
		}
		
		return dataString;
	}
}
