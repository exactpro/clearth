/******************************************************************************
 * Copyright 2009-2020 Exactpro Systems Limited
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

package com.exactprosystems.clearth.automation;

public class ActionsExecutionProgress
{
	private int successful, done;

	public static final String delimiter = " / ";


	public ActionsExecutionProgress(int successful, int done)
	{
		this.successful = successful;
		this.done = done;
	}
	
	public ActionsExecutionProgress()
	{
		this(0, 0);
	}
	
	
	public void incrementSuccessful()
	{
		successful++;
	}
	
	public void decrementSuccessful()
	{
		successful--;
	}
	
	public void incrementDone()
	{
		done++;
	}
	
	public void decrementDone()
	{
		done--;
	}
	
	
	public void setSuccessful(int successful)
	{
		this.successful = successful;
	}
	
	public int getSuccessful()
	{
		return successful;
	}
	
	public void setDone(int done)
	{
		this.done = done;
	}
	
	public int getDone()
	{
		return done;
	}

	public static String getDelimiter()
	{
		return delimiter;
	}

	@Override
	public String toString()
	{
		return successful + delimiter + done;
	}
}
