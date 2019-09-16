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

package com.exactprosystems.clearth.connectivity.flat;

import com.exactprosystems.clearth.connectivity.Dictionary;
import com.exactprosystems.clearth.utils.DictionaryLoadException;

import org.apache.commons.lang.StringUtils;

import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FlatMessageDictionary extends Dictionary<FlatMessageDesc, FlatMessageDictionaryDesc>
{
	public static final String RIGHT_ALIGNMENT = "right";
	public static final String LEFT_ALIGNMENT = "left";

	public FlatMessageDictionary(String fileName) throws DictionaryLoadException {
		super(fileName);
	}
	
	public FlatMessageDictionary(Reader reader) throws DictionaryLoadException {
		super(reader);
	}
	
	
	@Override
	protected Class[] getClassesToBeBound() {
		return new Class[]{FlatMessageDictionaryDesc.class};
	}

	@Override
	protected Map<String, FlatMessageDesc> convertToMessageDescMap(List<FlatMessageDesc> messageDescList) throws DictionaryLoadException
	{
		Map<String, FlatMessageDesc> messageDescMap = new LinkedHashMap<String, FlatMessageDesc>();
		for (FlatMessageDesc messageDesc : messageDescList)
		{
			String messageDescType = messageDesc.getType();
			if(messageDescMap.containsKey(messageDescType))
			{
				throw new DictionaryLoadException("Dictionary contains multiple descriptions of type '"+messageDescType+"'");
			}
			checkFlatMessageDesc(messageDesc);
			addToConditionsMaps(messageDesc);
			messageDescMap.put(messageDescType, messageDesc);
		}
		return messageDescMap;
	}

	@Override
	protected List<FlatMessageDesc> getMessageDescs(FlatMessageDictionaryDesc dictionaryDesc)
	{
		return dictionaryDesc.getMessageDesc();
	}

	private void checkFlatMessageDesc(FlatMessageDesc messageDesc) throws DictionaryLoadException
	{
		int prevPos = 1;
		for(FlatMessageFieldDesc fieldDesc : messageDesc.getFieldDesc())
		{
			int currPos = fieldDesc.getPosition();
			if(currPos < prevPos)
			{
				throw new DictionaryLoadException("Invalid value of 'position' field, it must be not less than end of previous block.");
			}
			currPos += fieldDesc.getLength();
			prevPos = currPos;

			String alignment = fieldDesc.getAlignment();
			if(!StringUtils.equals(alignment, LEFT_ALIGNMENT) && !StringUtils.equals(alignment, RIGHT_ALIGNMENT))
			{
				throw new DictionaryLoadException("Attribute 'alignment' accepts only values '"+LEFT_ALIGNMENT+"' and '"+RIGHT_ALIGNMENT+"'.");
			}
		}
	}
}
