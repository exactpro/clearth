/******************************************************************************
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

package com.exactprosystems.clearth;

import java.io.IOException;
import java.lang.reflect.Field;

import org.mockito.Mockito;

import com.exactprosystems.clearth.data.DataHandlersFactory;
import com.exactprosystems.clearth.utils.ClearThException;

public class HandlingApplicationManager extends ApplicationManager
{
	private final DataHandlersFactory dataHandlersFactory;
	
	public HandlingApplicationManager(DataHandlersFactory dataHandlersFactory) throws ClearThException
	{
		this.dataHandlersFactory = dataHandlersFactory;
		
		if (ClearThCore.getInstance() != null)
			resetInstance();
		initClearThInstance();
	}
	
	@Override
	protected ClearThCore getCoreInstance() throws ClearThException
	{
		ClearThCore core = super.getCoreInstance();
		Mockito.doReturn(dataHandlersFactory).when(core).createDataHandlersFactory();
		return core;
	}
	
	@Override
	public void dispose() throws IOException
	{
		super.dispose();
		
		resetInstance();
	}
	
	
	private void resetInstance()
	{
		try
		{
			Field field = ClearThCore.class.getDeclaredField("instance");
			field.setAccessible(true);
			field.set(ClearThCore.class, null);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
}
