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

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.isNotEmpty;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;

public class CollectionUtils
{	
	@SuppressWarnings("rawtypes")
	public static String join(Map map, String entrySeparator, String keyValueSeparator, String valueWrapper)
	{
		if (MapUtils.isEmpty(map))
			return "";
		
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		
		for (Object key : map.keySet())
		{
			if (first)
				first = false;
			else 
				sb.append(entrySeparator);
			
			sb.append(key).append(keyValueSeparator).append(valueWrapper).append(map.get(key)).append(valueWrapper);
		}
		return sb.toString();
	}

	@SuppressWarnings("rawtypes")
	public static String join(Map map, String entrySeparator, String keyValueSeparator)
	{
		return join(map, entrySeparator, keyValueSeparator, "'");
	}
	
	@SuppressWarnings("rawtypes")
	public static String join(Map map)
	{
		return join(map, ", ", " = ");
	}
	
	public static String join(Collection<String> collection, String wrapper, String separator)
	{
		if (isEmpty(collection))
			return "";
		
		CommaBuilder cb = new CommaBuilder(separator);
		for (String s : collection)
		{
			if (isNotEmpty(wrapper))
				s = wrapper + s + wrapper;
			cb.append(s);
		}
		return cb.toString();
	}
	
	public static String join(Collection<String> collection)
	{
		return join(collection, "'", ", ");
	}
	
	
	public static <T> Map<T, T> map(T... params)
	{
		if (params.length == 0)
			return Collections.emptyMap();
		else if (params.length == 2)
			return Collections.singletonMap(params[0], params[1]);
		else 
		{
			Map<T, T> map = new LinkedHashMap<>(params.length / 2);
			for (int i = 1; i < params.length; i += 2)
			{
				map.put(params[i - 1], params[i]);
			}
			return map;
		}
	}
	
	@SuppressWarnings("unchecked")
	public static <K, V> Map<K, V> mapOf(Object... params)
	{
		if (params.length == 0)
			return Collections.emptyMap();
		else if (params.length == 2)
			return Collections.singletonMap((K)params[0], (V)params[1]);
		else
		{
			Map<K, V> map = new LinkedHashMap<>(params.length / 2);
			for (int i = 1; i < params.length; i += 2)
			{
				map.put((K)params[i - 1], (V)params[i]);
			}
			return map;
		}
	}
}
