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

package com.exactprosystems.clearth.tools.matrixmaker;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.exactprosystems.clearth.tools.matrixmaker.initialdata.InitialData;

public class MMTemplate
{
	protected File template;
	protected String templateName;
	protected List<TemplateRow> templateRows;
	private Map<String, InitialData> initialData = new HashMap<String, InitialData>();
	IndexCounter counter;
	
	
	public String getTemplateName()
	{
		return templateName;
	}
	public void setTemplateName(String templateName)
	{
		this.templateName = templateName;
	}
	public List<TemplateRow> getTemplateRows()
	{
		return templateRows;
	}
	public void setTemplateRows(List<TemplateRow> templateRows)
	{
		this.templateRows = templateRows;
	}
	public Map<String, InitialData> getInitialData()
	{
		return initialData;
	}
	public void addInitialData(Map<String, InitialData> initialData)
	{
		this.initialData.putAll(initialData);
	}
	

	public static class IndexCounter
	{
		int index = 0;
		
		public int next()
		{
			return index++;
		}
	}
	
}

