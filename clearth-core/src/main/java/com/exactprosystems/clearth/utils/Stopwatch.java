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

/**
 * Watcher for elapsed time. Operates nanoseconds. 
 * Supports check for expiration against given timeout. 
 */
public class Stopwatch
{
	public static final long NANOS_MULTIPLIER = 1000000;
	
	private long startTime,
			timeout,
			elapsed;
	private boolean expired;
	
	public Stopwatch()
	{
		startTime = -1;
		timeout = -1;
		elapsed = 0;
		expired = false;
	}
	

	/**
	 * Creates Stopwatch and starts it.
	 */
	public static Stopwatch createAndStart()
	{
		Stopwatch result = new Stopwatch();
		result.start();
		return result;
	}
	
	/**
	 * Creates Stopwatch and starts it, allowing check for expiration.
	 * @param timeout stopwatch expiration time in milliseconds. It affects only result of {@link isExpired()} method, i.e. when timeout is expired, stopwatch is not automatically stopped.
	 */
	public static Stopwatch createAndStart(long timeout)
	{
		Stopwatch result = new Stopwatch();
		result.start(timeout);
		return result;
	}
	
	
	/**
	 * Starts stopwatch. If it is already started, resets it and starts.
	 */
	public void start()
	{
		startTime = System.nanoTime();
		timeout = -1;
		elapsed = 0;
		expired = false;
	}
	
	/**
	 * Starts stopwatch. If it is already started, resets it and starts.
	 * @param timeout duration (in milliseconds) of stopwatch run before expiration. It affects only result of {@link isExpired()} method, i.e. when timeout is expired, stopwatch is not automatically stopped.
	 */
	public void start(long timeout)
	{
		start();
		this.timeout = millisToNanos(timeout);  //Converting given milliseconds to nanoseconds for faster comparison
	}
	
	/**
	 * @return true if stopwatch is currently running.
	 */
	public boolean isRunning()
	{
		return startTime > -1;
	}
	
	/**
	 * @return number of nanoseconds passed since stopwatch start. If it is not currently running, returns result of previous run.
	 */
	public long getElapsedNanos()
	{
		if (!isRunning())
			return elapsed;
		return System.nanoTime()-startTime;
	}
	
	/**
	 * @return number of milliseconds passed since stopwatch start. If it is not currently running, returns result of previous run.
	 */
	public long getElapsedMillis()
	{
		long nanos = getElapsedNanos();
		if (nanos < 1)
			return nanos;
		return nanosToMillis(nanos);
	}
	
	/**
	 * @return true if time elapsed since stopwatch start exceeds timeout given on stopwatch start. Even if expired, stopwatch is not stopped by this method.
	 */
	public boolean isExpired()
	{
		if (timeout <= 0)
			return true;
		if (expired)
			return expired;
		expired = getElapsedNanos() >= timeout;
		return expired;
	}
	
	/**
	 * Stops the stopwatch, measuring time elapsed since its start. Stopwatch can be started again after that.
	 * @return number of milliseconds elapsed since stopwatch start.
	 */
	public long stop()
	{
		if (!isRunning())
			return 0;
		
		elapsed = getElapsedNanos();
		startTime = -1;
		return nanosToMillis(elapsed);
	}
	
	
	private static long nanosToMillis(long nanos)
	{
		return nanos / NANOS_MULTIPLIER;
	}
	
	private static long millisToNanos(long millis)
	{
		return millis * NANOS_MULTIPLIER;
	}
}
