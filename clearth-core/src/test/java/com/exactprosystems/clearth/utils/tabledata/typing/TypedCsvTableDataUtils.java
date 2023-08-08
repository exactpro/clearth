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

package com.exactprosystems.clearth.utils.tabledata.typing;

import com.exactprosystems.clearth.utils.tabledata.TableRow;
import org.assertj.core.api.Assertions;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.exactprosystems.clearth.utils.tabledata.typing.TableDataType.STRING;

public class TypedCsvTableDataUtils
{
	protected static final String SEPARATOR = System.lineSeparator();
	public static final String EXPECTED_HEADER = "name1,name2,name3" + SEPARATOR,
			EXPECTED_ROW = "name1,name2,name3" + SEPARATOR + "1,2,3" + SEPARATOR,
			EXPECTED_ROWS = "name1,name2,name3" + SEPARATOR + "1,2,3" + SEPARATOR + "4,5,6" + SEPARATOR + "7,8,9" +SEPARATOR;

	public static void assertHeaders(Set<TypedTableHeaderItem> actualHeader,
					Set<TypedTableHeaderItem> expectedHeader)
	{
		Assertions.assertThat(actualHeader).usingRecursiveComparison().isEqualTo(expectedHeader);
	}

	public static void assertTypedTableData(TypedTableData actualTableData, TypedTableData expectedTableData)
	{
		Assertions.assertThat(actualTableData).usingRecursiveComparison().isEqualTo(expectedTableData);
	}

	public static TypedTableHeader createHeader()
	{
		Set<TypedTableHeaderItem> headerSetItems = new LinkedHashSet<>();
		headerSetItems.add(new TypedTableHeaderItem("name1", STRING));
		headerSetItems.add(new TypedTableHeaderItem("name2", STRING));
		headerSetItems.add(new TypedTableHeaderItem("name3", STRING));

		return new TypedTableHeader(headerSetItems);
	}

	public static TableRow<TypedTableHeaderItem, Object> createRow(TypedTableHeader header,
					String value1, String value2, String value3)
	{
		List<Object> listValues = new ArrayList<>();
		listValues.add(value1);
		listValues.add(value2);
		listValues.add(value3);

		return new TypedTableRow(header, listValues);
	}

	public static List<TableRow<TypedTableHeaderItem, Object>> createRows(TypedTableHeader header)
	{
		List<TableRow<TypedTableHeaderItem, Object>> rowList = new ArrayList<>();
		rowList.add(createRow(header, "1", "2", "3"));
		rowList.add(createRow(header, "4", "5", "6"));
		rowList.add(createRow(header, "7", "8", "9"));
		return rowList;
	}

	public static TypedTableData createTypedTableData(TypedTableHeader header,
					List<TableRow<TypedTableHeaderItem, Object>> rows)
	{
		TypedTableData tableData = new TypedTableData(header);
		for (TableRow<TypedTableHeaderItem, Object> row : rows)
			tableData.add(row);
		return tableData;
	}
}
