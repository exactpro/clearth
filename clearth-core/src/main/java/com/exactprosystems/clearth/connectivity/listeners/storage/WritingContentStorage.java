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

package com.exactprosystems.clearth.connectivity.listeners.storage;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class WritingContentStorage<P, F> implements ContentStorage<P, F>
{
	private static final Logger logger = LoggerFactory.getLogger(WritingContentStorage.class);
	
	private static final String WRITING_THREAD_NAME = "WritingContentStorage";
	
	private static final long WRITE_DELAY = 500; // millis
	
	protected Thread writingThread;
	protected volatile boolean writeContent = true, writeBeforeDispose = true, writingThreadInterrupted = false;
	
	
	public WritingContentStorage()
	{
		this.writingThread = createWritingThread(getWritingThreadName());
	}
	
	
	@Override
	public void start()
	{
		getLogger().debug("Write thread starting...");
		writingThreadInterrupted = false;
		writingThread.start();
	}
	
	@Override
	public void dispose()
	{
		beforeDispose();

		logger.debug("Disposing writing content storage...");
		writingThread.interrupt();
		writingThreadInterrupted = true;
		logger.debug("Write thread has been interrupted");

		clearMemory();
	}
	
	protected void beforeDispose()
	{
		if (writeContent && writeBeforeDispose)
		{
			logger.debug("Writing content before dispose...");
			writeContent();
		}
	}
	
	
	protected Thread createWritingThread(String threadName)
	{
		return new Thread(threadName)
		{
			@Override
			public void run()
			{
				while (!Thread.interrupted())
				{
					writingIteration();
				}
			}
		};
	}
	

	protected void writingIteration()
	{
		if (writeContent)
			writeContent();
		else
			getLogger().trace("Writing stored content is turned off");
		
		try
		{
			Thread.sleep(getWriteDelay());
		}
		catch (InterruptedException e)
		{
			getLogger().error("Writing thread has been interrupted", e);
		}
	}
	
	protected abstract void writeContent();
	
	
	protected abstract String extractContentPassed(P item);
	
	protected abstract String extractContentFailed(F item);


	protected <T> Collection<T> extractAll(AbstractCollection<T> collection, boolean clearAfter)
	{
		@SuppressWarnings({ "rawtypes", "unchecked" })
		Collection<T> extracted = new ArrayList(Arrays.asList(collection.toArray()));
		if (clearAfter)
			collection.clear();
		return extracted;
	}
	
	
	public boolean getWriteContent()
	{
		return writeContent;
	}
	
	public void setWriteContent(boolean writeContent)
	{
		this.writeContent = writeContent;
	}
	
	
	public boolean getWriteBeforeDispose()
	{
		return writeBeforeDispose;
	}
	
	public void setWriteBeforeDispose(boolean writeBeforeDispose)
	{
		this.writeBeforeDispose = writeBeforeDispose;
	}
	
	
	protected String getWritingThreadName()
	{
		return WRITING_THREAD_NAME;
	}
	
	protected long getWriteDelay()
	{
		return WRITE_DELAY;
	}
	
	
	protected Logger getLogger()
	{
		return logger;
	}
}
