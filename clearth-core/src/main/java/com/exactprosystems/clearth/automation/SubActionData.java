/******************************************************************************
 * Copyright 2009-2022 Exactpro Systems Limited
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

import com.exactprosystems.clearth.automation.report.ReportParamValue;
import com.exactprosystems.clearth.automation.report.ReportStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@JsonIgnoreProperties({"exception"})
public class SubActionData
{
	private String name;
	private Map<String, String> params;
	private Set<String> matrixInputParams;
	private Map<String, String> formulas, specialParams, specialParamsFormulas;
	private LinkedHashMap<String, SubActionData> subActionData;
	//TODO add ExceptionWrapper after JSON reports migration to core
	public Exception exception;
	private String comment,
			failedComment;
	private String idInTemplate;
	private ReportStatus success;

	public SubActionData(String name,
			Map<String, String> params,
			Set<String> matrixInputParams,
			Map<String, String> formulas,
			Map<String, String> specialParams,
			Map<String, String> specialParamsFormulas,
			LinkedHashMap<String, SubActionData> subActionData)
	{
		this.name = name;
		this.params = params;
		this.matrixInputParams = matrixInputParams;
		this.formulas = formulas;
		this.specialParams = specialParams;
		this.specialParamsFormulas = specialParamsFormulas;
		this.subActionData = subActionData;
		success = new ReportStatus(true);
	}

	public SubActionData(Action action)
	{
		this(action.getName(), action.getInputParams(), action.getMatrixInputParams(), action.getFormulas(),
				action.getSpecialParams(), action.getSpecialParamsFormulas(), action.getSubActionData());
		idInTemplate = action.getIdInTemplate();
		comment = action.getComment();
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
		this.failedComment = comment;
		success = new ReportStatus(false, failedComment);
	}

	public void setException(Exception exception)
	{
		this.exception = exception;
		success = new ReportStatus(false, failedComment, exception);
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

	public Map<String, ReportParamValue> extractMatrixInputParams()
	{
		return ReportParamValue.collectParamValues(matrixInputParams, params, formulas);
	}

	public Map<String, ReportParamValue> extractSpecialParams()
	{
		return ReportParamValue.collectParamValues(getSpecialParamsNames(), specialParams, specialParamsFormulas);
	}

	public Map<String, String> getFormulas()
	{
		return formulas;
	}

	public Set<String> getSpecialParamsNames()
	{
		return specialParams == null ? Collections.emptySet() : specialParams.keySet();
	}

	public Map<String, String> getSpecialParams()
	{
		return specialParams;
	}

	public Map<String, String> getSpecialParamsFormulas()
	{
		return specialParamsFormulas;
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

	public String getFailedComment()
	{
		return failedComment;
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
