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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MvelExpressionHelper {
	
	private final List<MethodData> methodsList;

	public MvelExpressionHelper(Class c)
	{
		Method[] methods = getMethods(c);
		methodsList= getMethodDataList(methods);
	}
	
	public List<MethodData> getMethodsList()
	{
		return methodsList;
	}
	

	private Method[] getMethods(Class c)
	{
		return c.getMethods();
	}
	
	private List<MethodData> getMethodDataList(Method[] methods)
	{
		List<MethodData> result = new ArrayList<MethodData>();
		for (Method m : methods)
		{
			MethodDataModel rawData = m.getAnnotation(MethodDataModel.class);
			if (rawData == null)
				continue;
			
			Annotation[][] rawArgs = m.getParameterAnnotations();
			MethodData methodData;
			if (rawData.args().isEmpty())
			{
				methodData = new MethodData(rawData.group(),
				                            m.getName(),
				                            rawArgs,
				                            m.getParameterTypes(),
				                            m.getReturnType().getSimpleName(),
				                            rawData.description(),
				                            rawData.usage());
			}
			else
			{
				methodData = new MethodData(rawData.group(),
				                            m.getName(),
				                            rawData.args(),
				                            m.getParameterTypes(),
				                            m.getReturnType().getSimpleName(),
				                            rawData.description(),
				                            rawData.usage());
			}
			result.add(methodData);
		}
		
		Collections.sort(result, createMethodsComparator());
		return result;
	}
	
	
	protected Comparator<MethodData> createMethodsComparator()
	{
		return new MethodsComparator();
	}
}
