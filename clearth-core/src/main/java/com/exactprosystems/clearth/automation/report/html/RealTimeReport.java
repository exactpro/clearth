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
import com.exactprosystems.clearth.automation.Matrix;
import com.exactprosystems.clearth.automation.Step;
import com.exactprosystems.clearth.automation.report.ReportException;
import com.exactprosystems.clearth.automation.report.html.template.ReportTemplateFiles;
import freemarker.template.TemplateException;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class RealTimeReport extends HtmlReport
{
	private StringWriter htmlContent = null;

	public RealTimeReport(Matrix matrix, String userName, String reportName, Date startTime, Date endTime) throws IOException
	{
		this.matrix = matrix;
		this.userName = userName;
		this.scriptName = matrix.getName();
		this.testPassed = matrix.isSuccessful();

		if (startTime!=null)
			execStart = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(startTime);
		else
			execStart = "";

		if ((endTime!=null) && (startTime!=null))
			execTime = Double.toString((endTime.getTime()-startTime.getTime())/1000.0)+" sec";
		else
			execTime = "";
		htmlContent = new StringWriter();
	}
	
	@Override
	public void writeReport(List<Step> allSteps, List<String> matrixSteps, File actionsReportsDir, boolean onlyFailed) throws IOException, ReportException
	{
		try
		{
			Map<String, Object> parameters = initTemplateParameters(allSteps, matrixSteps, actionsReportsDir, onlyFailed);
			ClearThCore.getInstance().getReportTemplatesProcessor().processTemplate(htmlContent, parameters, ReportTemplateFiles.REPORT);
		}
		catch (TemplateException e)
		{
			String errMsg = "An error occurred while processing the template of the report";
			getLogger().error(errMsg, e);
			throw new ReportException(errMsg + ". See logs for details");
		}
	}

	public StringWriter getHtmlContent()
	{
		return htmlContent;
	}
}
