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

package com.exactprosystems.clearth.automation.report.results;

import java.util.ArrayList;
import java.util.List;

import com.exactprosystems.clearth.utils.LineBuilder;

public class DefaultTableResultDetail implements TableResultDetail
{
	private List<String> values;
	private boolean identical;
	private boolean info;
	
	public DefaultTableResultDetail()
	{
		values = new ArrayList<String>();
		identical = false;
		info = false;
	}
	
	public DefaultTableResultDetail(boolean identical,String ...params)
	{
		this.values = new ArrayList<String>();
		for (String param:params)
			this.values.add(param) ;
		this.identical = identical;
		this.info = false;
	}
	
	public DefaultTableResultDetail(boolean identical,List<String> params)
	{
		this.values = new ArrayList<String>();
		for (String param:params)
			this.values.add(param) ;
		this.identical = identical;
		this.info = false;
	}

	public DefaultTableResultDetail(String ...params)
	{
		this.values = new ArrayList<String>();
		for (String param:params)
			this.values.add(param) ;
		this.identical = true;
		this.info = true;
	}


	public DefaultTableResultDetail(List<String> params)
	{
		this.values = new ArrayList<String>();
		for (String param:params)
			this.values.add(param) ;
		this.identical = true;
		this.info = true;
	}

	public boolean isInfo() {
		return info;
	}

	public void setInfo(boolean info) {
		this.info = info;
	}

	public List<String> getValues()
	{
		return values;
	}
	
	
	
	public boolean isIdentical()
	{
		return identical;
	}
	
	public void setIdentical(boolean identical)
	{
		this.identical = identical;
	}

	@Override
	public String toString()
	{
		return toLineBuilder(new LineBuilder(), "").toString();
	}

	public LineBuilder toLineBuilder(LineBuilder builder, String prefix)
	{
		builder.add(prefix).add("Values").add(": ").add(identical).add(" / ").
				add(values.get(0)).eol();
		return builder;
	}
}
