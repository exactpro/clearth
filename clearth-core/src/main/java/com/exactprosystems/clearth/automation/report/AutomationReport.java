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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties({ "reportName" })
public class AutomationReport
{
	protected String reportName;
	
	protected String version;
	protected String matrixName;
	protected String handledTestExecutionId;
	protected String userName;
	protected String host;
	
	protected Date executionStart;
	protected Date executionEnd;
	protected String executionTime;
	protected boolean result;
	protected String description;
	protected Map<String, String> constants;
	
	protected List<StepReport> stepReports = new ArrayList<StepReport>();
	
	public AutomationReport()
	{
	}
	
	public void setExecutionTime(Date startTime, Date endTime)
	{
		if ((endTime != null) && (startTime != null))
			executionTime = Double.toString((endTime.getTime() - startTime.getTime()) / 1000.0) + " sec";
		else if (endTime == null)
			executionTime = Double.toString((new Date().getTime() - startTime.getTime()) / 1000.0) + " sec";
		else
			executionTime = "";
	}
	
	public void addStepReport(StepReport stepReport)
	{
		stepReports.add(stepReport);
	}
	
	public String getReportName()
	{
		return reportName;
	}
	
	public void setReportName(String reportName)
	{
		this.reportName = reportName;
	}
	
	public String getVersion()
	{
		return version;
	}
	
	public void setVersion(String version)
	{
		this.version = version;
	}
	
	public String getMatrixName()
	{
		return matrixName;
	}
	
	public void setMatrixName(String matrixName)
	{
		this.matrixName = matrixName;
	}
	
	public String getUserName()
	{
		return userName;
	}
	
	public void setUserName(String userName)
	{
		this.userName = userName;
	}
	
	public String getHost()
	{
		return host;
	}
	
	public void setHost(String host)
	{
		this.host = host;
	}
	
	public Date getExecutionStart()
	{
		return executionStart;
	}
	
	public void setExecutionStart(Date executionStart)
	{
		this.executionStart = executionStart;
	}
	
	public Date getExecutionEnd()
	{
		return executionEnd;
	}
	
	public void setExecutionEnd(Date executionEnd)
	{
		this.executionEnd = executionEnd;
	}
	
	public String getExecutionTime()
	{
		return executionTime;
	}
	
	public void setExecutionTime(String executionTime)
	{
		this.executionTime = executionTime;
	}
	
	public boolean isResult()
	{
		return result;
	}
	
	public void setResult(boolean result)
	{
		this.result = result;
	}
	
	public List<StepReport> getStepReports()
	{
		return stepReports;
	}
	
	public void setStepReports(List<StepReport> stepReports)
	{
		this.stepReports = stepReports;
	}
	
	public String getDescription()
	{
		return description;
	}
	
	public void setDescription(String description)
	{
		this.description = description;
	}
	
	public Map<String, String> getConstants()
	{
		return constants;
	}
	
	public void setConstants(Map<String, String> constants)
	{
		this.constants = constants;
	}
	
	public String getHandledTestExecutionId()
	{
		return handledTestExecutionId;
	}
	
	public void setHandledTestExecutionId(String handledTestExecutionId)
	{
		this.handledTestExecutionId = handledTestExecutionId;
	}
}
