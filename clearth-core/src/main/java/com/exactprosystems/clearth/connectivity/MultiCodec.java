/******************************************************************************
 * Copyright 2009-2021 Exactpro Systems Limited
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

package com.exactprosystems.clearth.connectivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.lang.NotImplementedException;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.ICodec;
import com.exactprosystems.clearth.utils.ExceptionUtils;
import com.exactprosystems.clearth.utils.LineBuilder;
import com.exactprosystems.clearth.utils.SettingsException;

/**
 * MultiCodec is useful to decode messages that belong to unknown protocol from given list. 
 * While decoding a message, MultiCodec will cycle through available codecs, trying to find the one that is able to parse the message.
 * Encoding is not supported by MultiCodec as it doesn't know which codec to use
 */
public class MultiCodec implements ICodec
{
	private final Collection<ICodec> codecs;
	
	public MultiCodec(String... codecNames) throws SettingsException
	{
		this(Arrays.asList(codecNames));
	}
	
	public MultiCodec(Collection<String> codecNames) throws SettingsException
	{
		codecs = new ArrayList<>();
		ClearThCore core = ClearThCore.getInstance();
		for (String cn : codecNames)
			codecs.add(core.createCodec(cn));
	}
	
	
	@Override
	public String encode(ClearThMessage<?> message) throws EncodeException
	{
		throw new NotImplementedException("MultiCodec cannot encode messages");
	}
	
	@Override
	public ClearThMessage<?> decode(String message) throws DecodeException
	{
		LineBuilder errors = null;
		for (ICodec codec : codecs)
		{
			try
			{
				return codec.decode(message);
			}
			catch (Exception e)
			{
				if (errors == null)
					errors = new LineBuilder();
				errors.append(ExceptionUtils.getDetailedMessage(e));
			}
		}
		throw new DecodeException("Could not decode message. Errors from codecs: \r\n"+errors.toString());
	}
	
	@Override
	public ClearThMessage<?> decode(String message, String type) throws DecodeException
	{
		LineBuilder errors = null;
		for (ICodec codec : codecs)
		{
			try
			{
				return codec.decode(message, type);
			}
			catch (Exception e)
			{
				if (errors == null)
					errors = new LineBuilder();
				errors.append(ExceptionUtils.getDetailedMessage(e));
			}
		}
		throw new DecodeException("Could not decode message with type '"+type+"'. Errors from codecs: \r\n"+errors.toString());
	}
}
