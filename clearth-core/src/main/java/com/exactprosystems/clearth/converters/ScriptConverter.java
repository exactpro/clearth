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

import com.exactprosystems.clearth.automation.ActionGenerator;
import com.exactprosystems.clearth.connectivity.CodecsStorage;
import com.exactprosystems.clearth.connectivity.iface.ICodecFactory;
import com.exactprosystems.clearth.xmldata.XmlScriptConverterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public abstract class ScriptConverter extends Converter
{
	private static final Logger logger = LoggerFactory.getLogger(ScriptConverter.class);
	
	public abstract String convert(String scriptToConvert, Map<String, XmlScriptConverterConfig> scriptConverterConfigs,
								   ICodecFactory codecFactory, CodecsStorage codecsConfig) throws Exception;
	public abstract List<String> getIncludeList();
	
	
	protected String getAction(String[] header, String[] values)
	{
		for (int i = 0; i < header.length; i++)
			if (header[i].equalsIgnoreCase(ActionGenerator.HEADER_DELIMITER + ActionGenerator.COLUMN_ACTION))
				return values[i];
		return null;
	}
}
