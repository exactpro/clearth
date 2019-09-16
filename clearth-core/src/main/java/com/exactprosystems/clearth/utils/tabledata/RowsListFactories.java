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

package com.exactprosystems.clearth.utils.tabledata;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class RowsListFactories
{
	private static final RowsListFactory ARRAY_LIST_FACTORY = new ArrayListFactory();
	private static final RowsListFactory LINKED_LIST_FACTORY = new LinkedListFactory();
	
	
	public static <A, B> RowsListFactory<A, B> arrayListFactory()
	{
		return (RowsListFactory<A, B>) ARRAY_LIST_FACTORY;
	}
	
	public static <A, B> RowsListFactory<A, B> linkedListFactory()
	{
		return (RowsListFactory<A, B>) LINKED_LIST_FACTORY;
	}
	
	
	private static class ArrayListFactory<A, B> implements RowsListFactory<A, B>
	{
		@Override
		public List<TableRow<A, B>> createRowsList()
		{
			return new ArrayList<TableRow<A, B>>();
		}
	}
	
	private static class LinkedListFactory<A, B> implements RowsListFactory<A, B>
	{
		@Override
		public List<TableRow<A, B>> createRowsList()
		{
			return new LinkedList<TableRow<A, B>>();
		}
	}
}
