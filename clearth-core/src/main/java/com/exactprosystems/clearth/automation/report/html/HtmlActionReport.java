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

package com.exactprosystems.clearth.automation.report.html;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.report.ActionReport;
import com.exactprosystems.clearth.automation.report.ReportStatus;
import com.exactprosystems.clearth.automation.report.html.template.*;
import freemarker.template.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public class HtmlActionReport extends ActionReport
{
	private static final Logger log = LoggerFactory.getLogger(HtmlActionReport.class);
	
	protected Logger getLogger()
	{
		return log;
	}

	protected boolean isStatusExpanded(ReportStatus status)
	{
		if (status.isPassed())
			return false;

		switch (status.getFailReason())
		{
			case CALCULATION : return true;
			case COMPARISON : return false;
			case EXCEPTION : return true;
			default : return true;
		}
	}

	protected Map<String, Object> getParameters(Action action, String containerId)
	{
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("action", action);
		ReportStatus status = new ReportStatus(action);
		parameters.put("status", status);
		parameters.put("containerId", containerId);
		parameters.put("statusExpanded", isStatusExpanded(status));
		parameters.put("comparisonUtils", ClearThCore.comparisonUtils());
		return parameters;
	}

	public void write(Writer out, Action action, String containerId, File actionsReportsDir, boolean onlyFailed) throws IOException
	{
		try
		{
			ClearThCore.getInstance().getReportTemplatesProcessor().processTemplate(out, getParameters(action, containerId), ReportTemplateFiles.ACTION);
		}
		catch (TemplateException e)
		{
			handleTemplateError(e, out);
		}
	}
	
	protected void handleTemplateError(TemplateException e, Writer out) throws IOException
	{
		getLogger().error("An error occurred while processing the template of the action: ", e);
		out.append("<span class=\"error_message\">An error occurred while writing action data. Please check logs for details</span>");
	}
}
