/******************************************************************************
 * Copyright 2009-2025 Exactpro Systems Limited
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

package com.exactprosystems.clearth.woodpecker;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.Scheduler;
import com.exactprosystems.clearth.woodpecker.configuration.DaemonsLoader;
import com.exactprosystems.clearth.woodpecker.configuration.WoodpeckerSettings;
import com.exactprosystems.clearth.woodpecker.configuration.settings.DaemonSettings;
import com.exactprosystems.clearth.woodpecker.daemons.DaemonsPool;
import com.exactprosystems.clearth.woodpecker.daemons.WoodpeckerDaemon;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerException;
import com.exactprosystems.clearth.woodpecker.message.generator.MessageGenerator;
import com.exactprosystems.clearth.woodpecker.misc.SchedulerSettingsForWoodpecker;
import com.exactprosystems.clearth.woodpecker.misc.encoder.MessageEncoder;
import com.exactprosystems.clearth.woodpecker.notifications.Notification;
import com.exactprosystems.clearth.woodpecker.notifications.WoodpeckerNotifications;
import com.exactprosystems.clearth.woodpecker.statistics.LoadingStatistics;
import com.exactprosystems.clearth.woodpecker.utils.WoodpeckerSettingsSaver;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.exactprosystems.clearth.utils.ExceptionUtils.getDetailedMessage;
import static com.exactprosystems.clearth.woodpecker.notifications.Notification.Severity.ERROR;
import static com.exactprosystems.clearth.woodpecker.notifications.Notification.Severity.INFO;
import static java.util.Collections.emptyList;

public class Woodpecker
{
	private static final Logger logger = LoggerFactory.getLogger(Woodpecker.class);

	protected static volatile Woodpecker instance;

	private Map<Integer, DaemonsPool> daemonPools;

	private final DaemonsLoader daemonsLoader;
	private final WoodpeckerObjectsFactory objectsFactory;
	
	private MessageGenerator messageGenerator;
	private MessageEncoder messageEncoder;
	
	private final WoodpeckerNotifications notifications = new WoodpeckerNotifications();

	private final SchedulerSettingsForWoodpecker schedulerSettings = new SchedulerSettingsForWoodpecker();

	private final Object setSchedulerMonitor = new Object();

	protected static volatile WoodpeckerSettings woodpeckerSettings;

	protected static WoodpeckerSettingsSaver settingsSaver;

	public static Woodpecker getInstance()
	{
		if (instance == null)
			throw new IllegalStateException("Woodpecker hasn't been initialized during application loading.");
		else
			return instance;
	}

	public static Woodpecker woodpecker()
	{
		return getInstance();
	}

	public Map<Integer, DaemonsPool> getDaemonPools()
	{
		return daemonPools;
	}
	
	
	/* Loading of new instances added on backend in runtime */
	
	public void update()
	{
		try
		{
			updateDaemons(daemonsLoader.loadDaemons());
		} 
		catch (Exception e)
		{
			processError(e, "Could not load daemons settings");
		}

		messageEncoder = objectsFactory.createMessageEncoder();
		messageGenerator = objectsFactory.createMessageGenerator(messageEncoder);
	}

	private void updateDaemons(List<DaemonsPool> woodpeckerDaemonsPools)
	{
		for(DaemonsPool daemonsPool : woodpeckerDaemonsPools)
		{
			String daemonsPoolType = daemonsPool.getType();
			
			DaemonsPool existDaemonPoll = findDaemonPool(daemonsPoolType);
			if(existDaemonPoll == null)
			{
				daemonPools.put(daemonPools.size(),daemonsPool);
				continue;
			}
			updateDaemons(existDaemonPoll, daemonsPool);
		}
	}

	public DaemonsPool findDaemonPool(String daemonsPoolType)
	{
		for (DaemonsPool daemonsPool : daemonPools.values())
			if (StringUtils.equals(daemonsPool.getType(),daemonsPoolType))
				return daemonsPool;
		return null;
	}

	private void updateDaemons(DaemonsPool oldWoodpeckerDaemonsPool, DaemonsPool newWoodpeckerDaemonsPool)
	{
		for (Map.Entry<String, WoodpeckerDaemon> daemon : newWoodpeckerDaemonsPool.getDaemons().entrySet())
		{
			if (oldWoodpeckerDaemonsPool.getDaemons().get(daemon.getKey()) != null)
				continue;
			oldWoodpeckerDaemonsPool.addDaemon(daemon.getKey(), daemon.getValue());
		}
	}
	
	
	/* Checking of daemons state */
	
	public boolean isDaemonRunning(int index, String instance)
	{
		return (daemonPools.get(index) != null)
				&& (daemonPools.get(index).getDaemon(instance) != null)
				&& daemonPools.get(index).getDaemon(instance).isRunning();
	}
	
	
	
	/* Starting daemons */
	
	public void startDaemon(int index, String instance)
	{
		WoodpeckerDaemon daemon = findDaemon(index, instance);
		if (daemon != null)
			startDaemon(daemon);
	}
	
	public void startDaemon(WoodpeckerDaemon daemon)
	{
		if (daemon.isRunning())
			return;
		try
		{
			daemon.start();			
			notifications.addNotification(new Notification(INFO, 
					daemon.getSettings().getFullName(),
					"Daemon started successfully"));
		}
		catch (WoodpeckerException e)
		{
			processError(e, "Cannot start daemon: " + daemon.getSettings().getFullName());
		}
	}
	
	
	private WoodpeckerDaemon findDaemon(int index, String instance)
	{
		DaemonsPool daemonPool = daemonPools.get(index);
		return daemonPool.getDaemon(instance);
	}
	
	
	/* Stopping daemons */

	public void stopAllDaemons()
	{
		for (DaemonsPool current : daemonPools.values()) 
		{
			for (WoodpeckerDaemon daemon : current.getDaemons().values()) 
			{
				stopDaemon(daemon);
			}
		}
	}

	public void stopDaemon(int index, String instance)
	{
		DaemonsPool daemonPool = daemonPools.get(index);
		WoodpeckerDaemon daemon = daemonPool.getDaemons().get(instance);
		stopDaemon(daemon);
	}
	
	public void stopDaemon(WoodpeckerDaemon daemon)
	{
		try
		{
			if (!daemon.isRunning())
				return;

			daemon.stop();
			String daemonName = daemon.getSettings().getFullName();
			Notification notification = new Notification(daemonName, "Daemon stopped");
			notifications.addNotification(notification);
		}
		catch (WoodpeckerException e)
		{
			processError(e, "Error while stop daemon: " + daemon.getSettings().getFullName());
		}
	}
	
	
	public Collection<String> getDaemonInstancesNames(int poolIndex)
	{
		DaemonsPool pool = daemonPools.get(poolIndex);
		return (pool != null) ? pool.getDaemons().keySet() : emptyList();
	}
	
	public WoodpeckerDaemon getDaemon(int poolIndex, String instanceName)
	{
		DaemonsPool pool = daemonPools.get(poolIndex);
		if (pool != null)
		{
			return pool.getDaemons().get(instanceName);
		}
		return null;
	}
	
	public LoadingStatistics getLoadingStatistics(int poolIndex, String instanceName)
	{
		DaemonsPool pool = daemonPools.get(poolIndex);
		if (pool != null)
		{
			WoodpeckerDaemon daemon = pool.getDaemons().get(instanceName);
			if (daemon != null)
				return daemon.getLoadingStatistics();
		}
		return null;
	}
	
	public DaemonSettings getDaemonSettings(int poolIndex, String instanceName)
	{
		WoodpeckerDaemon daemon = getDaemon(poolIndex, instanceName);
		return (daemon != null) ? daemon.getSettings() : null;
	}
	
	public void saveDaemonSettings(int poolIndex, String instanceName)
	{
		DaemonSettings settings = getDaemonSettings(poolIndex, instanceName);
		if (settings != null)
		{
			try 
			{
				settings.save();
			}
			catch (Exception e)
			{
				processError(e, "Error while saving settings.");
			}
		}
	}
	
	private void processError(Exception e, String errorMessage)
	{
		logger.error(errorMessage, e);
		notifications.addNotification(new Notification(ERROR, errorMessage, getDetailedMessage(e)));
	}

	public void setScheduler(String schedulerName, String userName) throws WoodpeckerException
	{
		synchronized (setSchedulerMonitor)
		{
			Scheduler scheduler = findScheduler(schedulerName, userName);

			String selectedSchedulerName = scheduler != null ? scheduler.getName() : null;

			logger.info("Selected scheduler: {}", selectedSchedulerName);
			schedulerSettings.setSelectedScheduler(scheduler);

			woodpeckerSettings.setSchedulerName(selectedSchedulerName);
			saveWoodpeckerSettings();
		}
	}

	private Scheduler findScheduler(String schedulerName, String userName)
	{
		Scheduler selectedScheduler = null;
		if (StringUtils.isNotEmpty(schedulerName))
			selectedScheduler = ClearThCore.getInstance().getSchedulersManager().getSchedulerByName(schedulerName, userName);

		return selectedScheduler;
	}


	public void saveWoodpeckerSettings() throws WoodpeckerException
	{
		settingsSaver.saveWoodpeckerSettings(woodpeckerSettings);
	}

	public SchedulerSettingsForWoodpecker getSchedulerSettings()
	{
		return schedulerSettings;
	}

	public WoodpeckerSettings getWoodpeckerSettings()
	{
		return woodpeckerSettings;
	}

	public String getSchedulerName()
	{
		if (woodpeckerSettings != null)
			return woodpeckerSettings.getSchedulerName();
		return null;
	}
	
	public MessageGenerator getMessageGenerator()
	{
		return messageGenerator;
	}

	public MessageEncoder getMessageEncoder()
	{
		return messageEncoder;
	}

	public WoodpeckerObjectsFactory getObjectsFactory()
	{
		return objectsFactory;
	}

	public WoodpeckerNotifications getNotifications()
	{
		return notifications;
	}

	public static void init(WoodpeckerObjectsFactory objectsFactory) throws WoodpeckerException, ReflectiveOperationException
	{
		if (instance == null)
		{
			DaemonsLoader daemonsLoader = objectsFactory.createDaemonsLoader();

			instance = new Woodpecker(daemonsLoader, objectsFactory);
			
			List<DaemonsPool> pools = daemonsLoader.loadDaemons();
			Map<Integer, DaemonsPool> map = new LinkedHashMap<>();
			for (int i = 0; i < pools.size(); i++)
			{
				map.put(i, pools.get(i));
			}
			instance.daemonPools = map;
		}
	}

	private Woodpecker(DaemonsLoader daemonsLoader,
	                   WoodpeckerObjectsFactory objectsFactory) throws ReflectiveOperationException
	{
		this.daemonsLoader = daemonsLoader;
		this.objectsFactory = objectsFactory;
		messageEncoder = objectsFactory.createMessageEncoder();
		messageGenerator = objectsFactory.createMessageGenerator(messageEncoder);
		settingsSaver = objectsFactory.createWoodpeckerSaver();
		woodpeckerSettings = objectsFactory.getWoodpeckerSettings(settingsSaver);
		Scheduler scheduler = findScheduler(getSchedulerName(), null);
		schedulerSettings.setSelectedScheduler(scheduler);
		logger.info("Woodpecker was created with scheduler: {}", scheduler != null ? scheduler.getName() : null);
	}
}
