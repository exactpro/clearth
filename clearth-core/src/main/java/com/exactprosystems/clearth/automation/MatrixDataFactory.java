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

import java.io.File;
import java.util.Date;

/**
 *         18 August 2017
 */
public abstract class MatrixDataFactory
{
	public abstract MatrixData createMatrixData();
	
	public MatrixData createMatrixData(File file, Date uploadDate, boolean execute, boolean trim)
	{
		MatrixData md = createMatrixData();
		md.setName(file.getName());
		md.setFile(file);
		md.setUploadDate(uploadDate);
		md.setExecute(execute);
		md.setTrim(trim);
		return md;
	}
	
	public MatrixData createMatrixData(String name, File file, Date uploadDate, boolean execute, boolean trim, 
	                                   String link, String type, boolean autoReload)
	{
		MatrixData md = createMatrixData();
		md.setName(name);
		md.setFile(file);
		md.setUploadDate(uploadDate);
		md.setExecute(execute);
		md.setTrim(trim);
		md.setLink(link);
		md.setType(type);
		md.setAutoReload(autoReload);
		return md;
	}
}
