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

package com.exactprosystems.clearth.tools.matrixmaker;

import static com.exactprosystems.clearth.tools.matrixmaker.MMCsvUtils.acceptsFilter;
import static com.exactprosystems.clearth.tools.matrixmaker.MMCsvUtils.makeFilterParams;
import static com.exactprosystems.clearth.utils.CollectionUtils.map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertEquals;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.exactprosystems.clearth.tools.matrixmaker.initialdata.CsvInitialData;
import com.exactprosystems.clearth.tools.matrixmaker.initialdata.CsvInitializer;
import com.exactprosystems.clearth.tools.matrixmaker.initialdata.CsvInitializer.FilterParam;
import org.mvel2.MVEL;
import org.testng.annotations.Test;

public class TestMMCsvUtils
{
	private static final String FILTER_PARAMS = "\"Param1=value1\"   , \"Param2=value2.1\", Param2=value2.2,Param3=value3";
	private static final String[] FILTER_PARAMS_ARR = {"Param1=value1", "Param2=value2.1", "Param2=value2.2", "Param3=value3"};
	private static final Path RES_DIR =  Paths.get(System.getProperty("user.dir"), "src", "test", "resources",
			TestMMCsvUtils.class.getSimpleName());
	private static final String DATA_FILE = RES_DIR.resolve("dataFile.csv").toString();

	@Test
	public void testReadCsv() throws GenerateException
	{
		List<CsvInitialData.CsvInitialRow> list = MMCsvUtils.readCsv(DATA_FILE, false, null);
		assertThat(list).usingRecursiveComparison().isEqualTo(createExpectedListRead());
	}
	
	@Test
	public void testReadUniqueCsv() throws GenerateException
	{
		List<CsvInitialData.CsvInitialRow> list = MMCsvUtils.readUniqueCsv(DATA_FILE, "P1", false);
		assertThat(list).usingRecursiveComparison().isEqualTo(createExpectedListUnique());
	}
	
	@Test
	public void testReadCsvByFilter() throws GenerateException
	{
		Map<String, CsvInitializer.FilterParam> map = new HashMap<>();
		map.put("P1", new FilterParam("P1", "1"));
		List<CsvInitialData.CsvInitialRow> list = MMCsvUtils.readCsvByFilter(DATA_FILE, map, null);
		assertThat(list).usingRecursiveComparison().isEqualTo(createExpectedListFilter());
	}
	
	@Test
	public void testReadCsvByMvel() throws GenerateException
	{
		List<CsvInitialData.CsvInitialRow> list = MMCsvUtils.readCsvByMvel(RES_DIR.resolve("dataFileInt.csv").toString(),
				MVEL.compileExpression("row.P1 > 1 && row.P1 < 6"), "");
		assertThat(list).usingRecursiveComparison().isEqualTo(createExpectedListMvel());
	}

	@Test
	public void testSplitLine() throws GenerateException
	{
		String[] actual = MMCsvUtils.splitLine(FILTER_PARAMS);
		assertEquals(FILTER_PARAMS_ARR, actual);
	}

	@Test
	public void testAcceptsFilter()
	{
		Map<String, FilterParam> filterParams = makeFilterParams(FILTER_PARAMS_ARR);
		Map<String, String> map = new HashMap<>();
		map.put("Param1", "value1");
		map.put("Param2", "value2.2");
		map.put("Param3", "value3");

		assertTrue(acceptsFilter(map, filterParams));
		map.put("Param2", "value2.3");

		assertFalse(acceptsFilter(map, filterParams));
	}
	
	private List<CsvInitialData.CsvInitialRow> createExpectedListRead()
	{
		List<CsvInitialData.CsvInitialRow> list = new ArrayList<>();
		list.add(new CsvInitialData.CsvInitialRow(2, map("P1", "1", "P2", "2")));
		list.add(new CsvInitialData.CsvInitialRow(3, map("P1", "1", "P2", "3")));
		list.add(new CsvInitialData.CsvInitialRow(4, map("P1", "2", "P2", "2")));
		list.add(new CsvInitialData.CsvInitialRow(5, map("P1", "Pp", "P2", "Gg")));
		list.add(new CsvInitialData.CsvInitialRow(6, map("P1", "1.5", "P2", "8,55")));
		
		return list;
	}
	
	private List<CsvInitialData.CsvInitialRow> createExpectedListUnique()
	{
		List<CsvInitialData.CsvInitialRow> list = new ArrayList<>();
		list.add(new CsvInitialData.CsvInitialRow(2, map("P1", "1", "P2", "2")));
		list.add(new CsvInitialData.CsvInitialRow(3, map("P1", "2", "P2", "2")));
		list.add(new CsvInitialData.CsvInitialRow(4, map("P1", "//1", "P2", "2")));
		list.add(new CsvInitialData.CsvInitialRow(5, map("P1", "Pp", "P2", "Gg")));
		list.add(new CsvInitialData.CsvInitialRow(6, map("P1", "1.5", "P2", "8,55")));
		
		return list;
	}
	
	private List<CsvInitialData.CsvInitialRow> createExpectedListFilter()
	{
		List<CsvInitialData.CsvInitialRow> list = new ArrayList<>();
		list.add(new CsvInitialData.CsvInitialRow(2, map("P1", "1", "P2", "2")));
		list.add(new CsvInitialData.CsvInitialRow(3, map("P1", "1", "P2", "3")));
		
		return list;
	}
	
	private List<CsvInitialData.CsvInitialRow> createExpectedListMvel()
	{
		List<CsvInitialData.CsvInitialRow> list = new ArrayList<>();
		list.add(new CsvInitialData.CsvInitialRow(2, map("P1", "2", "P2", "2")));
		list.add(new CsvInitialData.CsvInitialRow(3, map("P1", "4", "P2", "3")));

		return list;
	}
}
