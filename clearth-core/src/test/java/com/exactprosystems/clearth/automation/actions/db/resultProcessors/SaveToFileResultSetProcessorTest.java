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
package com.exactprosystems.clearth.automation.actions.db.resultProcessors;

import com.exactprosystems.clearth.ApplicationManager;
import com.exactprosystems.clearth.automation.actions.db.checkers.DefaultRecordChecker;
import com.exactprosystems.clearth.automation.actions.db.resultProcessors.settings.SaveToFileRSProcessorSettings;
import com.exactprosystems.clearth.automation.report.results.TableResult;
import com.exactprosystems.clearth.utils.ClearThException;
import com.exactprosystems.clearth.utils.FileOperationUtils;
import com.exactprosystems.clearth.utils.SettingsException;
import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.utils.sql.DefaultSqlObjectToStringTransformer;
import com.exactprosystems.clearth.utils.sql.StubValueTransformer;
import com.exactprosystems.clearth.utils.sql.conversion.ConversionSettings;
import com.exactprosystems.clearth.utils.sql.conversion.DBFieldMapping;
import com.exactprosystems.clearth.utils.sql.conversion.DBFieldMappingReader;
import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.Arrays;
import java.util.List;

public class SaveToFileResultSetProcessorTest
{
	private static final Path TEST_OUTPUT = Paths.get("testOutput").resolve(SaveToFileResultSetProcessorTest.class.getSimpleName());
	private static final String LN_SEPARATOR = System.lineSeparator(),
								DB_FILE = TEST_OUTPUT.resolve("file.db").toString();
	private Connection connection;
	private Path resDir;
	private ApplicationManager appManager;

	@BeforeClass
	public void init() throws IOException, ClearThException, SQLException, SettingsException
	{
		FileUtils.deleteDirectory(TEST_OUTPUT.toFile());
		Files.createDirectory(TEST_OUTPUT);

		resDir = Paths.get(FileOperationUtils.resourceToAbsoluteFilePath(SaveToFileResultSetProcessorTest.class.getSimpleName()));
		appManager = new ApplicationManager(resDir.resolve("clearth.cfg").toString());

		prepareTableToTest();
	}

	@AfterClass
	public void dispose() throws IOException
	{
		Utils.closeResource(connection);

		if (appManager != null)
			appManager.dispose();
	}

	@Test
	public void testSaveOutputFile() throws IOException, SQLException
	{
		File outputDir = TEST_OUTPUT.toFile();
		String fileName = "file.csv";
		SaveToFileRSProcessorSettings settings = createSettings(outputDir, fileName);
		try (SaveToFileResultSetProcessor processor = new SaveToFileResultSetProcessor(settings);
			 PreparedStatement statement = connection.prepareStatement("select * from table1"))
		{
			statement.execute();
			try (ResultSet result = statement.getResultSet())
			{
				result.next();
				processor.processHeader(result, settings.getConversionSettings().getMappings());
				processor.processRecords(result, 5);
			}
		}
		String actualData = FileUtils.readFileToString(TEST_OUTPUT.resolve(fileName).toFile(), Utils.UTF8),
				expectedData = "\"id\",\"MF1\",\"MF2\"" + LN_SEPARATOR + "\"1\",\"vvv\",\"hhh\"" + LN_SEPARATOR + "\"2\",\"lll\",\"kkk\"" + LN_SEPARATOR;
		Assert.assertEquals(actualData, expectedData);
	}

	private SaveToFileRSProcessorSettings createSettings(File file, String fileName) throws IOException
	{
		SaveToFileRSProcessorSettings settings = new SaveToFileRSProcessorSettings();
		settings.setAppend(false);
		settings.setConversionSettings(createConversionSettings());
		settings.setResult(createTableResult());
		settings.setCompressResult(false);
		settings.setFileDir(file);
		settings.setRecordChecker(new DefaultRecordChecker());
		settings.setDelimiter(',');
		settings.setFileName(fileName);
		settings.setUseQuotes(true);
		settings.setMaxDisplayedRows(5);
		settings.setObjectToStringTransformer(new DefaultSqlObjectToStringTransformer());
		settings.setValueTransformer(new StubValueTransformer());

		return settings;
	}

	private TableResult createTableResult()
	{
		return new TableResult("", Arrays.asList("id", "DB1", "DB2"), false);
	}

	private ConversionSettings createConversionSettings() throws IOException
	{
		return new ConversionSettings(createDBFieldMappingList());
	}

	private List<DBFieldMapping> createDBFieldMappingList() throws IOException
	{
		File file = resDir.resolve("mapping.csv").toFile();
		return new DBFieldMappingReader().readEntities(file);
	}

	private void prepareTableToTest() throws SQLException
	{
		connection = DriverManager.getConnection("jdbc:sqlite:" + DB_FILE);

		String[] createQuery = {"create table table1 (id INTEGER PRIMARY KEY, DB1 TEXT, DB2 TEXT)",
								"insert into table1 (id, DB1, DB2) values (1, \"vvv\", \"hhh\")",
								"insert into table1 (id, DB1, DB2) values (2, \"lll\", \"kkk\")"};

		for (String s : createQuery)
		{
			try (PreparedStatement statement = connection.prepareStatement(s))
			{
				statement.execute();
			}
		}
	}
}