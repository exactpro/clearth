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

import com.exactprosystems.clearth.automation.Matrix;
import com.exactprosystems.clearth.automation.Step;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.apache.commons.lang.StringUtils.isEmpty;

public class StepReport
{
	protected String stepName;
	protected String stepKind;
	protected boolean async;
	
	protected ReportStatus status;
	protected ExceptionWrapper error;
	
	protected Result result;
	
	protected List<ActionReport> actionReports = new ArrayList<ActionReport>();
	
	public StepReport()
	{
	}
	
	public StepReport(Step step, Matrix matrix)
	{
		this.stepName = step.getName();
		this.stepKind = step.getKind();
		this.setAsync(step.isAsync());
		this.processStatus(step, matrix);
		this.setResult(step.getResult());
	}
	
	private void processStatus(Step step, Matrix matrix)
	{
		ReportStatus stepStatus = new ReportStatus(step.isSuccessful() && matrix.isStepSuccessful(step.getName()));
		stepStatus.setStarted(step.getStarted());
		stepStatus.setFinished(step.getFinished());
		if (!isEmpty(step.getComment()))
			stepStatus.setComments(Collections.singletonList(step.getComment()));
		if (step.getError() != null)
			error = new ExceptionWrapper(step.getError());
		setStatus(stepStatus);
	}
	
	public void addActionReport(ActionReport actionReport)
	{
		actionReports.add(actionReport);
	}
	
	public String getStepName()
	{
		return stepName;
	}
	
	public void setStepName(String stepName)
	{
		this.stepName = stepName;
	}
	
	public String getStepKind()
	{
		return stepKind;
	}
	
	public void setStepKind(String stepKind)
	{
		this.stepKind = stepKind;
	}

	public boolean isAsync()
	{
		return async;
	}

	public void setAsync(boolean async)
	{
		this.async = async;
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
	
	public Result getResult()
	{
		return result;
	}
	
	public void setResult(Result result)
	{
		this.result = result;
	}
	
	public List<ActionReport> getActionReports()
	{
		return actionReports;
	}
	
	public void setActionReports(List<ActionReport> actionReports)
	{
		this.actionReports = actionReports;
	}
}
