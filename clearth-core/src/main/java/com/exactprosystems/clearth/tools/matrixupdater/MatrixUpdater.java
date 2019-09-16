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

package com.exactprosystems.clearth.tools.matrixupdater;

import com.exactprosystems.clearth.tools.matrixupdater.matrixReader.CsvMatrixReader;
import com.exactprosystems.clearth.tools.matrixupdater.matrixReader.XlsMatrixReader;
import com.exactprosystems.clearth.tools.matrixupdater.matrixWriter.CsvMatrixWriter;
import com.exactprosystems.clearth.tools.matrixupdater.matrixWriter.XlsMatrixWriter;
import com.exactprosystems.clearth.tools.matrixupdater.model.Matrix;
import com.exactprosystems.clearth.tools.matrixupdater.utils.MatrixUpdaterPathHandler;
import com.exactprosystems.clearth.tools.matrixupdater.utils.MatrixUpdaterUtils;
import com.exactprosystems.clearth.utils.FileOperationUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import java.io.*;
import java.nio.file.Path;
import java.util.*;

import com.exactprosystems.clearth.tools.matrixupdater.matrixModifier.MatrixModifier;
import com.exactprosystems.clearth.tools.matrixupdater.settings.MatrixUpdaterConfig;
import com.exactprosystems.clearth.tools.matrixupdater.settings.Update;
import com.exactprosystems.clearth.tools.matrixupdater.utils.SettingsLoader;
import com.exactprosystems.clearth.tools.matrixupdater.utils.SettingsSaver;

import static com.exactprosystems.clearth.utils.FileOperationUtils.unzipFile;
import static com.exactprosystems.clearth.utils.FileOperationUtils.zipDirectories;
import static org.apache.commons.io.FilenameUtils.getExtension;

public class MatrixUpdater
{
	private static Logger logger = LoggerFactory.getLogger(MatrixUpdater.class);

	public static final String
			RESULT_NAME_PREFIX = "updated_",
			DIR_MATRICES = "matrices";

	protected MatrixModifierFactory matrixModifierFactory;

	protected MatrixUpdaterConfig config;

	private volatile int filesCount;
	private volatile Integer progress = 0;

	private volatile File result;
	
	private volatile boolean running = false;
	private volatile boolean canceled = false;
	
	private final String username;

	public MatrixUpdater(String username)
	{
		this.username = username;
		
		createDirectory();		

		matrixModifierFactory = createModifierFactory();
		config = new MatrixUpdaterConfig();
	}
	
	protected MatrixModifierFactory createModifierFactory()
	{
		return new MatrixModifierFactory();
	}
	
	protected MatrixModifier initializeMatrixModifier(Update update) throws MatrixUpdaterException
	{
		switch (update.getProcess())
		{
			case ADD_ACTIONS: 	return matrixModifierFactory.initActionsAppender(update);
			case ADD_CELLS: 	return matrixModifierFactory.initActionCellAppender(update);
			case MODIFY_CELLS: 	return matrixModifierFactory.initActionCellModifier(update);
			case REMOVE_ACTIONS:return matrixModifierFactory.initActionRemover(update);
		}

		return null;
	}

	protected void validateFile(File matricesFile) throws MatrixUpdaterException
	{
		if (matricesFile == null)
			throw new MatrixUpdaterException("Matrix file is null");

		if (!MatrixUpdaterUtils.isValidExtension(matricesFile.getName()))
			throw new MatrixUpdaterException("Matrix file has an inappropriate extension");
	}
	
	public File update(File matricesFile) throws Exception
	{
		validateFile(matricesFile);

		logger.info("Update started");
		running 	= true;
		progress 	= 0;
		filesCount 	= 0;
		canceled 	= false;

		try
		{
			filesCount = countMatrices(matricesFile);

			if (canceled) return null;
			
			result = process(matricesFile);
			
			return result; 
		}
		finally
		{
			running = false;
			logger.info("Update finished");
		}
	}

	private File process(File matricesFile) throws Exception
	{
		if (canceled)
		{
			progress = null;
			return null;
		}

		File resultFile = null;

		if (isMatrixFile(matricesFile))
		{
			Matrix matrix = readMatrix(matricesFile);
			resultFile = writeMatrix(matricesFile, processMatrix(matrix));

			logger.trace("Matrices updated: {}", progress);
			progress ++;
		}
		else if (Extension.ZIP.equals(parseExt(matricesFile.getName())))
		{
			processZip(matricesFile);
			resultFile = zip(matricesFile);
		}

		return resultFile;
	}

	private void processZip(File matricesFile) throws Exception
	{
		List<File> matrices = unzip(matricesFile);

		for (File matrixFile : matrices)
		{	
			if (matrixFile.isDirectory())
				continue;

			process(matrixFile);
		}
	}

	protected Matrix readMatrix(File matrixFile) throws IOException
	{
		switch (parseExt(matrixFile.getName()))
		{
			case CSV: return new CsvMatrixReader().readMatrix(matrixFile);
			case XLSX:
			case XLS: return new XlsMatrixReader().readMatrix(matrixFile);
		}

		throw new IllegalArgumentException("'" + matrixFile.getName() + "' matrix file has an inappropriate extension");
	}

	private Matrix processMatrix(Matrix matrix) throws Exception
	{
		for (Update update : config.getUpdates())
		{
			if (canceled) break;

			MatrixModifier matrixModifier = initializeMatrixModifier(update);

			if (matrixModifier == null) break;

			matrixModifier.processMatrix(matrix);
		}

		return matrix;
	}

	protected File writeMatrix(File file, Matrix matrix) throws IOException
	{
		switch (parseExt(file.getName()))
		{
			case CSV: return new CsvMatrixWriter().writeMatrix(file, matrix);
			case XLSX:
			case XLS: return new XlsMatrixWriter().writeMatrix(file, matrix);
		}

		throw new IllegalArgumentException("'" + file.getName() + "' result file has an inappropriate extension");
	}

	protected Extension parseExt(String fileName)
	{
		return Extension.valueOf(getExtension(fileName).toUpperCase());
	}

	private void createDirectory()
	{
		File path = MatrixUpdaterPathHandler.userUploadsAbsoluteDirectory(this.username).toFile();
		File temp = MatrixUpdaterPathHandler.userConfigPath(this.username).toFile();
		try
		{
			if (!path.exists())
				path.mkdirs();
			else
				FileUtils.cleanDirectory(path);

			if (!temp.exists())
				temp.mkdirs();
			else
				FileUtils.cleanDirectory(temp);
		}
		catch (IOException e)
		{
			logger.error("Error while cleaning output directory", e);
		}
	}

	private List<File> unzip(File file) throws IOException
	{
		List<File> unzipped = unzipFile(file, file.getParentFile().toPath().resolve(DIR_MATRICES).toFile());
		FileUtils.deleteQuietly(file);

		return unzipped;
	}

	private File zip(File file) throws IOException
	{
		File source = file.getParentFile().toPath().resolve(DIR_MATRICES).toFile();
		File writePath = new File(file.getParent(), RESULT_NAME_PREFIX + file.getName());

		if (writePath.exists())
			FileUtils.deleteQuietly(writePath);

		File zipFile = writePath.getCanonicalFile();

		zipDirectories(zipFile, Collections.singletonList(source));
		FileUtils.deleteQuietly(source);

		return zipFile;
	}

	protected boolean isMatrixFile(File file)
	{
		Extension fileExtension = parseExt(file.getName());

		switch (fileExtension)
		{
			case CSV:
			case XLS:
			case XLSX: return true;
			default: return false;
		}
	}

	public MatrixUpdaterConfig getConfig()
	{
		return config;
	}

	public Path saveConfig() throws IOException, JAXBException
	{
		return SettingsSaver.saveSettings(config, this.username);
	}

	private int countMatrices(File matricesFile) throws IOException
	{
		Extension fileExtension = parseExt(matricesFile.getName());

		if (Extension.ZIP.equals(fileExtension))
		{
			return FileOperationUtils.countInnerFiles(matricesFile);
		}
		else
		{
			return 1;
		}
	}

	public Integer getProgress()
	{
		if(progress == null)
		{
			logger.info("current progress {}, return 100", progress);
			return 100;
		}

		if (progress == 0 || filesCount<1) return 0;
		
		if(progress >= filesCount && filesCount > 1)
		{
			return	100;
		}
		
		return (progress*100)/filesCount;
	}

	public File getResult()
	{
		return result;
	}

	public void setConfig(File settingsFile) throws MatrixUpdaterException, IOException, JAXBException
	{
		config = SettingsLoader.loadSettings(settingsFile, this.username);
	}

	public void reset()
	{
		config = new MatrixUpdaterConfig();
	}

	public enum Extension
	{
		CSV, ZIP, XLS, XLSX
	}
	
	public boolean isRunning()
	{
		return running;
	}
	
	public void cleanProgress()
	{
		this.filesCount = 0;
		this.progress = 0;
		this.result = null;
	}

	public boolean isCanceled()
	{
		return canceled;
	}

	public void cancel()
	{
		this.canceled = true;
	}
}
