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

package com.exactprosystems.clearth.tools.matrixupdater.utils;

import com.exactprosystems.clearth.ClearThCore;

import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class MatrixUpdaterPathHandler
{
	public static final String
			TOOL_DIR 		= "matrix_updater",
			INNER_FOLDER 	= "MatrixUpdaterConfig",
			SETTINGS_NAME 	= "MatrixUpdaterConfig";

	public static final Path
			TEMP_DIR				= Paths.get(ClearThCore.tempPath(), TOOL_DIR),
			UPLOADS_ABSOLUTE_DIR	= Paths.get(ClearThCore.configFiles().getUploadStorageDir(), TOOL_DIR);

	public static final String
			EXT_ZIP = ".zip",
			EXT_XML = ".xml";

	public static Path userConfigPath(String username)
	{
		return TEMP_DIR.resolve(username);
	}

	public static Path userConfigInnerDirectory(String username)
	{
		return userConfigPath(username).resolve(INNER_FOLDER);
	}

	public static Path userConfigXmlFile(String username)
	{
		return userConfigPath(username).resolve(INNER_FOLDER).resolve(SETTINGS_NAME + EXT_XML);
	}

	public static Path userConfigZipFile(String username)
	{
		return userConfigPath(username).resolve(INNER_FOLDER + EXT_ZIP);
	}

	public static Path userUploadsAbsoluteDirectory(String username)
	{
		return UPLOADS_ABSOLUTE_DIR.resolve(username);
	}
}
