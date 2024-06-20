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

package com.exactprosystems.clearth.web.beans.automation;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.Scheduler;
import com.exactprosystems.clearth.automation.SchedulerData;
import com.exactprosystems.clearth.automation.persistence.StateConfig;
import com.exactprosystems.clearth.automation.report.ModifiableReportsConfig;
import com.exactprosystems.clearth.automation.report.ReportsConfig;
import com.exactprosystems.clearth.connectivity.connections.ClearThConnection;
import com.exactprosystems.clearth.utils.CommaBuilder;
import com.exactprosystems.clearth.web.beans.ClearThBean;
import com.exactprosystems.clearth.web.misc.MessageUtils;
import com.exactprosystems.clearth.web.misc.WebUtils;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.file.UploadedFile;

import javax.faces.event.AjaxBehaviorEvent;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings({"WeakerAccess", "unused"})
public class ConfigurationAutomationBean extends ClearThBean {
	
	protected UploadedFile schedulerConfigurationUploadedFile;

	protected final Date emptyBaseTime;

	protected Date holidayDate;
	protected ModifiableReportsConfig reportsConfigToEdit;
	
	protected final AutomationStepsManagement stepsManagement;
	
	protected AutomationBean automationBean;
	
	public ConfigurationAutomationBean() {
		this.holidayDate = new Date();
		this.stepsManagement = createStepManagement();
		this.emptyBaseTime = createEmptyBaseTime();
	}

	protected AutomationStepsManagement createStepManagement() {
		return new AutomationStepsManagement(this);
	}

	public void setAutomationBean(AutomationBean automationBean) {
		this.automationBean = automationBean;
	}

	protected Date createEmptyBaseTime()
	{
		Calendar c = Calendar.getInstance();
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		return c.getTime();
	}

	public Scheduler selectedScheduler() {
		return this.automationBean.selectedScheduler;
	}
	
	

	public StreamedContent downloadSteps()
	{
		try
		{
			return WebUtils.downloadFile(new File(selectedScheduler().getSchedulerData().getConfigName()));
		}
		catch (IOException e)
		{
			WebUtils.logAndGrowlException("Could not download steps", e, getLogger());
			return null;
		}
	}
	

	public StreamedContent downloadSchedulerSettings()
	{
		SchedulerData schedulerData = selectedScheduler().getSchedulerData();
		try
		{
			File resultFile = ClearThCore.getInstance().getSchedulerSettingsTransmitter().exportSettings(schedulerData);
			String warnMess = "Configuration is empty";

			if (resultFile == null)
				getLogger().debug(warnMess);

			return WebUtils.downloadFile(resultFile, resultFile != null ? resultFile.getName() : null, warnMess);
		}
		catch (IOException e)
		{
			WebUtils.logAndGrowlException("Error while exporting scheduler settings", e, getLogger());
			return null;
		}
	}
	
	public void uploadSchedulerSettings(FileUploadEvent event)
	{
		UploadedFile file = event.getFile();
		if ((file==null) || (file.getContent().length==0))
			return;

		try
		{
			File storageDir = new File(ClearThCore.uploadStoragePath());
			File storedSettings = WebUtils.storeUploadedFile(file, storageDir, "settings_", ".zip");

			ClearThCore.getInstance().getSchedulerSettingsTransmitter().deploySettings(storedSettings, selectedScheduler());
			MessageUtils.addInfoMessage("Success", "Scheduler settings successfully uploaded");
			getLogger().info("uploaded settings '" + file.getFileName()	+ "' for scheduler '"+selectedScheduler().getName()+"'");
		}
		catch (Exception e)
		{
			String msg = "Error occurred while working with scheduler settings from file '"+file.getFileName()+"'";
			getLogger().error(msg, e);
			MessageUtils.addErrorMessage(msg, e.getMessage());
		}
	}

	public boolean isWeekendHoliday()
	{
		return selectedScheduler().getSchedulerData().isWeekendHoliday();
	}

	public void setWeekendHoliday(boolean weekendHoliday)
	{
		try
		{
			selectedScheduler().setWeekendHoliday(weekendHoliday);
		}
		catch (IOException e)
		{
			WebUtils.logAndGrowlException("Could not save changes in 'weekend is holiday' setting", e, getLogger());
		}
	}

	public void weekendHolidayChanged(AjaxBehaviorEvent event)
	{
		//No need to do anything here, setWeekendHoliday already called. This method needs to be present to allow auto-submit of changes in WeekendHoliday checkbox
	}

	public String getConfigFileName()
	{
		String cfg = selectedScheduler().getConfigFileName();
		return cfg!=null ? cfg : "<No config uploaded>";
	}

	public boolean isConfigChanged()
	{
		return selectedScheduler().isConfigChanged();
	}

	public Date getBaseTime()
	{
		Date bt = selectedScheduler().getSchedulerData().getBaseTime();
		if (bt == null)
			return emptyBaseTime;
		return bt;
	}

	public void setBaseTime(Date baseTime)
	{
		if (isCurrentTime())
			return;

		try
		{
			selectedScheduler().setBaseTime(baseTime);
			getLogger().info("set base time to '"+baseTime+"'");
		}
		catch (IOException e)
		{
			WebUtils.logAndGrowlException("Could not save changes in base time", e, getLogger());
		}
	}

	public boolean isCurrentDate()
	{
		return selectedScheduler().getSchedulerData().isUseCurrentDate();
	}

	public void setCurrentDate(boolean useCurrentDate)
	{
		try
		{
			selectedScheduler().setBusinessDay(useCurrentDate ? null : new Date());
			getLogger().info("toggled usage of current date to " + useCurrentDate);
		}
		catch (IOException e)
		{
			WebUtils.logAndGrowlException("Could not save changes in business day", e, getLogger());
		}
	}


	public boolean isCurrentTime()
	{
		return selectedScheduler().getSchedulerData().getBaseTime() == null;
	}

	public void setCurrentTime(boolean useCurrentTime)
	{
		try
		{
			if (useCurrentTime)
				selectedScheduler().useCurrentTime();
			else
				selectedScheduler().setBaseTime(new Date());

			getLogger().info("toggled usage of base time to "+useCurrentTime);
		}
		catch (IOException e)
		{
			WebUtils.logAndGrowlException("Could not save changes in base time", e, getLogger());
		}
	}

	public void useCurrentDateChanged(AjaxBehaviorEvent event)
	{
		//No need to do anything here, setCurrentDate or setCurrentTime is already called. This method needs to be present to allow auto-submit of changes in the checkbox
	}


	public Date getHolidayDate()
	{
		return holidayDate;
	}

	public void setHolidayDate(Date holidayDate)
	{
		this.holidayDate = holidayDate;
	}

	public void toggleHoliday(SelectEvent<Date> event)
	{
		Date d = event.getObject();
		if (d == null)
			return;

		try
		{
			selectedScheduler().toggleHoliday(d);
		}
		catch (IOException e)
		{
			WebUtils.logAndGrowlException("Could not save changes in calendar", e, getLogger());
		}
	}

	public String getHolidaysAsString()
	{
		String result = "";
		Map<String, Boolean> hols = selectedScheduler().getHolidays();
		return hols.entrySet().stream().map(ent -> "'" + ent + "=" + ent.getValue() + "'").collect(Collectors.joining(", "));
	}

	public Date getBusinessDay()
	{
		return selectedScheduler().getSchedulerData().getBusinessDay();
	}

	public void setBusinessDay(Date businessDay)
	{
		if (isCurrentDate())
			return;

		try
		{
			selectedScheduler().setBusinessDay(businessDay);
			getLogger().info("set business day to '"+businessDay+"'");
		}
		catch (IOException e)
		{
			WebUtils.logAndGrowlException("Could not save changes in business day", e, getLogger());
		}
	}
	
	
	public void setIgnoreAllConnectionsFailures(boolean ignoreAllConnectionsFailures)
	{
		try
		{
			SchedulerData schedulerData = selectedScheduler().getSchedulerData();
			schedulerData.setIgnoreAllConnectionsFailures(ignoreAllConnectionsFailures);
			schedulerData.saveIgnoreAllConnectionsFailures();
		}
		catch (IOException e)
		{
			WebUtils.logAndGrowlException("Couldn't save changes for ignoring all connections failures", e, getLogger());
		}
	}
	
	public boolean isIgnoreAllConnectionsFailures()
	{
		return selectedScheduler().getSchedulerData().isIgnoreAllConnectionsFailures();
	}
	
	public void ignoreAllConnectionsFailuresChanged(AjaxBehaviorEvent event)
	{
		// Nothing to do, because all necessary changes have been made and saved in set-method above
	}
	
	public List<String> getAllConnections()
	{
		return ClearThCore.connectionStorage().getConnections().stream().map(ClearThConnection::getName).collect(Collectors.toList());
	}
	
	public void setSelectedConnectionsToIgnoreFailures(List<String> selectedConnectionsToIgnoreFailures)
	{
		// This will be called once on saving connections names to the config file
		selectedScheduler().getSchedulerData().setConnectionsToIgnoreFailures(new LinkedHashSet<>(selectedConnectionsToIgnoreFailures));
	}
	
	public List<String> getSelectedConnectionsToIgnoreFailures()
	{
		return new ArrayList<>(selectedScheduler().getSchedulerData().getConnectionsToIgnoreFailures());
	}
	
	public String getConnectionsToIgnoreFailuresString()
	{
		Set<String> connections = selectedScheduler().getSchedulerData().getConnectionsToIgnoreFailures();
		if (connections.isEmpty())
			return "Connections to ignore failures are not specified";
		
		Iterator<String> connectionsIter = connections.iterator();
		CommaBuilder builder = new CommaBuilder();
		int shownCount = 0;
		while (connectionsIter.hasNext() && shownCount < 2)
		{
			builder.append("'").add(connectionsIter.next()).add("'");
			shownCount++;
		}
		String result = "Connection failures will be ignored for " + builder.toString();
		if (connections.size() > shownCount)
			result += " and " + (connections.size() - shownCount) + " more";
		return result;
	}
	
	public void saveConnectionsToIgnoreFailures()
	{
		try
		{
			selectedScheduler().getSchedulerData().saveConnectionsToIgnoreFailures();
			WebUtils.addCanCloseCallback(true);
			MessageUtils.addInfoMessage("Success", "Connections to ignore failures have been saved to the file");
		}
		catch (IOException e)
		{
			WebUtils.logAndGrowlException("Could not save selected connections to ignore failures", e, getLogger());
		}
	}
	
	
	public ReportsConfig getReportsConfig()
	{
		return selectedScheduler().getReportsConfig();
	}
	
	public void prepareReportsConfigToEdit()
	{
		reportsConfigToEdit = new ModifiableReportsConfig(getReportsConfig());
	}
	
	public ModifiableReportsConfig getReportsConfigToEdit()
	{
		return reportsConfigToEdit;
	}
	
	public void saveReportsConfig()
	{
		try
		{
			selectedScheduler().setReportsConfig(reportsConfigToEdit);
			MessageUtils.addInfoMessage("Success", "Reports configuration has been saved");
		}
		catch (Exception e)
		{
			WebUtils.logAndGrowlException("Could not save reports configuration", e, getLogger());
		}
	}
	
	
	public boolean isAutoSaveState()
	{
		return selectedScheduler().getStateConfig().isAutoSave();
	}
	
	public void setAutoSaveState(boolean autoSave)
	{
		try
		{
			selectedScheduler().setStateConfig(new StateConfig(autoSave));
		}
		catch (Exception e)
		{
			WebUtils.logAndGrowlException("Could not save 'Auto-save state' setting", e, getLogger());
		}
	}
	
	
	public UploadedFile getSchedulerConfigurationUploadedFile() {
		return schedulerConfigurationUploadedFile;
	}

	public void setSchedulerConfigurationUploadedFile(UploadedFile schedConfigUploadedFile) {
		this.schedulerConfigurationUploadedFile = schedConfigUploadedFile;
	}

	public AutomationStepsManagement getSteps() {
		return stepsManagement;
	}
}
