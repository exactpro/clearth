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

package com.exactprosystems.clearth.web.beans.automation;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.Scheduler;
import com.exactprosystems.clearth.automation.SchedulersManager;
import com.exactprosystems.clearth.automation.StartAtType;
import com.exactprosystems.clearth.utils.SettingsException;
import com.exactprosystems.clearth.web.beans.ClearThBean;
import com.exactprosystems.clearth.web.misc.MessageUtils;
import com.exactprosystems.clearth.web.misc.SchedulerEntry;
import com.exactprosystems.clearth.web.misc.UserInfoUtils;
import com.exactprosystems.clearth.web.misc.WebUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;

import static com.exactprosystems.clearth.automation.SchedulersManager.COMMON_SCHEDULERS_KEY;

@SuppressWarnings({"WeakerAccess", "unused"})
public class AutomationBean extends ClearThBean
{		
	protected final SchedulersManager schedulersManager;
	protected Scheduler selectedScheduler = null;
	protected SchedulerEntry newSchedulerEntry = null;
	
	protected int activeTab = 0;
	protected List<String> schedulersMenu;
	
	protected final AutomationMatrixChecker matrixChecker;

	public AutomationBean()
	{
		this.schedulersManager = ClearThCore.getInstance().getSchedulersManager();
		this.schedulersMenu = createSchedulersMenu();
		this.matrixChecker = createMatrixChecker();
		
		selectScheduler(getDefaultScheduler());
	}
	
	protected List<String> createSchedulersMenu()
	{
		return schedulersManager.getAvailableSchedulerNames(UserInfoUtils.getUserName());
	}

	protected AutomationMatrixChecker createMatrixChecker()
	{
		return new AutomationMatrixChecker();
	}

	public List<String> getSchedulersMenu()
	{
		return schedulersMenu;
	}

	protected Scheduler getDefaultScheduler()
	{
		if (ClearThCore.getInstance().isUserSchedulersAllowed())
			return schedulersManager.getDefaultUserScheduler(UserInfoUtils.getUserName());
		else
			return schedulersManager.getCommonSchedulers().get(0);
	}
	
	public SchedulerEntry getNewSchedulerEntry()
	{
		return this.newSchedulerEntry;
	}
	
	public boolean isAbleToCreateNewScheduler()
	{
		return UserInfoUtils.isAdmin() || (UserInfoUtils.isPowerUser() && isUserSchedulersAllowed());
	}
	
	public boolean isAbleToRemoveScheduler()
	{
		return UserInfoUtils.isAdmin() || UserInfoUtils.isPowerUser() && isUserSchedulersAllowed()
				&& selectedScheduler.getForUser().equals(UserInfoUtils.getUserName());
	}
	
	public boolean isUserSchedulersAllowed()
	{
		return ClearThCore.getInstance().isUserSchedulersAllowed();
	}
	
	public boolean isNotRemovableScheduler()
	{
		return selectedScheduler == schedulersManager.getCommonSchedulers().get(0)
				|| selectedScheduler == getDefaultScheduler();
	}
	
	public void createNewScheduler()
	{
		newSchedulerEntry = new SchedulerEntry(UserInfoUtils.isAdmin(), UserInfoUtils.isPowerUser() ? UserInfoUtils.getUserName() : "", "");
	}
	
	public void removeScheduler()
	{
		String msg = "Cannot remove scheduler";
		if (!isAbleToRemoveScheduler())
		{
			MessageUtils.addErrorMessage(msg, "You are not allowed to remove schedulers");
			return;
		}
		if (isNotRemovableScheduler())
		{
			MessageUtils.addErrorMessage(msg, "Scheduler is not removable");
			return;
		}
		try
		{
			String name = selectedScheduler.getName();
			schedulersManager.removeScheduler(selectedScheduler);
			getLogger().info("removed scheduler '{}'", name);
			
			selectedScheduler = getDefaultScheduler();
			schedulersMenu = createSchedulersMenu();
			MessageUtils.addInfoMessage(String.format("Scheduler '%s' has been removed", name), " Scheduler files are kept on backend");
		}
		catch (Exception e)
		{
			MessageUtils.addErrorMessage(msg, e.getMessage());
		}
	}
	
	public void saveNewScheduler()
	{
		// Check permissions for creating new scheduler
		if (!isAbleToCreateNewScheduler() || (UserInfoUtils.isPowerUser() && newSchedulerEntry.isCommon()))
		{
			MessageUtils.addErrorMessage("Permission denied", "You are not allowed to create new scheduler");
			return;
		}
		
		try
		{
			String forUser = newSchedulerEntry.getForUser();
			Scheduler scheduler = schedulersManager.addScheduler(newSchedulerEntry.isCommon() ? COMMON_SCHEDULERS_KEY : forUser,
					newSchedulerEntry.getName());
			getLogger().info("created scheduler '{}'", scheduler.getName());
			
			if (forUser.equals(UserInfoUtils.getUserName()) || newSchedulerEntry.isCommon())
				selectedScheduler = scheduler;
			schedulersMenu = createSchedulersMenu();
			WebUtils.addCanCloseCallback(true);
			MessageUtils.addInfoMessage("Success", "Scheduler '"+scheduler.getName()+"' has been created");
		}
		catch (SettingsException e)
		{
			MessageUtils.addErrorMessage("Error", e.getMessage());
		}
		catch (Exception e)
		{
			String errorMsg = "Could not init new scheduler";
			getLogger().error(errorMsg, e);
			MessageUtils.addErrorMessage("Error", errorMsg);
		}
	}
	
	public void onTimer()
	{
	}
	
	public int getActiveTab()
	{
		return activeTab;
	}
	
	public void setActiveTab(int activeTab)
	{
		this.activeTab = activeTab;
	}
	
	public void selectScheduler(Scheduler scheduler)
	{
		this.selectedScheduler = scheduler;
		this.matrixChecker.setSelectedScheduler(scheduler);
		this.matrixChecker.resetCheckedMatricesErrors();
	}
	
	public void setScheduler(String scheduler)
	{
		selectScheduler(schedulersManager.getSchedulerByName(scheduler, UserInfoUtils.getUserName()));
	}
	
	public String getScheduler()
	{
		return selectedScheduler.getName();
	}

	public Scheduler getSelectedScheduler()
	{
		return selectedScheduler;
	}
	

	public List<String> getStartAtTypes()
	{
		return Arrays.asList(StartAtType.END_STEP.getStringType(), StartAtType.START_STEP.getStringType(),
				StartAtType.START_SCHEDULER.getStringType(), StartAtType.START_EXECUTION.getStringType());
	}

	public String safeFileName(String value)
	{
		try
		{
			return URLEncoder.encode(value, "UTF-8").replace("+", "%20");
		}
		catch (UnsupportedEncodingException e)
		{
			return null;
		}
	}

	public AutomationMatrixChecker getMatrixChecker() {
		return matrixChecker;
	}
}
