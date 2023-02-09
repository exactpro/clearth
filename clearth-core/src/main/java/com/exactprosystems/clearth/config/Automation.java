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

package com.exactprosystems.clearth.config;

import javax.xml.bind.annotation.*;

@XmlType(name = "automation")
public class Automation
{
	private volatile boolean userSchedulersAllowed = true;

	private MatrixFatalErrors matrixFatalErrors;
	private SpecialActionParameters specialActionParameters;

	public Automation(){}

	public boolean isUserSchedulersAllowed()
	{
		return this.userSchedulersAllowed;
	}

	public void setUserSchedulersAllowed(boolean userSchedulersAllowed)
	{
		this.userSchedulersAllowed = userSchedulersAllowed;
	}

	public void setMatrixFatalErrors(MatrixFatalErrors matrixFatalErrors)
	{
		this.matrixFatalErrors = matrixFatalErrors;
	}

	public MatrixFatalErrors getMatrixFatalErrors()
	{
		if (matrixFatalErrors == null)
			matrixFatalErrors = new MatrixFatalErrors();
		return matrixFatalErrors;
	}

	public SpecialActionParameters getSpecialActionParameters()
	{
		if(specialActionParameters == null)
			specialActionParameters = new SpecialActionParameters();
		return specialActionParameters;
	}

	public void setSpecialActionParameters(SpecialActionParameters specialActionParameters)
	{
		this.specialActionParameters = specialActionParameters;
	}

	@Override
	public String toString()
	{
		return "[userSchedulersAllowed = " + this.isUserSchedulersAllowed() +
				"; matrixFatalErrors: " + this.getMatrixFatalErrors().toString() +
				"; specialActionParameters: " + this.getSpecialActionParameters().toString() +
				"]";
	}
}
