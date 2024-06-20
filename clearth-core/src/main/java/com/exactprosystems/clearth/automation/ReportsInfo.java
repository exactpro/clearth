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

package com.exactprosystems.clearth.automation;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.report.ReportsConfig;
import com.exactprosystems.clearth.xmldata.XmlMatrixInfo;
import com.exactprosystems.clearth.xmldata.XmlReportsConfig;
import org.apache.commons.io.FilenameUtils;

public class ReportsInfo
{
	private String path,
			actionReportsPath;
	private List<XmlMatrixInfo> matrices;
	
	private Date started;
	private Date finished;
	private XmlReportsConfig reportsConfig = new XmlReportsConfig();
	
	public ReportsInfo()
	{
		path = null;
		actionReportsPath = null;
		matrices = new ArrayList<XmlMatrixInfo>();
	}
	
	
	public String getPath()
	{
		return path;
	}
	
	public void setPath(String path)
	{
		this.path = path;
	}
	
	public boolean isReportsAvailable()
	{
		return path != null;
	}
	
	
	public String getActionReportsPath()
	{
		return actionReportsPath;
	}
	
	public void setActionReportsPath(String actionReportsPath)
	{
		this.actionReportsPath = actionReportsPath;
	}
	
	
	public List<XmlMatrixInfo> getMatrices()
	{
		return matrices;
	}
	
	public void setMatrices(List<XmlMatrixInfo> matrices)
	{
		this.matrices = matrices;
	}
	
	public String getRelativeUri()
	{
		if (path == null)
			return "";
		else
			return FilenameUtils.normalize(path).replace(
					FilenameUtils.normalize(ClearThCore.getInstance().getReportsPath()), "").
					replace("\\", "/");
	}

	public Date getFinished()
	{
		return finished;
	}

	public void setFinished(Date finished)
	{
		this.finished = finished;
	}

	public Date getStarted()
	{
		return started;
	}

	public void setStarted(Date started)
	{
		this.started = started;
	}
	

	public void setReportsConfig(ReportsConfig reportsConfig)
	{
		this.reportsConfig.setCompleteHtmlReport(reportsConfig.isCompleteHtmlReport());
		this.reportsConfig.setFailedHtmlReport(reportsConfig.isFailedHtmlReport());
		this.reportsConfig.setCompleteJsonReport(reportsConfig.isCompleteJsonReport());
	}
	
	public ReportsConfig getReportsConfig()
	{
		return new ReportsConfig(reportsConfig.isCompleteHtmlReport(), reportsConfig.isFailedHtmlReport(), reportsConfig.isCompleteJsonReport());
	}
	
	public void setXmlReportsConfig(XmlReportsConfig reportsConfig)
	{
		this.reportsConfig = reportsConfig;
	}
	
	public XmlReportsConfig getXmlReportsConfig()
	{
		return this.reportsConfig;
	}
}
