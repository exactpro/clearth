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

package com.exactprosystems.clearth.connectivity.iface;

import org.apache.commons.lang3.StringUtils;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.xmldata.XmlCodecConfig;

public class DefaultCodecFactory implements ICodecFactory
{
	public ICodec createCodec(XmlCodecConfig config) throws Exception
	{
		if (StringUtils.isEmpty(config.getDictionaryFile()))
			return (ICodec) Class.forName(config.getCodec()).getDeclaredConstructor().newInstance();
		
		String xmlFile = ClearThCore.getInstance().getDictsPath()+config.getDictionaryFile();
		
		Object dictionary = Class.forName(config.getDictionary()).getDeclaredConstructor(String.class).newInstance(xmlFile);
		
		return (ICodec) Class.forName(config.getCodec()).getDeclaredConstructor(dictionary.getClass()).newInstance(dictionary);
	}
}
