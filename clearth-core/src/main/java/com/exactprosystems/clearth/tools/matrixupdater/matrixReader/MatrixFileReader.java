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

package com.exactprosystems.clearth.tools.matrixupdater.matrixReader;

import java.util.*;

public abstract class MatrixFileReader implements MatrixReader
{
	protected Set<String> removeDuplicateFieldsHeader(String[] header, Map<Integer, String> duplicatedFields)
	{
		Set<String> newHeader = new LinkedHashSet<>();
		int i = 0;

		for (String s : header)
		{
			if(!newHeader.add(s))
				duplicatedFields.put(i, s);
			i++;
		}

		return newHeader;
	}

	protected List<String> removeDuplicatedFieldsValues(List<String> values, Map<Integer, String> duplicatedFields)
	{
		if (duplicatedFields.isEmpty())
			return values;

		List<String> updValues = new ArrayList<>();
		int i = 0;

		for (String s : values)
		{
			if (!duplicatedFields.containsKey(i))
				updValues.add(s);
			i++;
		}
		return updValues;
	}

}
