/******************************************************************************
 * Copyright 2009-2022 Exactpro Systems Limited
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

package com.exactprosystems.clearth.woodpecker.daemons;

import com.exactprosystems.clearth.woodpecker.WoodpeckerObjectsFactory;
import com.exactprosystems.clearth.woodpecker.configuration.daemonsDictionary.DaemonDesc;
import com.exactprosystems.clearth.woodpecker.configuration.settings.DaemonSettings;
import com.exactprosystems.clearth.woodpecker.configuration.start.StartScriptReader;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerDaemonException;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerException;
import com.exactprosystems.clearth.woodpecker.execution.DaemonContext;
import com.exactprosystems.clearth.woodpecker.execution.distribution.TaskDescriptionsTable;
import com.exactprosystems.clearth.woodpecker.execution.distribution.TaskDescriptionsTableFactory;
import com.exactprosystems.clearth.woodpecker.execution.threads.managers.BaseManagerThread;
import com.exactprosystems.clearth.woodpecker.execution.threads.managers.ManagerThreadFactory;
import com.exactprosystems.clearth.woodpecker.execution.threads.workers.WorkerThread;
import com.exactprosystems.clearth.woodpecker.execution.threads.workers.WorkerThreadsFactory;
import com.exactprosystems.clearth.woodpecker.execution.workQueue.WorkQueue;
import com.exactprosystems.clearth.woodpecker.initialdata.InitialData;
import com.exactprosystems.clearth.woodpecker.notifications.WoodpeckerNotifications;
import com.exactprosystems.clearth.woodpecker.reports.DaemonExecutionResultExporter;
import com.exactprosystems.clearth.woodpecker.statistics.LoadingStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.exactprosystems.clearth.woodpecker.Woodpecker.woodpecker;
import static com.exactprosystems.clearth.woodpecker.daemons.DaemonStatus.*;

public final class CommonDaemon implements WoodpeckerDaemon
{
	private static final Logger log = LoggerFactory.getLogger(CommonDaemon.class);

	private final Path replicaBaseDirPath;
	
	private final DaemonDesc description;
	private DaemonSettings settings;
	
	private final WoodpeckerNotifications notifications;
	
	private final WoodpeckerDaemonFactory daemonFactory;
	private final WoodpeckerObjectsFactory objectsFactory;
	
	private DaemonContext context;
	private InitialData initialData;

	private LoadingStatistics loadingStatistics;
	
	private TaskDescriptionsTable<?> taskDescsTable;
	
	private BaseManagerThread managerThread;
	private List<WorkerThread> workerThreads;

	private final AtomicInteger runningWorkersCount = new AtomicInteger();
	
	/*
	* Possible changes:
	* 
	* INACTIVE          -> INITIALIZING
	* 
	* INITIALIZING      -> INACTIVE         (initialization failed)
	*                   -> RUNNING          (initialization completed)
	*                   
	* RUNNING           -> INACTIVE         (daemon stopped internally: schedule completed or nothing to execute because of errors)
	*                   -> STOPPING         (stopping requested externally., f.e. by user)
	* 
	* STOPPING          -> INACTIVE
	* */
	private final AtomicReference<DaemonStatus> statusRef = new AtomicReference<>(INACTIVE);
	
	private LocalDateTime startTime;
	private LocalDateTime endTime;
	private volatile String runId;


	public CommonDaemon(WoodpeckerDaemonFactory daemonFactory, Path replicaBaseDirPath,
	                    DaemonSettings settings, DaemonDesc description)
	{
		this.daemonFactory = daemonFactory;
		this.replicaBaseDirPath = replicaBaseDirPath;
		this.description = description;
		this.settings = settings;
		this.notifications = woodpecker().getNotifications();
		this.objectsFactory = woodpecker().getObjectsFactory();
	}


	@Override
	public WoodpeckerDaemonFactory getFactory()
	{
		return daemonFactory;
	}

	
	@Override
	public DaemonStatus getStatus()
	{
		return statusRef.get();
	}
	
	@Override
	public LocalDateTime getStartTime()
	{
		return startTime;
	}	
	
	@Override
	public LocalDateTime getEndTime()
	{
		return endTime;
	}
	
	@Override
	public String getRunId()
	{
		return runId;
	}
	

	@Override
	public String start() throws WoodpeckerException
	{
		if (!statusRef.compareAndSet(INACTIVE, INITIALIZING))
			return runId;

		try
		{
			init();
		}
		catch (Exception e)
		{
			statusRef.set(INACTIVE);
			throw new WoodpeckerException(e);
		}

		startTime = LocalDateTime.now();
		endTime = null;
		runId = UUID.randomUUID().toString();
		statusRef.set(RUNNING);

		loadingStatistics.start();

		managerThread.start();
		runningWorkersCount.set(workerThreads.size());
		workerThreads.forEach(Thread::start);
		
		return runId;
	}
	
	private void init() throws Exception
	{
		try
		{
			context = daemonFactory.createDaemonContext(settings);
			
			StartScriptReader<?> startScriptReader = daemonFactory.createStartScriptReader(replicaBaseDirPath);
			startScriptReader.readScript();
			
			initialData = startScriptReader.loadInitialData(settings.getFullName(), context);
			
			loadingStatistics = daemonFactory.createLoadingStatistics(this, 
					startScriptReader.getExecutableOperationNames());
			
			TaskDescriptionsTableFactory tdtFactory = daemonFactory.createTaskDescriptionsTableFactory();
			taskDescsTable = tdtFactory.create(startScriptReader, settings, notifications, loadingStatistics, context);
			
			WorkerThreadsFactory workerThreadsFactory = objectsFactory.createWorkerThreadsFactory();
			workerThreads = new ArrayList<>();
			WorkQueue workQueue = new WorkQueue();
			workerThreadsFactory.createWorkerThreads(taskDescsTable, initialData, this::onWorkerStopped,
					workerThreads, workQueue);
			
			ManagerThreadFactory managerThreadFactory = objectsFactory.createManagerThreadsFactory();
			managerThread = managerThreadFactory.create(daemonFactory, settings, notifications, workQueue,
					taskDescsTable, this::onManagerStopped);
		}
		catch (Exception e)
		{
			if (initialData != null)
			{
				initialData.dispose();
				initialData = null;
			}
			throw e;
		}
	}

	
	@Override
	public void stop()
	{
		if (!statusRef.compareAndSet(RUNNING, STOPPING))
			return;
			
		managerThread.terminate();
	}	
	
	private void onManagerStopped()
	{
		workerThreads.forEach(WorkerThread::terminate);
	}
	
	private void onWorkerStopped()
	{
		if (runningWorkersCount.decrementAndGet() == 0)
		{
			workerThreads = null;
			
			taskDescsTable.dispose();
			taskDescsTable = null;
			
			managerThread = null;

			loadingStatistics.stop(100);

			context.closeResources(notifications);
			context = null;
			
			initialData.dispose();
			initialData = null;
			
			endTime = LocalDateTime.now();
			
			saveResult();
			
			runId = null;
			statusRef.set(INACTIVE);
		}
	}
	
	private void saveResult()
	{
		try
		{
			DaemonExecutionResultExporter exporter = daemonFactory.createDaemonExecutionResultExporter();
			exporter.export(this);
		}
		catch (WoodpeckerDaemonException e)
		{
			notifications.addWarning(settings.getFullName(), log, e);
		}
	}


	
	@Override
	public DaemonDesc getDescription()
	{
		return description;
	}

	@Override
	public DaemonSettings getSettings()
	{
		return settings;
	}

	public void setSettings(DaemonSettings daemonSettings)
	{
		this.settings = daemonSettings;
	}

	@Override
	public LoadingStatistics getLoadingStatistics()
	{
		return loadingStatistics;
	}
}
