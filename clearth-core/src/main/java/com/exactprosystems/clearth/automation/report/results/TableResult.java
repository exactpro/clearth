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

package com.exactprosystems.clearth.automation.report.results;

import com.exactprosystems.clearth.automation.report.FailReason;
import com.exactprosystems.clearth.automation.report.Result;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties({"useFailReasonColor"})
public class TableResult extends Result
{
	private static final long serialVersionUID = 6322958383407724285L;

	protected List<TableResultDetail> details = new ArrayList<TableResultDetail>();
	protected List<String> columns = new  ArrayList<String>();
	protected boolean hasStatus = true;
	protected String header;
	protected boolean useFailReasonColor = false;

	public TableResult()
	{
		super();
	}

	public TableResult(String header, List<String> columns, boolean hasStatus)
	{
		this.header=header;
		this.columns=columns;
		this.hasStatus=hasStatus;
	}
	
	@Override
	public boolean isSuccess(){
		return super.isSuccess();
	}
	

	protected static String getFailStatus(FailReason failReason)
	{
		switch (failReason)
		{
		case COMPARISON : return "failed_comparison";
		case CALCULATION : return "failed_calculation";
		default : return "failed";
		}
	}

	@Override
	public void clearDetails()
	{
		details.clear();
	}
	
	public void addDetail(TableResultDetail detail)
	{
		details.add(detail);
		if ((isSuccess()) && (!detail.isIdentical()))
		{
			setSuccess(false);
			setFailReason(FailReason.COMPARISON);
		}
	}

	public void addDetail(boolean identical,List<String> params)
	{
		TableResultDetail detail = new DefaultTableResultDetail(identical,params);
		addDetail(detail);
	}

	public List<TableResultDetail> getDetails()
	{
		return details;
	}

	public void setDetails(List<TableResultDetail> details)
	{
		this.details = details;
	}

	public List<String> getColumns()
	{
		return columns;
	}

	public void setColumns(List<String> columns)
	{
		this.columns = columns;
	}

	public boolean isHasStatus()
	{
		return hasStatus;
	}

	public void setHasStatus(boolean hasStatus)
	{
		this.hasStatus = hasStatus;
	}

	public String getHeader()
	{
		return header;
	}

	public void setHeader(String header)
	{
		this.header = header;
	}

	public boolean isUseFailReasonColor()
	{
		return useFailReasonColor;
	}

	public void setUseFailReasonColor(boolean useFailReasonColor)
	{
		this.useFailReasonColor = useFailReasonColor;
	}
}
