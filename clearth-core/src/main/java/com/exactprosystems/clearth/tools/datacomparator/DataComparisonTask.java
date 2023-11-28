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

package com.exactprosystems.clearth.tools.datacomparator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.utils.tabledata.readers.BasicTableDataReader;

public class DataComparisonTask implements Runnable
{
	private static final Logger logger = LoggerFactory.getLogger(DataComparisonTask.class);
	
	private final BasicTableDataReader<String, String, ?> expectedReader,
			actualReader;
	private final ComparisonSettings settings;
	private final DataComparatorTool comparator;
	private volatile boolean running;
	private volatile ComparisonResult result;
	private volatile Throwable error;
	
	public DataComparisonTask(BasicTableDataReader<String, String, ?> expectedReader, BasicTableDataReader<String, String, ?> actualReader, 
			ComparisonSettings settings, DataComparatorTool comparator)
	{
		this.expectedReader = expectedReader;
		this.actualReader = actualReader;
		this.settings = settings;
		this.comparator = comparator;
	}
	
	@Override
	public void run()
	{
		running = true;
		result = null;
		error = null;
		
		try
		{
			result = comparator.compare(expectedReader, actualReader, settings);
		}
		catch (Exception e)
		{
			error = e;
			logger.error("Error while comparing datasets", e);
		}
		finally
		{
			Utils.closeResource(actualReader);
			Utils.closeResource(expectedReader);
			running = false;
		}
	}
	
	
	public boolean isRunning()
	{
		return running;
	}
	
	public ComparisonResult getResult()
	{
		return result;
	}
	
	public Throwable getError()
	{
		return error;
	}
	
	public void interrupt()
	{
		comparator.interrupt();
	}
}
