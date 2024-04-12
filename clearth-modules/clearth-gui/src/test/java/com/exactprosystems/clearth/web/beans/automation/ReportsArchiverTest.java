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

package com.exactprosystems.clearth.web.beans.automation;

import com.exactprosystems.clearth.automation.ReportsInfo;
import com.exactprosystems.clearth.utils.FileOperationUtils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ReportsArchiverTest
{
	private static final Path TEST_OUTPUT = Path.of("testOutput", ReportsArchiverTest.class.getSimpleName()),
			RES_DIR = Path.of("src", "test", "resources", ReportsArchiverTest.class.getSimpleName()),
			EXPECTED_DIR = RES_DIR.resolve("expected");
	
	private ReportsArchiver archiver;
	
	@BeforeClass
	public void init()
	{
		archiver = new ReportsArchiver(null, TEST_OUTPUT.toString(), Function.identity());
	}
	
	@DataProvider(name = "reports")
	public Object[][] reports()
	{
		return new Object[][]
				{
					{createReportsInfo(RES_DIR.resolve("1")), EXPECTED_DIR.resolve("1")},
					{createReportsInfo(RES_DIR.resolve("2")), EXPECTED_DIR.resolve("2")},
					{createReportsInfo(RES_DIR.resolve("3")), EXPECTED_DIR.resolve("3")},
				};
	}
	
	@Test(dataProvider = "reports")
	public void zipSelectedReportsTest(ReportsInfo reportsInfo, Path expectedFilesDir) throws FileNotFoundException, IOException
	{
		String fileName = new File(reportsInfo.getPath()).getName()+"_reports.zip";
		File f = archiver.getZipSelectedReports(false, reportsInfo, fileName);
		
		Path actualFilesDir = TEST_OUTPUT.resolve(FilenameUtils.getBaseName(f.getName()));
		File actualFilesFolder = actualFilesDir.toFile();
		FileUtils.deleteDirectory(actualFilesFolder);
		FileOperationUtils.unzipFile(f, actualFilesFolder);
		assertDirs(actualFilesDir, expectedFilesDir);
	}
	
	@Test
	public void noReportsTest() throws FileNotFoundException, IOException
	{
		ReportsInfo ri = createReportsInfo(RES_DIR.resolve("no_reports"));
		File f = archiver.getZipSelectedReports(false, ri, "no_reports.zip");
		
		Assert.assertNull(f);
	}
	
	
	private ReportsInfo createReportsInfo(Path reportsDir)
	{
		ReportsInfo ri = new ReportsInfo();
		ri.setPath(reportsDir.toString());
		return ri;
	}
	
	
	private void assertDirs(Path actualFilesDir, Path expectedFilesDir) throws IOException
	{
		List<Path> actualFiles = getFilesList(actualFilesDir),
				expectedFiles = getFilesList(expectedFilesDir);
		
		List<String> actualNames = getFileNames(actualFiles),
				expectedNames = getFileNames(expectedFiles);
		Assert.assertEquals(actualNames, expectedNames, "File names");
		
		Iterator<Path> actIt = actualFiles.iterator(),
				expIt = expectedFiles.iterator();
		while (expIt.hasNext())
		{
			Path actFile = actIt.next(),
					expFile = expIt.next();
			String actContent = FileUtils.readFileToString(actFile.toFile(), StandardCharsets.UTF_8),
					expContent = FileUtils.readFileToString(expFile.toFile(), StandardCharsets.UTF_8);
			Assert.assertEquals(actContent, expContent, expFile.getFileName()+" content");
		}
	}
	
	private List<Path> getFilesList(Path dir) throws IOException
	{
		return Files.list(dir).sorted().collect(Collectors.toList());
	}
	
	private List<String> getFileNames(List<Path> files)
	{
		return files.stream().map(p -> p.getFileName().toString())
				.collect(Collectors.toList());
	}
}
