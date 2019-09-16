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

package com.exactprosystems.clearth.utils.multidata;

import java.util.ArrayList;
import java.util.List;

public class MultiData<T>
{
	private int firstIndex;
	private List<T> data;
	
	public MultiData(int firstIndex)
	{
		this.firstIndex = firstIndex;
		this.data = new ArrayList<T>();
	}

	
	public int getFirstIndex()
	{
		return firstIndex;
	}

	public void setFirstIndex(int firstIndex)
	{
		this.firstIndex = firstIndex;
	}
	
	
	public void addData(T data)
	{
		this.data.add(data);
	}
	
	public void removeData(int index)
	{
		int realIndex = calculateRealIndex(index);
		if ((realIndex >= 0) && (realIndex < data.size()))
			data.remove(realIndex);
	}
	
	public T getData(int index)
	{
		int realIndex = calculateRealIndex(index);
		if ((index < firstIndex) || (data.size() <= realIndex))
			return null;
		
		return data.get(realIndex);
	}
	
	public List<T> getData()
	{
		return data;
	}
	
	
	protected int calculateRealIndex(int index)
	{
		return index-firstIndex;
	}
}
