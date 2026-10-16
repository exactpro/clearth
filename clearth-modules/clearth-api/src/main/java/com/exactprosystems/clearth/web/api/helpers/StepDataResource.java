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

package com.exactprosystems.clearth.web.api.helpers;

import javax.xml.bind.annotation.XmlRootElement;

import com.exactprosystems.clearth.automation.Step;
import com.thoughtworks.xstream.annotations.XStreamAlias;

@XmlRootElement(name="step")
@XStreamAlias("step")
public class StepDataResource
{
	private String name;
	private String kind;
	private String startAt;
	private String parameter;
	private boolean askForContinue;
	private boolean askIfFailed;
	private boolean execute;
	private String comment;
	
	public StepDataResource(Step sd)
	{
		this.name = sd.getName();
		this.kind = sd.getKind();
		this.startAt = sd.getStartAt();
		this.parameter = sd.getParameter();
		this.askForContinue = sd.isAskForContinue();
		this.askIfFailed = sd.isAskIfFailed();
		this.execute = sd.isExecute();
		this.comment = sd.getComment();
	}

	
	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	
	public String getKind()
	{
		return kind;
	}

	public void setKind(String kind)
	{
		this.kind = kind;
	}

	
	public String getStartAt()
	{
		return startAt;
	}

	public void setStartAt(String startAt)
	{
		this.startAt = startAt;
	}

	
	public String getParameter()
	{
		return parameter;
	}

	public void setParameter(String parameter)
	{
		this.parameter = parameter;
	}


	public boolean isAskForContinue()
	{
		return askForContinue;
	}

	public void setAskForContinue(boolean askForContinue)
	{
		this.askForContinue = askForContinue;
	}


	public boolean isAskIfFailed()
	{
		return askIfFailed;
	}

	public void setAskIfFailed(boolean askIfFailed)
	{
		this.askIfFailed = askIfFailed;
	}


	public boolean isExecute()
	{
		return execute;
	}

	public void setExecute(boolean execute)
	{
		this.execute = execute;
	}


	public String getComment()
	{
		return comment;
	}

	public void setComment(String comment)
	{
		this.comment = comment;
	}
}
