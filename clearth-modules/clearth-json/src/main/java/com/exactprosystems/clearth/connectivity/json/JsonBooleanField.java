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

package com.exactprosystems.clearth.connectivity.json;

import org.apache.commons.lang.ObjectUtils;

public class JsonBooleanField extends JsonField<Boolean>
{
	public JsonBooleanField()
	{
		super();
	}
	
	public JsonBooleanField(Boolean value)
	{
		super(value);
	}

	@Override
	public String getTextValue()
	{
		return (value != null) ? String.valueOf(value) : null;
	}
	
	@Override
	public boolean equals(Object object)
	{
		if (this == object)
			return true;
		if (!(object instanceof JsonBooleanField))
			return false;
		return ObjectUtils.equals(value, ((JsonBooleanField) object).getValue());
	}

	@Override
	public JsonBooleanField clone()
	{
		return new JsonBooleanField(value);
	}
}
