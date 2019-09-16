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

package com.exactprosystems.clearth.utils.sql.conversion;

import org.apache.commons.collections4.BidiMap;

import java.util.Map;
import java.util.Objects;

public class DBFieldMapping
{
	private String srcField; // field name in matrix
	private String destField; // field name in DB
	private BidiMap<String, String> conversions; // old value and new value rules for data
	private Map<String, String> visualizations; // old value and new value rules for report output
	
	public DBFieldMapping()
	{
	}

	public DBFieldMapping(String srcField,
	                      String destField,
	                      BidiMap<String, String> conversions,
	                      Map<String, String> visualizations)
	{
		this.srcField = srcField;
		this.destField = destField;
		this.conversions = conversions;
		this.visualizations = visualizations;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (!(o instanceof DBFieldMapping)) return false;
		DBFieldMapping that = (DBFieldMapping) o;
		return Objects.equals(srcField, that.srcField) &&
				Objects.equals(destField, that.destField) &&
				Objects.equals(conversions, that.conversions) &&
				Objects.equals(visualizations, that.visualizations);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(srcField, destField, conversions, visualizations);
	}

	@Override
	public String toString()
	{
		return "DBFieldMapping{" +
				"srcField='" + srcField + '\'' +
				", destField='" + destField + '\'' +
				", conversions=" + conversions +
				", visualizations=" + visualizations +
				'}';
	}

	public String getSrcField()
	{
		return srcField;
	}

	public void setSrcField(String srcField)
	{
		this.srcField = srcField;
	}

	public String getDestField()
	{
		return destField;
	}

	public void setDestField(String destField)
	{
		this.destField = destField;
	}

	public BidiMap<String, String> getConversions()
	{
		return conversions;
	}

	public void setConversions(BidiMap<String, String> conversions)
	{
		this.conversions = conversions;
	}

	public Map<String, String> getVisualizations()
	{
		return visualizations;
	}

	public void setVisualizations(Map<String, String> visualizations)
	{
		this.visualizations = visualizations;
	}
}
