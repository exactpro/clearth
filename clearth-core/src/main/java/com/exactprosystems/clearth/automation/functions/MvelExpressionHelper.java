/******************************************************************************
 * Copyright 2009-2022 Exactpro Systems Limited
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
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MvelExpressionHelper {
	
	private final List<MethodData> methodsList;
	private final List<SpecialData> specialDataList;
	
	public MvelExpressionHelper(Class c)
	{
		Method[] methods = getMethods(c);
		methodsList = getMethodDataList(methods);
		
		specialDataList = Stream.concat(
				getSpecialValues(getFields(c)).stream(),
				getSpecialFunctions(getMethods(c)).stream()
		).collect(Collectors.toList());
	}
	
	public List<MethodData> getMethodsList()
	{
		return methodsList;
	}

	public List<SpecialData> getSpecialDataList()
	{
		return specialDataList;
	}

	private Method[] getMethods(Class c)
	{
		return c.getMethods();
	}
	
	private Field[] getFields(Class c)
	{
		return c.getFields();
	}
	
	private List<MethodData> getMethodDataList(Method[] methods)
	{
		List<MethodData> result = new ArrayList<>();
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
		
		result.sort(createMethodsComparator());
		return result;
	}
	
	private List<SpecialData> getSpecialData(AnnotatedElement[] elements, SpecialDataType type)
	{
		List<SpecialData> result = new ArrayList<>();
		
		Arrays.stream(elements).filter(el -> el.isAnnotationPresent(SpecialDataModel.class))
								.map(el -> el.getAnnotation(SpecialDataModel.class))
								.forEach(el -> result.add(new SpecialData(
										el.name(),
										type,
										el.value(),
										el.usage(),
										el.description()
								)));
		
		result.sort(Comparator.comparing(SpecialData::getName));
		
		return result;
	}
	
	private List<SpecialData> getSpecialValues(Field[] fields)
	{
		return getSpecialData(fields, SpecialDataType.VALUE);
	}
	
	private List<SpecialData> getSpecialFunctions(Method[] methods)
	{
		return getSpecialData(methods, SpecialDataType.FUNCTION);
	}
	
	protected Comparator<MethodData> createMethodsComparator()
	{
		return new MethodsComparator();
	}
}
