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

package com.exactprosystems.clearth.woodpecker.utils;

import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerConfigException;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static com.exactprosystems.clearth.ClearThCore.filesRoot;
import static com.exactprosystems.clearth.ClearThCore.rootRelative;
import static com.exactprosystems.clearth.woodpecker.configuration.WoodpeckerPaths.WOODPECKER_CONFIGS_BASE_DIR;
import static com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerConfigException.whenFileNotFound;
import static java.nio.file.Files.exists;

public class WoodpeckerConfigUtils
{
	public static Path findConfigFile(String relativePath, Path replicaBaseDir) throws WoodpeckerConfigException
	{
		Path path = Paths.get(relativePath);
		if (path.isAbsolute())
		{
			if (exists(path))
				return path;
			else
				throw whenFileNotFound(path);
		}

		List<Path> possibleLocations = new ArrayList<>();
		possibleLocations.add(replicaBaseDir);
		possibleLocations.add(Paths.get(rootRelative(WOODPECKER_CONFIGS_BASE_DIR)));
		possibleLocations.add(Paths.get(filesRoot()));

		Path location = possibleLocations.stream()
				.filter(p -> exists(p.resolve(relativePath)))
				.findFirst()
				.orElseThrow(() -> whenFileNotFound(relativePath, possibleLocations));

		return location.resolve(relativePath);
	}
}
