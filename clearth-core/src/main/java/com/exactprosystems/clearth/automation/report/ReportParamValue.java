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

package com.exactprosystems.clearth.automation.report;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@JsonInclude(Include.NON_NULL)
public class ReportParamValue
{
	private String value, formula;

	public ReportParamValue()
	{}

	public ReportParamValue(String value)
	{
		this.value = value;
	}

	public String getValue()
	{
		return value;
	}

	public void setValue(String value)
	{
		this.value = value;
	}

	public String getFormula()
	{
		return formula;
	}

	public void setFormula(String formula)
	{
		this.formula = formula;
	}

	public static ReportParamValue createReportParamValue(String paramName,
			Map<String, String> params,
			Map<String, String> formulas)
	{
		ReportParamValue value = new ReportParamValue(params.get(paramName));
		if (formulas != null)
			value.setFormula(formulas.get(paramName));
		return value;
	}

	public static Map<String, ReportParamValue> collectParamValues(Set<String> paramNames,
			Map<String, String> params,
			Map<String, String> formulas)
	{
		return paramNames.stream().collect(Collectors.toMap(Function.identity(),
				p -> createReportParamValue(p, params, formulas)));
	}
}
