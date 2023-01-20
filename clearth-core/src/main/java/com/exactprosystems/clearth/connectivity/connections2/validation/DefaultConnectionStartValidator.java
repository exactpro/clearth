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

package com.exactprosystems.clearth.connectivity.connections2.validation;

import com.exactprosystems.clearth.connectivity.connections2.ClearThConnection;
import com.exactprosystems.clearth.utils.LineBuilder;
import com.exactprosystems.clearth.utils.SettingsException;

import java.util.Set;

public class DefaultConnectionStartValidator implements ConnectionStartValidator
{

	@Override
	public void checkIfCanStartConnection(ClearThConnection connectionToCheck,
	                                      Set<ClearThConnectionValidationRule> rules) throws SettingsException
	{
		LineBuilder errorMessages = new LineBuilder();
		for (ClearThConnectionValidationRule rule : rules)
		{
			if (rule.isConnectionSuitable(connectionToCheck))
			{
				String errorMessage = rule.check(connectionToCheck);
				if (errorMessage != null)
					errorMessages.append(errorMessage);
			}
		}

		if (errorMessages.length() != 0)
			throw new SettingsException(errorMessages.toString());
	}
}