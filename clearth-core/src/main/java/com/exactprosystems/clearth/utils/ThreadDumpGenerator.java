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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

public class ThreadDumpGenerator
{
	private static final Logger logger = LoggerFactory.getLogger(ThreadDumpGenerator.class);

	public File writeThreadDump(File dir, String fileName)
	{
		File result = new File(dir, fileName);
		BufferedWriter writer = null;
		try
		{
			writer = new BufferedWriter(new FileWriter(result));
			generateThreadDump(writer);
			writer.flush();
		}
		catch (IOException e)
		{
			logger.error("Could not write thread dumps", e);
		}
		finally
		{
			Utils.closeResource(writer);
		}

		return result;
	}

	private void generateThreadDump(Writer writer) throws IOException
	{
		ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
		ThreadInfo[] threadInfo = threadMXBean.dumpAllThreads(true, true);

		for (ThreadInfo info : threadInfo)
		{
			StackTraceElement[] stackTrace = info.getStackTrace();
			writer.write(String.format("\"%s\" state=%s%s", info.getThreadName(), info.getThreadState(), Utils.EOL));


			if (info.getThreadState().equals(Thread.State.BLOCKED))
			{
				writer.write(String.format("blocks %s%s",info.getLockOwnerName(), Utils.EOL));
				writer.write(String.format("waiting for %s to release lock on %s%s",
						info.getLockOwnerName(), info.getLockName(), Utils.EOL));
			}

			for (StackTraceElement element : stackTrace)
			{
				writer.write(String.format("\t%s%s", element, Utils.EOL));
			}
			writer.write(Utils.EOL+Utils.EOL);
		}
	}
}
