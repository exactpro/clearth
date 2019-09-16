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

import com.exactprosystems.clearth.automation.Step;
import com.exactprosystems.clearth.automation.report.ReportStatus;

public class StepData
{
	private Step step;
	private ReportStatus stepStatus;
	private String pathToActionsFile;
	
	private boolean isStatusExpanded;
	
	private String stepName; //Name for displaying step on page (corrected for HTML)

	public StepData(Step step,
	                ReportStatus stepStatus,
	                String pathToActionsFile,
	                boolean isStatusExpanded,
	                String stepName)
	{
		this.step = step;
		this.stepStatus = stepStatus;
		this.pathToActionsFile = pathToActionsFile;
		this.isStatusExpanded = isStatusExpanded;
		this.stepName = stepName;
	}

	public boolean isStatusExpanded()
	{
		return isStatusExpanded;
	}

	public Step getStep()
	{
		return step;
	}

	public ReportStatus getStepStatus()
	{
		return stepStatus;
	}

	public String getPathToActionsFile()
	{
		return pathToActionsFile;
	}

	public String getStepName()
	{
		return stepName;
	}
}
