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

package com.exactprosystems.clearth.web.beans.automation;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.Scheduler;
import com.exactprosystems.clearth.automation.schedulerinfo.SchedulerInfoFile;
import com.exactprosystems.clearth.utils.ExceptionUtils;
import com.exactprosystems.clearth.web.beans.ClearThBean;
import com.exactprosystems.clearth.web.misc.MessageUtils;
import com.exactprosystems.clearth.web.misc.WebUtils;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import java.io.File;

@SuppressWarnings({"WeakerAccess", "unused"})
public class SchedulerInfoExportBean extends ClearThBean
{
	protected AutomationBean automationBean;
	
	protected SchedulerInfoFile selectedSchedulerInfoFiles;
	protected TreeNode schedulerInfoFilesRoot = new DefaultTreeNode();
	protected TreeNode[] selectedSchedulerInfoFilesNodes;
	
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
	
	
	public void collectSchedulerInfoFiles()
	{
		try
		{
			selectedSchedulerInfoFiles = ClearThCore.getInstance().getSchedulerInfoExporter().collectFiles(selectedScheduler());
			schedulerInfoFilesRoot = new DefaultTreeNode();
			processSchedulerInfoFile(selectedSchedulerInfoFiles, schedulerInfoFilesRoot);
		}
		catch (Exception e)
		{
			String errMsg = "Error while collecting scheduler info files";
			getLogger().error(errMsg, e);
			MessageUtils.addErrorMessage(errMsg, ExceptionUtils.getDetailedMessage(e));
		}
	}
	
	private void processSchedulerInfoFile(SchedulerInfoFile file, TreeNode parentNode)
	{
		for (SchedulerInfoFile child : file.getChildren())
		{
			// Not showing report resource files because they always should be in the export
			if (child.isResource())
				continue;
			
			TreeNode childNode = new DefaultTreeNode(child, parentNode);
			// Select all files by default
			childNode.setSelected(true);
			processSchedulerInfoFile(child, childNode);
		}
	}
	
	
	public void exportSelectedSchedulerInfoFiles()
	{
		if (selectedSchedulerInfoFilesNodes == null || selectedSchedulerInfoFilesNodes.length == 0)
		{
			MessageUtils.addErrorMessage("Nothing to export", "No scheduler information file selected");
			return;
		}
		
		try
		{
			// Mark necessary files to include in the export by its nodes
			for (TreeNode node : selectedSchedulerInfoFilesNodes)
			{
				((SchedulerInfoFile)node.getData()).setInclude(true);
				// Need to add parent files too because they may be not included in selected nodes collection
				includeParentFiles(node);
			}
			
			File exportZip = ClearThCore.getInstance().getSchedulerInfoExporter().exportSelectedZip(selectedSchedulerInfoFiles);
			WebUtils.addCanCloseCallback(true);
			WebUtils.redirectToFile(ClearThCore.getInstance().excludeRoot(exportZip.getAbsolutePath()));
		}
		catch (Exception e)
		{
			String errMsg = "Error occurred while exporting scheduler info files";
			getLogger().error(errMsg, e);
			MessageUtils.addErrorMessage(errMsg, ExceptionUtils.getDetailedMessage(e));
		}
	}
	
	private void includeParentFiles(TreeNode node)
	{
		// Check if it isn't a root node
		if (node.getParent() != null)
		{
			((SchedulerInfoFile)node.getData()).setInclude(true);
			includeParentFiles(node.getParent());
		}
	}
	
	
	public TreeNode getSchedulerInfoFilesRoot()
	{
		return schedulerInfoFilesRoot;
	}
	
	public void setSelectedSchedulerInfoFilesNodes(TreeNode[] selectedSchedulerInfoFilesNodes)
	{
		this.selectedSchedulerInfoFilesNodes = selectedSchedulerInfoFilesNodes;
	}
	
	public TreeNode[] getSelectedSchedulerInfoFilesNodes()
	{
		return selectedSchedulerInfoFilesNodes;
	}
}
