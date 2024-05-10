/******************************************************************************
 * Copyright 2009-2024 Exactpro Systems Limited
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

import com.exactprosystems.clearth.connectivity.CodecsStorage;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.ICodec;
import com.exactprosystems.clearth.connectivity.iface.ICodecFactory;
import com.exactprosystems.clearth.xmldata.XmlCodecConfig;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

public class MessageParserTool
{
	public static final String AUTO_FORMAT = "auto";

	protected final CodecsStorage codecs;
	protected final ICodecFactory codecFactory;
	
	protected String textToParse;
	protected String codecName;
	protected ICodec codec;
	protected String parsedText;
	protected ClearThMessage<?> parsedMsg;
	protected Map<String, Exception> exceptionMap;

	public MessageParserTool(CodecsStorage codecs, ICodecFactory codecFactory)
	{
		this.codecs = codecs;
		this.codecFactory = codecFactory;
		
		codecName = "";
		codec = null;
		
		textToParse = "";
		parsedText = "";
		parsedMsg = null;
	}

	/**
	 * override method to parse by type separately, don't forget to add
	 * 'preferOutbounds' or 'ValueGenerator' etc. to codec if you need
	 */
	public void parseText(final String text, String textToParseFormat)
	{
		codecName = "";
		codec = null;
		
		parsedText = "";
		parsedMsg = null;
		textToParse = text;
		
		exceptionMap = new LinkedHashMap<String, Exception>();
		
		if (StringUtils.isEmpty(textToParse))
			return;

		if (textToParseFormat.equals(AUTO_FORMAT))
		{
			for (String format : codecs.getCodecNames())
			{
				parse(format);
				if (!StringUtils.isEmpty(parsedText))
				{
					codecName = format;
					break;
				}
			}
			if (StringUtils.isEmpty(parsedText))
				codec = null;
		}
		else
		{
			codecName = textToParseFormat;
			parse(textToParseFormat);
		}
	}
	
	protected void parse(String textToParseFormat)
	{	
		try
		{
			XmlCodecConfig config = codecs.getCodecConfig(textToParseFormat);
			codec = createCodec(config);
			
			parsedMsg = codec.decode(textToParse);
			parsedText = parsedMsg.toString();
		}
		catch (Exception e)
		{
			exceptionMap.put(textToParseFormat, e);
		}
	}
	
	/**
	 * Use other method of Codec Factory to create codec with specific parameters
	 * 
	 * @param config
	 *          config
	 * @return instance of ICodec
	 * @throws Exception
	 */
	protected ICodec createCodec(XmlCodecConfig config) throws Exception
	{
		return codecFactory.createCodec(config);
	}

	public String getTextToParse()
	{
		return textToParse;
	}

	public String getCodecName()
	{
		return codecName;
	}

	public ICodec getCodec()
	{
		return codec;
	}

	public String getParsedText()
	{
		return parsedText;
	}

	public ClearThMessage<?> getParsedMsg()
	{
		return parsedMsg;
	}

	public Map<String, Exception> getExceptionMap()
	{
		return exceptionMap;
	}

	public CodecsStorage getCodecs()
	{
		return codecs;
	}
}
