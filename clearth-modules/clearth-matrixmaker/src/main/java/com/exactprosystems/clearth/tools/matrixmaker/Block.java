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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class Block extends MMTemplate
{
	public static final String INPUT = "#{input", OUTPUT = "#{output";
	public static final Logger logger = LoggerFactory.getLogger(Block.class);
	
	List<TemplateRow> init;
	Map<String,Block> blocks;
	String name;
	Map<String, String> inputParams = new HashMap<String, String>();
	Map<String,String> outputParams = new HashMap<String, String>();
	long initRowNumber = 0;
	
	public Block(String name, List<TemplateRow> rows, List<TemplateRow> init, Map<String,Block> blocks, long initRowNumber)
	{
		this.templateRows = rows;
		this.name = name;
		this.blocks = blocks;
		this.init = init;
		this.initRowNumber = initRowNumber;
		
	}
	
	@Override
	public String toString()
	{
		return name+": "+templateRows.size()+" rows, inner blocks: "+blocks.keySet();
	}
	

	public Map<String, Block> getBlocks()
	{
		return blocks;
	}

	public String getName()
	{
		return name;
	}
/*
	public List<String[]> getBlock(Map<String,String> params) throws GenerateException
	{
		Functions func = new Functions(new HashMap<>());
		for (String key : params.keySet())
		{
			func.saveThis("saveThis", new String[]{key,params.get(key)});
		}
		for (String[] row :  rows)
		{
			for (int i = 0; i < row.length; i++)
			{
				String val = row[i];
				if (StringUtils.startsWith(val, INPUT))
				{
					row[i] = findInputValue(val, func);
				}
			}
		}
		return null;
	}
	*/
	/*protected String findInputValue(String val, Functions func) throws GenerateException
	{
		return func.generateValue(val);
	}*/
	
	public static Collection<Block> findBlocksByName(Map<String,Block> blocks, String[] names, int level) throws GenerateException
	{
		String name = names[level].trim();
		if (name.equals("*"))
			return blocks.values();
		Block b = blocks.get(name);
		if (b == null)
			throw new GenerateException("Cannot find block with the name '"+name+"'");
		if (names.length > ++level)
			return findBlocksByName(b.getBlocks(), names, level);
		else return Arrays.asList(b);
	}
	
	public void parseInit() throws GenerateException
	{
		for (TemplateRow row : init)
		{
			for (int i=0;i<row.size();i++)
			{
				String val = row.get(i);
				if (row.get(i) == null)
					continue;
				if (val.startsWith("#{input"))
				{
					if (val.endsWith("}"))
						val = val.substring(0, val.length()-1);
					String[] ps = Functions.parsParams(val.substring(2,val.length()-1));
					val = ps[1];
					/*String res = pars.get(row.get(0));
					if (res == null)
						throw new GenerateException("Input parameter '"+row.get(0)+"' is not defined.");
					//logger.debug("res='"+res.substring(0,res.length()-1)+"'");
				
					if (res.startsWith("#{") && res.endsWith("}"))
						res = res.substring(0,res.length()-1);*/
					inputParams.put(row.get(0), ps[1]);//"#{"+val+"}";
				}
				else if (val.startsWith("#{output"))
				{
					logger.debug("val="+val);
					String[] ps = Functions.parsParams(val.substring(2,val.length()-1));
					if (ps.length < 3)
						throw new GenerateException("Expression " + val + " should have 2 parameters.");
					this.outputParams.put(row.get(0), ps[2]);
				}
			}
		}
		//replaceInputParams();
	}
	
	protected void replaceInputParams()
	{
		
		for (TemplateRow row : templateRows)
		{
			for (int i = 0; i < row.size(); i++)
			{
				String val = row.get(i);
				if (val.contains("#{"))
				{
					for (String k : inputParams.keySet())
					{
						//logger.debug("k= "+k);
						if (val.contains("#{"+k)){
							//logger.debug("replace "+k+" "+inputParams.get(k));
							String res = inputParams.get(k);
							if (res.startsWith("#{"))
								row.set(i, val.replace("#{"+k, inputParams.get(k)));
							else 
								row.set(i, val.replace("#{"+k, inputParams.get(k)).replaceFirst("}", ""));
							break;
						}
					}
				}
			}
		}
	}
	
	/*public Block createCopy()
	{
		return new Block(this.name, new LinkedList(rows), new LinkedList<String[]>(init), Map<String,Block> blocks, long initRowNumber);
	}*/
}
