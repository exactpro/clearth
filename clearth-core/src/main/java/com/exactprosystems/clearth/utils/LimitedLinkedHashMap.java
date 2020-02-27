/******************************************************************************
 * Copyright 2009-2020 Exactpro Systems Limited
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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class LimitedLinkedHashMap<K,V> extends LinkedHashMap<K,V> {

	private int limit = 0;
	private Consumer<Map.Entry<K,V>> action;

	public LimitedLinkedHashMap(int initialCapacity, float loadFactor, int limit) {
		super(initialCapacity, loadFactor);
		this.limit = limit;
	}

	public LimitedLinkedHashMap(int initialCapacity, int limit) {
		super(initialCapacity);
		this.limit = limit;
	}

	public LimitedLinkedHashMap(int limit) {
		this.limit = limit;
	}

	public LimitedLinkedHashMap(Map<? extends K, ? extends V> m, int limit) {
		super(m);
		this.limit = limit;
	}

	public LimitedLinkedHashMap(int initialCapacity, float loadFactor, boolean accessOrder, int limit) {
		super(initialCapacity, loadFactor, accessOrder);
		this.limit = limit;
	}

	public void setAction(Consumer<Map.Entry<K, V>> action) {
		this.action = action;
	}

	@Override
	protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
		boolean remove;
		if (remove = (size() > limit) && action != null) {
			action.accept(eldest);
		}
		return remove;
	}


}
