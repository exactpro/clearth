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

package com.exactprosystems.clearth.data.th2.tables;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ParamRow extends KeyValueRow
{
	@JsonProperty("Expression")
	private final String expression;
	
	public ParamRow(String name, String value, String expression)
	{
		super(name, value);
		this.expression = expression;
	}
	
	
	public String getExpression()
	{
		return expression;
	}
}
