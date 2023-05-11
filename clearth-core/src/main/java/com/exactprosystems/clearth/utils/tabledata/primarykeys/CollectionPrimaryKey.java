/*******************************************************************************
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

package com.exactprosystems.clearth.utils.tabledata.primarykeys;

import com.exactprosystems.clearth.utils.CommaBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Primary key that uses collection of values
 * @param <B> class of primary key values; to work properly, it must override {@link Object#hashCode()},
 * {@link Object#equals(Object)} and {@link Object#toString()} methods
 */
public class CollectionPrimaryKey<B> implements PrimaryKey
{
	protected final List<B> values;

	public CollectionPrimaryKey(List<B> values)
	{
		this.values = Collections.unmodifiableList(values);
	}

	@Override
	public String toString()
	{
		CommaBuilder builder = new CommaBuilder(",");
		for (B value : values)
			builder.append(value == null ? "null" : "\"" + value + "\"");
		
		return builder.toString();
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CollectionPrimaryKey<?> that = (CollectionPrimaryKey<?>) o;
		return Objects.equals(values, that.values);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(values);
	}

	public List<B> toList()
	{
		return values;
	}
}