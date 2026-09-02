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

package com.exactprosystems.clearth.woodpecker.execution.providers.csvTemplate;

import com.exactprosystems.clearth.woodpecker.configuration.settings.OperationSettings;
import com.exactprosystems.clearth.woodpecker.configuration.start.CsvTemplateDesc;
import com.exactprosystems.clearth.woodpecker.configuration.start.CsvTemplatesDesc;
import com.exactprosystems.clearth.woodpecker.configuration.start.MessageProviderDesc;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerException;
import com.exactprosystems.clearth.woodpecker.execution.providers.MessageProvider;
import com.exactprosystems.clearth.woodpecker.message.generator.dictionary.LinkedMessagesDescription;
import com.exactprosystems.clearth.woodpecker.message.generator.dictionary.MessageDescription;
import com.exactprosystems.clearth.woodpecker.message.generator.dictionary.MessageDescriptionLoader;
import com.exactprosystems.clearth.woodpecker.notifications.WoodpeckerNotifications;
import com.exactprosystems.clearth.woodpecker.execution.providers.MessageProviderFactory;

import java.nio.file.Path;

import static com.exactprosystems.clearth.woodpecker.utils.WoodpeckerConfigUtils.findConfigFile;

public class CsvTemplateMsgProviderFactory extends MessageProviderFactory
{
	private final MessageDescriptionLoader mdLoader = new MessageDescriptionLoader();

	@Override
	protected MessageProvider create(String daemonName, 
	                                 WoodpeckerNotifications notifications, 
	                                 MessageProviderDesc description, 
	                                 OperationSettings operationSettings, 
	                                 Path configsDirPath) throws WoodpeckerException
	{
		CsvTemplatesDesc templatesDesc = (CsvTemplatesDesc) description;
		return new CsvTemplateMsgProvider(operationSettings.getName(),
				daemonName, 
				notifications,
				templatesDesc,
				readOperationCsvTemplates(templatesDesc,
						operationSettings,
						configsDirPath));
	}

	private LinkedMessagesDescription readOperationCsvTemplates(CsvTemplatesDesc templatesDesc,
	                                                            OperationSettings os,
	                                                            Path instancePath) throws WoodpeckerException
	{
		LinkedMessagesDescription lmd = new LinkedMessagesDescription(os,
				templatesDesc.getCodecName(),
				templatesDesc.isBatchEnabled());

		for (CsvTemplateDesc templateDesc : templatesDesc.getTemplates())
		{
			lmd.addMessageDesc(readCsvTemplate(templateDesc, instancePath));
		}
		return lmd;
	}
	
	private MessageDescription readCsvTemplate(CsvTemplateDesc templateDesc, Path configsDirPath) throws WoodpeckerException
	{
		Path path = findConfigFile(templateDesc.getFile(), configsDirPath);
		return mdLoader.load(path);
	}
}
