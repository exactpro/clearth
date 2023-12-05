/******************************************************************************
 * Copyright 2009-2023 Exactpro Systems Limited
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

import com.exactprosystems.clearth.EnvVars;
import com.exactprosystems.clearth.GlobalConstants;

import static com.exactprosystems.clearth.automation.MatrixFunctions.GLOBAL_CONST;
import static com.exactprosystems.clearth.automation.MatrixFunctions.ENV_VARS;

public class MvelVariablesFactory
{
	private final EnvVars envVars;
	private final GlobalConstants globalConst;

	public MvelVariablesFactory(EnvVars envVars, GlobalConstants globalConst)
	{
		this.envVars = envVars;
		this.globalConst = globalConst;
	}
	
	public MvelVariables create()
	{
		MvelVariables mvelVars = new MvelVariables();
		if (envVars != null)
			mvelVars.put(ENV_VARS, envVars.getMap());
		if (globalConst != null)
			mvelVars.put(GLOBAL_CONST, globalConst.getAll());
		return mvelVars;
	}
	
	public EnvVars getEnvVars()
	{
		return envVars;
	}
	
	public GlobalConstants getGlobalConst()
	{
		return globalConst;
	}
}
