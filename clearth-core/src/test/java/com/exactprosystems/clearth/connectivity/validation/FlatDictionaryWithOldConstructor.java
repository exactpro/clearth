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
package com.exactprosystems.clearth.connectivity.validation;

import com.exactprosystems.clearth.connectivity.flat.FlatMessageDictionary;
import com.exactprosystems.clearth.utils.DictionaryLoadException;

import java.io.Reader;
import java.util.Map;

public class FlatDictionaryWithOldConstructor extends FlatMessageDictionary
{
	public FlatDictionaryWithOldConstructor(String fileName) throws DictionaryLoadException
	{
		super(fileName, null);
	}

	public FlatDictionaryWithOldConstructor(Reader reader) throws DictionaryLoadException
	{
		super(reader, null);
	}
}