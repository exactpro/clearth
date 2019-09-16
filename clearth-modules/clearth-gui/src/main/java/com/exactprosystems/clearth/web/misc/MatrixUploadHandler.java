/******************************************************************************
 * Copyright 2009-2019 Exactpro Systems Limited
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

package com.exactprosystems.clearth.web.misc;

import com.exactprosystems.clearth.automation.Scheduler;
import com.exactprosystems.clearth.utils.ClearThException;
import com.exactprosystems.clearth.utils.FileOperationUtils;
import com.exactprosystems.clearth.utils.exception.HandledInterruptionException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.primefaces.model.UploadedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

public class MatrixUploadHandler
{
	private static final Logger log = LoggerFactory.getLogger(MatrixUploadHandler.class);	
	
	public static final String[] MATRIX_FILE_SUFFIX_LIST = {".csv", ".xls", ".xlsx"};
	protected final static IOFileFilter MATRIX_FILES_FILE_FILTER = createMatrixFileFileFilter();
	protected File matrixUploadDir;

	public MatrixUploadHandler(File matrixUploadDir) {
		this.matrixUploadDir = matrixUploadDir;
	}

	public void handleUploadedFile(UploadedFile uploadedFile, Scheduler scheduler)
	{
		String uploadedFileName = uploadedFile.getFileName();

		if (uploadedFile.getContents().length == 0) {
			MessageUtils.addErrorMessage("Invalid file content", "Uploaded file is empty");
			return;
		}

		try {
			if (isMatrixFile(uploadedFileName)) {
				handleUploadedMatrixFile(uploadedFile, scheduler);
			} else if (isArchiveFile(uploadedFileName)) {
				handleUploadedArchive(uploadedFile, scheduler);
			} else {
				MessageUtils.addErrorMessage("Incorrect file uploaded",
						"Please choose .csv, .xls, .xslx and .zip files only");
			}
		} catch (HandledInterruptionException e) {
			// don't handle because it was already handled
		}
	}

	protected void handleUploadedMatrixFile(UploadedFile uploadedFile, Scheduler scheduler) throws HandledInterruptionException {
		String fileName = uploadedFile.getFileName();
		log.trace("Original filename: '{}'", fileName);
		fileName = normaliseUploadedFileName(fileName);
		log.trace("Filename to use: '{}'", fileName);

		File storedFile = createStoredFile(fileName, getMatrixUploadDir());
		storeUploadedFile(uploadedFile, storedFile);
		try {
			scheduler.addMatrix(storedFile, fileName);
			MessageUtils.addInfoMessage("Success", "File " + fileName + " is uploaded.");
			log.info("uploaded matrix " + fileName+" to scheduler '"+scheduler.getName()+"'");
		} catch (ClearThException e) {
			WebUtils.logAndGrowlException("Error while uploading matrix", e, log);
			throw new HandledInterruptionException(e);
		}
	}

	protected void handleUploadedArchive(UploadedFile uploadedFile, Scheduler scheduler) throws HandledInterruptionException
	{
		File storedFile = createStoredFile(uploadedFile.getFileName(), getMatrixUploadDir());
		storeUploadedFile(uploadedFile, storedFile);

		SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
		File unzipDir = FileOperationUtils.uniqueFileName(getMatrixUploadDir(), "matrices_" + df.format(new Date()), "");
		unzipDir.mkdirs();
		try
		{
			FileOperationUtils.unzipFile(storedFile, unzipDir);
		}
		catch (IOException e)
		{
			WebUtils.logAndGrowlException("Error while unzipping archive " + uploadedFile.getFileName(), e, log);
			throw new HandledInterruptionException(e);
		}


		Collection<File> files = FileUtils.listFiles(unzipDir, getMatrixFilesFileFilter(), FileFilterUtils.trueFileFilter());
		if (files.isEmpty())
		{
			MessageUtils.addInfoMessage("No matrices in archive", "Uploaded archive '"+uploadedFile.getFileName()+"' doesn't contain any matrices");
			throw new RuntimeException("No matrices in archive");
		}
		
		for (File f : files)
		{
			String fName = f.getName();
			try {
				scheduler.addMatrix(f, fName);
			} catch (ClearThException e) {
				WebUtils.logAndGrowlException("Could not add matrix file " + fName, e, log);
			}
			log.info("uploaded matrix '" + fName +"' to scheduler '"+scheduler.getName()+"'");
		}
	}

	protected void storeUploadedFile(UploadedFile uploadedFile, File storedFile) throws HandledInterruptionException {
		try
		{
			WebUtils.storeInputStream(uploadedFile.getInputstream(), storedFile);
		}
		catch (IOException e)
		{
			String uploadedFileName = uploadedFile.getFileName();
			log.error("Error while storing uploaded file '" + uploadedFileName + "'", e);
			MessageUtils.addErrorMessage("Error", "Error occurred while storing uploaded file "+ uploadedFileName + ": " +e.getMessage());
			throw new HandledInterruptionException(e);
		}
	}

	protected File createStoredFile(String fileName, File storageDir) throws HandledInterruptionException {
		String prefix = buildPrefix(fileName),
				suffix = getExtension(fileName);
		try {
			return File.createTempFile(prefix, suffix, storageDir);
		} catch (IOException e) {
			WebUtils.logAndGrowlException("Could not create storage file", e, log);
			throw new HandledInterruptionException(e);
		}
	}


	protected String normaliseUploadedFileName(String fileName) {
		fileName = fileName.replaceAll("\\\\", "/");  //From some browsers we can get file name with slashes or backslashes. Removing them to get exact file name
		if (fileName.contains("/")) {
			fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
		}
		return fileName;
	}

	protected String getExtension(String fileName) {
		return "."+ FilenameUtils.getExtension(fileName).toLowerCase();
	}

	protected boolean isMatrixFile(String fileName) {
		final String lowerCase = fileName.toLowerCase();
		for (String suffix : MATRIX_FILE_SUFFIX_LIST) {
			if (lowerCase.endsWith(suffix)) {
				return true;
			}
		}
		return false;
	}

	protected static IOFileFilter createMatrixFileFileFilter() {
		return new SuffixFileFilter(MATRIX_FILE_SUFFIX_LIST, IOCase.INSENSITIVE);
	}

	protected IOFileFilter getMatrixFilesFileFilter() {
		return MATRIX_FILES_FILE_FILTER;
	}

	protected boolean isArchiveFile(String fileName) {
		return fileName.toLowerCase().endsWith(".zip");
	}

	protected String buildPrefix(String fileName) {
		int length = fileName.length();
		String trailer = "_";
		// Prefix string must be at least 3 characters, so there are at least 7 symbols in name required (with extension), and count of "_" must be (7 - length)
		for (int i = length; i < 7; i++) {
			trailer += "_";
		}
		return fileName.substring(0, length - 4) + trailer;
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
