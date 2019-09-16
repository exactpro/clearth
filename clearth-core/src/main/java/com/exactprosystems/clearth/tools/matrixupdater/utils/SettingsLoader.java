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

package com.exactprosystems.clearth.tools.matrixupdater.utils;

import com.exactprosystems.clearth.tools.matrixupdater.MatrixUpdaterException;
import com.exactprosystems.clearth.tools.matrixupdater.settings.MatrixUpdaterConfig;
import com.exactprosystems.clearth.utils.FileOperationUtils;
import com.exactprosystems.clearth.utils.XmlUtils;
import org.apache.commons.io.FileUtils;

import javax.xml.bind.JAXBException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.exactprosystems.clearth.tools.matrixupdater.utils.MatrixUpdaterPathHandler.*;
import static java.lang.String.format;

public class SettingsLoader
{
	private static final String ERROR_MSG = "Invalid config : %s";

	/**
	 * Loads config for MatrixUpdater.
	 *
	 * Extracts archive into temp directory and then uses JAXB Unmarshaller to load settings.
	 * @see MatrixUpdaterPathHandler#userConfigPath()
	 *
	 * @param config Uploaded archive made in the tool.
	 */
	public static MatrixUpdaterConfig loadSettings(File config, String username) throws MatrixUpdaterException, IOException, JAXBException
	{
		validateConfig(config);

		Path currentCfgDir = userConfigPath(username);

		File currentCfg = currentCfgDir.toFile();

		if (currentCfg.exists())
			FileUtils.cleanDirectory(currentCfg);
		else
			Files.createDirectories(currentCfgDir);

		FileOperationUtils.unzipFile(config, currentCfg);

		File xmlCfg = userConfigXmlFile(username).toFile();

		return XmlUtils.unmarshalObject(MatrixUpdaterConfig.class, new FileInputStream(xmlCfg));
	}

	/**
	 * Checks if a file is 'MatrixUpdaterConfig.zip' archive,
	 * has nested 'MatrixUpdaterConfig' directory
	 * with nested file 'MatrixUpdaterConfig.xml'
	 */
	private static void validateConfig(File config) throws MatrixUpdaterException, IOException
	{
		if (config == null)
			throw new MatrixUpdaterException(format(ERROR_MSG, "config file is null"));

		final Enumeration<? extends ZipEntry> entries = new ZipFile(config).entries();

		boolean containCfgFolder = false;
		boolean containCfgFile = false;

		while (entries.hasMoreElements())
		{
			String name = entries.nextElement().getName();
			if (name.contains(INNER_FOLDER + "/"))
				containCfgFolder = true;

			if (name.contains(SETTINGS_NAME + EXT_XML))
				containCfgFile = true;
		}

		if (!containCfgFile)
			throw new MatrixUpdaterException(format(ERROR_MSG, "it must contain " + SETTINGS_NAME + EXT_XML + " file"));

		if (!containCfgFolder)
			throw new MatrixUpdaterException(format(ERROR_MSG, "it must contain " + INNER_FOLDER + " folder"));
	}
}
