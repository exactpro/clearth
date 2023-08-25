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

package com.exactprosystems.clearth.automation;

import com.exactprosystems.clearth.ApplicationManager;
import com.exactprosystems.clearth.utils.ClearThException;
import com.exactprosystems.clearth.utils.FileOperationUtils;
import com.exactprosystems.clearth.utils.Utils;
import org.apache.commons.io.FileUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertEquals;

public class ExecutedMatricesDataTest
{
	private static final Path TEST_OUTPUT = Paths.get("testOutput").resolve(ExecutedMatricesDataTest.class.getSimpleName());
	private static final String LN_SEPARATOR = System.lineSeparator();
	private Path resDir, matrices;
	private ApplicationManager clearThManager;

	@BeforeClass
	public void init() throws IOException, ClearThException
	{
		FileUtils.deleteDirectory(TEST_OUTPUT.toFile());
		Files.createDirectory(TEST_OUTPUT);

		resDir = Paths.get(FileOperationUtils.resourceToAbsoluteFilePath(ExecutedMatricesDataTest.class.getSimpleName()));
		matrices = resDir.resolve("matrices");

		clearThManager = new ApplicationManager();
	}

	@AfterClass
	public void dispose() throws IOException
	{
		if (clearThManager != null)
			clearThManager.dispose();
	}

	@Test
	public void testLoadExecutedMatrices() throws IOException
	{
		ExecutedMatricesData matricesData = new ExecutedMatricesData(resDir.toString());
		List<MatrixData> matricesList = matricesData.getExecutedMatrices();

		assertThat(matricesList).usingRecursiveComparison().isEqualTo(createMatricesData());
	}

	@Test
	public void testSetExecutedMatricesData() throws IOException
	{
		ExecutedMatricesData matricesData = new ExecutedMatricesData(TEST_OUTPUT.toString());
		List<MatrixData> matricesList = createMatricesData();
		matricesData.setExecutedMatrices(matricesList);

		String actualData = FileUtils.readFileToString(TEST_OUTPUT.resolve("executed_matrices.csv").toFile(), Utils.UTF8);
		String expectedData = "Name,Matrix,Uploaded,Execute,TrimSpaces" + LN_SEPARATOR +
				"matrix1,matrix1.csv,,true,false" + LN_SEPARATOR +
				"matrix2,matrix2.csv,,true,false" + LN_SEPARATOR +
				"matrix3,matrix3.csv,,true,false" + LN_SEPARATOR;
		assertEquals(actualData, expectedData);
	}

	private List<MatrixData> createMatricesData()
	{
		List<MatrixData> matrixDataList = new ArrayList<>();
		matrixDataList.add(createMatrixData("matrix1", true, matrices.resolve("matrix1.csv").toFile(), false));
		matrixDataList.add(createMatrixData("matrix2", true, matrices.resolve("matrix2.csv").toFile(), false));
		matrixDataList.add(createMatrixData("matrix3", true, matrices.resolve("matrix3.csv").toFile(), false));
		return matrixDataList;
	}

	private MatrixData createMatrixData(String name, boolean exec, File file, boolean trim)
	{
		MatrixData matrixData = new MatrixData();
		matrixData.setName(name);
		matrixData.setExecute(exec);
		matrixData.setFile(file);
		matrixData.setTrim(trim);
		return matrixData;
	}
}