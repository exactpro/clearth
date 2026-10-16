/******************************************************************************
 * Copyright 2009-2024 Exactpro Systems Limited
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

package com.exactprosystems.clearth.web.api;

public class RestConstants
{
	public static final String PARAM_NAME = "name",
			PARAM_TYPE = "type",
			PARAM_STATUS = "status",
			PARAM_ACTION = "action",
			PARAM_DATE = "date",
			PARAM_TEXT = "text",
			PARAM_MATRIX = "matrix",
			PARAM_SCRIPT = "script",
			PARAM_MESSAGE = "message",
			PARAM_FORMAT = "format",
			PARAM_REPORT = "report",
			PARAM_CONFIG = "config",
			PARAM_TIME = "time",
			PARAM_APPEND = "append",
			PARAM_SCHEDULER = "scheduler",
			ACTION_START = "start",
			ACTION_STOP = "stop",
			ACTION_REMOVE = "remove",
			ACTION_CHECK = "check",
			PARAM_EXTENDED = "extended",
			PARAM_CONNECTION_NAME = "connectionname",
			PARAM_QUERY = "query",
			PARAM_MAX_ROWS = "maxrows",
			PARAM_WITH_DETAILS = "withdetails";
	
	//Constants for Matrix Maker
	public static final String PARAM_FILE = "file",
			PARAM_TEMPLATE = "template",
			PARAM_RENAME_MATRICES = "renamematrices",
			PARAM_RANDOM_SCENARIOS = "randomscenarios";
	
	//Constants for ReportsConfig
	public static final String PARAM_COMPLETE_HTML = "completehtml",
			PARAM_FAILED_HTML = "failedhtml",
			PARAM_COMPLETE_JSON = "completejson";
}
