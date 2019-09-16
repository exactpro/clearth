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
import com.exactprosystems.clearth.connectivity.iface.ICodecFactory;
import com.exactprosystems.clearth.converters.ScriptConverter;
import com.exactprosystems.clearth.xmldata.XmlScriptConverterConfig;

import java.util.Map;

/**
 * Created by alexander.magomedov on 11/7/16.
 */
public class ScriptToMessageTool
{
	protected CodecsStorage codecs;
	protected ScriptConverter scriptConverter;
	protected Map<String, XmlScriptConverterConfig> scriptConverterConfigs;
	protected ICodecFactory codecFactory;
	
	public ScriptToMessageTool()
	{
		ClearThCore cthInstance = ClearThCore.getInstance();

		scriptConverter = cthInstance.getToolsFactory().createScriptConverter();
		codecs = cthInstance.getCodecs();
		scriptConverterConfigs = cthInstance.getScriptConverterConfigs();
		codecFactory = cthInstance.getCodecFactory();
	}
	
	public String convertScript(String scriptToConvert) throws Exception
	{
		return scriptConverter.convert(scriptToConvert, scriptConverterConfigs, codecFactory, codecs);
	}

	public CodecsStorage getCodecs()
	{
		return codecs;
	}

	public Map<String, XmlScriptConverterConfig> getScriptConverterConfigs()
	{
		return scriptConverterConfigs;
	}
}
