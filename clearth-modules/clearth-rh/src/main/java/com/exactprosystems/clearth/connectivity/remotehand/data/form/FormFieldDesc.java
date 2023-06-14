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

package com.exactprosystems.clearth.connectivity.remotehand.data.form;

public class FormFieldDesc
{
	private String id;
	private String paramName;
	private String frontendParamName;
	private FormFieldType type;
	private boolean required;
	private boolean enabled = true;
	
	public String getId()
	{
		return id;
	}
	
	public void setId(String value)
	{
		this.id = value;
	}
	
	
	public FormFieldType getType()
	{
		return type == null ? FormFieldType.TEXT : type;
	}
	
	public void setType(FormFieldType value)
	{
		this.type = value;
	}
	
	
	public boolean isRequired()
	{
		return required;
	}
	
	public void setRequired(boolean value)
	{
		this.required = value;
	}
	
	
	public boolean isEnabled()
	{
		return enabled;
	}
	
	public void setEnabled(boolean value)
	{
		this.enabled = value;
	}
	
	
	public String getParamName()
	{
		return paramName;
	}
	
	public void setParamName(String paramName)
	{
		this.paramName = paramName;
	}
	
	
	public String getFrontendParamName()
	{
		return frontendParamName;
	}
	
	public void setFrontendParamName(String frontendParamName)
	{
		this.frontendParamName = frontendParamName;
	}
}
