/******************************************************************************
 * Copyright 2009-2022 Exactpro Systems Limited
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

package com.exactprosystems.clearth.connectivity.swift;

import java.io.Reader;
import java.util.List;
import java.util.Map;

import com.exactprosystems.clearth.connectivity.Dictionary;
import com.exactprosystems.clearth.utils.DictionaryLoadException;

public class SwiftDictionary extends Dictionary<SwiftMessageDesc, SwiftDictionaryDesc>
{
	@Deprecated
	public SwiftDictionary(String fileName) throws Exception
	{
		this(fileName, null);
	}
	
	@Deprecated
	public SwiftDictionary(Reader reader) throws Exception
	{
		this(reader, null);
	}
	
	public SwiftDictionary(String fileName, Map<String, String> parameters) throws DictionaryLoadException
	{
		super(fileName, parameters);
	}
	
	public SwiftDictionary(Reader reader, Map<String, String> parameters) throws DictionaryLoadException
	{
		super(reader, parameters);
	}
	
	@Override
	protected Class[] getClassesToBeBound()
	{
		return new Class[] { SwiftDictionaryDesc.class };
	}

	@Override
	protected List<SwiftMessageDesc> getMessageDescs(SwiftDictionaryDesc dictionaryDesc)
	{
		return dictionaryDesc.getMessageDesc();
	}
}
