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
import com.exactprosystems.clearth.tools.matrixupdater.model.Cell;
import com.exactprosystems.clearth.tools.matrixupdater.model.Matrix;
import com.exactprosystems.clearth.tools.matrixupdater.settings.*;
import com.exactprosystems.clearth.utils.ClearThException;
import com.exactprosystems.clearth.utils.FileOperationUtils;
import com.exactprosystems.clearth.utils.Utils;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class MatrixUpdaterTest
{
	private ApplicationManager manager;
	private Path resDir, testOutput;
	private static final String ADMIN = "admin", USER = "user";

	@BeforeClass
	public void init() throws ClearThException, IOException
	{
		manager = new ApplicationManager();
		resDir = Paths.get(FileOperationUtils.resourceToAbsoluteFilePath(MatrixUpdaterTest.class.getSimpleName()));
		testOutput = Paths.get("testOutput").resolve(MatrixUpdaterTest.class.getSimpleName());
		if (testOutput.toFile().exists())
			FileUtils.cleanDirectory(testOutput.toFile());
		else
			Files.createDirectory(testOutput);
	}

	@AfterClass
	public void dispose() throws IOException
	{
		if (manager != null)
			manager.dispose();
	}

	@Test
	public void testConfigChangeAction() throws Exception
	{
		Path changeDir = resDir.resolve("Changes"),
			tempDir = testOutput.resolve("Changes");
		File matrixChangeFile = changeDir.resolve("action_matrix.csv").toFile(),
					matrixFile = changeDir.resolve("matrix.csv").toFile(),
					matrixCopy = changeDir.resolve("upd_matrix1.csv").toFile(),
					matrixCopy2 = changeDir.resolve("upd_matrix2.csv").toFile();

		MatrixUpdater matrixUpdater = new MatrixUpdater(ADMIN, tempDir),
					matrixUpdater2 = new MatrixUpdater(USER, tempDir);

		File copyChangeFile = matrixUpdater.getConfigDir().resolve("new_action_matrix.csv").toFile();
		FileUtils.copyFile(matrixChangeFile, copyChangeFile);

		addConfig(matrixUpdater.getConfig(), copyChangeFile.getName());
		File cfgZip = matrixUpdater.saveConfig().toFile();

		FileUtils.copyFile(matrixFile, matrixCopy);
		matrixUpdater.update(matrixCopy);

		matrixUpdater2.setConfig(cfgZip);
		FileUtils.copyFile(matrixFile, matrixCopy2);
		matrixUpdater2.update(matrixCopy2);

		Assertions.assertThat(matrixUpdater2.getConfig()).usingRecursiveComparison()
				.isEqualTo(matrixUpdater.getConfig());
		Assertions.assertThat(matrixUpdater.readMatrix(matrixCopy))
				.usingRecursiveComparison().isEqualTo(matrixUpdater2.readMatrix(matrixCopy2));
	}

	@Test
	public void testUpdateMatrixAfterSetConfigAddActions() throws Exception
	{
		Path updateMatrix = resDir.resolve("UpdateMatrix"),
				tempDir = testOutput.resolve("UpdateMatrix");
		File cfgZip = updateMatrix.resolve("MatrixUpdaterConfig.zip").toFile(),
			matrixCopy = updateMatrix.resolve("upd_matrix.csv").toFile();
		FileUtils.copyFile(updateMatrix.resolve("matrix.csv").toFile(), matrixCopy);

		MatrixUpdater matrixUpdater = new MatrixUpdater(ADMIN, tempDir);
		matrixUpdater.setConfig(cfgZip);
		matrixUpdater.update(matrixCopy);

		Matrix actualMatrix = matrixUpdater.readMatrix(matrixCopy);
		Matrix expectedMatrix = matrixUpdater.readMatrix(updateMatrix.resolve("exp_matrix.csv").toFile());
		Assertions.assertThat(actualMatrix).usingRecursiveComparison().isEqualTo(expectedMatrix);
	}

	@DataProvider(name = "duplicatedFields")
	public Object[][] duplicatedFields()
	{
		return new Object[][]
				{
						{"matrixWithDuplicatedHeaderFieldsCsv.csv"},
						{"matrixWithDuplicatedHeaderFieldsXls.xls"}
				};
	}

	@Test(dataProvider = "duplicatedFields")
	public void testDuplicatedFields(String fileName) throws Exception
	{
		Path duplicatesDir = resDir.resolve("Duplicates"),
			tempDir = testOutput.resolve("Duplicates");
		File fileDir = duplicatesDir.resolve(fileName).toFile(),
			copiedFile = tempDir.resolve("copy_" + fileName).toFile();

		FileUtils.copyFile(fileDir, copiedFile);

		MatrixUpdater matrixUpdater = new MatrixUpdater(ADMIN, tempDir);
		matrixUpdater.update(copiedFile);

		List<String> warnMessage = matrixUpdater.getDuplicatedHeaderFields();
		List<String> expectedMessage = new ArrayList<>();
		expectedMessage.add("Matrix has duplicate fields in headers:");
		expectedMessage.add("row 1, field names: [#Timeout, #Instrument]");
		expectedMessage.add("row 5, field names: [#Timeout]");

		Assert.assertEquals(warnMessage, expectedMessage);
	}

	@Test
	public void testUpdateMatrixWithoutExpressionEvaluation() throws Exception {
		Path exprCfgDir = resDir.resolve("ExpressionEvaluation"),
			tempDir = testOutput.resolve("ExpressionEvaluation");

		File configFile = exprCfgDir.resolve("config.zip").toFile(),
			matrixFile = exprCfgDir.resolve("matrix.csv").toFile(),
			copiedFile = exprCfgDir.resolve("copy_matrix.csv").toFile();

		FileUtils.copyFile(matrixFile, copiedFile);

		MatrixUpdater matrixUpdater = new MatrixUpdater(ADMIN, tempDir);
		matrixUpdater.setConfig(configFile);
		MatrixUpdaterConfig config = matrixUpdater.getConfig();

		String actualData = FileUtils.readFileToString(matrixUpdater.update(copiedFile), Utils.UTF8);
		String expectedData = "#ID,#GlobalStep,#Action,#Expected,#Actual\n" +
							"id1,Step1,Compare2Values,123,123\n" +
							"id2,Step1,Compare2Values,990,234\n" +
							"id3,Step1,Compare2Values,@{isNotEmpty},456\n";

		Assert.assertFalse(config.getUpdate("Modification").getSettings().getConditions().get(0).getCells().get(0).isUseExpression());
		Assert.assertEquals(actualData, expectedData);
	}

	@Test (expectedExceptions = MatrixUpdaterException.class)
	public void testSetIrrelevantArchiveToConfig() throws IOException, JAXBException, MatrixUpdaterException
	{
		Path cfgDir = resDir.resolve("SetAndSaveConfig"),
			tempDir = testOutput.resolve("SetAndSaveConfig");
		File file = cfgDir.resolve("IrrelevantArchive.zip").toFile();
		MatrixUpdater matrixUpdater = new MatrixUpdater(ADMIN, tempDir);
		matrixUpdater.setConfig(file);
	}

	@Test
	public void testSaveConfig() throws JAXBException, IOException
	{
		Path tempDir = testOutput.resolve("SetAndSaveConfig");
		MatrixUpdater matrixUpdater = new MatrixUpdater(ADMIN, tempDir);
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
		Path cfgDir = resDir.resolve("SetAndSaveConfig"),
			tempDir = testOutput.resolve("SetAndSaveConfig");
		File file = cfgDir.resolve(fileName).toFile();
		MatrixUpdater matrixUpdater = new MatrixUpdater(ADMIN, tempDir);

		matrixUpdater.setConfig(file);
		assertMatrixUpdaterConfig(matrixUpdater.getConfig().getUpdates());
	}

	private void assertMatrixUpdaterConfig(List<Update> list)
	{
		Assert.assertEquals(list.size(), 1);
		for (Update update : list)
			Assert.assertEquals(update.getName(), "NewActions");
	}

	private void addConfig(MatrixUpdaterConfig config, String fileName)
	{
		Settings settings = new Settings();
		List<Condition> conditions = new ArrayList<>();
		List<Cell> cells = new ArrayList<>();
		Condition condition = new Condition("Condition");

		cells.add(new Cell("#Action", "AddAction1"));
		condition.setCells(cells);
		conditions.add(condition);

		settings.setConditions(conditions);
		settings.setChange(new Change(false, fileName, false, new ArrayList<>()));

		Update update = config.addUpdate("NewActions", UpdateType.ADD_ACTIONS);
		update.setSettings(settings);
	}

}