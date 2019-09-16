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

package com.exactprosystems.clearth.automation.report;

import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.SubActionData;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties({"actionReportWriter"})
public class ActionReport
{
	private ActionReportWriter actionReportWriter;
	
	protected String actionId;
	protected String actionName;
	protected String comment;
	
	protected ReportStatus status;
	protected ExceptionWrapper error;
	
	protected Map<String, String> inputParams;
	protected Map<String, String> outputParams;
	
	protected Result result;
	
	protected List<ActionReport> subActions = new ArrayList<ActionReport>();
	
	public ActionReport()
	{
	}
	
	public ActionReport(Action action, ActionReportWriter actionReportWriter)
	{
		this.actionReportWriter = actionReportWriter;
		this.setActionId(action.getIdInMatrix());
		this.setActionName(action.getName());
		this.setInputParams(action.getInputParams());
		this.setOutputParams(action.getOutputParams());
		this.setStatus(new ReportStatus(action));
		
		Result result = action.getResult();
		this.setResult(result);
		if (result != null && result.getError() != null)
			this.error = new ExceptionWrapper(result.getError());
		
		processSubActionsData(action);
	}
	
	public void processSubActionsData(Action action)
	{
		LinkedHashMap<String, SubActionData> subActionsData = action.getSubActionData();
		if (subActionsData == null)
			return;
		
		for (Map.Entry<String, SubActionData> subActionDataEntry : subActionsData.entrySet())
		{
			String subActionId = subActionDataEntry.getKey();
			SubActionData subActionData = subActionDataEntry.getValue();
			processSubActionData(this, subActionId, subActionData);
		}
	}
	
	private void processSubActionData(ActionReport actionReport, String subActionId, SubActionData subActionData)
	{
		ActionReport subActionReport = actionReportWriter.createActionReport();
		subActionReport.setActionId(subActionId);
		subActionReport.setActionName(subActionData.getName());
		subActionReport.setComment(subActionData.getComment());
		subActionReport.setStatus(subActionData.getSuccess());
		subActionReport.setInputParams(subActionData.getParams());
		
		actionReport.addSubActionReport(subActionReport);
		
		if (subActionData.getSubActionData() == null)
			return;
		
		for (Map.Entry<String, SubActionData> subActionDataEntry : subActionData.getSubActionData().entrySet())
			processSubActionData(subActionReport, subActionDataEntry.getKey(), subActionDataEntry.getValue());
	}
	
	private void addSubActionReport(ActionReport subActionReport)
	{
		subActions.add(subActionReport);
	}
	
	public String getActionId()
	{
		return actionId;
	}
	
	public void setActionId(String actionId)
	{
		this.actionId = actionId;
	}
	
	public String getActionName()
	{
		return actionName;
	}
	
	public void setActionName(String actionName)
	{
		this.actionName = actionName;
	}
	
	public String getComment()
	{
		return comment;
	}
	
	public void setComment(String comment)
	{
		this.comment = comment;
	}
	
	public ReportStatus getStatus()
	{
		return status;
	}
	
	public void setStatus(ReportStatus status)
	{
		this.status = status;
	}
	
	public ExceptionWrapper getError()
	{
		return error;
	}
	
	public void setError(ExceptionWrapper error)
	{
		this.error = error;
	}
	
	public Map<String, String> getInputParams()
	{
		return inputParams;
	}
	
	public void setInputParams(Map<String, String> inputParams)
	{
		this.inputParams = inputParams;
	}
	
	public Map<String, String> getOutputParams()
	{
		return outputParams;
	}
	
	public void setOutputParams(Map<String, String> outputParams)
	{
		this.outputParams = outputParams;
	}
	
	public Result getResult()
	{
		return result;
	}
	
	public void setResult(Result result)
	{
		this.result = result;
	}
	
	public List<ActionReport> getSubActions()
	{
		return subActions;
	}
	
	public void setSubActions(List<ActionReport> subActions)
	{
		this.subActions = subActions;
	}
}
