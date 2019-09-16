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

package com.exactprosystems.clearth.tools;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.connectivity.CodecsStorage;
import com.exactprosystems.clearth.connectivity.DecodeException;
import com.exactprosystems.clearth.connectivity.iface.ICodec;
import com.exactprosystems.clearth.connectivity.iface.ICodecFactory;
import com.exactprosystems.clearth.converters.MessageConverter;
import com.exactprosystems.clearth.xmldata.XmlMessageConverterConfig;

import java.util.Map;

/**
 * Created by alexander.magomedov on 11/7/16.
 */
public class MessageToScriptTool
{
	protected final CodecsStorage codecs;
	protected final MessageConverter messageConverter;
	protected final Map<String, XmlMessageConverterConfig> messageConverterConfigs;
	protected final ICodecFactory codecFactory;
	
	public MessageToScriptTool()
	{
		ClearThCore cthInstance = ClearThCore.getInstance();

		messageConverter = cthInstance.getToolsFactory().createMessageConverter();
		codecs = cthInstance.getCodecs();
		messageConverterConfigs = cthInstance.getMessageConverterConfigs();
		codecFactory = cthInstance.getCodecFactory();
	}
	
	public String convertMessage(String messageToConvert, String messageConvertFormat) throws Exception
	{
		messageConverter.resetId();
		ICodec codec;

		if (messageConvertFormat.equals("auto"))
		{
			for (Map.Entry<String, XmlMessageConverterConfig> converterConfig : messageConverterConfigs.entrySet())
			{
				try
				{
					codec = codecFactory.createCodec(codecs.getCodecConfig(converterConfig.getValue().getCodec()));
					return messageConverter.convert(messageToConvert, converterConfig.getValue(), codec);
				}
				catch (Exception e)
				{
					// Just try next config
				}
			}

			throw new DecodeException("Unknown format");
		}
		else
		{
			codec = codecFactory.createCodec(codecs.getCodecConfig(messageConverterConfigs.get(messageConvertFormat).getCodec()));
			return messageConverter.convert(messageToConvert, messageConverterConfigs.get(messageConvertFormat), codec);
		}
	}

	public CodecsStorage getCodecs()
	{
		return codecs;
	}

	public Map<String, XmlMessageConverterConfig> getMessageConverterConfigs()
	{
		return messageConverterConfigs;
	}
}
