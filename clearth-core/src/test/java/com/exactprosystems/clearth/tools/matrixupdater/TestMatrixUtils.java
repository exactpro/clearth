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

package com.exactprosystems.clearth.tools.matrixupdater;

import com.exactprosystems.clearth.tools.matrixupdater.model.Block;
import com.exactprosystems.clearth.tools.matrixupdater.model.Matrix;
import com.exactprosystems.clearth.tools.matrixupdater.model.Row;
import com.exactprosystems.clearth.utils.tabledata.TableHeader;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class TestMatrixUtils
{
	public static void equalsMatrices(Matrix actual, Matrix expected)
	{
		assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
	}

	public static Matrix createMatrix()
	{
		Set<String> header = new LinkedHashSet<>();
		header.add("#Param1");
		header.add("#Param2");
		header.add("#Param3");

		List<String> row1 = new ArrayList<>();
		row1.add("1.1");
		row1.add("1.2");
		row1.add("1.3");

		List<String> row2 = new ArrayList<>();
		row2.add("");
		row2.add("2,2");
		row2.add("'2,3'");

		List<String> row3 = new ArrayList<>();
		row3.add("'3.1\"");
		row3.add("3.2");
		row3.add("3.3");

		TableHeader<String> tableHeader = new TableHeader<>(header);

		Block block = new Block(tableHeader);
		block.addAction(new Row(tableHeader, row1));
		block.addAction(new Row(tableHeader, row2));
		block.addAction(new Row(tableHeader, row3));

		Matrix matrix = new Matrix();
		matrix.addBlock(block);

		return matrix;
	}
}
