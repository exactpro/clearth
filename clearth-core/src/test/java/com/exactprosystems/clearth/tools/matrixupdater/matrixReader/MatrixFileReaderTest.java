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

package com.exactprosystems.clearth.tools.matrixupdater.matrixReader;

import com.exactprosystems.clearth.tools.matrixupdater.model.Block;
import com.exactprosystems.clearth.tools.matrixupdater.model.Matrix;
import com.exactprosystems.clearth.tools.matrixupdater.model.Row;
import com.exactprosystems.clearth.tools.matrixupdater.model.XlsMatrix;
import com.exactprosystems.clearth.utils.FileOperationUtils;
import com.exactprosystems.clearth.utils.tabledata.TableHeader;
import org.assertj.core.api.Assertions;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

public class MatrixFileReaderTest
{
	@DataProvider(name = "duplicatedFields")
	public Object[][] duplicatedFields()
	{
		return new Object[][]
			{
				{"matrixWithDuplicatedHeaderFieldsCsv.csv", new CsvMatrixReader(),
						createMatrix(new Matrix(), true)},
				{"matrixWithDuplicatedHeaderFieldsXls.xls", new XlsMatrixReader(),
						createMatrix(new XlsMatrix("matrixWithDuplicatedHeaderFields"), false)}
			};
	}

	@Test(dataProvider = "duplicatedFields")
	public void testReadMatrixWithDuplicatedHeaderFields(String fileName, MatrixReader matrixReader, Matrix expectedMatrix) throws IOException {
		File file = new File(FileOperationUtils.resourceToAbsoluteFilePath(
				Paths.get(MatrixFileReaderTest.class.getSimpleName()).resolve(fileName).toString()));

		Matrix matrix = matrixReader.readMatrix(file);
		Assertions.assertThat(matrix).usingRecursiveComparison().isEqualTo(expectedMatrix);
	}

	private Matrix createMatrix(Matrix matrix, boolean hasCsvExt)
	{
		matrix.addBlock(createBlock(Arrays.asList("id1", "Step1", "SetStatic", "0", "AAD", "BBQ", "100"),
									Arrays.asList("id2", "Step1", "SetStatic", "1000", "xxx", "yyy", "100")));

		if (hasCsvExt)
		{
			Block block = new Block(new TableHeader<>(Collections.emptySet()));
			block.addAction(new Row(Collections.singletonList("")));
			matrix.addBlock(block);
		}

		matrix.addBlock(createBlock(Arrays.asList("id3", "Step1", "SetStatic", "0", "AAD", "BBQ", "100"),
									Arrays.asList("id4", "Step1", "SetStatic", "1000", "xxx", "yyy", "100")));

		matrix.addDuplicatedFields(1, Arrays.asList("#Timeout", "#Instrument"));
		matrix.addDuplicatedFields(5, Collections.singletonList("#Timeout"));

		return matrix;
	}

	private Block createBlock(List<String> row1, List<String> row2)
	{
		TableHeader<String> header = new TableHeader<>(createHeader());
		Block block = new Block(header);

		block.addAction(new Row(header, row1));
		block.addAction(new Row(header, row2));

		return block;
	}

	private Set<String> createHeader()
	{
		Set<String> header = new LinkedHashSet<>();
		header.add("#ID");
		header.add("#GlobalStep");
		header.add("#Action");
		header.add("#Timeout");
		header.add("#Instrument");
		header.add("#Currency");
		header.add("#Quantity");

		return header;
	}
}