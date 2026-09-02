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

package com.exactprosystems.clearth.woodpecker.configuration.start;

import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerConfigException;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.concurrent.TimeUnit;

import static com.exactprosystems.clearth.utils.ClearThEnumUtils.valueOfIgnoreCase;

public class TimeUnitAdapter extends XmlAdapter<String, TimeUnit>
{
	@Override
	public TimeUnit unmarshal(String value) throws WoodpeckerConfigException
	{
		TimeUnit tu = valueOfIgnoreCase(TimeUnit.class, value);
		if (tu == null)
			throw new WoodpeckerConfigException("'%s' isn't valid time unit name.", value);
		return tu;
	}

	@Override
	public String marshal(TimeUnit timeUnit)
	{
		return timeUnit.name().toLowerCase();
	}
}
