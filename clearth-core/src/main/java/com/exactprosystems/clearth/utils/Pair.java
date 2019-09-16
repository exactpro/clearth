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

package com.exactprosystems.clearth.utils;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.io.Serializable;

public class Pair<A extends Object, B extends Object> implements Serializable
{
	private A first = null;
	private B second = null;

	public Pair() {
	}
	
	public Pair(A first, B second)
	{
		this.first = first;
		this.second = second;
	}

	
	public A getFirst()
	{
		return first;
	}

	public void setFirst(A first)
	{
		this.first = first;
	}


	public B getSecond()
	{
		return second;
	}
	
	public void setSecond(B second)
	{
		this.second = second;
	}
	
	@Override
	public String toString() {
		return String.valueOf(this.first) + "=" + String.valueOf(this.second);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;

		if (o == null || getClass() != o.getClass()) return false;

		Pair<?, ?> pair = (Pair<?, ?>) o;

		return new EqualsBuilder()
				.append(first, pair.first)
				.append(second, pair.second)
				.isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37)
				.append(first)
				.append(second)
				.toHashCode();
	}
}
