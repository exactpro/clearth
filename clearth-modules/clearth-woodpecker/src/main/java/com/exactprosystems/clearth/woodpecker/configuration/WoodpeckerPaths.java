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

public interface WoodpeckerPaths
{
	String WOODPECKER_CONFIGS_BASE_DIR = "cfg/woodpecker/";

	String WOODPECKER_DIR = "woodpecker/";

	String WOODPECKER_DAEMONS_SETTINGS_DIR = WOODPECKER_DIR + "daemonssettings/";
	
	String STATISTICS_DIR = WOODPECKER_DIR + "statistics";
	
	String DAEMONS_DICTIONARY_FILE = "cfg/woodpecker/daemons.xml";
}
