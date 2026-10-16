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

package com.exactprosystems.clearth.tools.matrixmaker;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.exactprosystems.clearth.tools.matrixmaker.initialdata.InitialData;


/**
 * Created by victor.akimov on 4/14/17.
 */
public class Cycle extends MMTemplate
{
	private String initDataName;
	private String initFormula;
	private List<Cycle> nestedCycles = new LinkedList<Cycle>();
	private MMTemplate template;
	boolean nested;
	protected InitialData cycleInitData;
	protected Set<String> cycleIds = new HashSet<>();
	
	public Cycle(MMTemplate template)
	{
		this.template = template;
	}

	public String getInitDataName()
	{
		return initDataName;
	}

	public void setInitDataName(String initDataName)
	{
		this.initDataName = initDataName;
	}

	public String getInitFormula()
	{
		return initFormula;
	}

	public void setInitFormula(String initFormula)
	{
		this.initFormula = initFormula;
	}

	public boolean isNested()
	{
		return nested;
	}

	public void setNested(boolean nested)
	{
		this.nested = nested;
	}

	public List<Cycle> getNestedCycles()
	{
		return nestedCycles;
	}

	public void setNestedCycles(List<Cycle> nestedCycles)
	{
		this.nestedCycles = nestedCycles;
	}

	public void addNestedCycle(Cycle nestedCycle)
	{
		this.nestedCycles.add(nestedCycle);
	}

	public MMTemplate getTemplate()
	{
		return template;
	}

	public InitialData getCycleInitData()
	{
		return cycleInitData;
	}

	public void setCycleInitData(InitialData initData)
	{
		this.cycleInitData = initData;
	}

	public Set<String> getCycleIds()
	{
		return cycleIds;
	}

	public void addCycleId(String cycleId)
	{
		cycleIds.add(cycleId);
	}

	
}
