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

import com.exactprosystems.clearth.woodpecker.execution.providers.MessageProviderTypes;

import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "CsvTemplatesDesc")
public class CsvTemplatesDesc extends MessageProviderDesc
{
    @XmlElement(name = "template", required = true)
    private List<CsvTemplateDesc> templates;
    @XmlAttribute(name = "codec", required = true)
    private String codecName;


	@Override
	public String getProviderType()
	{
		return MessageProviderTypes.CSV_TEMPLATE;
	}
	

	public List<CsvTemplateDesc> getTemplates()
    {
        return templates;
    }

    public void setTemplates(List<CsvTemplateDesc> templates)
    {
        this.templates = templates;
    }

    
    public String getCodecName()
    {
        return codecName;
    }

    public void setCodecName(String codecName)
    {
        this.codecName = codecName;
    }
}
