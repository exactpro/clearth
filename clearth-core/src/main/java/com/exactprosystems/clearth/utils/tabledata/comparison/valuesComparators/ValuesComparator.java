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

package com.exactprosystems.clearth.utils.tabledata.comparison.valuesComparators;

/**
 * Interface to compare table data values.
 * @param <A> class of column member.
 * @param <B> class of table values.
 */
@FunctionalInterface
public interface ValuesComparator<A, B>
{
	/**
	 * Compares two specified values of one column.
	 * @param value1 first value of comparison, usually marked as 'expected'.
	 * @param value2 second value of comparison, usually marked as 'actual'.
	 * @param column name for both values.
	 * @throws Exception if any errors occurred while performing comparison.
	 * @return {@code true}, if values are equal according to comparison logic; {@code false} otherwise.
	 */
	boolean compareValues(B value1, B value2, A column) throws Exception;
}
