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

package com.exactprosystems.clearth.config;

import javax.xml.bind.annotation.*;


@XmlType(name = "location")
public class ReplacedPath
{
	
	protected volatile String originalPath;
	protected volatile String newPath;

	public ReplacedPath() {}
	
	public void setOriginalPath(String originalPath)
	{
		this.originalPath = originalPath;
	}

	public String getOriginalPath() 
	{
		return this.originalPath;	
	}

	public void setNewPath(String newPath)
	{
		this.newPath = newPath;
	}

	public String getNewPath()
	{
		return this.newPath;
	}

	@Override
	public String toString()
	{
		return " [Old path = " + this.originalPath + " with new path = " + this.newPath + " ]";
	}
}
