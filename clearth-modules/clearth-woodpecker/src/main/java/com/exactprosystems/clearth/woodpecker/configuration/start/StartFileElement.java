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

package com.exactprosystems.clearth.woodpecker.configuration.start;

import static java.lang.String.format;

public enum StartFileElement
{
	PROVIDERS_BLOCK("Message providers block", "messageProviders", "XML tag"),
	
	SENDERS_BLOCK("Message senders block", "messageSenders", "XML tag"),
	
	ACTIONS_BLOCK("Actions block", "actions", "XML tag");
	
	private final String description;
	private final String elementName;
	private final String elementType;

	StartFileElement(String description, String elementName, String elementType)
	{
		this.description = description;
		this.elementName = elementName;
		this.elementType = elementType;
	}

	public String getDescription()
	{
		return description;
	}

	public String getElementName()
	{
		return elementName;
	}

	public String getElementType()
	{
		return elementType;
	}


	@Override
	public String toString()
	{
		return format("%s (%s '%s')", description, elementType, elementName);
	}
}
