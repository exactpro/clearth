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

package com.exactprosystems.clearth.web.api.helpers;

import java.util.Date;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.exactprosystems.clearth.automation.MatrixData;
import com.thoughtworks.xstream.annotations.XStreamAlias;

@XmlRootElement(name="matrix")
@XStreamAlias("matrix")
public class MatrixDataResource
{
	@XmlElement
	private String file;
	@XmlElement
	private Date uploadDate;
	@XmlElement
	private boolean execute;
	
	public MatrixDataResource(MatrixData md)
	{
		this.file = md.getFile() != null ? md.getFile().getName() : null;
		this.uploadDate = md.getUploadDate();
		this.execute = md.isExecute();
	}
	
	
	public String getFile()
	{
		return file;
	}
	
	public void setFile(String file)
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
}
