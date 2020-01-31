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

package com.exactprosystems.clearth.web.beans.automation;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.Scheduler;
import com.exactprosystems.clearth.automation.schedulerinfo.SchedulerInfoFile;
import com.exactprosystems.clearth.utils.ExceptionUtils;
import com.exactprosystems.clearth.web.beans.ClearThBean;
import com.exactprosystems.clearth.web.misc.MessageUtils;
import com.exactprosystems.clearth.web.misc.SchedulerInfoExportStats;
import com.exactprosystems.clearth.web.misc.WebUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@SuppressWarnings({"WeakerAccess", "unused"})
public class SchedulerInfoExportBean extends ClearThBean
{
	protected AutomationBean automationBean;
	
	protected MultiValuedMap<String, SchedulerInfoFile> storage;
	protected String regex;
	
	protected List<SchedulerInfoExportStats> stats = new ArrayList<>();
	protected int totalCount;
	protected String totalSize;
	
	public SchedulerInfoExportBean()
	{ }
	
	public void setAutomationBean(AutomationBean automationBean)
	{
		this.automationBean = automationBean;
	}
	
	protected Scheduler selectedScheduler()
	{
		return automationBean.selectedScheduler;
	}
	
	
	public void collectFiles()
	{
		try
		{
			regex = "";
			storage = ClearThCore.getInstance().getSchedulerInfoExporter().collectFiles(selectedScheduler());
			updateStats();
		}
		catch (IOException e)
		{
			String errMsg = "Error while collecting scheduler info files";
			getLogger().error(errMsg, e);
			MessageUtils.addErrorMessage(errMsg, ExceptionUtils.getDetailedMessage(e));
		}
	}
	
	public void applyFilter()
	{
		// Reset all filters if regex pattern is blank
		Pattern pattern = StringUtils.isBlank(regex) ? null : Pattern.compile(regex);
		for (SchedulerInfoFile file : storage.values())
			file.setInclude(pattern == null || !pattern.matcher(file.getOutputPath()).find());
		updateStats();
	}
	
	public void exportSelectedFiles()
	{
		if (totalCount == 0)
		{
			MessageUtils.addErrorMessage("Nothing to export", "No scheduler information file selected");
			return;
		}
		
		try
		{
			File exportZip = ClearThCore.getInstance().getSchedulerInfoExporter().exportSelectedZip(storage);
			WebUtils.addCanCloseCallback(true);
			WebUtils.redirectToFile(ClearThCore.getInstance().excludeRoot(exportZip.getAbsolutePath()));
		}
		catch (Exception e)
		{
			String errMsg = "Could not download scheduler info files";
			getLogger().error(errMsg, e);
			MessageUtils.addErrorMessage(errMsg, ExceptionUtils.getDetailedMessage(e));
		}
	}
	
	
	public void setRegex(String regex)
	{
		this.regex = regex;
	}
	
	public String getRegex()
	{
		return regex;
	}
	
	public List<SchedulerInfoExportStats> getStats()
	{
		return stats;
	}
	
	public int getTotalCount()
	{
		return totalCount;
	}
	
	public String getTotalSize()
	{
		return totalSize;
	}
	
	
	protected void updateStats()
	{
		// Collect statistics about number and sizes of files being exported
		stats.clear();
		totalCount = 0;
		
		long totalSizeLong = 0;
		for (Map.Entry<String, Collection<SchedulerInfoFile>> type : storage.asMap().entrySet())
		{
			int selectedCount = 0;
			long selectedSize = 0;
			for (SchedulerInfoFile file : type.getValue())
			{
				if (file.isInclude())
				{
					selectedCount++;
					selectedSize += file.getOriginalFile().length();
				}
			}
			
			stats.add(new SchedulerInfoExportStats(type.getKey(), selectedCount, selectedSize));
			totalCount += selectedCount;
			totalSizeLong += selectedSize;
		}
		totalSize = FileUtils.byteCountToDisplaySize(totalSizeLong);
	}
}
