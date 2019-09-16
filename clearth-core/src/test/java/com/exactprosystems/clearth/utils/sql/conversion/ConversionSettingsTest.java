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

package com.exactprosystems.clearth.utils.sql.conversion;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ConversionSettingsTest {
	private ConversionSettings testSettings;

	@Before
	public void initTestConverter() throws IOException {
		File file = FileUtils.toFile(ClassLoader.getSystemClassLoader().getResource("DBFieldMapping/testDBMapping.csv"));

		if (file == null)
			throw new FileNotFoundException("DBFieldMapping/testDBMapping.csv file not found");
		
		testSettings = ConversionSettings.loadFromCSVFile(file);
	}

	@Test
	public void getDBHeader() {
		assertEquals("A", testSettings.getDBHeader("tableA"));
		assertEquals("B", testSettings.getDBHeader("tableB"));
		assertEquals("tableC", testSettings.getDBHeader("tableC"));
		assertEquals("tableD", testSettings.getDBHeader("tableD"));
	}

	@Test
	public void getTableHeader() {
		assertEquals("tableA", testSettings.getTableHeader("A"));
		assertEquals("tableB", testSettings.getTableHeader("B"));
		assertEquals("C", testSettings.getTableHeader("C"));
		assertEquals("D", testSettings.getTableHeader("D"));
	}

	@Test
	public void getConvertedDBValue() {
		assertEquals("a", testSettings.getConvertedDBValue("A", "newA"));
		assertEquals("aaa", testSettings.getConvertedDBValue("A", "aaa"));
		assertEquals("cc", testSettings.getConvertedDBValue("C", "cc"));
		assertEquals("d", testSettings.getConvertedDBValue("D", "newD"));
	}

	@Test
	public void getConvertedTableValue() {
		assertEquals("newA", testSettings.getConvertedTableValue("tableA", "a"));
		assertEquals("newAA", testSettings.getConvertedTableValue("tableA", "aa"));
		assertEquals("d", testSettings.getConvertedTableValue("tableD", "d"));
		assertEquals("newCC", testSettings.getConvertedTableValue("tableC", "cc"));
	}

	@Test
	public void createConvertedParams() {
		Map<String, String> params = new HashMap<String, String>();
		params.put("tableA", "a");
		params.put("tableB", "bb");
		params.put("A", "a");
		params.put("tableC", "ccc");

		Map<String, String> expectedParams = new HashMap<String, String>();
		expectedParams.put("tableA", "newA");
		expectedParams.put("tableB", "newBB");
		expectedParams.put("A", "a");
		expectedParams.put("tableC", "ccc");

		assertEquals(expectedParams, testSettings.createConvertedParams(params));
	}
}
