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

package com.exactprosystems.clearth.tools.matrixupdater.matrixWriter;

import com.exactprosystems.clearth.tools.matrixupdater.matrixReader.CsvMatrixReader;
import com.exactprosystems.clearth.tools.matrixupdater.model.Matrix;
import com.exactprosystems.clearth.utils.Utils;
import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.exactprosystems.clearth.tools.matrixupdater.TestMatrixUtils.*;

public class CsvMatrixWriterTest
{
	private static final String DIR_NAME = CsvMatrixWriterTest.class.getSimpleName(), SEPARATOR = System.lineSeparator();
	private static final Path TEST_OUT = Paths.get("testOutput").resolve(DIR_NAME);

	@BeforeClass
	public void init() throws IOException
	{
		FileUtils.deleteDirectory(TEST_OUT.toFile());
		Files.createDirectories(TEST_OUT);
	}

	@Test
	public void testWriteMatrix() throws IOException
	{
		Matrix expMatrix = createMatrix();
		File file = writeToFile("testWriteMatrixCsv.csv", new CsvMatrixWriter(), expMatrix);
		String actualData = FileUtils.readFileToString(file, Utils.UTF8);
		String expectedData = "#Param1,#Param2,#Param3" + SEPARATOR + "1.1,1.2,1.3" + SEPARATOR +
								",\"2,2\",\"'2,3'\"" + SEPARATOR + "\"'3.1\"\"\",3.2,3.3" + SEPARATOR;

		Assert.assertEquals(actualData, expectedData);
	}

	@Test
	public void testWriteAndReadMatrix() throws IOException
	{
		Matrix expMatrix = createMatrix();
		File file = writeToFile("testWriteMatrixCsv.csv", new CsvMatrixWriter(), expMatrix);

		CsvMatrixReader matrixReader = new CsvMatrixReader();
		Matrix matrix = matrixReader.readMatrix(file);

		equalsMatrices(matrix, expMatrix);
	}

	private File writeToFile(String fileName, MatrixWriter matrixWriter, Matrix matrix) throws IOException
	{
		return matrixWriter.writeMatrix(TEST_OUT.resolve(fileName).toFile(), matrix);
	}

}