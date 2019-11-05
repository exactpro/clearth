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
import java.util.Set;
import java.util.stream.Collectors;

@JsonIgnoreProperties({"exception"})
public class SubActionData
{
	private String name;
	private Map<String, String> params;
	private Set<String> matrixInputParams;
	private Map<String, String> formulas;
	private LinkedHashMap<String, SubActionData> subActionData;
	//TODO add ExceptionWrapper after JSON reports migration to core
	public Exception exception;
	private String comment;
	private String idInTemplate;
	private ReportStatus success;

	private SubActionData(String name,
	                     Map<String, String> params,
	                     Map<String, String> formulas,
	                     LinkedHashMap<String, SubActionData> subActionData)
	{
		this.name = name;
		this.params = params;
		this.formulas = formulas;
		this.subActionData = subActionData;
		success = new ReportStatus(true);
	}

	public SubActionData(Action action)
	{
		this(action.getName(), action.getInputParams(), action.getFormulas(), action.getSubActionData());

		idInTemplate = action.getIdInTemplate();
		matrixInputParams = action.getMatrixInputParams();
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

	public Map<String, String> getParams()
	{
		return params;
	}

	public Set<String> getMatrixInputParams()
	{
		return matrixInputParams;
	}

	public Map<String, String> extractMatrixInputParams()
	{
		return matrixInputParams.stream().collect(Collectors.toMap(p -> p, params::get));
	}

	public Map<String, String> getFormulas()
	{
		return formulas;
	}

	public void setSubActionData(LinkedHashMap<String, SubActionData> subActionData)
	{
		this.subActionData = subActionData;
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

	public String getIdInTemplate()
	{
		return idInTemplate;
	}

	public void setIdInTemplate(String idInTemplate)
	{
		this.idInTemplate = idInTemplate;
	}
}
