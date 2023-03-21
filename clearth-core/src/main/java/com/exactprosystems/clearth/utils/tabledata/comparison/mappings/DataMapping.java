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

package com.exactprosystems.clearth.utils.tabledata.comparison.mappings;

import com.exactprosystems.clearth.utils.tabledata.comparison.mappings.descs.FieldDesc;
import com.exactprosystems.clearth.utils.tabledata.comparison.mappings.descs.MappingDesc;
import com.exactprosystems.clearth.utils.tabledata.readers.HeaderMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import java.math.BigDecimal;
import java.util.*;

import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;

public abstract class DataMapping<A>
{
	private final Map<A, A> expectedConversionMap;
	private final Map<A, A> actualConversionMap;
	private final Set<A> keyColumns;
	private final Map<A, BigDecimal> numericColumns;
	private final Set<A> ignoreColumns;
	private final Set<A> infoColumns;
	
	public DataMapping(MappingDesc mappingDesc)
	{
		Map<A, A> expectedConversionMap = new HashMap<>();
		Map<A, A> actualConversionMap = new HashMap<>();
		Set<A> keyColumns = new HashSet<>();
		Map<A, BigDecimal> numericColumns = new HashMap<>();
		Set<A> ignoreColumns = new HashSet<>();
		Set<A> infoColumns = new HashSet<>();
		
		handleMapping(mappingDesc, expectedConversionMap, actualConversionMap, keyColumns, numericColumns, 
				ignoreColumns, infoColumns);

		this.expectedConversionMap = unmodifiableMap(expectedConversionMap);
		this.actualConversionMap = unmodifiableMap(actualConversionMap);
		this.keyColumns = unmodifiableSet(keyColumns);
		this.numericColumns = unmodifiableMap(numericColumns);
		this.ignoreColumns = unmodifiableSet(ignoreColumns);
		this.infoColumns = unmodifiableSet(infoColumns);
	}

	protected void handleAdditionalKeyColumns(Set<A> keyColumns, Set<A> additionalKeyColumns)
	{
		if (!CollectionUtils.isEmpty(additionalKeyColumns))
			keyColumns.addAll(additionalKeyColumns);
	}

	protected void handleAdditionalNumericColumns(Map<A, BigDecimal> numericColumns, Map<A, BigDecimal> additionalNumericColumns)
	{
		if (!MapUtils.isEmpty(additionalNumericColumns))
			numericColumns.putAll(additionalNumericColumns);
	}

	protected void handleMapping(MappingDesc mappingDesc, Map<A, A> expectedConversionMap,
	                             Map<A, A> actualConversionMap,
	                         Set<A> keyColumns, Map<A, BigDecimal> numericColumns, Set<A> ignoredColumns, Set<A> infoColumns)
	{
		for (FieldDesc fieldDesc : mappingDesc.getFields())
		{
			A localName = transform(fieldDesc.getLocalName());
			A expectedName = transform(fieldDesc.getExpectedName());
			A actualName = transform(fieldDesc.getActualName());

			expectedConversionMap.put(expectedName, localName);
			actualConversionMap.put(actualName, localName);
			if (fieldDesc.isKey())
				keyColumns.add(localName);
			if (fieldDesc.isNumeric())
				numericColumns.put(localName, fieldDesc.getPrecision());
			if (fieldDesc.isIgnore())
				ignoredColumns.add(localName);
			if (fieldDesc.isInfo())
				infoColumns.add(localName);
		}
	}

	protected abstract A transform(String name);

	public Set<A> getKeyColumns()
	{
		return keyColumns;
	}

	public Map<A, BigDecimal> getNumericColumns()
	{
		return numericColumns;
	}

	public HeaderMapper<A> getHeaderMapper(boolean forExpected)
	{
		return new HeaderMapper<>(forExpected ? expectedConversionMap : actualConversionMap);
	}

	public boolean isNumeric(A column)
	{
		return numericColumns.containsKey(column);
	}
	
	public boolean isIgnore(A column)
	{
		return ignoreColumns.contains(column);
	}

	public boolean isInfo(A column)
	{
		return infoColumns.contains(column);
	}
}