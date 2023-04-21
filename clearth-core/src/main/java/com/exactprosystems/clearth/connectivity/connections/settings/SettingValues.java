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
	private final Map<String, SettingAccessor> allSettings,
			supportedSettings;
	
	public SettingValues(SettingsModel model, ClearThConnection owner, boolean includeNameSetting)
	{
		Map<String, SettingAccessor> allSettingsMap = new LinkedHashMap<>(),
				supportedSettingsMap = new LinkedHashMap<>();
		buildSettingsMaps(model, owner, includeNameSetting, allSettingsMap, supportedSettingsMap);
		
		this.allSettings = Collections.unmodifiableMap(allSettingsMap);
		this.supportedSettings = Collections.unmodifiableMap(supportedSettingsMap);
	}
	
	
	public Collection<SettingAccessor> getAllSettings()
	{
		return allSettings.values();
	}
	
	public Collection<SettingAccessor> getSupportedSettings()
	{
		return supportedSettings.values();
	}
	
	public SettingAccessor getSetting(String fieldName)
	{
		return allSettings.get(fieldName);
	}
	
	
	private void buildSettingsMaps(SettingsModel model, ClearThConnection owner, boolean includeNameSetting,
			Map<String, SettingAccessor> allSettings, Map<String, SettingAccessor> supportedSettings)
	{
		FieldsModel fieldsModel = model.getFieldsModel();
		
		if (includeNameSetting)
		{
			SettingProperties nameProps = fieldsModel.getNameProps();
			//Name is not member of settings but should be in the same list of accessors to be processed in one table in GUI
			String fieldName = nameProps.getFieldName();
			SettingAccessor setting = new SettingAccessor(nameProps, owner);
			allSettings.put(fieldName, setting);
			supportedSettings.put(fieldName, setting);
		}
		
		ClearThConnectionSettings settings = owner.getSettings();
		for (SettingProperties prop : fieldsModel.getSettingsProps())
		{
			ValueType type = prop.getValueTypeInfo().getType();
			SettingAccessor setting = type == ValueType.ENUM
					? new EnumSettingAccessor((EnumSettingProperties)prop, settings)
					: new SettingAccessor(prop, settings);
			
			String fieldName = setting.getProperties().getFieldName();
			allSettings.put(fieldName, setting);
			if (type != ValueType.SPECIAL)
				supportedSettings.put(fieldName, setting);
		}
	}
}
