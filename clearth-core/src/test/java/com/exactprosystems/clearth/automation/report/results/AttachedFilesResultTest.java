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

package com.exactprosystems.clearth.automation.report.results;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.exactprosystems.clearth.automation.report.Result;

public class AttachedFilesResultTest
{
	private Path filesRoot;
	
	@BeforeClass
	private void init() throws IOException
	{
		filesRoot = Paths.get("src", "test", "resources", "AttachedFilesResultTest");
		Files.createDirectories(filesRoot);
	}
	
	@Test
	public void storedDetails()
	{
		String id = "someId";
		Path file = filesRoot.resolve("someFile.txt");
		
		AttachedFilesResult result = new AttachedFilesResult();
		result.attach(id, file);
		
		Assert.assertEquals(result.getPath(id), file);
	}
	
	@Test
	public void updatedDetailsAfterProcessing() throws IOException
	{
		String id = "id1";
		Path file = filesRoot.resolve("file1.txt"),
				destination = filesRoot.resolve("destination");
		
		byte[] fileContent = "dummy data".getBytes();
		Files.write(file, fileContent);
		try
		{
			AttachedFilesResult result = new AttachedFilesResult();
			result.attach(id, file);
			result.processDetails(destination.toFile(), null);
			
			Path attached = result.getPath(id);
			Assert.assertEquals(attached, 
					destination.resolve(Result.DETAILS_DIR).resolve(file.getFileName()), 
					"Path to attached file after processing");
			Assert.assertEquals(FileUtils.readFileToByteArray(attached.toFile()), 
					fileContent,
					"Content of attached file after processing");
		}
		finally
		{
			FileUtils.deleteDirectory(destination.toFile());
		}
	}
}
