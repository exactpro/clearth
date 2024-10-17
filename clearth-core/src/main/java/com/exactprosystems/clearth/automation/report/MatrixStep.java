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

import java.util.Objects;

import com.exactprosystems.clearth.automation.Matrix;
import com.exactprosystems.clearth.automation.Step;

public class MatrixStep
{
	private final Matrix matrix;
	private final Step step;
	
	public MatrixStep(Matrix matrix, Step step)
	{
		this.matrix = matrix;
		this.step = step;
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(matrix, step);
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
		MatrixStep other = (MatrixStep) obj;
		return Objects.equals(matrix, other.matrix) && Objects.equals(step, other.step);
	}
	
	
	public Matrix getMatrix()
	{
		return matrix;
	}
	
	public Step getStep()
	{
		return step;
	}
}
