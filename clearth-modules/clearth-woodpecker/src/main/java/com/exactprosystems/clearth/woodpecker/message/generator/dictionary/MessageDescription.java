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

package com.exactprosystems.clearth.woodpecker.message.generator.dictionary;

import java.nio.file.Path;
import java.util.*;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

public class MessageDescription
{
	private final Path sourceFilePath;
	private String messageType;
	private final List<FieldDescription> fields = new ArrayList<>();
	private List<RgDescription> repeatingGroupsDescs;
	
	
	MessageDescription(Path sourceFilePath)
	{
		this.sourceFilePath = requireNonNull(sourceFilePath, "sourceFilePath");
	}
	
	
	public Path getSourceFilePath()
	{
		return sourceFilePath;
	}
	

	public String getMessageType()
	{
		return messageType;
	}

	void setMessageType(String messageType)
	{
		this.messageType = messageType;
	}
	

	public boolean containsFields()
	{
		return !fields.isEmpty();
	}
	
	public List<FieldDescription> getFields()
	{
		return fields;
	}
	
	void addField(FieldDescription fd)
	{
		fields.add(fd);
	}
	
	
	public boolean containsRepeatingGroups()
	{
		return isNotEmpty(repeatingGroupsDescs);
	}

	public List<RgDescription> getRepeatingGroupsDescs()
	{
		return repeatingGroupsDescs;
	}
	
	void addRepeatingGroupDesc(RgDescription desc)
	{
		if (repeatingGroupsDescs == null)
			repeatingGroupsDescs = new ArrayList<>();
		repeatingGroupsDescs.add(desc);
	}
}
