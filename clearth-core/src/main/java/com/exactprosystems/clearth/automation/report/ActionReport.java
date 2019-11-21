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
import com.exactprosystems.clearth.automation.async.WaitAsyncEnd;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties({"actionReportWriter"})
public class ActionReport
{
	private ActionReportWriter actionReportWriter;
	
	protected String actionId;
	protected String actionName;
	protected long timeout;
	protected boolean async;
	protected String comment;
	protected String idInTemplate;

	protected String formulaExecutable, formulaInverted, formulaComment, formulaTimeout,
			formulaAsync, formulaAsyncGroup, formulaWaitAsyncEnd, formulaIdInTemplate;
	protected boolean payloadFinished;
	protected String asyncGroup, waitAsyncEndStep;
	protected WaitAsyncEnd waitAsyncEnd;
	
	protected ReportStatus status;
	protected ExceptionWrapper error;
	
	protected Map<String, ReportParamValue> inputParams;
	protected Map<String, String> outputParams;
	
	protected Result result;
	
	protected List<ActionReport> subActions = new ArrayList<ActionReport>();
	
	public ActionReport()
	{
	}
	
	public ActionReport(Action action, ActionReportWriter actionReportWriter)
	{
		init(action, actionReportWriter);
	}
	
	protected void init(Action action, ActionReportWriter actionReportWriter)
	{
		this.actionReportWriter = actionReportWriter;
		this.setActionId(action.getIdInMatrix());
		this.setIdInTemplate(action.getIdInTemplate());
		this.setActionName(action.getName());
		this.setTimeout(action.getTimeOut());
		this.setAsync(action.isAsync());
		this.setComment(action.getComment());
		this.setInputParams(action.extractMatrixInputParams());
		this.setOutputParams(action.getOutputParams());
		this.setStatus(createReportStatus(action));

		Result result = action.getResult();
		this.setResult(result);
		if (result != null && result.getError() != null)
			this.error = createExceptionWrapper(result.getError());

		this.setFormulaExecutable(action.getFormulaExecutable());
		this.setFormulaInverted(action.getFormulaInverted());
		this.setFormulaComment(action.getFormulaComment());
		this.setFormulaTimeout(action.getFormulaTimeout());
		this.setFormulaAsync(action.getFormulaAsync());
		this.setFormulaAsyncGroup(action.getFormulaAsyncGroup());
		this.setFormulaWaitAsyncEnd(action.getFormulaWaitAsyncEnd());
		this.setFormulaIdInTemplate(action.getFormulaIdInTemplate());
		this.setPayloadFinished(action.isPayloadFinished());
		this.setAsyncGroup(action.getAsyncGroup());
		this.setWaitAsyncEndStep(action.getWaitAsyncEndStep());
		this.setWaitAsyncEnd(action.getWaitAsyncEnd());
		
		setCustomFields(action);

		processSubActionsData(action);
	}


	/* Override methods below to support custom Action's fields in JSON reports */
	
	protected void setCustomFields(@SuppressWarnings("unused") Action action) { /* Nothing to do by default */ }
	
	protected void setSubActionCustomFields(@SuppressWarnings("unused") SubActionData subActionData,
	                                        @SuppressWarnings("unused") ActionReport subActionReport) 
	{ /* Nothing to do by default */ }
	
	protected ReportStatus createReportStatus(Action action)
	{
		return new ReportStatus(action);
	}

	protected ExceptionWrapper createExceptionWrapper(Throwable error)
	{
		return new ExceptionWrapper(error);
	}

	private void processSubActionsData(Action action)
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
		subActionReport.setIdInTemplate(subActionData.getIdInTemplate());
		subActionReport.setStatus(subActionData.getSuccess());
		subActionReport.setInputParams(subActionData.extractMatrixInputParams());
		
		setSubActionCustomFields(subActionData, subActionReport);
		
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

	public long getTimeout()
	{
		return timeout;
	}

	public void setTimeout(long timeout)
	{
		this.timeout = timeout;
	}

	public boolean isAsync()
	{
		return async;
	}

	public void setAsync(boolean async)
	{
		this.async = async;
	}

	public String getComment()
	{
		return comment;
	}
	
	public String getIdInTemplate()
	{
		return idInTemplate;
	}
	
	public void setIdInTemplate(String idInTemplate)
	{
		this.idInTemplate = idInTemplate;
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
	
	public Map<String, ReportParamValue> getInputParams()
	{
		return inputParams;
	}
	
	public void setInputParams(Map<String, ReportParamValue> inputParams)
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

	public String getFormulaExecutable()
	{
		return formulaExecutable;
	}

	public void setFormulaExecutable(String formulaExecutable)
	{
		this.formulaExecutable = formulaExecutable;
	}

	public String getFormulaInverted()
	{
		return formulaInverted;
	}

	public void setFormulaInverted(String formulaInverted)
	{
		this.formulaInverted = formulaInverted;
	}

	public String getFormulaComment()
	{
		return formulaComment;
	}

	public void setFormulaComment(String formulaComment)
	{
		this.formulaComment = formulaComment;
	}

	public String getFormulaTimeout()
	{
		return formulaTimeout;
	}

	public void setFormulaTimeout(String formulaTimeout)
	{
		this.formulaTimeout = formulaTimeout;
	}

	public String getFormulaAsync()
	{
		return formulaAsync;
	}

	public void setFormulaAsync(String formulaAsync)
	{
		this.formulaAsync = formulaAsync;
	}

	public String getFormulaAsyncGroup()
	{
		return formulaAsyncGroup;
	}

	public void setFormulaAsyncGroup(String formulaAsyncGroup)
	{
		this.formulaAsyncGroup = formulaAsyncGroup;
	}

	public String getFormulaWaitAsyncEnd()
	{
		return formulaWaitAsyncEnd;
	}

	public void setFormulaWaitAsyncEnd(String formulaWaitAsyncEnd)
	{
		this.formulaWaitAsyncEnd = formulaWaitAsyncEnd;
	}

	public String getFormulaIdInTemplate()
	{
		return formulaIdInTemplate;
	}

	public void setFormulaIdInTemplate(String formulaIdInTemplate)
	{
		this.formulaIdInTemplate = formulaIdInTemplate;
	}

	public boolean isPayloadFinished()
	{
		return payloadFinished;
	}

	public void setPayloadFinished(boolean payloadFinished)
	{
		this.payloadFinished = payloadFinished;
	}

	public String getAsyncGroup()
	{
		return asyncGroup;
	}

	public void setAsyncGroup(String asyncGroup)
	{
		this.asyncGroup = asyncGroup;
	}

	public String getWaitAsyncEndStep()
	{
		return waitAsyncEndStep;
	}

	public void setWaitAsyncEndStep(String waitAsyncEndStep)
	{
		this.waitAsyncEndStep = waitAsyncEndStep;
	}

	public WaitAsyncEnd getWaitAsyncEnd()
	{
		return waitAsyncEnd;
	}

	public void setWaitAsyncEnd(WaitAsyncEnd waitAsyncEnd)
	{
		this.waitAsyncEnd = waitAsyncEnd;
	}
}
