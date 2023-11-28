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

package com.exactprosystems.clearth.web.beans.tools.datacomparator;

import org.primefaces.model.file.UploadedFile;

public class FileSettings
{
	private FileSource source;
	private UploadedFile uploadedFile;
	private String pathOnBackend;
	
	public FileSettings(FileSource source)
	{
		this.source = source;
	}
	
	
	public FileSource getSource()
	{
		return source;
	}
	
	public void setSource(FileSource source)
	{
		this.source = source;
	}
	
	
	public UploadedFile getUploadedFile()
	{
		return uploadedFile;
	}
	
	public void setUploadedFile(UploadedFile uploadedFile)
	{
		this.uploadedFile = uploadedFile;
	}
	
	
	public String getPathOnBackend()
	{
		return pathOnBackend;
	}
	
	public void setPathOnBackend(String pathOnBackend)
	{
		this.pathOnBackend = pathOnBackend;
	}
}
