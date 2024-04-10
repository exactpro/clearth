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

package com.exactprosystems.clearth.automation;

import com.exactprosystems.clearth.automation.exceptions.AutomationException;

import java.io.File;
import java.util.List;
import java.util.Map;

public interface IExecutor
{
	boolean isTerminated();
	boolean isSuspended();
	boolean isReplayEnabled();
	boolean isExecutionInterrupted();
	
	void start();
	void interruptExecution() throws AutomationException;
	void pauseExecution();
	void continueExecution();
	void replayStep();
	
	void tryAgainMain();
	void tryAgainAlt();
	boolean isFailover();
	int getFailoverActionType();
	int getFailoverReason();
	String getFailoverReasonString();
	String getFailoverConnectionName();
	void setFailoverRestartAction(boolean needRestart);
	void setFailoverSkipAction(boolean needSkipAction);
	
	Step getCurrentStep();
	List<Step> getSteps();
	Map<String, Boolean> getHolidays();
	boolean isCurrentStepIdle();
	
	void clearLastReportsInfo();
	String getReportsDir();
	String getCompletedReportsDir();
	void copyActionReports(File pathToStoreReports);
	ReportsInfo getLastReportsInfo();
	void makeCurrentReports(String pathToStoreReports, boolean deleteAfterExecution);
}