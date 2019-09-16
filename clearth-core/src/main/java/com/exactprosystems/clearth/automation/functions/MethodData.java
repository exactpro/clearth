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

package com.exactprosystems.clearth.automation.functions;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MethodData {

	public static final String ARGS_START = "(",
			ARGS_END = ")",
			ARGS_SPACE = ", ",
			DEFAULT_VALUE = "",
			VAR = "var",
			DEFAULT_TYPE = "default",
			SPACE = " ";
	
	public String group;
	public String name;
	public String args;
	public String returnType;
	public String description;
	public List<String> usage = new ArrayList<String>();

	public String getGroup()
	{
		return group;
	}

	public void setGroup(String group)
	{
		this.group = group;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getArgs()
	{
		return args;
	}

	public void setArgs(String args)
	{
		this.args = args;
	}

	public String getReturnType()
	{
		return returnType;
	}

	public void setReturnType(String returnType)
	{
		this.returnType = returnType;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public List<String> getUsage()
	{
		return usage;
	}

	public void setUsage(List<String> usage)
	{
		this.usage = usage;
	}

	private final Map<String, String> exampleValues = createExamplesMap();

	public MethodData()
	{
		this.args = "";
		this.init("", "", null, new Class[0], "", "", "");
	}

	public MethodData(String group, String name, String args, Class<?>[] parameterTypes, String returnType, String description, String usage)
	{
		this.args = args;
		this.init(group, name, null, parameterTypes, returnType, description, usage);
	}

	public MethodData(String group, String name, Annotation[][] args, Class<?>[] parameterTypes, String returnType, String description, String usage)
	{
		this.args = refineArgs(args, parameterTypes);
		this.init(group, name, args, parameterTypes, returnType, description, usage);
	}

	private void init(String group, String name, Annotation[][] args, Class<?>[] parameterTypes, String returnType, String description, String usage) {
		this.group = group;
		this.name = name;
		this.returnType = returnType;
		this.description = description;
		if(!usage.equals(DEFAULT_VALUE))
		{
			this.usage.add(usage);
		}
		String generatedUsage = createUsage(name, args, parameterTypes);
		if(!generatedUsage.replaceAll(" ", "").equals(usage))
		{
			this.usage.add(generatedUsage);
		}
	}

	public String refineArgs(Annotation[][] args, Class<?>[] parameterTypes)
	{
		StringBuilder sb = new StringBuilder();

		for(int i = 0; i < args.length; i++)
		{
			sb.append(parameterTypes[i].getSimpleName() + SPACE);
			if(args[i].length == 0)
			{
				sb.append(VAR + (i + 1));
			}
			else
			{
				for (int j = 0; j < args[i].length; j++)
				{
					String name = ((MethodArgument)args[i][j]).name();
					sb.append(name);
				}
			}

			if(i != args.length-1)
				sb.append(ARGS_SPACE);
		}

		return sb.toString();
	}

	public String createUsage(String name, Annotation[][] args, Class<?>[] parameterTypes)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(name);
		sb.append(ARGS_START);
		
		for(int i = 0; i < parameterTypes.length; i++)
		{
			if(args == null || args.length <= i || args[i].length == 0)
			{
				if(exampleValues.containsKey(parameterTypes[i].toString()))
				{
					sb.append(exampleValues.get(parameterTypes[i].toString()));
				}
				else
				{
					sb.append(exampleValues.get(DEFAULT_TYPE));
				}
			}
			else
			{
				for (int j = 0; j < args[i].length; j++)
				{
					String example = ((MethodArgument)args[i][j]).example();
					if(example.equals(DEFAULT_VALUE))
					{
						sb.append(exampleValues.get(parameterTypes[i].toString()));
					}
					else
					{
						sb.append(example);
					}
				}
			}

			if(i != parameterTypes.length-1)
				sb.append(ARGS_SPACE);
		}
		sb.append(ARGS_END);

		return sb.toString();
	}
	
	public HashMap<String,String> createExamplesMap()
	{
		HashMap<String, String> examples = new HashMap<String, String>();

		examples.put("int","0");
		examples.put("long","0");
		examples.put("float","0.0");
		examples.put("double","0.0");
		examples.put("class java.lang.String","'abc'");
		examples.put("class java.math.BigDecimal","0");
		examples.put("default","?");
		
		return examples;
	}
}
