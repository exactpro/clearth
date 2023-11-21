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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.html.template.ReportTemplatesProcessor;
import com.exactprosystems.clearth.utils.FileOperationUtils;

import freemarker.template.TemplateException;
import freemarker.template.TemplateModelException;

public class HtmlResultsTest
{
	private static final String TEMPLATES = "templates";
	private final Path outputDir,
			templatesDir,
			resourcesDir;
	
	public HtmlResultsTest() throws IOException
	{
		outputDir = Path.of("testOutput", HtmlResultsTest.class.getSimpleName());
		FileUtils.deleteDirectory(outputDir.toFile());
		Files.createDirectories(outputDir);
		
		resourcesDir = Path.of(FileOperationUtils.resourceToAbsoluteFilePath(HtmlResultsTest.class.getSimpleName()));
		templatesDir = outputDir.resolve(TEMPLATES);
		//Merging in one directory the built-in ClearTH templates and templates for tests
		createTemplates(templatesDir,
				Path.of("..", "cfg", TEMPLATES),
				resourcesDir.resolve(TEMPLATES));
	}
	
	@Test
	public void attachedFilesInContainer() throws TemplateModelException, IOException, TemplateException
	{
		Path reportDir = outputDir.resolve("attached_in_container"),
				dummyFile = outputDir.resolve("dummy.txt");
		Files.createFile(dummyFile);
		
		AttachedFilesResult files = new AttachedFilesResult();
		files.attach("File1", dummyFile);
		
		ContainerResult result = ContainerResult.createPlainResult("Plain container");
		result.addDetail(files);
		result.processDetails(reportDir.toFile(), null);
		
		File reportFile = reportDir.resolve("report.html").toFile();
		writeResult(result, reportFile, "attached_in_container.ftl");
		
		assertResult(reportFile, resourcesDir.resolve("attached_in_container.html").toFile());
	}
	
	
	private void createTemplates(Path destination, Path clearThTemplates, Path testTemplates) throws IOException
	{
		Files.createDirectories(destination);
		copyFiles(clearThTemplates, destination);
		copyFiles(testTemplates, destination);
	}
	
	private void copyFiles(Path from, Path to) throws IOException
	{
		try (Stream<Path> content = Files.list(from).filter(f -> Files.isRegularFile(f)))
		{
			Iterator<Path> it = content.iterator();
			while (it.hasNext())
			{
				Path f = it.next();
				Files.copy(f, to.resolve(f.getFileName()));
			}
		}
	}
	
	private void writeResult(Result result, File target, String templateName) throws IOException, TemplateModelException, TemplateException
	{
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(target)))
		{
			new ReportTemplatesProcessor(templatesDir).processTemplate(writer, createParameters(result), templateName);
		}
	}
	
	private void assertResult(File actualFile, File expectedFile) throws IOException
	{
		String actualText = readFile(actualFile),
				expectedText = readFile(expectedFile);
		Assert.assertEquals(actualText, expectedText);
	}
	
	private Map<String, Object> createParameters(Result result)
	{
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("resultObject", result);
		parameters.put("containerId", String.valueOf(System.currentTimeMillis()));
		return parameters;
	}
	
	private String readFile(File file) throws IOException
	{
		return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
	}
}
