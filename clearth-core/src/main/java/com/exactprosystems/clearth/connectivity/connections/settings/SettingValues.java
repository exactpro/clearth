/******************************************************************************
 * Copyright 2009-2023 Exactpro Systems Limited
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

package com.exactprosystems.clearth.connectivity.connections.settings;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.exactprosystems.clearth.connectivity.connections.ClearThConnection;
import com.exactprosystems.clearth.connectivity.connections.ClearThConnectionSettings;

public class SettingValues
{
	private final Map<String, SettingAccessor> settings;
	
	public SettingValues(SettingsModel model, ClearThConnection owner, boolean includeNameSetting)
	{
		this.settings = Collections.unmodifiableMap(buildSettingsMap(model, owner, includeNameSetting));
	}
	
	
	public Collection<SettingAccessor> getSettings()
	{
		return settings.values();
	}
	
	public SettingAccessor getSetting(String fieldName)
	{
		return settings.get(fieldName);
	}
	
	
	private Map<String, SettingAccessor> buildSettingsMap(SettingsModel model, ClearThConnection owner, boolean includeNameSetting)
	{
		Map<String, SettingAccessor> result = new LinkedHashMap<>();
		
		FieldsModel fieldsModel = model.getFieldsModel();
		
		if (includeNameSetting)
		{
			SettingProperties nameProps = fieldsModel.getNameProps();
			//Name is not member of settings but should be in the same list of accessors to be processed in one table in GUI
			result.put(nameProps.getFieldName(), new SettingAccessor(nameProps, owner));
		}
		
		ClearThConnectionSettings settings = owner.getSettings();
		for (SettingProperties prop : fieldsModel.getSettingsProps())
		{
			SettingAccessor setting = prop.getValueClass() == ValueClass.ENUM
					? new EnumSettingAccessor((EnumSettingProperties)prop, settings)
					: new SettingAccessor(prop, settings);
			result.put(setting.getProperties().getFieldName(), setting);
		}
		return result;
	}
}
