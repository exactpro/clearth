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

package com.exactprosystems.clearth.messages;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import com.exactprosystems.clearth.connectivity.DecodeException;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.ICodec;
import com.exactprosystems.clearth.connectivity.iface.SimpleClearThMessage;
import com.exactprosystems.clearth.utils.Utils;

/**
 * Class that gets one message from file, i.e. whole file is treated as one message
 * @author vladimir.panarin
 */
public class FileMessageSource implements MessageSource
{
	protected final File file;
	protected final ICodec codec;
	protected boolean fileProcessed;
	
	public FileMessageSource(File file, ICodec codec)
	{
		this.file = file;
		this.codec = codec;
		fileProcessed = false;
	}
	
	
	@Override
	public ClearThMessage<?> nextMessage() throws IOException
	{
		if (fileProcessed)
			return null;
		
		String message = readMessage();
		try
		{
			ClearThMessage<?> result = decodeMessage(message);
			fileProcessed = true;
			return result;
		}
		catch (DecodeException e)
		{
			throw new IOException("Could not decode message from file '"+file.getAbsolutePath()+"'", e);
		}
	}
	
	@Override
	public void removeMessage()
	{
		//This operation is not supported for FileMessageSource
	}
	
	@Override
	public void removeMessage(ClearThMessage<?> message)
	{
		//This operation is not supported for FileMessageSource
	}
	
	
	public File getFile()
	{
		return file;
	}
	
	
	protected String readMessage() throws IOException
	{
		return FileUtils.readFileToString(file, Utils.UTF8);
	}
	
	protected ClearThMessage<?> decodeMessage(String message) throws DecodeException
	{
		if (codec != null)
			return codec.decode(message);
		
		ClearThMessage<?> result = new SimpleClearThMessage();
		result.setEncodedMessage(message);
		return result;
	}
}
