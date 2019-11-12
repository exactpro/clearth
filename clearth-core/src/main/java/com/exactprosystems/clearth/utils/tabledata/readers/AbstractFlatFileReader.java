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

package com.exactprosystems.clearth.utils.tabledata.readers;

import com.exactprosystems.clearth.connectivity.flat.FlatMessageDesc;
import com.exactprosystems.clearth.connectivity.flat.FlatMessageFieldDesc;
import com.exactprosystems.clearth.utils.tabledata.BasicTableData;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.Set;

public abstract class AbstractFlatFileReader<C extends BasicTableData<String, String>> extends AbstractFixedLengthDataReader<C>
{
	protected final LinkedHashMap<String, FlatMessageFieldDesc> fieldsDescs;
	
	public AbstractFlatFileReader(File file, FlatMessageDesc messageDesc) throws IOException
	{
		super(file);
		fieldsDescs = fillFieldsDescs(messageDesc);
	}
	
	public AbstractFlatFileReader(Reader reader, FlatMessageDesc messageDesc)
	{
		super(reader);
		fieldsDescs = fillFieldsDescs(messageDesc);
	}
	
	private LinkedHashMap<String, FlatMessageFieldDesc> fillFieldsDescs(FlatMessageDesc messageDesc)
	{
		return messageDesc.getFieldDesc().stream().collect(LinkedHashMap::new,
				(map, fieldDesc) -> map.put(fieldDesc.getName(), fieldDesc), (map1, map2) -> { });
	}
	
	
	@Override
	protected Set<String> readHeader() throws IOException
	{
		return fieldsDescs.keySet();
	}
	
	@Override
	protected String parseColumnValue(String column, String line) throws IOException
	{
		FlatMessageFieldDesc fieldDesc = fieldsDescs.get(column);
		if (fieldDesc == null)
			throw new IOException("Couldn't find dictionary description to parse field '" + column + "'.");
		
		int startIndex = fieldDesc.getPosition() - 1, endIndex = startIndex + fieldDesc.getLength();
		if (endIndex > line.length())
			throw new IOException("Unexpected end of the line: it's expected to be at least " + endIndex + " characters long.");
		return line.substring(startIndex, endIndex);
	}
}
