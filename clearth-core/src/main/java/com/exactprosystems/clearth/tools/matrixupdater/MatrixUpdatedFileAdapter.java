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

package com.exactprosystems.clearth.tools.matrixupdater;

import com.exactprosystems.clearth.tools.matrixupdater.utils.MatrixUpdaterPathHandler;
import org.apache.commons.io.FileUtils;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.io.File;

public class MatrixUpdatedFileAdapter extends XmlAdapter<String, File>
{
	private final String username;

	public MatrixUpdatedFileAdapter(String username) {
		this.username = username;
	}
	
	@Override
	public File unmarshal(String v) throws Exception
	{
		return MatrixUpdaterPathHandler.userConfigInnerDirectory(username).resolve(v).toFile();
	}

	@Override
	public String marshal(File v) throws Exception
	{
		if (v == null) return null;

		File to = MatrixUpdaterPathHandler.userConfigInnerDirectory(username).toFile();
		File check = new File(to, v.getName());

		if (!check.exists())
			FileUtils.copyFileToDirectory(v, to);

		return v.getName();
	}
}
