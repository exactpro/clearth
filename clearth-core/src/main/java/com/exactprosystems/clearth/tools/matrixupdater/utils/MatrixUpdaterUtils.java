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

package com.exactprosystems.clearth.tools.matrixupdater.utils;

import com.exactprosystems.clearth.tools.matrixupdater.MatrixUpdater;
import com.exactprosystems.clearth.tools.matrixupdater.model.Cell;
import com.exactprosystems.clearth.utils.ClearThEnumUtils;
import com.exactprosystems.clearth.utils.tabledata.TableHeader;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MatrixUpdaterUtils
{
	public static List<String> headerToList(TableHeader<String> header)
	{
		return new ArrayList<>(Arrays.asList(headerToArray(header)));
	}

	public static String[] headerToArray(TableHeader<String> header)
	{
		String[] arrayHeader = new String[header.size()];
		int i = 0;

		for (String value : header)
			arrayHeader[i++] = value;

		return arrayHeader;
	}

	public static List<String> getListHeader(List<Cell> list)
	{
		List<String> header = new ArrayList<>(list.size());

		for (Cell cell : list)
			header.add(cell.getColumn());

		return header;
	}

	public static String[] getListValues(List<Cell> list)
	{
		String[] header = new String[list.size()];
		int i = 0;

		for (Cell cell : list)
			header[i++] = cell.getValue();

		return header;
	}

	public static boolean isValidExtension(String fileName)
	{
		String ext = FilenameUtils.getExtension(fileName.toUpperCase());
		return ClearThEnumUtils.enumToTextValues(MatrixUpdater.Extension.class).contains(ext);
	}

	public static boolean isZip(File file)
	{
		return MatrixUpdater.Extension.ZIP.name().equals(FilenameUtils.getExtension(file.getName().toUpperCase()));
	}
}
