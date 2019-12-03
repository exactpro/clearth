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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.utils.Utils;

public abstract class FileContentStorage<P, F> extends WritingContentStorage<P, F>
{
	private static final Logger logger = LoggerFactory.getLogger(FileContentStorage.class);
	private static final String STORE_THREAD_NAME = "FileContentStorage";
	protected static final int BUFFER_SIZE = 0x100000; // 1 mb
	
	protected MemoryContentStorage<P, F> memoryStorage;
	
	protected ConcurrentLinkedQueue<P> insertQueue, fileContents;
	
	protected final RandomAccessFile contentsFile;
	protected final FileChannel channel;
	protected final ByteBuffer buffer;
	
	protected boolean rewriteFile = false, clearFile = false, storeTimestamp = false;


	public FileContentStorage (String contentsFilePath) throws IOException
	{
		super();
		
		memoryStorage = new MemoryContentStorage<P, F>();
		
		File contentsFile = new File(ClearThCore.rootRelative(contentsFilePath));
		if (contentsFile.exists())
		{
			if (contentsFile.isDirectory())
				throw new IllegalArgumentException("Unable to use directory as contents file");
			if (contentsFile.length() != 0)
				clearFile = true;
		}
		
		this.contentsFile = new RandomAccessFile(contentsFilePath, "rw");
		this.channel = this.contentsFile.getChannel();
		this.channel.force(false);
		this.buffer = ByteBuffer.allocate(getBufferSize());
		
		this.insertQueue = new ConcurrentLinkedQueue<P>();
		this.fileContents = new ConcurrentLinkedQueue<P>();
	}
	
	public FileContentStorage(String contentsFilePath, boolean storeTimestamp) throws IOException
	{
		this(contentsFilePath);
		this.storeTimestamp = storeTimestamp;
	}
	
	@Override
	public void start()
	{
		memoryStorage.start();
		
		if (!channel.isOpen())
		{
			getLogger().error("File channel is closed, unable to write in file");
			return;
		}
		super.start();
	}
	
	@Override
	protected void beforeDispose()
	{
		super.beforeDispose();
		Utils.closeResource(channel);
		Utils.closeResource(contentsFile);
	}
	
		
	@Override
	public void insertPassed(long id, P item)
	{	
		memoryStorage.insertPassed(id, item);
		
		if (item == null)
			getLogger().trace("Unable to write 'null' item");
		else
			insertQueue.add(item);
	}
	
	@Override
	public void insertFailed(long id, F item)
	{	
		memoryStorage.insertFailed(id, item);
	}
	
	@Override
	public void removePassed(P item)
	{
		memoryStorage.removePassed(item);
		
		if (item == null)
		{
			getLogger().trace("Unable to remove 'null' item");
		}
		else if (!insertQueue.remove(item))
		{
			if (fileContents.remove(item))
				rewriteFile = true;
		}
	}
	
	@Override
	public void removePassed(long itemId)
	{
		memoryStorage.removePassed(itemId);
	}
	
	@Override
	public void removeFailed(F item)
	{
		memoryStorage.removeFailed(item);
	}
	
	@Override
	public void removeFailed(long itemId)
	{
		memoryStorage.removeFailed(itemId);
	}


	@Override
	public void clearMemory()
	{
		memoryStorage.clearMemory();
	}

	@Override
	public void clearPassed()
	{
		memoryStorage.clearPassed();
		clearFile = true;
		if (writingThreadInterrupted)
			writingIteration();
	}
	
	@Override
	public void clearFailed()
	{
		memoryStorage.clearFailed();
	}
	
	
	@Override
	public Map<Long, P> getContentPassed()
	{
		return memoryStorage.getContentPassed();
	}
	
	@Override
	public Map<Long, F> getContentFailed()
	{
		return memoryStorage.getContentFailed();
	}
	
	@Override
	public Map<Long, P> getContentPassedAfterId(long id)
	{
		return memoryStorage.getContentPassedAfterId(id);
	}

	@Override
	public Map<Long, F> getContentFailedAfterId(long id)
	{
		return memoryStorage.getContentFailedAfterId(id);
	}
	
	
	@Override
	protected void writeContent()
	{
		if (clearFile)
		{
			clearFile();
			clearFile = false;
		}
		
		if (rewriteFile)
		{
			fileContents.addAll(extractAll(insertQueue, true));
			rewriteFile(extractAll(fileContents, false));
			rewriteFile = false;
		}
		else if (!insertQueue.isEmpty())
		{
			appendToFile(extractAll(insertQueue, true));
		}
		else
		{
			getLogger().trace("Nothing to store");
		}
	}
	
	protected void appendToFile(Collection<P> appendContent)
	{
		if (getLogger().isTraceEnabled())
			getLogger().trace("Appending messages to file. New message count: "+appendContent.size());
		
		fileContents.addAll(appendContent);
		writeInFile(appendContent, true);
	}
	
	protected void rewriteFile(Collection<P> content)
	{
		if (getLogger().isTraceEnabled())
			getLogger().trace("File will be rewriten. Total message count: "+content.size());
		
		writeInFile(content, false);
	}
	
	protected void clearFile()
	{
		fileContents.clear();
		writeInFile(null, false);
	}
	
	protected void writeInFile(Collection<P> content, boolean append)
	{
		if (!append)
		{
			getLogger().trace("Clearing file...");
			try
			{
				channel.truncate(0);
			}
			catch (IOException e)
			{
				getLogger().error("Unable to clear file", e);
			}
		}
		
		if (content == null || content.isEmpty())
			return;
		
		getLogger().trace("Writing in file...");
		try
		{
			for (P item : content)
			{
				buffer.clear();
				if (storeTimestamp)
                {
                    buffer.put(extractTimestampPassed(item).getBytes());
                    buffer.put(Utils.EOL.getBytes());
                }
				buffer.put(extractContentPassed(item).getBytes());
				buffer.put("\r\n\r\n\r\n".getBytes());
				buffer.flip();
				channel.write(buffer);
			}
		}
		catch (IOException e)
		{
			getLogger().error("Unable to write data in file", e);
		}
	}
	
	protected abstract String extractTimestampPassed(P item);

	
	protected int getBufferSize()
	{
		return BUFFER_SIZE;
	}
	
	@Override
	protected String getWritingThreadName()
	{
		return STORE_THREAD_NAME;
	}
	
	@Override
	protected Logger getLogger()
	{
		return logger;
	}
}
