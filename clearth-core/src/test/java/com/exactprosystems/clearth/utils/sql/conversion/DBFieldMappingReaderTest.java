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

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;


public class DBFieldMappingReaderTest {
	@Test
	public void parseFileTest() throws IOException {
		DBFieldMappingReader reader = new DBFieldMappingReader();
		
		File file = FileUtils.toFile(ClassLoader.getSystemClassLoader().getResource("DBFieldMapping/testDBMapping.csv"));
		
		if (file == null)
			throw new FileNotFoundException("DBFieldMapping/testDBMapping.csv file not found");
		
		List<DBFieldMapping> mappings = reader.readEntities(file);

		for (DBFieldMapping mapping : mappings) {
			System.out.println(mapping);
		}
	}

	@Test
	public void testVisualisationValues() throws IOException
	{
		List<DBFieldMapping> mapping = createMappingFile();
		Map<String, String> visualisations0 = mapping.get(0).getVisualizations();
		Map<String, String> visualisations1 = mapping.get(1).getVisualizations();
		Map<String, String> visualisations2 = mapping.get(2).getVisualizations();

		Assert.assertEquals(0, visualisations0.size());
		Assert.assertEquals(0, visualisations1.size());
		Assert.assertEquals("{b=newB, bb=newBB, bbb=null}", visualisations2.toString());
	}

	@Test
	public void testConversionValues() throws IOException
	{
		List<DBFieldMapping> mapping = createMappingFile();
		BidiMap<String, String> conversions = mapping.get(0).getConversions();
		Assert.assertEquals("{a=newA, aa=newAA, aaa=null}", conversions.toString());
	}

	private List<DBFieldMapping> createMappingFile() throws IOException
	{
		DBFieldMappingReader mappingReader = new DBFieldMappingReader();
		String fileName = "DBFieldMapping/testMappingFilePairs.csv";
		File file = FileUtils.toFile(ClassLoader.getSystemClassLoader().getResource(fileName));

		if (file == null)
			throw new FileNotFoundException(fileName + " file not found");

		return mappingReader.readEntities(file);
	}
}
