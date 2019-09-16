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

package com.exactprosystems.clearth.utils;

import com.exactprosystems.clearth.ClearThCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class DefaultExceptionHandler implements Thread.UncaughtExceptionHandler {

	protected static final Logger logger = LoggerFactory.getLogger(DefaultExceptionHandler.class);

	@Override
	public void uncaughtException(Thread thread, Throwable exception) {
		processOutOfMemoryError(exception);
		processException(thread, exception);
		logger.error(String.format("Exception in thread \"%s\"", thread.getName()), exception);
	}

	protected void processException(Thread thread, Throwable exception)
	{

	}

	private void processOutOfMemoryError(Throwable exception)
	{
		if (exception instanceof OutOfMemoryError)
		{
			File outputDir = new File(ClearThCore.getInstance().getLogsPath());
			if (!outputDir.exists())
				outputDir.mkdirs();

			ThreadDumpGenerator threadDumpGenerator = new ThreadDumpGenerator();

			File result = threadDumpGenerator.writeThreadDump(outputDir, "outOfMemory_thread_dump.txt");
			logger.info("Thread dumps for OutOfMemoryError saved to "+result.getAbsolutePath());
		}
	}
}
