/******************************************************************************
 * Copyright 2009-2024 Exactpro Systems Limited
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

package com.exactprosystems.clearth.data.th2;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.th2.common.grpc.EventBatch;
import com.exactpro.th2.common.grpc.RawMessageBatch;
import com.exactpro.th2.common.schema.factory.CommonFactory;
import com.exactpro.th2.common.schema.grpc.router.GrpcRouter;
import com.exactpro.th2.common.schema.message.MessageRouter;
import com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.GroupBatch;
import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.data.DataHandlersFactory;
import com.exactprosystems.clearth.data.DataHandlingException;
import com.exactprosystems.clearth.data.MessageHandler;
import com.exactprosystems.clearth.data.TestExecutionHandler;
import com.exactprosystems.clearth.data.th2.config.StorageConfig;
import com.exactprosystems.clearth.data.th2.events.EventFactory;
import com.exactprosystems.clearth.data.th2.events.ResultSaver;
import com.exactprosystems.clearth.data.th2.events.ResultSavingConfig;
import com.exactprosystems.clearth.utils.ClearThException;

public class Th2DataHandlersFactory implements DataHandlersFactory
{
	private static final Logger logger = LoggerFactory.getLogger(Th2DataHandlersFactory.class);
	
	public static final String FILE_RABBIT_CONFIG = "rabbitMQ.json",
			FILE_STORAGE_CONFIG = "storage.json";
	
	private final StorageConfig storageConfig;
	private final CommonFactory factory;
	private final EventFactory eventFactory;
	
	public Th2DataHandlersFactory() throws ClearThException
	{
		this(Paths.get(ClearThCore.rootRelative("cfg/th2/")));
	}
	
	public Th2DataHandlersFactory(Path configDir) throws ClearThException
	{
		Path rabbitConfigFile = getRabbitConfigFile(configDir);
		if (!Files.isRegularFile(rabbitConfigFile))
			throw new ClearThException("File with configuration to connect to th2 doesn't exist: "+rabbitConfigFile);
		
		Path storageConfigFile = getStorageConfigFile(configDir);
		if (!Files.isRegularFile(storageConfigFile))
			throw new ClearThException("File with storage configuration doesn't exist: "+storageConfigFile);
		
		storageConfig = createStorageConfig(storageConfigFile);
		logger.info("Storage configuration: {}", storageConfig);
		factory = createCommonFactory(configDir);
		eventFactory = createEventFactory(storageConfig);
	}
	
	
	@Override
	public void close() throws Exception
	{
		factory.close();
	}
	
	@Override
	public MessageHandler createMessageHandler(String connectionName) throws DataHandlingException
	{
		try
		{
			logger.debug("Creating message handler for '{}'", connectionName);
			return new Th2MessageHandler(connectionName, createGroupBatchRouter(), storageConfig);
		}
		catch (Exception e)
		{
			throw new DataHandlingException("Error while creating message handler. Check th2 configuration files and whether th2 components are available", e);
		}
	}
	
	@Override
	public TestExecutionHandler createTestExecutionHandler(String schedulerName) throws DataHandlingException
	{
		try
		{
			logger.debug("Creating test execution handler for '{}'", schedulerName);
			MessageRouter<EventBatch> eventRouter = createEventBatchRouter();
			ResultSaver resultSaver = createResultSaver(eventRouter, storageConfig);
			return new Th2TestExecutionHandler(schedulerName, eventRouter, eventFactory, resultSaver);
		}
		catch (DataHandlingException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new DataHandlingException("Error while creating test execution handler. Check th2 configuration files and whether th2 components are available", e);
		}
	}
	
	public MessageRouter<EventBatch> createEventBatchRouter()
	{
		return factory.getEventBatchRouter();
	}
	
	public MessageRouter<GroupBatch> createGroupBatchRouter()
	{
		return factory.getTransportGroupBatchRouter();
	}
	
	public GrpcRouter createGrpcRouter()
	{
		return factory.getGrpcRouter();
	}
	
	
	public String getBook()
	{
		return storageConfig.getBook();
	}
	
	
	protected Path getRabbitConfigFile(Path configDir)
	{
		return configDir.resolve(FILE_RABBIT_CONFIG);
	}
	
	protected Path getStorageConfigFile(Path configDir)
	{
		return configDir.resolve(FILE_STORAGE_CONFIG);
	}
	
	
	protected StorageConfig createStorageConfig(Path configFile) throws ClearThException
	{
		try
		{
			return StorageConfig.load(configFile);
		}
		catch (Exception e)
		{
			throw new ClearThException("Could not load storage configuration", e);
		}
	}
	
	protected CommonFactory createCommonFactory(Path configDir) throws ClearThException
	{
		try
		{
			return CommonFactory.createFromArguments("-c", configDir.toString());
		}
		catch (Exception e)
		{
			throw new ClearThException("Could not create th2 common factory", e);
		}
	}
	
	protected ResultSaver createResultSaver(MessageRouter<EventBatch> eventRouter, StorageConfig storageConfig) throws DataHandlingException
	{
		ResultSavingConfig config = new ResultSavingConfig();
		config.setMaxBatchSize(storageConfig.getEvents().getMaxBatchSize());
		return new ResultSaver(eventRouter, config);
	}
	
	protected EventFactory createEventFactory(StorageConfig config) throws ClearThException
	{
		return new EventFactory(config);
	}
}
