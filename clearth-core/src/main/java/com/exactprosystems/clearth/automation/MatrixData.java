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

package com.exactprosystems.clearth.automation;

import com.exactprosystems.clearth.ClearThCore;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Date;

import static org.apache.commons.lang.StringUtils.isEmpty;

public class MatrixData implements Cloneable
{
	public static final Logger logger = LoggerFactory.getLogger(MatrixData.class);

	private String name;
	private File file;
	private Date uploadDate;
	private boolean execute;
	private boolean trim;

	//Linked matrix fields
	private String link;
	private String type;
	private boolean autoReload;

	protected MatrixData() { }

	public void setName(String name)
	{
		this.name = name;
	}
	
	public String getName() 
	{
		return name;
	}

	public File getFile()
	{
		return file;
	}
	
	public void setFile(File file)
	{
		this.file = file;
	}
	
	
	public Date getUploadDate()
	{
		return uploadDate;
	}

	public void setUploadDate(Date uploadDate)
	{
		this.uploadDate = uploadDate;
	}

	public boolean isExecute()
	{
		return execute;
	}
	
	public void setExecute(boolean execute)
	{
		this.execute = execute;
	}

	public boolean isTrim()
	{
		return trim;
	}
	
	public void setTrim(boolean trim)
	{
		this.trim = trim;
	}

	public boolean isLinked()
	{
		return !isEmpty(link);
	}

	public String getLink()
	{
		return link;
	}

	public void setLink(String link)
	{
		this.link = link;
	}

	public String getType()
	{
		return type;
	}

	public void setType(String type)
	{
		this.type = type;
	}

	public boolean isAutoReload()
	{
		return autoReload;
	}

	public void setAutoReload(boolean autoReload)
	{
		this.autoReload = autoReload;
	}

	@Override
	public MatrixData clone()
	{
		MatrixDataFactory f = ClearThCore.getInstance().getMatrixDataFactory();
		return f.createMatrixData(name, file, uploadDate, execute, trim, link, type, autoReload);
	}
	
	
	public String getExtension() {
		return FilenameUtils.getExtension(name).toLowerCase();
	}

	public boolean isCsv() {
		return getExtension().equals("csv");
	}

	public boolean isXls() {
		return getExtension().equals("xls");
	}

	public boolean isXlsx() {
		return getExtension().equals("xlsx");
	}
}
