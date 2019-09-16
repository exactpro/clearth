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

package com.exactprosystems.clearth.web.beans.tools;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.connectivity.DecodeException;
import com.exactprosystems.clearth.tools.ScriptToMessageTool;
import com.exactprosystems.clearth.utils.ExceptionUtils;
import com.exactprosystems.clearth.web.beans.ClearThBean;
import com.exactprosystems.clearth.web.misc.MessageUtils;
import com.exactprosystems.clearth.xmldata.XmlScriptConverterConfig;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Map;

/**
 * Created by alexander.magomedov on 10/31/16.
 */
public class ScriptToMessageToolBean extends ClearThBean
{
	protected ScriptToMessageTool scriptToMessageTool;
	protected Map<String, XmlScriptConverterConfig> scriptConverterConfigs;
	protected String scriptToConvert;
	protected String convertedScript = "";
	
	@PostConstruct
	public void init()
	{
		scriptToMessageTool = ClearThCore.getInstance().getToolsFactory().createScriptToMessageTool();
		scriptConverterConfigs = scriptToMessageTool.getScriptConverterConfigs();
	}

	public void convertScript()
	{
		if (scriptToConvert == null || scriptToConvert.isEmpty())
		{
			MessageUtils.addErrorMessage("Nothing to convert!", "Script to convert is empty.");
			return;
		}
		convertedScript = "";
		try {
			convertedScript = scriptToMessageTool.convertScript(scriptToConvert);
		}
		catch (IOException e) {
			handleException("Error while reading input text", e);
		}
		catch (DecodeException e) {
			handleException("Could not decode script", e);
		}
		catch (Exception e) {
			handleException("Error while converting script!", e);
		}
	}

	protected void handleException(String message, Exception e) {
		getLogger().warn(message, e);
		MessageUtils.addErrorMessage(message, ExceptionUtils.getDetailedMessage(e));
	}

	public boolean isScriptConvertersAvailable()
	{
		return scriptConverterConfigs.size() > 0;
	}

	public String getScriptToConvert()
	{
		return this.scriptToConvert;
	}

	public void setScriptToConvert(String scriptToConvert)
	{
		this.scriptToConvert = scriptToConvert;
	}

	public String getConvertedScript()
	{
		return this.convertedScript;
	}
}
