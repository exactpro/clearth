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

package com.exactprosystems.clearth.tools.datacomparator;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

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
		assertTextAndArchive(result.getPassedDetails(), dirWithPassed.resolve(FILE_PASSED), "Details");
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
		assertTextAndArchive(result.getPassedDetails(), dirWithFiles.resolve(FILE_PASSED), "Passed details");
		assertTextAndArchive(result.getFailedDetails(), dirWithFiles.resolve(FILE_FAILED), "Failed details");
		assertTextAndArchive(result.getNotFoundDetails(), dirWithFiles.resolve(FILE_NOT_FOUND), "Not found rows details");
		assertTextAndArchive(result.getExtraDetails(), dirWithFiles.resolve(FILE_EXTRA), "Extra rows details");
		
		assertTexts(result.getErrors().toFile(), dirWithFiles.resolve("errors.txt").toFile(), "Errors");
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
