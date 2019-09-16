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

package com.exactprosystems.clearth.automation;

import com.exactprosystems.clearth.automation.report.ReportStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.LinkedHashMap;
import java.util.Map;

@JsonIgnoreProperties({"exception"})
public class SubActionData
{
	public String name;
	public LinkedHashMap<String, String> params;
	public Map<String, String> formulas;
	public LinkedHashMap<String, SubActionData> subActionData;
	//TODO add ExceptionWrapper after JSON reports migration to core
	public Exception exception;
	public String comment;
	public ReportStatus success;
	
	public SubActionData(String name, LinkedHashMap<String, String> params, Map<String, String> formulas, LinkedHashMap<String, SubActionData> subActionData)
	{
		this.name = name;
		this.params = params;
		this.formulas = formulas;
		this.subActionData = subActionData;
		success = new ReportStatus(true);
	}

	public void setComment(String comment)
	{
		this.comment = comment;
	}

	public void setSuccess(boolean success)
	{
		this.success = new ReportStatus(success);
	}

	public void setFailedComment(String comment)
	{
		this.comment = comment;
		success = new ReportStatus(false, comment);
	}

	public void setException(Exception exception)
	{
		this.exception = exception;
		success = new ReportStatus(false, comment, exception);
	}

	public String getName()
	{
		return name;
	}

	public LinkedHashMap<String, String> getParams()
	{
		return params;
	}

	public Map<String, String> getFormulas()
	{
		return formulas;
	}

	public LinkedHashMap<String, SubActionData> getSubActionData()
	{
		if (subActionData == null)
			subActionData = new LinkedHashMap<String, SubActionData>();
		return subActionData;
	}

	public Exception getException()
	{
		return exception;
	}

	public String getComment()
	{
		return comment;
	}

	public ReportStatus getSuccess()
	{
		return success;
	}
}
