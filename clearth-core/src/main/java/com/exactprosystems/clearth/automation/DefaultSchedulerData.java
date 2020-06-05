/******************************************************************************
 * Copyright 2009-2020 Exactpro Systems Limited
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

import static com.exactprosystems.clearth.automation.Step.StepParams;
import static com.exactprosystems.clearth.automation.Step.StepParams.*;

public class DefaultSchedulerData extends SchedulerData
{
	public static final String[] CONFIG_HEADER = new String[] {
			GLOBAL_STEP.getValue(),
			STEP_KIND.getValue(),
			START_AT.getValue(),
			START_AT_TYPE.getValue(),
			WAIT_NEXT_DAY.getValue(),
			PARAMETER.getValue(),
			ASK_FOR_CONTINUE.getValue(),
			ASK_IF_FAILED.getValue(),
			StepParams.EXECUTE.getValue(),
			COMMENT.getValue()
	};

	public static final String[] EXECUTED_STEP_DATA_HEADER = new String[] {
			GLOBAL_STEP.getValue(),
			STEP_KIND.getValue(),
			START_AT.getValue(),
			ASK_FOR_CONTINUE.getValue(),
			ASK_IF_FAILED.getValue(),
			StepParams.EXECUTE.getValue(),
			STARTED.getValue(),
			ACTIONS_SUCCESSFUL.getValue(),
			FINISHED.getValue()
	};


	public DefaultSchedulerData(String name, String configsRoot, String schedulerDirName, String matricesDir,
	                            String lastExecutedDataDir, StepFactory stepFactory) throws Exception
	{
		super(name, configsRoot, schedulerDirName, matricesDir, lastExecutedDataDir, stepFactory);
	}


	@Override
	public String[] getConfigHeader()
	{
		return CONFIG_HEADER;
	}

	@Override
	public String[] getExecutedStepsDataHeader()
	{
		return EXECUTED_STEP_DATA_HEADER;
	}
}
