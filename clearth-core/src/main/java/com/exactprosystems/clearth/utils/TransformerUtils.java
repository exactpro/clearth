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

package com.exactprosystems.clearth.utils;

import com.exactprosystems.clearth.utils.tabledata.StringTableData;
import com.exactprosystems.clearth.utils.tabledata.TableRow;

public class TransformerUtils
{
	/**
	 * Transforms data in StringTableData according to specified IValueTransformer
	 * @param stringTableData StringTableData instance to transform
	 * @param transformer IValueTransformer instance
	 */
	public static void transformStringTableData(StringTableData stringTableData, IValueTransformer transformer) {
		for (TableRow<String, String> row : stringTableData) {
			for (String column : row.getHeader()) {
				row.setValue(column, transformer.transform(row.getValue(column)));
			}
		}
	}
}
