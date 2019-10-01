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

package com.exactprosystems.clearth.automation.schedulerinfo;

import com.exactprosystems.clearth.automation.MatrixData;
import com.exactprosystems.clearth.automation.Step;
import com.exactprosystems.clearth.xmldata.XmlMatrixInfo;

import java.util.List;

public class SchedulerSummaryFile extends SchedulerInfoFile
{
	protected final List<Step> steps;
	protected final List<MatrixData> matricesData;
	protected final List<XmlMatrixInfo> matricesInfo;
	
	public SchedulerSummaryFile(String name, List<Step> steps, List<MatrixData> matricesData,
			List<XmlMatrixInfo> matricesInfo, SchedulerInfoFile parent)
	{
		super(name, parent);
		isDirectory = false;
		
		this.steps = steps;
		this.matricesData = matricesData;
		this.matricesInfo = matricesInfo;
	}
	
	public List<Step> getSteps()
	{
		return steps;
	}
	
	public List<MatrixData> getMatricesData()
	{
		return matricesData;
	}
	
	public List<XmlMatrixInfo> getMatricesInfo()
	{
		return matricesInfo;
	}
	
	
	public void setFilePath(String filePath)
	{
		this.filePath = filePath;
	}
}
