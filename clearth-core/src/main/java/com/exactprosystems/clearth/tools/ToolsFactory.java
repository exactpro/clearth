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

package com.exactprosystems.clearth.tools;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.report.html.ReportParser;
import com.exactprosystems.clearth.converters.DefaultMessageConverter;
import com.exactprosystems.clearth.converters.DefaultScriptConverter;
import com.exactprosystems.clearth.converters.MessageConverter;
import com.exactprosystems.clearth.converters.ScriptConverter;
import com.exactprosystems.clearth.tools.datacomparator.DataComparatorTool;

public class ToolsFactory
{
	public CollectorScannerTool createCollectorScannerTool()
	{
		return new CollectorScannerTool();
	}
	
	public ConfigMakerTool createConfigMakerTool()
	{
		return new ConfigMakerTool();
	}
	
	public ExpressionCalculatorTool createExpressionCalculatorTool()
	{
		return new ExpressionCalculatorTool(ClearThCore.getInstance().getMvelVariablesFactory().create().getVariables());
	}
	
	public MessageParserTool createMessageParserTool()
	{
		return new MessageParserTool();
	}
	
	public MessageToScriptTool createMessageToScriptTool()
	{
		return new MessageToScriptTool();
	}
	
	public ScriptToMessageTool createScriptToMessageTool()
	{
		return new ScriptToMessageTool();
	}

	public MessageConverter createMessageConverter() {
		return new DefaultMessageConverter();
	}

	public ScriptConverter createScriptConverter()
	{
		return new DefaultScriptConverter();
	}

	public ReportParser createReportParser()
	{
		return new ReportParser();
	}

	public DictionaryValidatorTool createDictionaryValidatorTool()
	{
		return new DictionaryValidatorTool();
	}
	
	public DataComparatorTool createDataComparatorTool()
	{
		return new DataComparatorTool();
	}
}
