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

package com.exactprosystems.clearth.automation.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("StateObjects")
public class ExecutorStateObjects
{
	private List<MatrixState> matrices = new ArrayList<MatrixState>();
	private Map<String, String> fixedIDs = null;
	
	
	public List<MatrixState> getMatrices()
	{
		return matrices;
	}
	
	public void setMatrices(List<MatrixState> matrices)
	{
		this.matrices = matrices;
	}
	
	
	public Map<String, String> getFixedIDs()
	{
		return fixedIDs;
	}
	
	public void setFixedIDs(Map<String, String> fixedIDs)
	{
		this.fixedIDs = fixedIDs;
	}
}
