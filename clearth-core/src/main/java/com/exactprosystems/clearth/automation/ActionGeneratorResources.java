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

import com.exactprosystems.clearth.config.MatrixFatalErrors;
import com.exactprosystems.clearth.config.SpecialActionParameters;

public class ActionGeneratorResources
{
	private final SpecialActionParameters specialActionParameters;
	private final ActionFactory actionFactory;
	private final MvelVariablesFactory mvelFactory;
	private final MatrixFunctions matrixFunctions;
	private final MatrixFatalErrors matrixFatalErrors;
	
	public ActionGeneratorResources(SpecialActionParameters specialActionParameters, ActionFactory actionFactory, MvelVariablesFactory mvelFactory,
									MatrixFunctions matrixFunctions, MatrixFatalErrors matrixFatalErrors)
	{
		this.specialActionParameters = specialActionParameters;
		this.actionFactory = actionFactory;
		this.mvelFactory = mvelFactory;
		this.matrixFunctions = matrixFunctions;
		this.matrixFatalErrors = matrixFatalErrors;
	}
	
	public SpecialActionParameters getSpecialActionParameters()
	{
		return specialActionParameters;
	}
	
	public ActionFactory getActionFactory()
	{
		return actionFactory;
	}
	
	public MvelVariablesFactory getMvelFactory()
	{
		return mvelFactory;
	}
	
	public MatrixFunctions getMatrixFunctions()
	{
		return matrixFunctions;
	}

	public MatrixFatalErrors getMatrixFatalErrors()
	{
		return matrixFatalErrors;
	}
}