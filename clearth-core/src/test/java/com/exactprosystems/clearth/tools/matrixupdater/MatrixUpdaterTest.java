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

import com.exactprosystems.clearth.ApplicationManager;
import com.exactprosystems.clearth.tools.matrixupdater.settings.Update;
import com.exactprosystems.clearth.utils.ClearThException;
import com.exactprosystems.clearth.utils.FileOperationUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

public class MatrixUpdaterTest
{
	private ApplicationManager manager;

	@BeforeClass
	public void init() throws ClearThException
	{
		manager = new ApplicationManager();
	}

	@AfterClass
	public void dispose() throws IOException
	{
		if (manager != null)
			manager.dispose();
	}

	@Test (expectedExceptions = MatrixUpdaterException.class)
	public void testSetIrrelevantArchiveToConfig() throws IOException, JAXBException, MatrixUpdaterException
	{
		File file = new File(FileOperationUtils
				.resourceToAbsoluteFilePath(Paths.get(MatrixUpdaterTest.class.getSimpleName())
				.resolve("IrrelevantArchive.zip").toString()));
		MatrixUpdater matrixUpdater = new MatrixUpdater("admin");
		matrixUpdater.setConfig(file);
	}

	@Test
	public void testSaveConfig() throws JAXBException, IOException
	{
		MatrixUpdater matrixUpdater = new MatrixUpdater("admin");
		File file = matrixUpdater.saveConfig().toFile();
		Assert.assertTrue(file.exists());
	}

	@DataProvider(name = "zipFile")
	public Object[][] zipFiles()
	{
		return new Object[][]
		{
				{"MatrixUpdaterConfigWindows.zip"},
				{"MatrixUpdaterConfigLinux.zip"}
		};
	}

	@Test (dataProvider = "zipFile")
	public void testSetConfig(String fileName) throws MatrixUpdaterException, JAXBException, IOException
	{
		File file = new File(FileOperationUtils
				.resourceToAbsoluteFilePath(Paths.get(MatrixUpdaterTest.class.getSimpleName())
				.resolve(fileName).toString()));
		MatrixUpdater matrixUpdater = new MatrixUpdater("admin");
		matrixUpdater.setConfig(file);
		assertMatrixUpdaterConfig(matrixUpdater.getConfig().getUpdates());
	}

	private void assertMatrixUpdaterConfig(List<Update> list)
	{
		Assert.assertEquals(list.size(), 1);
		for (Update update : list)
			Assert.assertEquals(update.getName(), "NewActions");
	}
}