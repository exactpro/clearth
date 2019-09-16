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

package com.exactprosystems.clearth.automation;

public enum ActionGeneratorMessageKind
{
	/* Warnings */
	EXCESSIVE_VALUES("Excessive values"),
	MISSING_VALUES("Missing values"),
	MISSING_HEAD("Missing header columns"),
	MISSING_ID("Missing ID"),
	MISSING_GLOBALSTEP("Missing global step"),
	NONEXISTENT_GLOBALSTEP("Nonexistent global step"),
	INVALID_TIMEOUT("Invalid timeout value"),
	DUPLICATE_PARAMS("Duplicate parameters"),
	DUPLICATE_ID("Duplicate ID"),
	UNKNOWN_ACTION_TYPE("Unknown action type"),
	ACTION_INSTANCE_ERROR("Could not instantiate action"),
	MISSING_EXPECTED_PARAMS("Missing expected parameters"),
	INVALID_ACTION_ID("Invalid action ID"),
	UNEXPECTED_STEP_KIND("Unexpected step kind"),
	/* Errors */
	UNEXPECTED_GENERATING_ERROR("Unexpected generating error"),
	HEADER_NOT_DEFINED_FOR_ACTION("Header not defined"),
	UNSUPPORTED_FILE_EXTENSION("Unsupported file extension");
	
	private String description;
	
	private ActionGeneratorMessageKind(String description)
	{
		this.description = description;
	}
	
	public String getDescription()
	{
		return description;
	}
}
