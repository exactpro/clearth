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

package com.exactprosystems.clearth.connectivity.validation;

import java.util.HashSet;
import java.util.Set;

import com.exactprosystems.clearth.connectivity.connections.ClearThConnection;
import com.exactprosystems.clearth.utils.LineBuilder;
import com.exactprosystems.clearth.utils.SettingsException;

/**
 * Validator to check that connection can be safely started without conflicts with already running connections.
 * Example of such validations:
 * - there should be no started connections that read the same queues;
 * - there should be no connections with listeners writing to the same files (File Listener and Collector file).
 */
public class ConnectionStartValidator
{
	private final Set<ClearThConnectionValidationRule> rules = new HashSet<>();

	public void addRule(ClearThConnectionValidationRule rule)
	{
		rules.add(rule);
	}

	public void checkIfCanStartConnection(ClearThConnection<?, ?> connectionToCheck) throws SettingsException
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
