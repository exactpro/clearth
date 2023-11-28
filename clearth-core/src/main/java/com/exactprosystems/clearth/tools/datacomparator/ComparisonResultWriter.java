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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang3.StringUtils;

import com.exactprosystems.clearth.utils.FileOperationUtils;

public class ComparisonResultWriter
{
	public void write(ComparisonResult compResult, Path file) throws IOException
	{
		Path directory = file.getParent();
		Files.createDirectories(directory);
		
		Path summaryFile = Files.createTempFile(directory, "summary_", ".txt");
		writeSummary(compResult, summaryFile);
		try (ZipOutputStream zipOut = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(file.toFile()))))
		{
			byte[] buffer = new byte[4096];
			writeFileToArchive(summaryFile, "summary.txt", zipOut, buffer);
			writeFileFromArchiveToArchive(compResult.getPassedDetails(), "passed.csv", zipOut, buffer);
			writeFileFromArchiveToArchive(compResult.getFailedDetails(), "failed.csv", zipOut, buffer);
			writeFileFromArchiveToArchive(compResult.getNotFoundDetails(), "not_found.csv", zipOut, buffer);
			writeFileFromArchiveToArchive(compResult.getExtraDetails(), "extra.csv", zipOut, buffer);
			writeFileToArchive(compResult.getErrors(), "errors.txt", zipOut, buffer);
		}
		Files.delete(summaryFile);
	}
	
	protected void writeSummary(ComparisonResult compResult, Path file) throws IOException
	{
		try (BufferedWriter writer = FileOperationUtils.newBlockingBufferedWriter(file))
		{
			boolean empty = true;
			
			LocalDateTime start = compResult.getStarted();
			if (start != null)
			{
				writer.write("Comparison started: ");
				writer.write(start.toString());
				writer.newLine();
				empty = false;
			}
			
			LocalDateTime finish = compResult.getFinished();
			if (finish != null)
			{
				writer.write("Comparison finished: ");
				writer.write(finish.toString());
				writer.newLine();
				empty = false;
			}
			
			if (!StringUtils.isEmpty(compResult.getDescription()))
			{
				if (!empty)
					writer.newLine();
				writer.write(compResult.getDescription());
				writer.newLine();
				empty = false;
			}
			
			if (!empty)
				writer.newLine();
			writer.write("Rows compared: ");
			writer.write(Integer.toString(compResult.getTotal()));
			writer.newLine();
			
			writer.write("Passed: ");
			writer.write(Integer.toString(compResult.getPassed()));
			writer.newLine();
			
			writer.write("Failed: ");
			writer.write(Integer.toString(compResult.getFailed()));
			writer.newLine();
			
			writer.write("Not found in actual data: ");
			writer.write(Integer.toString(compResult.getNotFound()));
			writer.newLine();
			
			writer.write("Extra in actual data: ");
			writer.write(Integer.toString(compResult.getExtra()));
			writer.newLine();
		}
	}
	
	protected final void writeFileToArchive(Path file, String name, ZipOutputStream archive, byte[] buffer) throws IOException
	{
		if (file == null)
			return;
		
		try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file.toFile()), buffer.length))
		{
			writeStreamToArchive(bis, name, archive, buffer);
		}
	}
	
	protected final void writeFileFromArchiveToArchive(Path file, String name, ZipOutputStream archive, byte[] buffer) throws IOException
	{
		if (file == null)
			return;
		
		try (ZipFile zip = new ZipFile(file.toFile()))
		{
			Enumeration<? extends ZipEntry> entries = zip.entries();
			if (!entries.hasMoreElements())
				return;
			
			ZipEntry entry = entries.nextElement();
			writeStreamToArchive(zip.getInputStream(entry), name, archive, buffer);
		}
	}
	
	
	private void writeStreamToArchive(InputStream is, String name, ZipOutputStream archive, byte[] buffer) throws IOException
	{
		archive.putNextEntry(new ZipEntry(name));
		int count;
		while ((count = is.read(buffer)) != -1)
			archive.write(buffer, 0, count);
	}
}