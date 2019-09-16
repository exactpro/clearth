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

import com.exactprosystems.clearth.ClearThCore;

import java.nio.file.Path;
import java.nio.file.Paths;

import static com.exactprosystems.clearth.ClearThCore.rootRelative;

public abstract class MatrixUpdaterPathHandler
{
	public static final String
			TOOL_DIR 		= "matrix_updater",
			INNER_FOLDER 	= "MatrixUpdaterConfig",
			SETTINGS_NAME 	= "MatrixUpdaterConfig";

	public static final String
			TEMP_DIR 				= ClearThCore.tempPath() + TOOL_DIR + "/",
			TEMP_DIR_TO_REDIRECT 	= ClearThCore.configFiles().getTempDir() + TOOL_DIR + "/",
			UPLOADS_DIR_TO_REDIRECT = ClearThCore.configFiles().getUploadStorageDir() + TOOL_DIR,
			UPLOADS_ABSOLUTE_DIR 	= rootRelative(UPLOADS_DIR_TO_REDIRECT);

	public static final String
			EXT_ZIP = ".zip",
			EXT_XML = ".xml";

	public static Path userConfigPath(String username)
	{
		return Paths.get(TEMP_DIR, username);
	}

	public static Path userConfigInnerDirectory(String username)
	{
		return userConfigPath(username).resolve(INNER_FOLDER);
	}

	public static Path userConfigXmlFile(String username)
	{
		return userConfigPath(username).resolve(Paths.get(INNER_FOLDER, SETTINGS_NAME + EXT_XML));
	}

	public static Path userConfigZipFile(String username)
	{
		return userConfigPath(username).resolve(INNER_FOLDER + EXT_ZIP);
	}

	public static Path userConfigPathToRedirect(String username)
	{
		return Paths.get(TEMP_DIR_TO_REDIRECT, username);
	}

	public static Path userUploadsDirectoryToRedirect(String username)
	{
		return Paths.get(UPLOADS_DIR_TO_REDIRECT, username);
	}

	public static Path userUploadsAbsoluteDirectory(String username)
	{
		return Paths.get(UPLOADS_ABSOLUTE_DIR, username);
	}
}
