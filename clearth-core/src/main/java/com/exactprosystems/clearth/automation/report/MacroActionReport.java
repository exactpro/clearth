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

import com.exactprosystems.clearth.automation.actions.macro.MacroAction;
import com.exactprosystems.clearth.automation.actions.macro.NestedAction;

import java.util.ArrayList;
import java.util.List;

public class MacroActionReport extends ActionReport
{
	protected List<ActionReport> nestedActions = new ArrayList<>();
	
	public MacroActionReport()
	{
		super();
	}
	
	public MacroActionReport(MacroAction action, ActionReportWriter actionReportWriter)
	{
		super(action, actionReportWriter);
		List<NestedAction> actionNestedActions = action.getNestedActions();
		if (actionNestedActions != null)
		{
			for (NestedAction na : actionNestedActions)
				nestedActions.add(actionReportWriter.createActionReport(na.getAction()));
		}
	}
	
	public List<ActionReport> getNestedActions()
	{
		return nestedActions;
	}
	
	public void setNestedActions(List<ActionReport> nestedActions)
	{
		this.nestedActions = nestedActions;
	}
}
