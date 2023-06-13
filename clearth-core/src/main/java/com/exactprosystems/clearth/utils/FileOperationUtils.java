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

package com.exactprosystems.clearth.utils;

import com.csvreader.CsvWriter;
import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.exceptions.ParametersException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static com.exactprosystems.clearth.utils.Utils.closeResource;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;

public class FileOperationUtils
{
	private static final String EXT_ZIP = "ZIP";

	public static final String FILE_SEPARATOR = "/",
								FILE_WINDOWS_SEPARATOR = "\\";

	public static void writeToFile(String filePath, String text) throws IOException
	{
		FileWriter writer = null;
		try
		{
			writer = new FileWriter(new File(filePath));
			writer.write(text);
			writer.flush();
		}
		finally
		{
			Utils.closeResource(writer);
		}
	}
	
	public static void copyFile(String src, String dest) throws IOException
	{
		FileInputStream is = null;
		FileOutputStream os = null;
		try
		{
			is = new FileInputStream(src);
			os = new FileOutputStream(dest);
			byte[] b = new byte[4096];
			int len;
			while ((len = is.read(b)) > 0)
				os.write(b, 0, len);
			os.flush();
		}
		finally
		{
			closeResource(is);
			closeResource(os);
		}
	}
	
	public static void copyFile(String fileName, String srcPath, String destPath) throws IOException
	{
		copyFile(srcPath + fileName, destPath+fileName);
	}
	
	
	public static void zipFiles(File zipFile, File[] files, String[] names) throws IOException
	{
		ZipOutputStream zipOut = null;
		try
		{
			zipOut = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)));
			byte buffer[] = new byte[4096];
			for (int i = 0; i < files.length; i++)
			{
				if (!files[i].exists())
					continue;
				
				if (files[i].isFile())
				{
					BufferedInputStream fileInput = null;
					try
					{
						fileInput = new BufferedInputStream(new FileInputStream(files[i]), buffer.length);
						
						ZipEntry entry;
						if (names != null && i < names.length && names[i] != null && !names[i].trim().isEmpty())
							entry = new ZipEntry(names[i]);
						else
							entry = new ZipEntry(files[i].getName());
						zipOut.putNextEntry(entry);
						
						int count;
						while ((count = fileInput.read(buffer)) != -1)
							zipOut.write(buffer, 0, count);
					}
					finally
					{
						closeResource(fileInput);
					}
				}
				else
					zipDirectory(zipOut, files[i], files[i]);
			}
		}
		finally
		{
			if (zipOut != null)
				zipOut.close();
		}
	}
	
	public static void zipFiles(File zipFile, List<File> files, List<String> names) throws IOException
	{
		zipFiles(zipFile, files.toArray(new File[files.size()]), names.toArray(new String[names.size()]));
	}
	
	public static void zipFiles(File zipFile, File[] files) throws IOException
	{
		zipFiles(zipFile, files, null);
	}
	
	public static void zipFiles(File zipFile, List<File> files) throws IOException
	{
		zipFiles(zipFile, files.toArray(new File[files.size()]));
	}

	private static void zipDirectory(ZipOutputStream zipOut, File dir, File rootDir) throws IOException
	{
		if (!dir.exists())
			return;
		
		if (dir.isDirectory())
		{
			File[] listFiles = dir.listFiles();
			if ((listFiles == null) || (listFiles.length == 0))
				return;
			
			for (File f : listFiles)
				zipDirectory(zipOut, f, rootDir);
			return;
		}
		
		int bytesIn;
		byte[] buffer = new byte[4096];
		FileInputStream fis = new FileInputStream(dir);
		try
		{
			ZipEntry anEntry = new ZipEntry(rootDir.getName() + dir.getPath().substring(rootDir.getPath().length()));
			zipOut.putNextEntry(anEntry);

			while ((bytesIn = fis.read(buffer)) != -1)
				zipOut.write(buffer, 0, bytesIn);
		}
		finally
		{
			closeResource(fis);
		}
	}
	
	public static void zipDirectories(File zipFile, List<File> dirs) throws IOException
	{
		ZipOutputStream zipOut = null;
		try
		{
			zipOut = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)));
			for (File dir : dirs)
				zipDirectory(zipOut, dir, dir);
		}
		finally
		{
			closeResource(zipOut);
		}
	}
	
	public static List<File> unzipFile(File zipFile, File destFolder) throws IOException
	{
		List<File> result = new ArrayList<File>();
		ZipFile zip = null;
		try
		{
			zip = new ZipFile(zipFile);
			// ZipInputStream zipIn = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)));
			byte[] buffer = new byte[4096];
			Enumeration<? extends ZipEntry> entries = zip.entries();
			Path destPath = destFolder.toPath();

			while (entries.hasMoreElements())
			{
				ZipEntry entry = entries.nextElement();
				String entryName = entry.getName().replace(FILE_SEPARATOR, File.separator)
						.replace(FILE_WINDOWS_SEPARATOR, File.separator);

				if (entryName.contains(File.separator))
				{
					String parentPath = entryName.substring(0, entryName.lastIndexOf(File.separator));
					Files.createDirectories(destPath.resolve(parentPath));
				}

				File f = new File(destFolder, entryName);
				result.add(f);
				if (entry.isDirectory())
					Files.createDirectories(f.toPath());
				else
				{
					InputStream is = zip.getInputStream(entry);
					BufferedOutputStream fileOutput = null;
					try
					{
						fileOutput = new BufferedOutputStream(new FileOutputStream(f), buffer.length);
						int read;
						while ((read = is.read(buffer)) > -1)
							fileOutput.write(buffer, 0, read);
						fileOutput.flush();
					}
					finally
					{
						closeResource(fileOutput);
					}
				}
			}
		}
		finally
		{
			if (zip != null)
				zip.close();
		}
		return result;
	}
	
	public static boolean isZip(File file)
	{
		ZipFile zipfile = null;
		try
		{
			zipfile = new ZipFile(file);
			return true;
		}
		catch (IOException e)
		{
			return false;
		}
		finally
		{
			try
			{
				if (zipfile != null)
					zipfile.close();
			}
			catch (IOException e)
			{
				// Just skip
			}
		}
	}
	
	public static List<File> unzipIfNeed(File inputFile, String dirPrefix) throws IOException
	{
		if (!isZip(inputFile))
			return Collections.singletonList(inputFile);
		else
			return unzipFile(inputFile, createTempDirectory(dirPrefix, inputFile.getParentFile()));
	}
	
	public static List<File> unzipIfNeed(File inputFile) throws IOException
	{
		return unzipIfNeed(inputFile, "temp");
	}
	
	public static List<File> unzipIfNeed(File inputFile, String dirPrefix, File dest) throws IOException
	{
		if (!isZip(inputFile))
			return Collections.singletonList(inputFile);
		else
			return unzipFile(inputFile, dest);
	}
	
	public static List<File> getFilesFromDir(String dirPath, String extension, boolean deepSearch)
	{
		List<File> result = new ArrayList<File>();
		File dir = new File(dirPath);
		if (!dir.isDirectory())
			return result;
		for (final File fileEntry : dir.listFiles())
		{
			if (fileEntry.isDirectory())
			{
				if (deepSearch)
					result.addAll(getFilesFromDir(fileEntry.getAbsolutePath(), extension, deepSearch));
			}
			else if (extension == null || FilenameUtils.isExtension(fileEntry.getName(), extension))
				result.add(fileEntry);
		}
		return result;
	}

	public static List<File> getDirsFromDir(String dirPath)
	{
		List<File> result = new ArrayList<File>();
		File dir = new File(dirPath);
		if (!dir.isDirectory())
			return result;
		for (final File fileEntry : dir.listFiles())
		{
			if (fileEntry.isDirectory())
				result.add(fileEntry);
		}
		return result;
	}
	
	public static List<File> findFiles(String fileName) throws ParametersException
	{
		File f = new File(fileName), parent = f.getParentFile();
		if (parent == null)
			return Arrays.asList(f);
		
		FileFilter fileFilter = new WildcardFileFilter(f.getName());
		List<File> files = Arrays.asList(parent.listFiles(fileFilter));
		if ((files == null) || (files.size() == 0))
			throw new ParametersException("No one file corresponds to given path: '" + fileName + "'.");
		return files;
	}
	
	
	public static File uniqueFileName(File destFolder, String destFileName, String dotExtension)
	{
		File newF = new File(destFolder, destFileName + dotExtension);
		int i = 0;
		while (newF.exists())
		{
			i++;
			newF = new File(destFolder, destFileName + "_" + i + dotExtension);
		}
		return newF;
	}
	
	public static File createTempDirectory(String name, File parentDir) throws IOException
	{
		File tempDir = File.createTempFile(name, "", parentDir);

		if (!(tempDir.delete()))
			throw new IOException("Could not delete temp file: " + tempDir.getAbsolutePath());

		if (!(tempDir.mkdir()))
			throw new IOException("Could not create temp directory: " + tempDir.getAbsolutePath());
		
		return tempDir;
	}
	
	public static File createCsvFile(File fileToStore, String[] header, List<String[]> table) throws IOException
	{
		Writer writer = new OutputStreamWriter(new FileOutputStream(fileToStore));
		CsvWriter csvWriter = null;
		try
		{
			csvWriter = new CsvWriter(writer, ',');
			csvWriter.writeRecord(header);
			for (String[] row : table)
				csvWriter.writeRecord(row);
			csvWriter.flush();
			return fileToStore;
		}
		finally
		{
			closeResource(csvWriter);
		}
	}

	public static void clearFileContent(File file) throws IOException
	{
		FileChannel.open(file.toPath(), StandardOpenOption.WRITE).truncate(0).close();
	}

	public static int countInnerFiles(File zipOrDirectory) throws IOException
	{
		if (zipOrDirectory == null) return 0;

		if (zipOrDirectory.isDirectory())
			return countInnerFilesInDirectory(zipOrDirectory);

		String ext = FilenameUtils.getExtension(zipOrDirectory.getName()).toUpperCase();

		if (EXT_ZIP.equals(ext))
			return countInnerFilesInArchive(zipOrDirectory);
		else
			return 0;
	}

	private static int countInnerFilesInDirectory(File directory) throws IOException
	{
		if (directory == null)
			return 0;

		File[] files = directory.listFiles();

		if (files == null) return 0;

		int count = 0;

		for (File file : files)
		{
			if (file.isDirectory())
			{
				count += countInnerFilesInDirectory(file);
				continue;
			}

			String ext = FilenameUtils.getExtension(file.getName()).toUpperCase();

			count += EXT_ZIP.equals(ext) ? countInnerFilesInArchive(file) : 1;
		}

		return count;
	}

	private static int countInnerFilesInArchive(File zipFile) throws IOException
	{
		if (zipFile == null)
			return 0;

		String fileExtension = FilenameUtils.getExtension(zipFile.getName()).toUpperCase();

		int count = 0;

		if (EXT_ZIP.equals(fileExtension))
		{
			File files = Paths.get(ClearThCore.tempPath()).resolve(FilenameUtils.removeExtension(zipFile.getName())).toFile();

			List<File> unzipped = unzipFile(zipFile, files);

			for (File file : unzipped)
			{
				// skip directories as all its nested files gets on the list after unzipping
				if (file.isDirectory()) continue;

				String ext = FilenameUtils.getExtension(file.getName()).toUpperCase();

				if (EXT_ZIP.equals(ext))
					count += countInnerFilesInArchive(file);
				else
					count += 1;
			}

			FileUtils.deleteDirectory(files);
		}

		return count;
	}

	/* Methods Files.newBufferedReader() and Files.newBufferedWriter() create reader and writer
	 * that can be interrupted by setting interruption flag of Thread.
	 * If you need blocking reader or writer that can't be interrupted use methods "newBlockingBufferedReader" or
	 * "newBlockingBufferedWriter" */
	
	public static BufferedReader newBlockingBufferedReader(Path path) throws IOException
	{
		return newBlockingBufferedReader(path.toFile());
	}
	
	public static BufferedReader newBlockingBufferedReader(File path) throws IOException
	{
		return new BufferedReader(new InputStreamReader(new FileInputStream(path), UTF_8));
	}
	
	public static BufferedWriter newBlockingBufferedWriter(Path path) throws IOException
	{
		return newBlockingBufferedWriter(path.toFile(), false);
	}

	public static BufferedWriter newBlockingBufferedWriter(Path path, boolean append) throws IOException
	{
		return newBlockingBufferedWriter(path.toFile(), append);
	}

	public static BufferedWriter newBlockingBufferedWriter(File path) throws IOException
	{
		return newBlockingBufferedWriter(path, false);
	}
	
	public static BufferedWriter newBlockingBufferedWriter(File path, boolean append) throws IOException
	{
		return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path, append), UTF_8));
	}

	public static String resourceToAbsoluteFilePath(String file) throws FileNotFoundException
	{
		File compiledFile = FileUtils.toFile(ClassLoader.getSystemClassLoader().getResource(file));

		if (compiledFile == null)
			throw new FileNotFoundException(format("file '%s' not found", file));

		return compiledFile.getAbsolutePath();
	}

	public static File storeToFile(InputStream is, File storageDir, String prefix, String suffix) throws IOException
	{
		File result = File.createTempFile(prefix, suffix, storageDir);
		return storeToFile(is, result);
	}

	public static File storeToFile(InputStream is, File storageDir, String fileName) throws IOException
	{
		File result = new File(storageDir, fileName);
		return storeToFile(is, result);
	}

	public static File storeToFile(InputStream is, File storageFile) throws IOException
	{
		try (BufferedInputStream bis = new BufferedInputStream(is);
		     BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(storageFile)))
		{
			byte[] bytes = new byte[1024 * 4];
			int read;
			while ((read = bis.read(bytes)) != -1)
			{
				bos.write(bytes, 0, read);
			}
			bos.flush();

			return storageFile;
		}
	}
}
