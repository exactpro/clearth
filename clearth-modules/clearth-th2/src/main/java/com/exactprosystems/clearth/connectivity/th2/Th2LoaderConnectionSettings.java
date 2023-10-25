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

package com.exactprosystems.clearth.connectivity.th2;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.exactprosystems.clearth.connectivity.connections.ClearThConnectionSettings;
import com.exactprosystems.clearth.connectivity.connections.settings.ConnectionSetting;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class Th2LoaderConnectionSettings implements ClearThConnectionSettings
{
	@XmlElement
	@ConnectionSetting(name = "URL")
	private String url;
	
	@Override
	public void copyFrom(ClearThConnectionSettings other)
	{
		Th2LoaderConnectionSettings loaderSettings = (Th2LoaderConnectionSettings)other;
		this.url = loaderSettings.getUrl();
	}
	
	
	public String getUrl()
	{
		return url;
	}
	
	public void setUrl(String url)
	{
		this.url = url;
	}
}
