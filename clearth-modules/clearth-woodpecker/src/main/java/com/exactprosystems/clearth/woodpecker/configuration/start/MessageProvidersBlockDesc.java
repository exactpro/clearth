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

import java.util.*;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

@XmlAccessorType(XmlAccessType.NONE)
public class MessageProvidersBlockDesc 
{
	@XmlElement(name = "csvTemplates")
    private List<CsvTemplatesDesc> csvTemplates;


	public List<MessageProviderDesc> getProviderDescs()
	{
		List<MessageProviderDesc> providerDescs = new ArrayList<>();
		providerDescs.addAll(filterEnabled(csvTemplates));
		providerDescs.addAll(filterEnabled(getAdditionalProviderDescs()));
		return providerDescs;
	}
	
	protected List<MessageProviderDesc> getAdditionalProviderDescs()
	{
		return emptyList();
	}
	
	private <T extends MessageProviderDesc> List<T> filterEnabled(List<T> providerDescs)
	{
		if (providerDescs == null)
			return emptyList();
		else if (providerDescs.isEmpty())
			return providerDescs;
		else 
			return providerDescs.stream()
					.filter(MessageProviderDesc::isEnabled)
					.collect(toList());
	}

	
	public Set<String> getOperationNames()
	{
		Set<String> operationNames = new LinkedHashSet<>();
		getProviderDescs().stream()
				.map(MessageProviderDesc::getOperationName)
				.forEach(operationNames::add);
		return operationNames;
	}

    
    public List<CsvTemplatesDesc> getCsvTemplates()
    {
        return csvTemplates;
    }

    public void setCsvTemplates(List<CsvTemplatesDesc> csvTemplates)
    {
        this.csvTemplates = csvTemplates;
    }
}
