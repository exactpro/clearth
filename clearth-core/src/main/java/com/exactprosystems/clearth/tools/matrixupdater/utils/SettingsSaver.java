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

package com.exactprosystems.clearth.tools.matrixupdater.utils;

import com.exactprosystems.clearth.tools.matrixupdater.settings.Change;
import com.exactprosystems.clearth.tools.matrixupdater.settings.MatrixUpdaterConfig;
import com.exactprosystems.clearth.tools.matrixupdater.settings.Settings;
import com.exactprosystems.clearth.tools.matrixupdater.settings.Update;
import com.exactprosystems.clearth.utils.FileOperationUtils;
import com.exactprosystems.clearth.utils.XmlUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

public class SettingsSaver
{
	
	public static void copyAdditionFiles(MatrixUpdaterConfig config, String username, Path pathToFiles) throws IOException {
		Path toDir = MatrixUpdaterPathHandler.userConfigInnerDirectory(username);

		for (Update update : config.getUpdates())
		{
			Settings settings;
			Change change;
			String fileName;
			if ((settings = update.getSettings()) != null &&
					(change = settings.getChange()) != null &&
					(fileName = change.getAddition()) != null)
			{
				File dstFile = toDir.resolve(fileName).toFile(),
					file = pathToFiles.resolve(fileName).toFile();
				if (!StringUtils.equals(file.getAbsolutePath(), dstFile.getAbsolutePath()))
				{
					dstFile.delete();
					FileUtils.copyFile(file, dstFile);
				}
			}
		}
	}
	
	/**
	 * Uses JAXB Marshaller to save config as xml file and additions
	 * and then compresses config directory to ZIP archive.
	 *
	 * @param config Config for MatrixUpdater
	 * @param username Name of ClearTH user
	 * @param pathToFiles Path to configuration files for MatrixUpdater
	 * @return Path to ZIP archive with MatrixUpdater configuration.
	 */
	public static Path saveSettings(MatrixUpdaterConfig config, String username, Path pathToFiles) throws JAXBException, IOException
	{
		Path dir = MatrixUpdaterPathHandler.userConfigPath(username);
		if (!Files.isDirectory(dir))
			Files.createDirectories(dir);
		
		Path dirToZip = MatrixUpdaterPathHandler.userConfigInnerDirectory(username);
		if (!Files.isDirectory(dirToZip))
			Files.createDirectories(dirToZip);
		
		Path xmlCfg = MatrixUpdaterPathHandler.userConfigXmlFile(username);
		if (!Files.isRegularFile(xmlCfg))
			Files.createFile(xmlCfg);
		
		copyAdditionFiles(config, username, pathToFiles);
		XmlUtils.marshalObject(config, xmlCfg.toAbsolutePath().toString());
		
		Path result = MatrixUpdaterPathHandler.userConfigZipFile(username);
		FileOperationUtils.zipDirectories(result.toFile(), Collections.singletonList(dirToZip.toFile()));
		return result;
	}
}
