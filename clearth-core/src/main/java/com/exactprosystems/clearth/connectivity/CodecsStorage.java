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

package com.exactprosystems.clearth.connectivity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exactprosystems.clearth.xmldata.XmlCodecConfig;

public class CodecsStorage {
	private List<XmlCodecConfig> configsList;
	private Map<String, Integer> configsMap;
	private Set<String> codecNames = null;

	public CodecsStorage(List<XmlCodecConfig> configs)
	{
		configsList = new ArrayList<XmlCodecConfig>(configs);
		initConfigsMaps(configsList);
	}

	private void initConfigsMaps(List<XmlCodecConfig> configsList)
	{
		configsMap = new LinkedHashMap<String, Integer>();
			for (int i = 0; i < configsList.size(); i++)
			{
				this.configsMap.put(configsList.get(i).getName(), i);
				this.configsMap.put(configsList.get(i).getAltName(), i);
			}
	}

	public List<XmlCodecConfig> getConfigsList()
	{
		return configsList;
	}

	public XmlCodecConfig getCodecConfig(String name)
	{
		Integer index = configsMap.get(name);
		if(index != null)
			return configsList.get(index);
		return null;
	}

	public Set<String> getCodecNames() {
		if (this.codecNames == null) {
			this.codecNames = new LinkedHashSet<String>();
			for (XmlCodecConfig xmlCodecConfig : configsList) {
				this.codecNames.add(xmlCodecConfig.getName());
			}
		}
		return codecNames;
	}
}
