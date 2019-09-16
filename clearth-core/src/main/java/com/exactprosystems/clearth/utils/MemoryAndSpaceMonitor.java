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

import java.io.File;

public class MemoryAndSpaceMonitor
{
	private static Runtime runtime;
	private static File file;

	private static final int FACTOR = 1024;

	public static final int MB_FACTOR = FACTOR * FACTOR;
	public static final int GB_FACTOR = MB_FACTOR * FACTOR;

	private static final long maxMemory;

	private MemoryAndSpaceMonitor() {}

	static
	{
		runtime = Runtime.getRuntime();
		file = new File(ClearThCore.filesRoot());
		maxMemory = runtime.maxMemory() / MB_FACTOR;
	}

	/** MEMORY */

	public static long getMaxMemoryMb()
	{
		return maxMemory;
	}

	public static long getTotalMemoryMb()
	{
		return runtime.totalMemory() / MB_FACTOR;
	}

	public static long getFreeMemoryMb()
	{
		return runtime.freeMemory() / MB_FACTOR;
	}

	public static long getUsedMemoryMb()
	{
		return ((runtime.totalMemory() - runtime.freeMemory()) / MB_FACTOR);
	}

	public static boolean isMemoryBreachLimit(int percentLimitOfMaxMemory)
	{
		return (getUsedMemoryMb() > (maxMemory / 100) * percentLimitOfMaxMemory);
	}

	/** SPACE */

	public static long getTotalSpace(int factor)
	{
		return file.getTotalSpace() / factor;
	}

	public static long getFreeSpace(int factor)
	{
		return file.getFreeSpace() / factor;
	}

	public static long getUsedSpace(int factor)
	{
		return (file.getTotalSpace() - file.getFreeSpace()) / factor;
	}

	/** GARBAGE COLLECTOR */

	public static void collectGarbage()
	{
		runtime.gc();
	}
}
