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

package com.exactprosystems.clearth.woodpecker.initialdata.tables.csv;

import com.exactprosystems.clearth.woodpecker.configuration.blacklist.BlackList;
import com.exactprosystems.clearth.woodpecker.configuration.start.CsvTableDesc;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerException;
import com.exactprosystems.clearth.woodpecker.execution.DaemonContext;
import com.exactprosystems.clearth.woodpecker.initialdata.tables.internal.TableDataLoader;
import com.exactprosystems.clearth.woodpecker.initialdata.tables.internal.TableDataLoaderFactory;

import java.nio.file.Path;

import static com.exactprosystems.clearth.woodpecker.utils.WoodpeckerConfigUtils.findConfigFile;

public class CsvTableDataLoaderFactory implements TableDataLoaderFactory<CsvTableDesc>
{
	@Override
	public TableDataLoader create(CsvTableDesc tableDesc, 
	                              DaemonContext context,
	                              Path configsDirPath, 
	                              BlackList blackList) throws WoodpeckerException
	{
		Path csvPath = findConfigFile(tableDesc.getFile(), configsDirPath);
		return new CsvTableDataLoader(tableDesc, csvPath, blackList);
	}
}
