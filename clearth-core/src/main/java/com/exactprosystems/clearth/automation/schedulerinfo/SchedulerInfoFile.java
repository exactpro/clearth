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

package com.exactprosystems.clearth.automation.schedulerinfo;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SchedulerInfoFile
{
	protected final String name;
	protected String filePath;
	protected boolean isDirectory, isResource, include = false;
	protected long size = -1;
	
	protected final SchedulerInfoFile parent;
	protected final List<SchedulerInfoFile> children = new ArrayList<>();
	
	public SchedulerInfoFile(String directoryName, SchedulerInfoFile parent)
	{
		name = directoryName;
		filePath = null;
		isDirectory = true;
		isResource = false;
		this.parent = parent;
	}
	
	public SchedulerInfoFile(File file, SchedulerInfoFile parent)
	{
		name = file.getName();
		filePath = file.getAbsolutePath();
		isDirectory = file.isDirectory();
		isResource = isResourceFile(file);
		if (!isDirectory)
			size = file.length();
		include = isResource; // If file is a resource, include it by default
		this.parent = parent;
	}
	
	public String getName()
	{
		return name;
	}
	
	public String getFilePath()
	{
		return filePath;
	}
	
	public boolean isResource()
	{
		return isResource;
	}
	
	public boolean isDirectory()
	{
		return isDirectory;
	}
	
	public String getSize()
	{
		return size > 0 ? FileUtils.byteCountToDisplaySize(size) : "-";
	}
	
	
	public void setInclude(boolean include)
	{
		this.include = include;
	}
	
	public boolean isInclude()
	{
		return include;
	}
	
	
	public SchedulerInfoFile getParent()
	{
		return parent;
	}
	
	public void addChildFile(SchedulerInfoFile child) throws IOException
	{
		if (!isDirectory())
			throw new IOException("Couldn't add child node to non-directory parent.");
		
		if (child != null)
			children.add(child);
	}
	
	public List<SchedulerInfoFile> getChildren()
	{
		return children;
	}
	
	
	protected boolean isResourceFile(File file)
	{
		return FilenameUtils.getExtension(file.getName()).equals("gif");
	}
}
