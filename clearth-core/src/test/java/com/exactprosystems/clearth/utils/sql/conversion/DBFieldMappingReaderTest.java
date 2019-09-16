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
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;


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
}
