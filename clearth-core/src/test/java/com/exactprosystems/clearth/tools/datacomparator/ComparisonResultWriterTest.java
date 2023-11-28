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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

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

public class ComparisonResultWriterTest
{
	private static final Path RES_DIR = Path.of("src", "test", "resources", ComparisonResultWriterTest.class.getSimpleName()),
			OUTPUT_DIR = Path.of("testOutput", ComparisonResultWriterTest.class.getSimpleName());
	private static final String FILE_EXPECTED = "expected.csv",
			FILE_ACTUAL = "actual.csv",
			FILE_PASSED = "passed.csv",
			FILE_FAILED = "failed.csv",
			FILE_NOT_FOUND = "not_found.csv",
			FILE_EXTRA = "extra.csv",
			FILE_SUMMARY = "summary.txt",
			FILE_ERRORS = "errors.txt";
	
	@Test
	public void nothingToCompare() throws IOException, ParametersException, ComparisonException
	{
		String empty = "empty";
		Path dirWithEmpty = RES_DIR.resolve(empty);
		Map<String, File> resultFiles = compareFiles(dirWithEmpty, OUTPUT_DIR.resolve(empty), null);
		Assert.assertEquals(resultFiles.size(), 1, "Files in archive");
		assertSummary(resultFiles.get(FILE_SUMMARY), dirWithEmpty.resolve(FILE_SUMMARY).toFile());
	}
	
	@Test
	public void passed() throws IOException, ParametersException, ComparisonException
	{
		String passed = "passed";
		Path dirWithPassed = RES_DIR.resolve(passed);
		Map<String, File> resultFiles = compareFiles(dirWithPassed, OUTPUT_DIR.resolve(passed), null);
		Assert.assertEquals(resultFiles.size(), 2, "Files in archive");
		
		assertSummary(resultFiles.get(FILE_SUMMARY), dirWithPassed.resolve(FILE_SUMMARY).toFile());
		assertTexts(resultFiles.get(FILE_PASSED), dirWithPassed.resolve(FILE_PASSED).toFile(), passed);
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
		
		Map<String, File> resultFiles = compareFiles(dirWithFiles, OUTPUT_DIR.resolve(diff), new StringDataMapping(mapping));
		Assert.assertEquals(resultFiles.size(), 6, "Files in archive");
		
		assertSummary(resultFiles.get(FILE_SUMMARY), dirWithFiles.resolve(FILE_SUMMARY).toFile());
		assertTexts(resultFiles.get(FILE_PASSED), dirWithFiles.resolve(FILE_PASSED).toFile(), "Passed details");
		assertTexts(resultFiles.get(FILE_FAILED), dirWithFiles.resolve(FILE_FAILED).toFile(), "Failed details");
		assertTexts(resultFiles.get(FILE_NOT_FOUND), dirWithFiles.resolve(FILE_NOT_FOUND).toFile(), "Not found rows details");
		assertTexts(resultFiles.get(FILE_EXTRA), dirWithFiles.resolve(FILE_EXTRA).toFile(), "Extra rows details");
		assertTexts(resultFiles.get(FILE_ERRORS), dirWithFiles.resolve(FILE_ERRORS).toFile(), "Errors");
	}
	
	
	private Map<String, File> compareFiles(Path filesDirectory, Path resultStorage, DataMapping<String> mapping) throws IOException, ParametersException, ComparisonException
	{
		try (CsvDataReader expectedReader = createReader(filesDirectory.resolve(FILE_EXPECTED));
				CsvDataReader actualReader = createReader(filesDirectory.resolve(FILE_ACTUAL)))
		{
			ComparisonSettings settings = new ComparisonSettings(resultStorage, mapping, new ComparisonUtils());
			ComparisonResult result = new DataComparatorTool().compare(expectedReader, actualReader, settings);
			
			Path resultFile = resultStorage.resolve("result.zip");
			new ComparisonResultWriter().write(result, resultFile);
			
			List<File> files = FileOperationUtils.unzipFile(resultFile.toFile(), resultStorage.toFile());
			return files.stream().collect(Collectors.toMap(f -> f.getName(), f -> f));
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
	
	private List<String> readTextLines(File file) throws IOException
	{
		return FileUtils.readLines(file, StandardCharsets.UTF_8);
	}
	
	
	private void assertSummary(File actualFile, File expectedFile) throws IOException
	{
		List<String> expectedLines = readTextLines(expectedFile),
				actualLines = readTextLines(actualFile);
		Assert.assertEquals(actualLines.size(), expectedLines.size(), "Number of lines");
		
		Iterator<String> expectedIt = expectedLines.iterator(),
				actualIt = actualLines.iterator();
		while (expectedIt.hasNext())
		{
			String expected = expectedIt.next(),
					actual = actualIt.next();
			Pattern p = Pattern.compile(expected);
			Assert.assertTrue(p.matcher(actual).matches(), "Line '"+actual+"' matches '"+expected+"'");
		}
	}
	
	private void assertTexts(File actualFile, File expectedFile, String kind) throws IOException
	{
		String expectedDetails = readTextFile(expectedFile),
				actualDetails = readTextFile(actualFile);
		Assert.assertEquals(actualDetails, expectedDetails, kind);
	}
}