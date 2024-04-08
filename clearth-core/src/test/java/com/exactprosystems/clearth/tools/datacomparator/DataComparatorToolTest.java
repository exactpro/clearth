/******************************************************************************
 * Copyright 2009-2024 Exactpro Systems Limited
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

package com.exactprosystems.clearth.tools.datacomparator;

import static org.testng.Assert.assertNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.StringJoiner;

import javax.xml.bind.JAXBException;

import com.exactprosystems.clearth.connectivity.ConnectivityException;
import com.exactprosystems.clearth.connectivity.db.DbConnection;
import com.exactprosystems.clearth.utils.SettingsException;
import com.exactprosystems.clearth.utils.XmlUtils;
import com.exactprosystems.clearth.utils.tabledata.readers.DbDataReader;
import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import com.exactprosystems.clearth.automation.exceptions.ParametersException;
import com.exactprosystems.clearth.utils.ComparisonUtils;
import com.exactprosystems.clearth.utils.FileOperationUtils;
import com.exactprosystems.clearth.utils.csv.readers.ClearThCsvReaderConfig;
import com.exactprosystems.clearth.utils.tabledata.comparison.ComparisonException;
import com.exactprosystems.clearth.utils.tabledata.comparison.mappings.DataMapping;
import com.exactprosystems.clearth.utils.tabledata.comparison.mappings.StringDataMapping;
import com.exactprosystems.clearth.utils.tabledata.comparison.mappings.descs.FieldDesc;
import com.exactprosystems.clearth.utils.tabledata.comparison.mappings.descs.MappingDesc;
import com.exactprosystems.clearth.utils.tabledata.readers.CsvDataReader;

public class DataComparatorToolTest
{
	private static final Path RES_DIR = Path.of("src", "test", "resources", DataComparatorToolTest.class.getSimpleName()),
			OUTPUT_DIR = Path.of("testOutput", DataComparatorToolTest.class.getSimpleName());
	private static final String TABLE1 = "tOne", TABLE2 = "tTwo", DB = "db";
	private static final String FILE_EXPECTED = "expected.csv",
			FILE_ACTUAL = "actual.csv",
			FILE_PASSED = "passed.csv",
			FILE_FAILED = "failed.csv",
			FILE_NOT_FOUND = "not_found.csv",
			FILE_EXTRA = "extra.csv";
	
	@Test
	public void nothingToCompare() throws IOException, ParametersException, ComparisonException
	{
		String empty = "empty";
		Path dirWithEmpty = RES_DIR.resolve(empty);
		ComparisonResult result = compareFiles(dirWithEmpty, OUTPUT_DIR.resolve(empty), null);
		assertCounts(result, 0, 0, 0, 0);
		Assert.assertEquals(result.getDescription(), "Both datasets are empty", "Description");
	}
	
	@Test
	public void passed() throws IOException, ParametersException, ComparisonException
	{
		String passed = "passed";
		Path dirWithPassed = RES_DIR.resolve(passed);
		ComparisonResult result = compareFiles(dirWithPassed, OUTPUT_DIR.resolve(passed), null);
		assertCounts(result, 2, 0, 0, 0);
		assertPassedDetails(result, dirWithPassed);
	}
	
	@Test
	public void differentResults() throws IOException, ParametersException, ComparisonException
	{
		String diff = "different";
		Path dirWithFiles = RES_DIR.resolve(diff);
		
		FieldDesc keyField = new FieldDesc();
		keyField.setLocalName("C1");
		keyField.setKey(true);
		
		FieldDesc numField = new FieldDesc();
		numField.setLocalName("C3");
		numField.setNumeric(true);
		
		List<FieldDesc> fields = List.of(keyField, numField);
		
		MappingDesc mapping = new MappingDesc();
		mapping.setFields(fields);
		
		ComparisonResult result = compareFiles(dirWithFiles, OUTPUT_DIR.resolve(diff), new StringDataMapping(mapping));
		assertCounts(result, 1, 2, 1, 1);
		assertAllDetails(result, dirWithFiles);
		
		assertTexts(result.getErrors().toFile(), dirWithFiles.resolve("errors.txt").toFile(), "Errors");
	}
	
	@Test
	public void mappedColumns() throws IOException, JAXBException, ParametersException, ComparisonException
	{
		String mapped = "mapped";
		Path dirWithFiles = RES_DIR.resolve(mapped);
		
		MappingDesc mapping = XmlUtils.unmarshalObject(MappingDesc.class, dirWithFiles.resolve("mapping.xml").toString());
		
		ComparisonResult result = compareFiles(dirWithFiles, OUTPUT_DIR.resolve(mapped), new StringDataMapping(mapping));
		assertCounts(result, 1, 1, 1, 1);
		assertAllDetails(result, dirWithFiles);
		
		assertNull(result.getErrors(), "Errors");
	}
	
	@Test
	public void compareDB() throws Exception
	{
		Path dbPath = OUTPUT_DIR.resolve(DB),
				resFiles = RES_DIR.resolve(DB);
		String sqlQuery = "select * from ";
		
		DbConnection dbConnection = createDbConnection(dbPath);
		Connection connection = dbConnection.getConnection();
		createTable(connection, TABLE1, List.of("param1", "param2"), List.of("123, 123", "456, 456"));
		createTable(connection, TABLE2, List.of("param1", "param2"), List.of("111, 222", "456, 456"));
		
		DbDataReader expectedReader = createDbReader(dbConnection, sqlQuery + TABLE1);
		DbDataReader actualReader = createDbReader(dbConnection, sqlQuery + TABLE2);
		
		ComparisonSettings settings = new ComparisonSettings(dbPath, null, new ComparisonUtils());
		ComparisonResult result = new DataComparatorTool().compare(expectedReader, actualReader, settings);
		
		assertPassedDetails(result, resFiles);
		assertFailedDetails(result, resFiles);
	}
	
	
	private DbConnection createDbConnection(Path dbPath) throws Exception
	{
		FileUtils.deleteDirectory(dbPath.toFile());
		Files.createDirectories(dbPath);
		
		DbConnection connection = new DbConnection();
		connection.getSettings().setJdbcUrl("jdbc:sqlite:" + dbPath.resolve("file.db"));
		return connection;
	}
	
	private void createTable(Connection connection, String table, List<String> columnList, List<String> valuesList) throws SQLException
	{
		StringJoiner tableCols = new StringJoiner(","),
				cols = new StringJoiner(",");
		tableCols.add("id INTEGER PRIMARY KEY");
		columnList.stream().map(column -> column + " INTEGER").forEach(tableCols::add);
		execStatement(connection, String.format("create table %s (%s)", table, tableCols));
		
		columnList.forEach(cols::add);
		for (String value : valuesList)
			execStatement(connection, String.format("insert into %s (%s) values (%s)", table, cols, value));
	}
	private void execStatement(Connection connection, String query) throws SQLException
	{
		try(PreparedStatement prStatement = connection.prepareStatement(query))
		{
			prStatement.execute();
		}
	}
	
	private DbDataReader createDbReader(DbConnection connection, String query) throws ConnectivityException, SettingsException, SQLException
	{
		return new DbDataReader(connection.getConnection().prepareStatement(query), true);
	}
	
	private ComparisonResult compareFiles(Path filesDirectory, Path resultStorage, DataMapping<String> mapping) throws IOException, ParametersException, ComparisonException
	{
		try (CsvDataReader expectedReader = createReader(filesDirectory.resolve(FILE_EXPECTED));
				CsvDataReader actualReader = createReader(filesDirectory.resolve(FILE_ACTUAL)))
		{
			ComparisonSettings settings = new ComparisonSettings(resultStorage, mapping, new ComparisonUtils());
			return new DataComparatorTool().compare(expectedReader, actualReader, settings);
		}
	}
	
	private CsvDataReader createReader(Path file) throws IOException
	{
		return new CsvDataReader(file.toFile(), ClearThCsvReaderConfig.withFirstLineAsHeader());
	}
	
	private String readTextFile(File file) throws IOException
	{
		return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
	}
	
	
	private void assertCounts(ComparisonResult result, int passed, int failed, int notFound, int extra)
	{
		SoftAssert soft = new SoftAssert();
		soft.assertEquals(result.getPassed(), passed, "Passed");
		soft.assertEquals(result.getFailed(), failed, "Failed");
		soft.assertEquals(result.getNotFound(), notFound, "Not found");
		soft.assertEquals(result.getExtra(), extra, "Extra");
		soft.assertAll();
	}
	
	private void assertPassedDetails(ComparisonResult result, Path expectedFilesDir) throws IOException
	{
		assertTextAndArchive(result.getPassedDetails(), expectedFilesDir.resolve(FILE_PASSED), "Passed details");
	}
	
	private void assertFailedDetails(ComparisonResult result, Path expectedFilesDir) throws IOException
	{
		assertTextAndArchive(result.getFailedDetails(), expectedFilesDir.resolve(FILE_FAILED), "Failed details");
	}
	
	private void assertNotFoundDetails(ComparisonResult result, Path expectedFilesDir) throws IOException
	{
		assertTextAndArchive(result.getNotFoundDetails(), expectedFilesDir.resolve(FILE_NOT_FOUND), "Not found rows details");
	}
	
	private void assertExtraDetails(ComparisonResult result, Path expectedFilesDir) throws IOException
	{
		assertTextAndArchive(result.getExtraDetails(), expectedFilesDir.resolve(FILE_EXTRA), "Extra rows details");
	}
	
	private void assertAllDetails(ComparisonResult result, Path expectedFilesDir) throws IOException
	{
		assertPassedDetails(result, expectedFilesDir);
		assertFailedDetails(result, expectedFilesDir);
		assertNotFoundDetails(result, expectedFilesDir);
		assertExtraDetails(result, expectedFilesDir);
	}
	
	private void assertTextAndArchive(Path actualArchive, Path expectedText, String kind) throws IOException
	{
		File archive = actualArchive.toFile();
		List<File> files = FileOperationUtils.unzipFile(archive, archive.getParentFile());
		assertTexts(files.get(0), expectedText.toFile(), kind);
	}
	
	private void assertTexts(File actualFile, File expectedFile, String kind) throws IOException
	{
		String expectedDetails = readTextFile(expectedFile),
				actualDetails = readTextFile(actualFile);
		Assert.assertEquals(actualDetails, expectedDetails, kind);
	}
}
