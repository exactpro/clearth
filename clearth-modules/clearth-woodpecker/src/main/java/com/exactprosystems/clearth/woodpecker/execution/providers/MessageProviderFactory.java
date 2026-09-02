/******************************************************************************
 * Copyright 2009-2025 Exactpro Systems Limited
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

package com.exactprosystems.clearth.woodpecker.execution.providers;

import com.exactprosystems.clearth.woodpecker.configuration.settings.OperationSettings;
import com.exactprosystems.clearth.woodpecker.configuration.start.MessageProviderDesc;
import com.exactprosystems.clearth.woodpecker.configuration.start.OperationUnit;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerConfigException;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerException;
import com.exactprosystems.clearth.woodpecker.notifications.WoodpeckerNotifications;

import java.nio.file.Path;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

public abstract class MessageProviderFactory
{	
	protected abstract MessageProvider create(String daemonName,
	                                          WoodpeckerNotifications notifications,
	                                          MessageProviderDesc description,
	                                          OperationSettings operationSettings,
	                                          Path configsDirPath) throws WoodpeckerException;

	
	public MessageProvider create(String daemonName,
	                              WoodpeckerNotifications notifications,
	                              MessageProviderDesc description,
	                              Path configsDirPath) throws WoodpeckerException
	{
		return create(daemonName,
				notifications,
				description,
				createOperationSettings(description),
				configsDirPath);
	}	
	
	private OperationSettings createOperationSettings(MessageProviderDesc  description) throws WoodpeckerException
	{
		OperationSettings os = new OperationSettings(description.getOperationName(), 
				description.getOperationUnit());		
		
		os.setRgType(description.getOperationRg());
		
		if ((os.getUnit() == OperationUnit.RG) && isBlank(os.getRgType()))
			throw new WoodpeckerConfigException(format("'operationRg' isn't set for '%s' provider with RG operation unit.",
					os.getName()));
		
		return os;
	}
}
