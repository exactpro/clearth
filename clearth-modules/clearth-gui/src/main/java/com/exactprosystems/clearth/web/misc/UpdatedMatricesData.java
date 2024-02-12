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

package com.exactprosystems.clearth.web.misc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.exactprosystems.clearth.automation.MatrixData;

public class UpdatedMatricesData
{
	private List<MatrixData> matrices;
	private Throwable error;
	private String result;
	
	public void addMatrix(MatrixData md)
	{
		if (matrices == null)
			matrices = new ArrayList<>();
		
		matrices.add(md);
	}
	
	public void cleanMatrices()
	{
		matrices = null;
	}
	
	public List<MatrixData> getMatrices()
	{
		return matrices == null ? Collections.emptyList() : Collections.unmodifiableList(matrices);
	}
	
	
	public Throwable getError()
	{
		return error;
	}
	
	public void setError(Throwable error)
	{
		this.error = error;
	}
	
	
	public String getResult()
	{
		return result;
	}
	
	public void setResult(String result)
	{
		this.result = result;
	}
	
	
	public void setResultAndError(String result, Throwable error)
	{
		setResult(result);
		setError(error);
	}
}