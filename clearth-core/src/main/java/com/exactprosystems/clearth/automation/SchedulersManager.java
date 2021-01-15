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

package com.exactprosystems.clearth.automation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.JAXBException;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.utils.ClearThException;
import com.exactprosystems.clearth.utils.CommaBuilder;
import com.exactprosystems.clearth.utils.KeyValueUtils;
import com.exactprosystems.clearth.utils.SettingsException;
import com.exactprosystems.clearth.xmldata.XmlSchedulerLaunchInfo;
import com.exactprosystems.clearth.xmldata.XmlSchedulerLaunches;

public class SchedulersManager
{
	protected static final Logger logger = LoggerFactory.getLogger(SchedulersManager.class);
	public static final String COMMON_SCHEDULERS_KEY = "Common", PERSONAL_TAG = " (personal)";
	
	protected final Map<String, List<Scheduler>> schedulers;
	protected final SchedulerFactory schedulerFactory;
	protected final String schedulersDir;
	protected final File cfgFile;
	
	public SchedulersManager(SchedulerFactory schedulerFactory, String schedulersDir, String schedulersCfgFilePath)
	{
		this.schedulers = new HashMap<String, List<Scheduler>>();
		this.schedulerFactory = schedulerFactory;
		this.schedulersDir = schedulersDir;
		this.cfgFile = new File(schedulersCfgFilePath);
	}
	
	
	public void loadSchedulers() throws Exception
	{
		logger.debug("Loading schedulers");
		if (!cfgFile.isFile())
			FileUtils.writeStringToFile(cfgFile, "Common=Main,Extra", "UTF-8");
		schedulers.clear();
		
		for (Entry<String, String> schedulersFromFile : KeyValueUtils.loadKeyValueFile(cfgFile.getAbsolutePath(), false).entrySet())
		{
			String forUser = schedulersFromFile.getKey();
			if (forUser.isEmpty())
				continue;
			
			if (!forUser.equals(COMMON_SCHEDULERS_KEY))
				addScheduler(forUser, createScheduler(forUser, !isCommonSchedulerExists(forUser) ? forUser : forUser + PERSONAL_TAG));
			for (String currentSchedulerName : schedulersFromFile.getValue().split("\\s*,\\s*"))
			{
				if (!currentSchedulerName.isEmpty() && !isSchedulerExists(forUser, currentSchedulerName))
					addScheduler(forUser, createScheduler(forUser, currentSchedulerName));
			}
		}
		
		if (schedulers.size() == 0)
			throw new Exception("No schedulers defined in file '" + cfgFile.getAbsolutePath() + "'");
	}
	
	private void moveDirIfNeeded(String sourceDirPath, String targetDirPath) throws IOException
	{
		File source = new File(sourceDirPath), target = new File(targetDirPath);
		if (source.isDirectory() && !new File(target, source.getName()).isDirectory())
			FileUtils.moveDirectoryToDirectory(source, target, true);
	}
	
	private void packDirIfNeeded(String sourceDirPath, String packDirName, boolean checkSource) throws IOException
	{
		File source = new File(sourceDirPath);
		if ((!checkSource || source.isDirectory()) && !new File(source.getParent() + File.separator + packDirName, source.getName()).isDirectory())
		{
			File target = new File(source.getParent(), packDirName + "temp_" + System.currentTimeMillis());
			FileUtils.moveDirectoryToDirectory(source, target, true);
			target.renameTo(new File(source.getParent(), packDirName));
		}
	}
	
	private void updateLaunchesInfo(Scheduler scheduler) throws JAXBException, ClearThException
	{
		for (XmlSchedulerLaunchInfo launch : scheduler.getSchedulerData().getLaunches().getLaunchesInfo())
				launch.setReportsPath(scheduler.getForUser() + "/" + launch.getReportsPath());

		scheduler.getSchedulerData().saveLaunches();
	}
	
	
	public Scheduler addScheduler(String forUser, String schedulerName) throws SettingsException, Exception
	{
		validateScheduler(schedulerName, forUser);
		Scheduler s = createScheduler(forUser, schedulerName);
		addScheduler(forUser, s);
		updateConfig();
		return s;
	}
	
	public Scheduler addDefaultUserScheduler(String userName) throws SettingsException, Exception
	{
		return addScheduler(userName, !isCommonSchedulerExists(userName) ? userName : userName + PERSONAL_TAG);
	}
	
	protected void validateScheduler(String schedulerName, String forUser) throws SettingsException
	{
		if (StringUtils.isBlank(schedulerName) || isSchedulerExists(forUser, schedulerName))
			throw new SettingsException("Scheduler must have non-empty and unique name");
	}
	
	protected Scheduler createScheduler(String forUser, String schedulerName) throws Exception
	{
		Scheduler scheduler = schedulerFactory.createScheduler(schedulerName.trim(), schedulersDir, forUser);
		scheduler.init();
		return scheduler;
	}
	
	protected void addScheduler(String forUser, Scheduler scheduler) throws Exception
	{
		synchronized (schedulers)
		{
			List<Scheduler> target = schedulers.get(forUser);
			if (target == null)
			{
				target = new ArrayList<Scheduler>();
				schedulers.put(forUser, target);
			}
			target.add(scheduler);
		}
	}
	
	protected void updateConfig()
	{
		Map<String, String> keysValues = new LinkedHashMap<String, String>();
		
		CommaBuilder common = new CommaBuilder(",");
		for (Scheduler s : getCommonSchedulers())
			common.append(s.getName());
		keysValues.put(COMMON_SCHEDULERS_KEY, common.toString());
		
		for (Entry<String, List<Scheduler>> uss : getUsersSchedulers().entrySet())
		{
			String userName = uss.getKey();
			List<Scheduler> userSchedulers = uss.getValue();
			
			if ((userSchedulers.size() == 0) || (userSchedulers.size() == 1 && isDefaultUserScheduler(userSchedulers.get(0), userName)))
				continue;
			
			CommaBuilder cb = new CommaBuilder();
			for (Scheduler s : userSchedulers)
			{
				if (!isDefaultUserScheduler(s, userName))
					cb.append(s.getName());
			}
			keysValues.put(userName, cb.toString());
		}
		
		KeyValueUtils.saveKeyValueFile(keysValues, cfgFile.getAbsolutePath());
	}
	
	
	public Scheduler getSchedulerByName(String schedulerName, String userName)
	{
		Scheduler scheduler = getUserSchedulerByName(userName, schedulerName);
		if (scheduler == null)
			scheduler = getCommonSchedulerByName(schedulerName);
		return scheduler;
	}
	
	protected Scheduler findScheduler(String schedulerName, List<Scheduler> schedulers)
	{
		if (schedulers != null)
		{
			for (Scheduler s : schedulers)
			{
				if (s.getName().equals(schedulerName))
					return s;
			}
		}
		return null;
	}
	
	
	public boolean isSchedulerExists(String forUser, String schedulerName)
	{
		return (forUser.equals(COMMON_SCHEDULERS_KEY) && isCommonSchedulerExists(schedulerName)) ||
				isUserSchedulerExists(forUser, schedulerName);
	}
	
	public boolean isCommonSchedulerExists(String schedulerName)
	{
		return getCommonSchedulerByName(schedulerName) != null;
	}
	
	public boolean isUserSchedulerExists(String userName, String schedulerName)
	{
		return getUserSchedulerByName(userName, schedulerName) != null;
	}
	
	
	public List<Scheduler> getCommonSchedulers()
	{
		return schedulers.get(COMMON_SCHEDULERS_KEY);
	}
	
	public Scheduler getCommonSchedulerByName(String commonSchedulerName)
	{
		List<Scheduler> commonSchedulers = getCommonSchedulers();
		return findScheduler(commonSchedulerName, commonSchedulers);
	}
	
	
	public Map<String, List<Scheduler>> getUsersSchedulers()
	{
		Map<String, List<Scheduler>> usersSchedulers = new HashMap<String, List<Scheduler>>(schedulers);
		usersSchedulers.remove(COMMON_SCHEDULERS_KEY);
		return usersSchedulers;
	}
	
	public List<Scheduler> getUserSchedulers(String userName)
	{
		return schedulers.get(userName);
	}
	
	public Scheduler getUserSchedulerByName(String userName, String schedulerName)
	{
		List<Scheduler> userSchedulers = getUserSchedulers(userName);
		return findScheduler(schedulerName, userSchedulers);
	}
	
	public Scheduler getDefaultUserScheduler(String userName)
	{
		Scheduler sch = getUserSchedulerByName(userName, userName);
		if (sch == null)
			sch = getUserSchedulerByName(userName, userName + PERSONAL_TAG);
		return sch;
	}
	
	public boolean isDefaultUserScheduler(Scheduler scheduler, String userName)
	{
		String name = scheduler.getName();
		return name.equals(userName) || (name.equals(userName + PERSONAL_TAG));
	}
	
	
	public List<String> getAvailableSchedulerNames(String userName)
	{
		List<String> result = new ArrayList<String>();
		for (Scheduler s : getCommonSchedulers())
			result.add(s.getName());
		
		if (ClearThCore.getInstance().isUserSchedulersAllowed())
		{
			List<Scheduler> uss = getUserSchedulers(userName);
			if (uss != null)
			{
				for (Scheduler s : uss)
					result.add(s.getName());
			}
		}
		return result;
	}
	
	public List<Scheduler> getAvailableSchedulers(String userName)
	{
		List<Scheduler> result = new ArrayList<Scheduler>();
		result.addAll(getCommonSchedulers());
		
		if (ClearThCore.getInstance().isUserSchedulersAllowed())
		{
			List<Scheduler> uss = getUserSchedulers(userName);
			if (uss != null)
				result.addAll(uss);
		}
		return result;
	}
}
