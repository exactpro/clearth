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

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;

@XmlType(name = "specialActionParameters")
@XmlAccessorType(XmlAccessType.NONE)
public class SpecialActionParameters
{
	@XmlElement(name = "parameter")
	private Set<String> parameters;
	
	public SpecialActionParameters() {}
	
	
	public Set<String> getParameters()
	{
		if(parameters == null)
			parameters = new HashSet<>();
		return parameters;
	}
	
	public void setParameters(Set<String> params)
	{
		this.parameters = params;
		processParameters();
	}
	
	
	public boolean isSpecialParam(String name)
	{
		return isSpecialParamLowCase(name.toLowerCase());
	}
	
	public boolean isSpecialParamLowCase(String lowCaseName)
	{
		return getParameters().contains(lowCaseName);
	}
	
	
	void afterUnmarshal(Unmarshaller unmarshaller, Object parent)
	{
		processParameters();
	}
	
	private void processParameters()
	{
		if (parameters == null)
			return;
		
		Set<String> newParameters = new HashSet<>(parameters.size());
		for (String p : parameters)
			newParameters.add(p.toLowerCase());
		parameters = newParameters;
	}
	
	
	@Override
	public String toString()
	{
		if(parameters == null || parameters.isEmpty())
			return "";

		StringJoiner params = new StringJoiner(", ");
		for(String s: parameters)
			params.add(s);

		return params.toString();
	}
}
