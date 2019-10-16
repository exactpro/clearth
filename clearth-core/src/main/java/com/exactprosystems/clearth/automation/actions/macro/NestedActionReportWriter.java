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

package com.exactprosystems.clearth.automation.actions.macro;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.report.ActionReportWriter;

import java.io.File;

public class NestedActionReportWriter extends ActionReportWriter
{
	private final File reportFile;
	private boolean writeReport = true;
	
	public NestedActionReportWriter(String nestedActionsReportFilePath)
	{
		reportFile = new File(ClearThCore.rootRelative(nestedActionsReportFilePath));
		reportFile.getParentFile().mkdirs();
	}
	
	public void setWriteReport(boolean writeReport)
	{
		this.writeReport = writeReport;
	}
	
	
	@Override
	public void writeReport(Action action, String actionsReportsDir, String stepFileName, boolean writeFailedReport)
	{
		if (writeReport || !action.isPassed())
			super.writeReport(action, actionsReportsDir, stepFileName, false);
	}
	
	@Override
	protected void writeJsonActionReport(Action action, String actionsReportsDir, String actionsReportFile)
	{
		// JSON report will be written while writing one for whole macro action
	}
	
	@Override
	protected File getReportDir(String actionsReportsDir, Action action)
	{
		return reportFile.getParentFile();
	}
	
	@Override
	protected File getReportFile(File reportDir, String actionsReportFile, boolean onlyFailed)
	{
		return reportFile;
	}
}
