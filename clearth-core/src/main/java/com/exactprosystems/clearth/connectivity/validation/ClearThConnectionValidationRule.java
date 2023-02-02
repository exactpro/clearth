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

package com.exactprosystems.clearth.connectivity.validation;

import com.exactprosystems.clearth.connectivity.connections.ClearThConnection;

/**
 * Rule for Connection settings validation.
 */
public interface ClearThConnectionValidationRule
{
	/**
	 * Tests whether this rule is applicable for this connection.
	 * @param connectionToCheck connection to test.
	 * @return true if this rule is applicable to specified connection.
	 */
	boolean isConnectionSuitable(ClearThConnection connectionToCheck);

	/**
	 * Validates connection and returns error message if validation is failed.
	 * @param connectionToCheck connection to check.
	 * @return error description if validation is failed. In case of passed validation can return null or empty String.
	 */
	String check(ClearThConnection connectionToCheck);
}
