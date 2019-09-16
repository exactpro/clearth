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
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;

public class SettingsSaver
{
	
	public static void copyAdditionFiles(MatrixUpdaterConfig config, String username) throws IOException {
		File toDir = MatrixUpdaterPathHandler.userConfigInnerDirectory(username).toFile();

		for (Update update : config.getUpdates()) {
			Settings settings;
			Change change;
			File file;
			if ((settings = update.getSettings()) != null &&
					(change = settings.getChange()) != null &&
					(file = change.getAdditionFile()) != null) {
				String filename = file.getName();
				File dstFile = new File(toDir, filename);
				if (!StringUtils.equals(file.getAbsolutePath(), dstFile.getAbsolutePath())) {
					dstFile.delete();
					FileUtils.moveFile(file, dstFile);
					change.setAdditionFile(dstFile);					
				}
			}
		}
	}
	
	/**
	 * Uses JAXB Marshaller to save config as xml file and additions
	 * and then compresses config directory to zip archive.
	 *
	 * @param config Config for MatrixUpdater
	 */
	public static Path saveSettings(MatrixUpdaterConfig config, String username) throws JAXBException, IOException
	{
		File dir = MatrixUpdaterPathHandler.userConfigPath(username).toFile();

		if (!dir.exists())
			dir.mkdirs();

		File dirToZip = MatrixUpdaterPathHandler.userConfigInnerDirectory(username).toFile();

		if (!dirToZip.exists())
			dirToZip.mkdirs();

		File xmlCfg = MatrixUpdaterPathHandler.userConfigXmlFile(username).toFile();

		if (!xmlCfg.exists())
			xmlCfg.createNewFile();

		copyAdditionFiles(config, username);

		XmlUtils.marshalObject(config, new FileOutputStream(xmlCfg));

		Path result = MatrixUpdaterPathHandler.userConfigZipFile(username);

		FileOperationUtils.zipDirectories(result.toFile(), Collections.singletonList(dirToZip));

		return result;
	}
}
