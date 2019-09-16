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

package com.exactprosystems.clearth.messages;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import com.exactprosystems.clearth.utils.Utils;

public class StringFileMessageSource implements StringMessageSource 
{
	protected final File file;
	protected boolean fileProcessed;
	
	public StringFileMessageSource(File file)
	{
		this.file = file;
		fileProcessed = false;
	}
	
	@Override
	public String nextStringMessage() throws IOException 
	{
		if (fileProcessed)
			return null;
		String message = readMessage();
		fileProcessed = true;
		return message;
	}

	protected String readMessage() throws IOException
	{
		return FileUtils.readFileToString(file, Utils.UTF8);
	}
	
	@Override
	public void removeMessage() 
	{
		// This operation is not supported for StringFileMessageSource
	}

}
