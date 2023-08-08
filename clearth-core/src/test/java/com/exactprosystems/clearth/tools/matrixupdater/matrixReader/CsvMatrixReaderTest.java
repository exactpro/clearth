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

import com.exactprosystems.clearth.tools.matrixupdater.model.Matrix;
import com.exactprosystems.clearth.utils.FileOperationUtils;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.exactprosystems.clearth.tools.matrixupdater.TestMatrixUtils.*;

public class CsvMatrixReaderTest
{
	private Path resDir;

	@BeforeClass
	public void init() throws FileNotFoundException
	{
		resDir = Paths.get(FileOperationUtils.resourceToAbsoluteFilePath(CsvMatrixReaderTest.class.getSimpleName()));
	}

	@Test
	public void testReadMatrix() throws IOException
	{
		CsvMatrixReader matrixReader = new CsvMatrixReader();
		Matrix matrix = matrixReader.readMatrix(resDir.resolve("testReadMatrixCsv.csv").toFile());

		equalsMatrices(matrix, createMatrix());
	}

}