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
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.exactprosystems.clearth.utils.csv.readers.ClearThCsvReader;
import com.exactprosystems.clearth.utils.csv.readers.ClearThCsvReaderConfig;
import org.apache.commons.lang3.StringUtils;

import com.exactprosystems.clearth.tools.matrixmaker.initialdata.InitialData;

public class MMTemplateReader
{
	private File template;

	public MMTemplateReader(File template)
	{
		super();
		this.template = template;
	}
	
	public void read(List<MMTemplate> templates, Map<String, Block> blocks, 
			Map<String,InitialData> initialData) throws GenerateException
	{
		int rowNum = 0;
		String fName = template.getAbsolutePath();
		List<TemplateRow> templ = new LinkedList<TemplateRow>();
		try (ClearThCsvReader reader = new ClearThCsvReader(fName, createCsvReaderConfig()))
		{
			while(reader.hasNext())
			{
				rowNum++;
				String[] values = reader.getValues();
				if (values == null || values.length == 0)
					continue;
				
				TemplateRow row = new TemplateRow(values, "", rowNum);

				String col0 = values[0];
				if (StringUtils.isEmpty(col0))
				{
					templ.add(row);
					continue;
				}
				
				if (col0.equals(MatrixGenerator.START_BLOCK))
				{
					rowNum = retrieveBlock(reader, blocks, rowNum);
					continue;
				}
				else if (col0.equals(MatrixGenerator.END_BLOCK))
				{
					continue;
				}
				else 
				{
					templ.add(row);
				}
			}
			MMTemplate mmTempl = new MMTemplate();
			mmTempl.setTemplateName(template.getName());
			mmTempl.setTemplateRows(templ);
			templates.add(mmTempl);
		}
		catch (IOException e)
		{
			throw new GenerateException(e.getMessage() != null ? e.getMessage() : "", e);
		}
	}
	
	protected ClearThCsvReaderConfig createCsvReaderConfig()
	{
		ClearThCsvReaderConfig config = new ClearThCsvReaderConfig();
		config.setSkipEmptyRecords(false);
		return config;
	}

	protected int retrieveBlock(ClearThCsvReader reader, Map<String,Block> blocks, int rowNum) throws IOException, GenerateException
	{
		List<TemplateRow> rows = new LinkedList<>();
		List<TemplateRow> init = new LinkedList<>();
		Map<String, Block> innerBlocks = new HashMap<>();
		boolean blockHasInit = false;
		boolean insideInit = false;
		String blockName = "";
		
		while(reader.hasNext())
		{
			rowNum++;
			String[] values = reader.getValues();
			if (values.length < 1)
				continue;
			
			TemplateRow row = new TemplateRow(values, "", rowNum);
			String col0 = values[0];
			if (StringUtils.isEmpty(col0))
			{
				rows.add(row);
				continue;
			}
			
			if (col0.equals(MatrixGenerator.START_BLOCK))
			{
				rowNum = retrieveBlock(reader, innerBlocks, rowNum);
			}
			else if (col0.equals(MatrixGenerator.END_BLOCK))
				break;
			else if (col0.equals(MatrixGenerator.START_INIT))
			{
				insideInit = true;
			}
			else if (!blockHasInit && insideInit && col0.equals(MatrixGenerator.BLOCK_NAME))
			{
				blockName = values.length >1 ? values[1] : "";
			}
			
			if (!blockHasInit && insideInit)
				init.add(row);
			else
				rows.add(row);
			
			if (col0.equals(MatrixGenerator.END_INIT))
			{
				insideInit = false;
				blockHasInit = true;
			}
		}
		
		Block b = new Block(blockName, rows, init, innerBlocks, rowNum);
		b.parseInit();
		
		blocks.put(blockName, b);
		return rowNum;
	}
}
