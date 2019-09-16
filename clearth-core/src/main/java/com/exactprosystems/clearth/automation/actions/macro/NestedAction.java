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

import com.exactprosystems.clearth.automation.Action;

public class NestedAction
{
	private final Action action;
	private boolean showInReport, continueIfFailed;
	
	public NestedAction(Action action)
	{
		this.action = action;
		this.showInReport = true;
	}
	
	public Action getAction()
	{
		return action;
	}
	
	public void setShowInReport(boolean showInReport)
	{
		this.showInReport = showInReport;
	}
	
	public boolean isShowInReport()
	{
		return showInReport;
	}
	
	public void setContinueIfFailed(boolean continueIfFailed)
	{
		this.continueIfFailed = continueIfFailed;
	}
	
	public boolean isContinueIfFailed()
	{
		return continueIfFailed;
	}
}
