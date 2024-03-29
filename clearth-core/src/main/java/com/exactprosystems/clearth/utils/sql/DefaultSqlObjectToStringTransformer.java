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

package com.exactprosystems.clearth.utils.sql;

import com.exactprosystems.clearth.utils.ObjectToStringTransformer;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.stream.Collectors;

public class DefaultSqlObjectToStringTransformer implements ObjectToStringTransformer
{
	@Override
	public String transform(Object value) throws SQLException, IOException
	{
		if (value == null)
			return null;

		if(value instanceof Clob)
		{
			try (BufferedReader reader = new BufferedReader(((Clob) value).getCharacterStream()))
			{
				return reader.lines().collect(Collectors.joining(System.lineSeparator()));
			}
		}

		if (value instanceof Number)
			return SQLUtils.getStringFromNumber((Number)value);
		return extendedTransform(value);
	}

	protected String extendedTransform(Object value) throws SQLException, IOException
	{
		return value.toString();
	}
}
