/******************************************************************************
 * Copyright 2009-2021 Exactpro Systems Limited
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

package com.exactprosystems.clearth.web.api.helpers;

import com.exactprosystems.clearth.xmldata.XmlSchedulerLaunchInfo;
import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.*;

@XmlRootElement(name = "launchInfo")
@XStreamAlias("launchInfo")
public class SchedulerLaunchInfoResource
{
	@XmlElement
	private String started;
	@XmlElement
	private String finished;
	@XmlElement
	private boolean successful;
	
	@XmlElementWrapper
	private List<MatrixInfoResource> matricesInfo;
	
	public String getStarted()
	{
		return started;
	}
	
	public void setStarted(String value)
	{
		this.started = value;
	}
	
	
	public String getFinished()
	{
		return finished;
	}
	
	public void setFinished(String value)
	{
		this.finished = value;
	}
	
	
	public boolean isSuccessful()
	{
		return successful;
	}
	
	public void setSuccessful(boolean value)
	{
		this.successful = value;
	}
	
	
	public List<MatrixInfoResource> getMatricesInfo()
	{
		if (matricesInfo == null)
			matricesInfo = new ArrayList<>();
		return matricesInfo;
	}
	
	
	public static SchedulerLaunchInfoResource fromLaunchInfo(XmlSchedulerLaunchInfo info)
	{
		DateFormat timestampDF = new SimpleDateFormat("yyyyMMddHHmmssSSS");
		
		SchedulerLaunchInfoResource t = new SchedulerLaunchInfoResource();
		t.started = timestampDF.format(info.getStarted());
		t.finished = timestampDF.format(info.getFinished());
		t.successful = info.isSuccess();
		
		List<MatrixInfoResource> mis = t.getMatricesInfo();
		info.getMatricesInfo().stream()
				.forEach(matrixInfo -> mis.add(new MatrixInfoResource(matrixInfo)));
		
		t.successful = info.isSuccess();
		return t;
	}
}