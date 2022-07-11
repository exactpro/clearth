/******************************************************************************
 * Copyright 2009-2022 Exactpro Systems Limited
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

package com.exactprosystems.clearth.automation.report.results;

import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.report.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class AttachedFilesResult extends Result
{
	private static final Logger logger = LoggerFactory.getLogger(AttachedFilesResult.class);

	private final Map<String, String> storage = new LinkedHashMap<>();

	public void attach(String id, String pathString)
	{
		storage.put(id, pathString);
	}

	public void attach(String id, Path path)
	{
		attach(id, path.toString());
	}

	public String detach(String id)
	{
		return storage.remove(id);
	}

	public Set<String> getIds()
	{
		return storage.keySet();
	}

	public String getPathString(String id)
	{
		if (id == null)
			return null;

		return storage.get(id);
	}

	@Override
	public void processDetails(File reportDir, Action linkedAction)
	{
		Path source = null,
				target = null;

		for (String id : getIds())
		{
			try
			{
				source = getPath(id);
				target = getDetailsPath(reportDir, linkedAction);

				if (!Files.exists(target))
					Files.createDirectories(target);

				Path newPath = Files.move(source, target.resolve(source.getFileName()));
				attach(id, newPath);
			}
			catch (IOException e)
			{
				logger.error(String.format("Error while moving file from '%s' to '%s'", source, target), e);
			}
		}
	}

	public Path getPath(String id)
	{
		String pathString = getPathString(id);
		if (pathString != null)
			return Paths.get(pathString);

		return null;
	}

	@Override
	public void clearDetails()
	{
		storage.clear();
	}
}
