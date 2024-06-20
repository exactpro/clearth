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

package com.exactprosystems.clearth.automation.persistence.db;

import java.util.Objects;

public class MatrixStepReference
{
	private final int matrixId,
			stepId;
	
	public MatrixStepReference(int matrixId, int stepId)
	{
		this.matrixId = matrixId;
		this.stepId = stepId;
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(matrixId, stepId);
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MatrixStepReference other = (MatrixStepReference) obj;
		return matrixId == other.matrixId && stepId == other.stepId;
	}
	
	
	public int getMatrixId()
	{
		return matrixId;
	}
	
	public int getStepId()
	{
		return stepId;
	}
}