/******************************************************************************
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

package com.exactprosystems.clearth.tools.matrixupdater.model;

import com.exactprosystems.clearth.utils.FileOperationUtils;
import org.assertj.core.api.Assertions;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class AdditionTest
{
	private Path resDir;

	@BeforeClass
	public void init() throws FileNotFoundException
	{
		resDir = Paths.get(FileOperationUtils.resourceToAbsoluteFilePath(AdditionTest.class.getSimpleName()));
	}

	@Test
	public void testGetAdditions() throws IOException
	{
		Addition addition = new Addition(resDir.resolve("addition.csv").toFile(), false);
		List<Map<String, String>> additions = addition.listAdditions();
		Assertions.assertThat(additions).usingRecursiveComparison().isEqualTo(createAdditions());
	}

	private List<Map<String, String>> createAdditions()
	{
		Map<String, String> record1 = new LinkedHashMap<>(),
							record2 = new LinkedHashMap<>();
		record1.put("#id", "id1");
		record1.put("#GlobalStep", "Step1");
		record1.put("#Action", "NewAction");
		record1.put("#Param1", "222");
		record1.put("#Param2", "111");

		record2.put("#id", "id2");
		record2.put("#GlobalStep", "Step1");
		record2.put("#Action", "NewAction");
		record2.put("#Param1", "777");
		record2.put("#Param2", "999");

		List<Map<String, String>> list = new ArrayList<>();
		list.add(record1);
		list.add(record2);
		return list;
	}
}