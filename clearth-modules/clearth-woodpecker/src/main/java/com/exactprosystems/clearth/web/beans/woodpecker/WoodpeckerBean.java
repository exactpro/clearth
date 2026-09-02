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

package com.exactprosystems.clearth.web.beans.woodpecker;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.utils.ExceptionUtils;
import com.exactprosystems.clearth.web.beans.ClearThBean;
import com.exactprosystems.clearth.web.misc.MessageUtils;
import com.exactprosystems.clearth.web.misc.UserInfoUtils;
import com.exactprosystems.clearth.web.misc.WebUtils;
import com.exactprosystems.clearth.woodpecker.Woodpecker;
import com.exactprosystems.clearth.woodpecker.WoodpeckerObjectsFactory;
import com.exactprosystems.clearth.woodpecker.configuration.daemonsDictionary.DaemonDesc;
import com.exactprosystems.clearth.woodpecker.configuration.settings.DaemonSettings;
import com.exactprosystems.clearth.woodpecker.configuration.settings.loading.LoadingUnit;
import com.exactprosystems.clearth.woodpecker.daemons.DaemonsPool;
import com.exactprosystems.clearth.woodpecker.daemons.WoodpeckerDaemon;
import com.exactprosystems.clearth.woodpecker.daemons.WoodpeckerDaemonFactory;
import com.exactprosystems.clearth.woodpecker.daemons.action.ActionLoadingStatistics;
import com.exactprosystems.clearth.woodpecker.daemons.message.MessageDaemonDesc;
import com.exactprosystems.clearth.woodpecker.daemons.message.MessageDaemonSettings;
import com.exactprosystems.clearth.woodpecker.daemons.message.MessageLoadingStatistics;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerException;
import com.exactprosystems.clearth.woodpecker.notifications.Notification;
import com.exactprosystems.clearth.woodpecker.notifications.NotificationsSubscriber;
import com.exactprosystems.clearth.woodpecker.statistics.LoadingStatistics;
import com.exactprosystems.clearth.woodpecker.utils.charts.ChartExporter;
import com.exactprosystems.clearth.woodpecker.utils.charts.LoadingChart;
import com.exactprosystems.clearth.woodpecker.utils.charts.LoadingChartBuilder;
import com.exactprosystems.clearth.woodpecker.utils.charts.LoadingChartJsonWriter;
import org.primefaces.component.tabview.Tab;
import org.primefaces.event.TabChangeEvent;
import org.primefaces.event.TabCloseEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import javax.activation.MimetypesFileTypeMap;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.event.AjaxBehaviorEvent;
import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;

import static com.exactprosystems.clearth.woodpecker.Woodpecker.woodpecker;
import static org.apache.commons.lang3.StringUtils.capitalize;

@SuppressWarnings("unused")
public class WoodpeckerBean extends ClearThBean implements NotificationsSubscriber
{
	private static final int NOTIFICATIONS_BUFFER_CAPACITY = 100;
	
	private static final DateTimeFormatter UI_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
	
	private final Woodpecker woodpecker;	
	private final Queue<Notification> notifications = new LinkedBlockingDeque<Notification>(NOTIFICATIONS_BUFFER_CAPACITY);
	
	private int activeTab = 0;

	// Working with state of accordionPanel UI element
	private final Set<Integer> expandedPanelsIndexes = new TreeSet<Integer>();
	
	// Visualisation
	private Map<String, LoadingChart> loadingCharts = new LinkedHashMap<>();
	private Map<String, LoadingChartBuilder> loadingChartBuilders = new HashMap<>();
	private final LoadingChartJsonWriter loadingChartJsonWriter = new LoadingChartJsonWriter();
	
	
	public WoodpeckerBean()
	{
		woodpecker = woodpecker();
	}
	
	
	@PostConstruct
	private void subscribeToNotifications()
	{
		woodpecker.getNotifications().subscribe(this);
	}

	@PreDestroy
	private void unSubscribeFromNotifications()
	{
		woodpecker.getNotifications().unSubscribe(this);
	}

	@Override
	public void notify(Notification notification)
	{
		if (!notifications.offer(notification))
			getLogger().warn("Too many user messages to show in popup...");
	}

	public boolean isDaemonRunning(int index, String instance)
	{
		return woodpecker.isDaemonRunning(index, instance);
	}

	public void startDaemon(int index, String instance)
	{
		woodpecker.startDaemon(index, instance);
	}

	public void stopDaemon(int index, String instance)
	{
		woodpecker.stopDaemon(index, instance);
	}

	public void stopAllDaemons()
	{
		woodpecker.stopAllDaemons();
	}

	public Collection<String> getDaemonInstancesNames(int poolIndex)
	{
		return woodpecker.getDaemonInstancesNames(poolIndex);
	}

	public WoodpeckerDaemon getDaemon(int poolIndex, String instanceName)
	{
		return woodpecker.getDaemon(poolIndex, instanceName);
	}

	public DaemonSettings getDaemonSettings(int poolIndex, String instanceName)
	{
		return woodpecker.getDaemonSettings(poolIndex, instanceName);
	}
	
	public void saveDaemonSettings(int poolIndex, String instanceName)
	{
		woodpecker.saveDaemonSettings(poolIndex, instanceName);
	}
	
	public boolean isMessageSettingsAvailable(DaemonSettings s)
	{
		return s instanceof MessageDaemonSettings;
	}

	public boolean isLoadingStatisticsAvailable(int poolIndex, String instanceName)
	{
		return woodpecker.getLoadingStatistics(poolIndex, instanceName) != null;
	}
	
	public boolean isMessageLoadingStatistics(LoadingStatistics statistics)
	{
		return statistics instanceof MessageLoadingStatistics;
	}
	
	public boolean isActionLoadingStatistics(LoadingStatistics statistics)
	{
		return statistics instanceof ActionLoadingStatistics;
	}

	public LoadingStatistics getLoadingStatistics(int poolIndex, String instanceName)
	{
		return woodpecker.getLoadingStatistics(poolIndex, instanceName);
	}

	public int getActiveTab()
	{
		return activeTab;
	}

	public void setActiveTab(int activeTab)
	{
		this.activeTab = activeTab;
	}

	public Map<Integer, DaemonsPool> getDaemonPools()
	{
		return woodpecker.getDaemonPools();
	}

	public DaemonsPool getDaemonPool(Integer index)
	{
		return woodpecker.getDaemonPools().get(index);
	}
	
	
	/* Visualisation */

	public List<LoadingChart> getLineCharts()
	{
		return new ArrayList<>(loadingCharts.values());
	}

	public void createCharts()
	{
		WoodpeckerObjectsFactory factory = woodpecker.getObjectsFactory();
		
		for (DaemonsPool pool : woodpecker.getDaemonPools().values())
		{
			for (WoodpeckerDaemon daemon : pool.getDaemons().values())
			{
				if (daemon.getLoadingStatistics() != null)
				{
					String daemonName = daemon.getSettings().getFullName();

					WoodpeckerDaemonFactory daemonFactory = daemon.getFactory();
					LoadingChartBuilder chartBuilder = daemonFactory.createLoadingChartBuilder();
					loadingChartBuilders.put(daemonName, chartBuilder);
					
					loadingCharts.put(daemonName, chartBuilder.createChart(daemon));
				}
			}
		}
	}

	public void updateCharts()
	{
		WoodpeckerObjectsFactory factory = woodpecker.getObjectsFactory();
		
		for (DaemonsPool pool : woodpecker.getDaemonPools().values())
		{
			for (WoodpeckerDaemon daemon : pool.getDaemons().values())
			{
				String daemonName = daemon.getSettings().getFullName();
				LoadingChart chart = loadingCharts.get(daemonName);
				
				LoadingStatistics statistics = daemon.getLoadingStatistics();
				if (statistics == null)
					return;

				if ((chart == null) || (chart.getStartTime() < statistics.getStartTimestamp()))
				{
					WoodpeckerDaemonFactory daemonFactory = daemon.getFactory();
					LoadingChartBuilder chartBuilder = daemonFactory.createLoadingChartBuilder();
					loadingChartBuilders.put(daemonName, chartBuilder);
					
					loadingCharts.put(daemonName, chartBuilder.createChart(daemon));
					return;
				}

				if (chart.isCompleted())
					return;
				
				LoadingChartBuilder chartBuilder = loadingChartBuilders.get(daemonName);
				chartBuilder.updateChart(daemon, loadingCharts.get(daemonName));
			}
		}
	}
	
	public String initCharts()
	{
		createCharts();
		return loadingChartJsonWriter.createJson(loadingCharts.values(), false);
	}
	
	public String getChartsJson()
	{
		// First update all charts
		for (LoadingChart chart : loadingCharts.values())
		{
			String daemonName = chart.getDaemonName();
			loadingChartBuilders.get(daemonName).updateChart(getDaemonByName(daemonName), chart);
		}
		return loadingChartJsonWriter.createJson(loadingCharts.values(), true);
	}
	
	public WoodpeckerDaemon getDaemonByName(String daemonName)
	{
		for (DaemonsPool pool : woodpecker.getDaemonPools().values())
		{
			for (WoodpeckerDaemon daemon : pool.getDaemons().values())
			{
				if(daemon.getSettings().getFullName().equals(daemonName))
				{
					return  daemon;
				}
			}
		}
		return null;
	}

	public void updateWoodpecker()
	{
		woodpecker.update();
	}
	
	/*   Working with state of accordionPanel UI element   */

	public String getExpandedPanelsIndexes()
	{
		return expandedPanelsIndexes.stream()
				.map(String::valueOf)
				.collect(Collectors.joining(","));
	}

	public void setExpandedPanelsIndexes(String expandedPanelsIndexes) { /* Just to avoid exception in PrimeFaces */ }

	public void onTabChanged(TabChangeEvent event)
	{
		int changedIndex = findChangedTabIndex(event, event.getTab());
		if (!expandedPanelsIndexes.contains(changedIndex))
		{
			expandedPanelsIndexes.add(changedIndex);
			getLogger().trace("Tab #{} opened", changedIndex);
		}
	}

	public void onTabClosed(TabCloseEvent event)
	{
		int changedIndex = findChangedTabIndex(event, event.getTab());
		if (expandedPanelsIndexes.contains(changedIndex))
		{
			expandedPanelsIndexes.remove(changedIndex);
			getLogger().trace("Tab #{} closed", changedIndex);
		}
	}
	
	private int findChangedTabIndex(AjaxBehaviorEvent event, Tab tab)
	{
		int index = 0;
		for (UIComponent c : event.getComponent().getChildren())
		{
			if (c instanceof Tab)
			{
				if (c == tab)
					return index;
				index++;
			}
		}
		return index;
	}

	public void getMessagesForUI()
	{
		Notification message;
		while ((message = notifications.poll()) != null)
		{
			MessageUtils.addMessage(getFacesSeverity(message.getSeverity()), 
					message.getSummary(), 
					message.getDetails());
		}
	}

	private FacesMessage.Severity getFacesSeverity(Notification.Severity severity)
	{
		switch (severity)
		{
			case INFO : return FacesMessage.SEVERITY_INFO;
			case WARN : return FacesMessage.SEVERITY_WARN;
			case ERROR : return FacesMessage.SEVERITY_ERROR;
			case FATAL : return FacesMessage.SEVERITY_FATAL;
		}
		return FacesMessage.SEVERITY_INFO;
	}

	public void setSelectedScheduler(String selectedScheduler)
	{
		try
		{
			woodpecker.setScheduler(selectedScheduler, UserInfoUtils.getUserName());
		}
		catch (WoodpeckerException e)
		{
			WebUtils.logAndGrowlException("Unable to save Woodpecker settings", e, getLogger());
		}
	}

	public String getSelectedScheduler()
	{
		return woodpecker.getSchedulerName();
	}
	
	public List<String> getSchedulers()
	{
		List<String> schedulers = ClearThCore.getInstance().getSchedulersManager().getAvailableSchedulerNames(UserInfoUtils.getUserName());
		if (woodpecker.getSchedulerName() != null && !schedulers.contains(woodpecker.getSchedulerName()))
			schedulers.add(woodpecker.getSchedulerName());
		return schedulers;
	}

	public String getCapitalizedPluralUnitName(int poolIndex, String instanceName)
	{
		return capitalize(getPluralUnitName(poolIndex, instanceName));
	}
	
	public String getPluralUnitName(int poolIndex, String instanceName)
	{
		WoodpeckerDaemon daemon = woodpecker.getDaemon(poolIndex, instanceName);
		if (daemon == null)
			return "";
		DaemonDesc desc = daemon.getDescription();
		return desc.getPluralUnitName();
	}

	public boolean showStatisticsByOperations(int poolIndex, String instanceName)
	{
		WoodpeckerDaemon daemon = woodpecker.getDaemon(poolIndex, instanceName);
		if (daemon == null)
			return false;
		DaemonDesc desc = daemon.getDescription();
		return (desc instanceof MessageDaemonDesc) 
				&& (((MessageDaemonDesc) desc).getLoadingUnit() == LoadingUnit.Operation);
	}

	// PrimeFaces can't convert LocalDateTime to String automatically. f:convertDateTime supports only Date class.
	
	public String getDaemonStartTime(int poolIndex, String instanceName)
	{
		WoodpeckerDaemon daemon = woodpecker.getDaemon(poolIndex, instanceName);
		if (daemon == null)
			return null;
		LocalDateTime startTime = daemon.getStartTime();
		return (startTime != null) ? UI_TIME_FORMATTER.format(startTime) : null;
	}
	
	public String getDaemonEndTime(int poolIndex, String instanceName)
	{
		WoodpeckerDaemon daemon = woodpecker.getDaemon(poolIndex, instanceName);
		if (daemon == null)
			return null;
		LocalDateTime endTime = daemon.getEndTime();
		return (endTime != null) ? UI_TIME_FORMATTER.format(endTime) : null;
	}
	
	
	public StreamedContent saveChartData(String chartName)
	{
		File f;
		try
		{
			ChartExporter exporter = new ChartExporter();
			f = exporter.exportChartToTempDir(loadingCharts.get(chartName));
			return WebUtils.downloadFile(f);
		} 
		catch (Exception e)
		{
			String errMsg = "Unable to create chart report";
			getLogger().error(errMsg, e);
			MessageUtils.addErrorMessage(errMsg, ExceptionUtils.getDetailedMessage(e));
			return null;
		}
	}
}
