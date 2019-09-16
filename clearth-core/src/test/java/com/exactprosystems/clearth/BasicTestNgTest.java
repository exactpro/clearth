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

package com.exactprosystems.clearth;

import com.exactprosystems.clearth.utils.ComparisonUtils;
import org.testng.annotations.BeforeClass;

import java.lang.reflect.Field;

import static org.mockito.Mockito.*;

public abstract class BasicTestNgTest
{
	protected void mockOtherApplicationFields(@SuppressWarnings("unused") ClearThCore application) 
			throws ReflectiveOperationException {}
	
	
	@BeforeClass
	public void mockApplication() throws ReflectiveOperationException
	{
		ClearThCore application = mock(ClearThCore.class, CALLS_REAL_METHODS);
		setStaticField(ClearThCore.class, "instance", application);
		
		mockApplicationFields(application);
		mockOtherApplicationFields(application);
	}
	
	
	protected void mockApplicationFields(ClearThCore application) throws ReflectiveOperationException
	{
		when(application.getRootRelative(anyString())).thenAnswer(i -> i.getArguments()[0]);
		
		ComparisonUtils comparisonUtils = new ComparisonUtils();
		when(application.getComparisonUtils()).thenReturn(comparisonUtils);
	}
	
	
	protected void setStaticField(Class<?> clazz, String fieldName, Object fieldValue) throws ReflectiveOperationException
	{
		Field field = clazz.getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(clazz, fieldValue);
	}
}
