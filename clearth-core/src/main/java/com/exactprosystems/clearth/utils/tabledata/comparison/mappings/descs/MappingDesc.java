/*******************************************************************************
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

package com.exactprosystems.clearth.utils.tabledata.comparison.mappings.descs;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Used in cases when expected and actual sources have different column names. Example of a mapping file:
 * <pre>{@code
 * <MappingDesc">
 *     <field name="BOTH_DESCS_PRESENTED">
 *         <expected name="expectedA"/>
 *         <actual name="actualA"/>
 *     </field>
 *     <field name="ONLY_EXPECTED_DESC_PRESENTED">
 *         <expected name="expectedB"/>
 *     </field>
 *     <field name="ONLY_ACTUAL_DESC_PRESENTED">
 *         <actual name="actualC"/>
 *     </field>
 *     <field name="IGNORED_AND_NOT_PRESENTED_FIELD" ignored="true"/>
 * </MappingDesc>}
 * </pre>
 * If some side is not presented, by default local name (attribute "name" of tag "field") is used
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "MappingDesc")
public class MappingDesc
{
	@XmlElement(name = "field")
	private List<FieldDesc> fields;

	public List<FieldDesc> getFields()
	{
		if (fields == null)
			fields = new ArrayList<>();
		return fields;
	}

	public void setFields(List<FieldDesc> fields)
	{
		this.fields = fields;
	}
}
