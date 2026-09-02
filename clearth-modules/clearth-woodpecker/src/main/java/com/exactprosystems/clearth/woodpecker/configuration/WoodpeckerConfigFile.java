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

package com.exactprosystems.clearth.woodpecker.configuration;

public enum WoodpeckerConfigFile
{
	START_FILE("start.xml", "daemon's start config"),
	LOADING_CONFIG("loading.cfg", "loading config"),
	START_XSD("cfg/xsd/daemonStart.xsd", "start config schema");

	private final String fileName;
	private final String description;

	WoodpeckerConfigFile(String fileName, String description)
	{
		this.fileName = fileName;
		this.description = description;
	}

	public String fileName()
	{
		return fileName;
	}

	public String description()
	{
		return description;
	}
}
