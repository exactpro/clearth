/******************************************************************************
 * Copyright 2009-2020 Exactpro Systems Limited
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
import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.matrix.linked.LocalMatrixProvider;
import com.exactprosystems.clearth.utils.ClearThException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.testng.annotations.*;

import static com.exactprosystems.clearth.utils.FileOperationUtils.resourceToAbsoluteFilePath;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import static com.exactprosystems.clearth.ApplicationManager.USER_DIR;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class LinkedMatrixExtensionsTest
{
	private final String userName = "test", schedulerName = "Test";
	private static final Path RESOURCE_PATH = Paths.get("LinkedMatrixExtensionsTest");
	private static final Path TEST_OUTPUT_PATH =
			USER_DIR.getParent().resolve("testOutput").resolve("LinkedMatrixExtensionsTest");
	private static final Path SCHEDULER_DATA_MATRICES_PATH =
			USER_DIR.getParent().resolve("testOutput").resolve("appRoot").resolve("cfg").resolve("schedulers")
					.resolve("Test").resolve("test");
	public static final String EXCEPTION_MESSAGE = "Matrix file .* has unexpected type. Only .* are supported.";
	private static ApplicationManager clearThManager;
	private Scheduler scheduler;

	@BeforeClass
	public void startTestApplication() throws ClearThException, IOException
	{
		clearThManager = new ApplicationManager();
		Files.createDirectories(TEST_OUTPUT_PATH);
		scheduler = clearThManager.getScheduler(userName, schedulerName);
	}

	@DataProvider(name = "passed-matrices")
	Object[] createDataForPassedUploading()
	{
		return new Object[]{"xls.xls", "csv.csv", "xlsx.xlsx"};
	}

	@Test(dataProvider = "passed-matrices")
	public void testPassedUploading(String matrixFile) throws Exception
	{
		scheduler.getSchedulerData().getMatrices().clear();
		String originalFileExtension = FilenameUtils.getExtension(matrixFile);
		MatrixData matrixData = prepareMatrixData(matrixFile);
		String uploadedFileExtension = uploadMatrixFile(matrixData);
		scheduler.checkMatrices(Collections.singletonList(matrixData));
		cleanDirs();
		assertEquals(originalFileExtension, uploadedFileExtension);
	}

	@DataProvider(name = "failed-matrices")
	Object[] createDataForFailedUploading()
	{
		return new Object[]{"abc.abc", "cde.cde"};
	}

	@Test(dataProvider = "failed-matrices", expectedExceptions = ClearThException.class,
			expectedExceptionsMessageRegExp = EXCEPTION_MESSAGE)
	public void testFailedUploading(String matrixFile) throws Exception
	{
		MatrixData matrixData = prepareMatrixData(matrixFile);
		uploadMatrixFile(matrixData);
	}

	private MatrixData prepareMatrixData(String matrixFile) throws FileNotFoundException
	{
		MatrixDataFactory mdf = ClearThCore.getInstance().getMatrixDataFactory();
		MatrixData matrixData = mdf.createMatrixData();
		matrixData.setLink(resourceToAbsoluteFilePath(RESOURCE_PATH.resolve(matrixFile).toString()));
		matrixData.setType(LocalMatrixProvider.TYPE);
		return matrixData;
	}

	private String uploadMatrixFile(MatrixData matrixData) throws Exception
	{
		when(ClearThCore.automationStoragePath()).thenReturn(TEST_OUTPUT_PATH.toString());
		scheduler.addLinkedMatrix(matrixData);
		File[] uploadedFiles = TEST_OUTPUT_PATH.toFile().listFiles();
		if(ArrayUtils.isNotEmpty(uploadedFiles))
			return FilenameUtils.getExtension(uploadedFiles[0].getName());
		else return null;
	}

	private static void cleanDirs() throws IOException
	{
		if(SCHEDULER_DATA_MATRICES_PATH.toFile().exists())
			FileUtils.cleanDirectory(SCHEDULER_DATA_MATRICES_PATH.toFile());
		FileUtils.cleanDirectory(TEST_OUTPUT_PATH.toFile());
	}

	@AfterClass
	public static void disposeTestApplication() throws IOException
	{
		cleanDirs();
		if (clearThManager != null) clearThManager.dispose();
	}
}