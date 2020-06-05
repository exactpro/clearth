/******************************************************************************
 * Copyright 2009-2020 Exactpro Systems Limited
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.automation.Scheduler;
import com.exactprosystems.clearth.utils.exception.MatrixUploadHandlerException;

public class MatrixUploadHandler
{
	private static final Logger logger = LoggerFactory.getLogger(MatrixUploadHandler.class);

	public static final String[] MATRIX_FILE_SUFFIX_LIST = {".csv", ".xls", ".xlsx"};
	public static final String ARCHIVE_FILE_SUFFIX = ".zip";

	protected final static IOFileFilter MATRIX_FILES_FILE_FILTER = createMatrixFileFilter();
	protected final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");
	protected final static Pattern UPLOADED_FILENAME_PATTERN = Pattern.compile("\\\\");

	protected File matrixUploadDir;


	public MatrixUploadHandler(File matrixUploadDir)
	{
		Objects.requireNonNull(matrixUploadDir, "Matrix upload directory cannot be null");
		this.matrixUploadDir = matrixUploadDir;
	}


	public void handleUploadedFile(InputStream fileStream, String fileName, Scheduler scheduler) throws MatrixUploadHandlerException
	{
		if (isMatrixFile(fileName))
		{
			handleUploadedMatrixFile(fileStream, fileName, scheduler);
		}
		else if (isArchiveFile(fileName))
		{
			handleUploadedArchive(fileStream, fileName, scheduler);
		}
		else
		{
			String matrixExtensions = String.join(" ,", MATRIX_FILE_SUFFIX_LIST);
			String exceptionMessage = MessageFormat.format("Unsupported file extension '{0}'", fileName);
			String exceptionDetails = MessageFormat.format("Please choose {0}, {1} files only",
					matrixExtensions, ARCHIVE_FILE_SUFFIX);
			throw new MatrixUploadHandlerException(exceptionMessage, exceptionDetails);
		}
	}

	protected void handleUploadedMatrixFile(InputStream matrixStream, String fileName, Scheduler scheduler) throws MatrixUploadHandlerException
	{
		logger.trace("Original filename: '{}'", fileName);
		String normalizedFileName = normalizeUploadedFileName(fileName);
		logger.trace("Filename to use: '{}'", normalizedFileName);

		File storedFile = createStoredFile(matrixStream, normalizedFileName, getMatrixUploadDir());
		addMatrixToScheduler(scheduler, storedFile, normalizedFileName);
	}

	private void addMatrixToScheduler(Scheduler scheduler, File storedFile, String originalName) throws MatrixUploadHandlerException
	{
		try
		{
			scheduler.addMatrix(storedFile, originalName);
			logger.info("Uploaded matrix '{}' to scheduler '{}'", originalName, scheduler.getName());
		}
		catch (ClearThException e)
		{
			String errorMessage = MessageFormat.format("Error while uploading matrix '{0}'", originalName);
			throw handleError(errorMessage, e);
		}
	}

	protected void handleUploadedArchive(InputStream fileStream, String fileName, Scheduler scheduler) throws MatrixUploadHandlerException
	{
		File unzipDir = getUnzipDir(fileStream, fileName);

		Collection<File> unzippedFiles = FileUtils.listFiles(unzipDir, getMatrixFileFilter(), FileFilterUtils.trueFileFilter());
		if (unzippedFiles.isEmpty())
		{
			String exceptionDetails = MessageFormat.format("Uploaded archive '{0}' doesn't contain any matrices", fileName);
			throw new MatrixUploadHandlerException("No matrices in archive", exceptionDetails);
		}

		List<String> errorMessages = new LinkedList<>();
		for (File file : unzippedFiles)
		{
			try
			{
				addMatrixToScheduler(scheduler, file, file.getName());
			}
			catch (MatrixUploadHandlerException e)
			{
				String errorMessage = MessageFormat.format("Could not add matrix file '{0}'", file.getName());
				errorMessages.add(errorMessage + ": " + e.getMessage());
				logger.error(errorMessage, e);
			}
		}

		if (!errorMessages.isEmpty())
		{
			throw new MatrixUploadHandlerException(String.join("; \n", errorMessages));
		}
	}

	private File getUnzipDir(InputStream fileStream, String fileName) throws MatrixUploadHandlerException
	{
		File storedFile = createStoredFile(fileStream, fileName, getMatrixUploadDir());
		File unzipDir = FileOperationUtils.uniqueFileName(getMatrixUploadDir(), "matrices_" + DATE_FORMAT.format(new Date()), "");

		try
		{
			Files.createDirectories(unzipDir.toPath());
			FileOperationUtils.unzipFile(storedFile, unzipDir);
		}
		catch (IOException e)
		{
			String errorMessage = MessageFormat.format("Error while unzipping archive '{0}'", fileName);
			throw handleError(errorMessage, e);
		}

		return unzipDir;
	}

	private MatrixUploadHandlerException handleError(String errorMessage, Throwable e)
	{
		logger.error(errorMessage, e);
		return new MatrixUploadHandlerException(errorMessage, e);
	}

	protected File createStoredFile(InputStream fileStream, String fileName, File storageDir) throws MatrixUploadHandlerException
	{
		String prefix = buildPrefix(fileName), suffix = getExtension(fileName);
		try
		{
			return FileOperationUtils.storeToFile(fileStream, storageDir, prefix, suffix);
		}
		catch (IOException e)
		{
			String errorMessage = MessageFormat.format("Could not create storage file '{0}'", fileName);
			throw handleError(errorMessage, e);
		}
	}

	protected String normalizeUploadedFileName(String fileName)
	{
		// From some browsers we can get file name with slashes or backslashes. Removing them to get exact file name
		fileName = UPLOADED_FILENAME_PATTERN.matcher(fileName).replaceAll("/");
		if (fileName.contains("/"))
			fileName = fileName.substring(fileName.lastIndexOf('/') + 1);

		return fileName;
	}

	protected String getExtension(String fileName)
	{
		return "."+ FilenameUtils.getExtension(fileName).toLowerCase();
	}

	protected boolean isMatrixFile(String fileName)
	{
		String lowerCase = fileName.toLowerCase();
		for (String suffix : MATRIX_FILE_SUFFIX_LIST)
		{
			if (lowerCase.endsWith(suffix))
				return true;
		}

		return false;
	}

	protected static IOFileFilter createMatrixFileFilter()
	{
		return new SuffixFileFilter(MATRIX_FILE_SUFFIX_LIST, IOCase.INSENSITIVE);
	}

	protected IOFileFilter getMatrixFileFilter()
	{
		return MATRIX_FILES_FILE_FILTER;
	}

	protected boolean isArchiveFile(String fileName)
	{
		return fileName.toLowerCase().endsWith(ARCHIVE_FILE_SUFFIX);
	}

	protected String buildPrefix(String fileName)
	{
		int length = fileName.length();
		/*
		Prefix string must be at least 3 characters,
		so there are at least 7 symbols in name required (with extension),
		and count of "_" must be (7 - length)
		*/
		StringBuilder prefix = new StringBuilder(fileName.substring(0, length - 4));
		for (int i = length; i < 7; i++)
		{
			prefix.append("_");
		}

		return prefix.append("_").toString();
	}

	public File getMatrixUploadDir()
	{
		return matrixUploadDir;
	}

	public void setMatrixUploadDir(File matrixUploadDir)
	{
		this.matrixUploadDir = matrixUploadDir;
	}
}
