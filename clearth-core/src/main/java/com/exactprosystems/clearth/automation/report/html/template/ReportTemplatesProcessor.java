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

package com.exactprosystems.clearth.automation.report.html.template;

import com.exactprosystems.clearth.automation.actions.macro.MacroAction;
import com.exactprosystems.clearth.automation.report.results.*;
import com.exactprosystems.clearth.automation.report.results.subclasses.DataTable;
import com.exactprosystems.clearth.automation.report.results.subclasses.TextLabel;
import com.exactprosystems.clearth.templates.TemplatesProcessor;
import com.exactprosystems.clearth.utils.ComparisonUtils;
import freemarker.template.Configuration;
import freemarker.template.TemplateModelException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ReportTemplatesProcessor extends TemplatesProcessor
{
	protected static final Map<String, Object> PARAMETERS = new HashMap<String, Object>()
	{{
		putAll(COMMON_PARAMETERS);
		put("ComplexResult", ComplexResult.class);
		put("DetailedResult", DetailedResult.class);
		put("MultiDetailedResult", MultiDetailedResult.class);
		put("RichTextResult", RichTextResult.class);
		put("DataTable", DataTable.class);
		put("TextLabel", TextLabel.class);
		put("TableResult", TableResult.class);
		put("ContainerResult", ContainerResult.class);
		put("MacroAction", MacroAction.class);
		put("AttachedFilesResult", AttachedFilesResult.class);
		put("CsvDetailedResult", CsvDetailedResult.class);
	}};

	public ReportTemplatesProcessor() throws IOException, TemplateModelException
	{
		super();
	}

	@Override
	protected Map<String, Object> getParameters()
	{
		return PARAMETERS;
	}

	@Override
	public void addParameter(String key, Object value)
	{
		PARAMETERS.put(key, value);
	}

	@Override
	protected Configuration createConfiguration() throws IOException, TemplateModelException
	{
		Configuration configuration = super.createConfiguration();
		configuration.setSharedVariable("node", new NodeViewDirective());
		configuration.setSharedVariable("specialCompValues", ComparisonUtils.SPECIAL_VALUES);
		return configuration;
	}
}
