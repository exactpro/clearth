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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class CalledBlock
{
	public static final Logger logger = LoggerFactory.getLogger(CalledBlock.class);
	
	Block block;
	Map<String, String> inputParams = new HashMap<String, String>();
	Map<String,String> outputParams = new HashMap<String, String>();
	List<TemplateRow> rows;
	String externalName;
	
	public CalledBlock(Block block)
	{
		this.block = block;
		this.rows = copyRows();
	}

	public void parseInit(Functions functions, Map<String,String> pars, int startRow) throws GenerateException
	{
		for (TemplateRow row : block.init)
		{
			for (int i=0;i<row.size();i++)
			{
				String val = row.get(i);
				if (val == null)
					continue;
				/*if (val.startsWith("#{input"))
				{
					if (val.endsWith("}"))
						val = val.substring(0, val.length()-1);
					String[] ps = functions.parsParams(val.substring(2,val.length()-1));
					val = ps[1];
					String res = pars.get(row.get(0));
					if (res == null)
						throw new GenerateException("Input parameter '"+row.get(0)+"' is not defined.");
					//logger.debug("res='"+res.substring(0,res.length()-1)+"'");
				
					if (res.startsWith("#{") && res.endsWith("}"))
						res = res.substring(0,res.length()-1);
					inputParams.put(row.get(0), res);//"#{"+val+"}";
				}
				else if (val.startsWith("#{output"))
				{
					String[] ps = functions.parsParams(val.substring(2,val.length()-1));
					this.outputParams.put(row.get(0), ps[2]);
				}*/
				for (String inPar : block.inputParams.keySet())
				{
					String res = pars.get(inPar);
					inputParams.put(inPar, res);
				}
				/*for (Entry<String,String> outPar : block.outputParams.entrySet())
				{
					
				}*/
			}
		}
		logger.debug("Block "+externalName + " inputParams: " + inputParams + " outputParams: "+ outputParams);
		replaceInputParams();
	}
	
	protected String replaceInputParam(String val)
	{
		
		if (val.contains("#{"))
		{
			for (String k : inputParams.keySet())
			{
				//logger.debug("k= "+k);
				if (val.contains("#{"+k)){

					//logger.debug("replace "+k+" "+inputParams.get(k));
					String res = inputParams.get(k);
					if (res.startsWith("#{"))
						val = val.replace("#{"+k, inputParams.get(k));
					else 
						val = val.replace("#{"+k+"}", inputParams.get(k));
					//break;
					//val = row.get(i);
				}
			}
		}
		return val;
	}
	
	protected void replaceInputParams() throws GenerateException
	{
		for (TemplateRow row : rows)
		{
			for (int i = 0; i < row.size(); i++)
			{
				String val = row.get(i);
				if (val.contains("#{"))
				{
					for (String k : inputParams.keySet())
					{
						//logger.debug("k= "+k);
						if (val.contains("#{"+k+"}")){

							logger.debug("replace "+k+" "+inputParams.get(k));
							String res = inputParams.get(k);
							if (res == null)
								throw new GenerateException("Input parameter " + k + " is null.");
							row.set(i,  val.replace("#{"+k+"}", res));
							//if (res.startsWith("#{"))
							//	row.set(i,  val.replace("#{"+k+"}", inputParams.get(k)));
								/*	row.set(i, val.replace("#{"+k, inputParams.get(k)));
							else */
								
							//break;
							val = row.get(i);
						}
					}
				}
			}
		}
	}
	
	protected List<TemplateRow> copyRows()
	{
		List<TemplateRow> rows = block.getTemplateRows();
		List<TemplateRow> resRows = new LinkedList<TemplateRow>();
		for (TemplateRow row : rows)
		{
			List<String> newRow = new ArrayList<String>(row.getRow());
			resRows.add(new TemplateRow(newRow,row.getTemplateName(),row.getRowNumber()));
			
		}
		return resRows;
	}
	

	public String getName()
	{
		return block.getName();
	}
	

	public List<TemplateRow> getRows()
	{
		return rows;
	}
	
	public String findOutputParam(String name)
	{
		return null;
	}

	public String getExternalName()
	{
		return externalName;
	}

	public void setExternalName(String externalName)
	{
		this.externalName = externalName;
	}
	
	

	
}
