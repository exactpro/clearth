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

package com.exactprosystems.clearth.converters;

import java.util.ArrayList;
import java.util.List;

import com.exactprosystems.clearth.utils.Utils;

/**
 * Default MessageSplitter implementation that is used by DefaultMessageConverter.
 * Splits messages by empty line. If messages are XMLs, they can be united if look like not separate ones
 * @author vladimir.panarin
 */
public class DefaultMessageSplitter implements MessageSplitter
{
	@Override
	public List<String> splitMessages(String messages)
	{
		String splitter = getSplitterString();
		String[] split = messages.split(splitter);
		
		List<String> result = new ArrayList<String>();
		StringBuilder sb = null;
		for (String s : split)
		{
			if (isNewMessage(s))
			{
				if (sb != null)
					result.add(sb.toString());
				
				sb = new StringBuilder();
				sb.append(s);
			}
			else
			{
				if (sb == null)
					sb = new StringBuilder();
				else
					sb.append(splitter);
				sb.append(s);
			}
		}
		
		if (sb != null)
			result.add(sb.toString());
		return result;
	}
	
	
	protected String getSplitterString()
	{
		return Utils.EOL + Utils.EOL;
	}
	
	
	/**
	 * Checks if given text is a part of new message or if it should be added to previous part
	 * @param part text to check
	 * @return true if given text can be treated as a new message part
	 */
	protected boolean isNewMessage(String part)
	{
		return (part.trim().startsWith("<?xml"));
	}
}
