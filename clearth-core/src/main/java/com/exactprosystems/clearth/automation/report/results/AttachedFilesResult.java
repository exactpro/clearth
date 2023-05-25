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

package com.exactprosystems.clearth.automation.report.results;

import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.report.Result;
import com.fasterxml.jackson.annotation.JsonIgnore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class AttachedFilesResult extends Result
{
	private static final long serialVersionUID = 5309370111339031607L;
	private static final Logger logger = LoggerFactory.getLogger(AttachedFilesResult.class);
	
	private final Map<String, Path> storage = new LinkedHashMap<>();
	
	public void attach(String id, Path path)
	{
		storage.put(id, path);
	}
	
	public Path detach(String id)
	{
		return storage.remove(id);
	}
	
	@JsonIgnore
	public Set<String> getIds()
	{
		return Collections.unmodifiableSet(storage.keySet());
	}
	
	public Path getPath(String id)
	{
		if (id == null)
			return null;
		
		return storage.get(id);
	}
	
	public String getName(String id)
	{
		Path path = getPath(id);
		return path != null ? path.getFileName().toString() : null;
	}
	
	@Override
	public void processDetails(File reportDir, Action linkedAction)
	{
		Set<String> ids = getIds();
		if (ids.isEmpty())
			return;
		
		Path target = getDetailsPath(reportDir, linkedAction);
		try
		{
			if (!Files.exists(target))
				Files.createDirectories(target);
		}
		catch (Exception e)
		{
			logger.error("Error while creating target directory '{}'", target, e);
			return;
		}
		
		for (String id : ids)
		{
			Path source = getPath(id);
			try
			{
				Path newPath = target.resolve(source.getFileName());
				newPath = Files.move(source, newPath, StandardCopyOption.REPLACE_EXISTING);
				attach(id, newPath);
			}
			catch (IOException e)
			{
				logger.error(String.format("Error while moving file from '%s' to '%s'", source, target), e);
			}
		}
	}
	
	@Override
	public void clearDetails()
	{
		storage.clear();
	}
}
