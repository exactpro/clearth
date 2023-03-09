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

import java.math.BigDecimal;

public class JsonNumericField extends JsonField<BigDecimal>
{
	public JsonNumericField()
	{
		super();
	}
	
	public JsonNumericField(BigDecimal value)
	{
		super(value);
	}

	@Override
	public String getTextValue()
	{
		return (value != null) ? value.stripTrailingZeros().toPlainString() : null;
	}
	
	@Override
	public boolean equals(Object object)
	{
		if (this == object)
			return true;
		if (!(object instanceof JsonNumericField))
			return false;
		JsonNumericField field = (JsonNumericField) object;
		return ObjectUtils.compare(this.value, field.value) == 0;
	}

	@Override
	public JsonNumericField clone()
	{
		return new JsonNumericField(value);
	}
}
