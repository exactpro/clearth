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

package com.exactprosystems.clearth.automation.report;

import com.exactprosystems.clearth.utils.CommaBuilder;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class ReportsConfig
{
	protected boolean completeHtmlReport = true,
			failedHtmlReport = true,
			completeJsonReport = true;
	
	public ReportsConfig()
	{
	}
	
	public ReportsConfig(boolean completeHtmlReport, boolean failedHtmlReport,
			boolean completeJsonReport)
	{
		this.completeHtmlReport = completeHtmlReport;
		this.failedHtmlReport = failedHtmlReport;
		this.completeJsonReport = completeJsonReport;
	}
	
	public ReportsConfig(ReportsConfig copyFrom)
	{
		this.completeHtmlReport = copyFrom.isCompleteHtmlReport();
		this.failedHtmlReport = copyFrom.isFailedHtmlReport();
		this.completeJsonReport = copyFrom.isCompleteJsonReport();
	}
	
	
	public boolean isCompleteHtmlReport()
	{
		return completeHtmlReport;
	}
	
	public boolean isFailedHtmlReport()
	{
		return failedHtmlReport;
	}
	
	public boolean isCompleteJsonReport()
	{
		return completeJsonReport;
	}
	
	@JsonIgnore
	public boolean isAnyReportEnabled()
	{
		return completeHtmlReport || failedHtmlReport || completeJsonReport;
	}
	
	@JsonIgnore
	public String getDisabledReports()
	{
		if (completeHtmlReport && failedHtmlReport && completeJsonReport)
			return null;
		
		CommaBuilder cb = new CommaBuilder();
		if (!completeHtmlReport)
			cb.append("HTML report");
		if (!failedHtmlReport)
			cb.append("'only failed' HTML report");
		if (!completeJsonReport)
			cb.append("JSON report");
		return cb.toString();
	}
}