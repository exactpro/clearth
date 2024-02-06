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

package com.exactprosystems.clearth.data;

import java.util.Map;

public class HandledTestExecutionIdStorage
{
	private final HandledTestExecutionId executionId;
	private final Map<String, HandledTestExecutionId> matrixIdMap;
	
	public HandledTestExecutionIdStorage(HandledTestExecutionId executionId, Map<String, HandledTestExecutionId> matrixIdMap)
	{
		this.executionId = executionId;
		this.matrixIdMap = matrixIdMap;
	}
	
	public HandledTestExecutionId getMatrixId(String matrixName)
	{
		return matrixIdMap.get(matrixName);
	}
	
	public HandledTestExecutionId getExecutionId()
	{
		return executionId;
	}
}
