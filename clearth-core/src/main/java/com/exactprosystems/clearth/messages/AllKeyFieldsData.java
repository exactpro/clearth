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

package com.exactprosystems.clearth.messages;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class AllKeyFieldsData
{
	private final KeyFieldsData keys;
	private final RgKeyFieldNames rgKeyFieldNames;
	private final List<KeyFieldsData> keysInRgs;
	
	public AllKeyFieldsData(KeyFieldsData keys, RgKeyFieldNames rgKeyFieldNames, List<KeyFieldsData> keysInRgs)
	{
		this.keys = keys;
		this.rgKeyFieldNames = rgKeyFieldNames;
		this.keysInRgs = keysInRgs;
	}
	
	public KeyFieldsData getKeys()
	{
		return keys;
	}
	
	public RgKeyFieldNames getRgKeyFieldNames()
	{
		return rgKeyFieldNames;
	}
	
	public List<KeyFieldsData> getKeysInRgs()
	{
		return keysInRgs;
	}
	
	
	public boolean hasKeys()
	{
		return keys != null && keys.hasKeys(); 
	}
	
	public boolean hasKeysInRgs()
	{
		if (keysInRgs == null)
			return false;
		
		for (KeyFieldsData kfd : keysInRgs)
		{
			if (kfd.hasKeys())
				return true;
		}
		return false;
	}
	
	public boolean hasAnyKeys()
	{
		return hasKeys() || hasKeysInRgs();
	}
}
