/******************************************************************************
 * Copyright 2009-2022 Exactpro Systems Limited
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

package com.exactprosystems.clearth.utils.sql.conversion;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ConversionSettings
{
	private static final Logger log = LoggerFactory.getLogger(ConversionSettings.class);
	// Mapping list representation in Map, created to avoid cycle searching in list
	private final Map<String, DBFieldMapping> mappingsByDBField; 	// map by db field name
	private final Map<String, DBFieldMapping> mappingsByTableField;	// map by table field name

	private final BidiMap<String, String> headersDBtoTable;			// Key - DB column name, Value - table field name

	private final List<DBFieldMapping> mappings;

	public ConversionSettings(List<DBFieldMapping> mappings) {
		if (CollectionUtils.isEmpty(mappings)) {
			throw new IllegalArgumentException("Empty or null mapping list statement");
		}

		this.mappings = mappings;
		this.mappingsByDBField = initMappingsByDBField(mappings);
		this.mappingsByTableField = initMappingsByTableField(mappings);
		this.headersDBtoTable = initHeadersDBtoTableMap(mappings);
	}

	/**
	 * Returns DB header related to specified table header depending on mapping
	 * @param tableHeader table header
	 * @return DB header
	 */
	public String getDBHeader(String tableHeader) {
		return headersDBtoTable.containsValue(tableHeader) ? headersDBtoTable.getKey(tableHeader) : tableHeader;
	}

	/**
	 * Returns table header related to specified DB header depending on mapping
	 * @param dbHeader DB header
	 * @return table header
	 */
	public String getTableHeader(String dbHeader) {
		return MapUtils.getString(headersDBtoTable, dbHeader, dbHeader);
	}

	/**
	 * Returns converted value that belongs to specified DB header
	 * @param dbHeader DB column
	 * @param value value defined in specified DB column
	 * @return converted value if it described in mapping, if there is no description for the column returns same value
	 */
	public String getConvertedDBValue(String dbHeader, String value) {
		DBFieldMapping mapping = mappingsByDBField.get(dbHeader);
		return mapping != null ? convertValue(value, mapping.getConversions().inverseBidiMap()) : value;
	}

	/**
	 * Returns converted value that belongs to specified table header
	 * @param tableHeader table header
	 * @param value value defined in specified table header
	 * @return converted value if it described in mapping, if there is no description for the header returns same value
	 */
	public String getConvertedTableValue(String tableHeader, String value) {
		DBFieldMapping mapping = mappingsByTableField.get(tableHeader);
		return mapping != null ? convertValue(value, mapping.getConversions()) : value;
	}

	/**
	 * Returns params map with converted values
	 * @param params original params map
	 * @return converted params map
	 */
	public Map<String, String> createConvertedParams(Map<String, String> params) {
		Map<String, String> convertedParams = new LinkedHashMap<String, String>();
		for (String param : params.keySet()) {
			convertedParams.put(param, getConvertedTableValue(param, params.get(param)));
		}
		return convertedParams;
	}

	protected String convertValue(String value, Map<String, String> conversions) {
		String convertedValue = conversions.get(value);
		if (convertedValue != null) {
			return convertedValue;
		}
		return value;
	}


	protected Map<String, DBFieldMapping> initMappingsByTableField(List<DBFieldMapping> mappings)
	{
		Map<String, DBFieldMapping> resultMap = new LinkedHashMap<String, DBFieldMapping>();
		for (DBFieldMapping mapping : mappings) {
			resultMap.put(mapping.getSrcField(), mapping);
		}
		return resultMap;
	}

	protected Map<String, DBFieldMapping> initMappingsByDBField(List<DBFieldMapping> mappings)
	{
		Map<String, DBFieldMapping> resultMap = new LinkedHashMap<String, DBFieldMapping>();
		for (DBFieldMapping mapping : mappings) {
			resultMap.put(mapping.getDestField(), mapping);
		}
		return resultMap;
	}

	protected BidiMap<String, String> initHeadersDBtoTableMap(List<DBFieldMapping> mappings)
	{
		BidiMap<String, String> headersDBToTableMap = new DualHashBidiMap<String, String>();
		for (DBFieldMapping mapping : mappings) {
			String matrixField = mapping.getSrcField(),
					dbField = mapping.getDestField();
			if (StringUtils.isNotBlank(matrixField) && StringUtils.isNotBlank(dbField)) {
				headersDBToTableMap.put(dbField, matrixField);
			}
		}
		return headersDBToTableMap;
	}

	public List<DBFieldMapping> getMappings()
	{
		return mappings;
	}

	/**
	 * A factory method for creation settings described in CSV file
	 * @param file csv file
	 * @return an instance of ConversionSettings
	 * @throws IOException
	 */
	public static ConversionSettings loadFromCSVFile(File file) throws IOException {
		List<DBFieldMapping> mappings = new DBFieldMappingReader().readEntities(file);
		return new ConversionSettings(mappings);
	}
}
