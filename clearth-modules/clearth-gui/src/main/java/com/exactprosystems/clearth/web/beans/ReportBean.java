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

package com.exactprosystems.clearth.web.beans;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.SimpleExecutor;
import com.exactprosystems.clearth.automation.Matrix;
import com.exactprosystems.clearth.automation.MatrixData;
import com.exactprosystems.clearth.automation.Scheduler;
import com.exactprosystems.clearth.automation.matrix.linked.MatrixProvider;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.html.RealTimeReport;
import com.exactprosystems.clearth.utils.ExceptionUtils;
import com.exactprosystems.clearth.utils.Pair;
import com.exactprosystems.clearth.utils.TagUtils;
import com.exactprosystems.clearth.web.misc.MessageUtils;
import com.exactprosystems.clearth.web.misc.UserInfoUtils;
import org.apache.commons.lang.StringUtils;

import javax.annotation.PostConstruct;
import javax.faces.context.FacesContext;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ReportBean
{
	private String userName;
	private String reportHead, reportBody;
	
	@PostConstruct
	protected void init()
	{
		userName = UserInfoUtils.getUserName();
	}
	
	public void buildReport()
	{
		Scheduler scheduler = getScheduler(FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("schedulerName"));
		if (scheduler == null)
			return;
		
		String matrixName = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("matrixName");
		Matrix matrix = getMatrix(matrixName, scheduler);
		if (matrix == null)
			return;
		
		File actionsReports = new File(ClearThCore.appRootRelative(scheduler.getActionReportsDir()));
		Map<String, List<String>> matricesSteps = SimpleExecutor.getStepsByMatricesMap(actionsReports);
		changeNamesForLinkedMatrices(matricesSteps, scheduler);
		
		RealTimeReport report;
		try
		{
			report = createReportWriter(matrix, userName, scheduler);
			report.writeReport(scheduler.getSteps(), matricesSteps.get(matrixName), actionsReports, false);
		}
		catch (Exception e)
		{
			MessageUtils.addErrorMessage("An error occurred during building report", ExceptionUtils.getDetailedMessage(e));
			return;
		}
		
		String reportHtml = report.getHtmlContent().getBuffer().toString();
		setReportHead(TagUtils.getTagValue("head", reportHtml));
		String body = TagUtils.getTagValue("body", reportHtml);
		setReportBody(body != null ? replaceLinksToFiles(body, scheduler, matrix) : null);
	}
	
	
	protected Scheduler getScheduler(String schedulerName)
	{
		if (StringUtils.isEmpty(schedulerName))
		{
			MessageUtils.addErrorMessage("No scheduler selected", "");
			return null;
		}
		
		Scheduler result = ClearThCore.getInstance().getSchedulersManager().getSchedulerByName(schedulerName, userName);
		if (result == null)
		{
			MessageUtils.addErrorMessage("No scheduler with name '" + schedulerName + "'", "");
			return null;
		}
		else if (!result.isRunning())
		{
			MessageUtils.addErrorMessage("Scheduler is not running", "");
			return null;
		}
		return result;
	}
	
	protected Matrix getMatrix(String matrixName, Scheduler scheduler)
	{
		if (StringUtils.isEmpty(matrixName))
		{
			MessageUtils.addErrorMessage("No matrix selected", "");
			return null;
		}
		
		Matrix matrixFound = scheduler.getMatrices().stream()
				.filter(matrix -> matrix.getName().equals(matrixName)).findFirst().orElse(null);
		if (matrixFound == null)
		{
			MessageUtils.addErrorMessage("No matrix with name '" + matrixName + "'", "");
			return null;
		}
		else if (matrixFound.getStarted() == null)
		{
			MessageUtils.addErrorMessage("Matrix '" + matrixName + "' is not running", "");
			return null;
		}
		return matrixFound;
	}
	
	protected RealTimeReport createReportWriter(Matrix matrix, String userName, Scheduler scheduler) throws IOException
	{
		return new RealTimeReport(matrix, userName, matrix.getName(), scheduler.getStartTime(), new Date());
	}
	
	private void changeNamesForLinkedMatrices(Map<String, List<String>> matricesSteps, Scheduler scheduler)
	{
		Set<String> matricesDirs = new HashSet<>(matricesSteps.keySet());
		for (String mDir : matricesDirs)
		{
			if (mDir.contains(MatrixProvider.STORED_MATRIX_PREFIX))
			{
				String linkedMatrixSourceName = scheduler.getMatricesData().stream()
						.filter(md -> md.isLinked() && md.getFile().getName().equals(mDir))
						.findFirst().map(MatrixData::getName).orElse(null);
				matricesSteps.put(linkedMatrixSourceName, matricesSteps.remove(mDir));
			}
		}
	}
	
	protected String replaceLinksToFiles(String reportBody, Scheduler scheduler, Matrix matrix)
	{
		try
		{
			Pair<String, Integer> linkTagAndStart;
			int searchFrom = 0;
			while ((linkTagAndStart = TagUtils.getTagAndStart("a", reportBody, searchFrom)) != null)
			{
				// Obtaining 'a' tag and value of its 'href' attribute
				int linkTagStart = linkTagAndStart.getSecond();
				String linkTag = linkTagAndStart.getFirst(),
						linkValue = TagUtils.getTagAttribute("a", "href", linkTag);
				searchFrom = linkTagStart + linkTag.length();
				if (linkValue == null || !linkValue.startsWith(Result.DETAILS_DIR))
					continue;
				
				// Constructing full path to the file
				Path actionReportsDir = Paths.get(scheduler.getActionReportsDir());
				int dirsInPathCount = actionReportsDir.getNameCount();
				String newLinkValue = ClearThCoreApplicationBean.getInstance().getAppContextPath() + "/"
						+ actionReportsDir.subpath(dirsInPathCount - 5, dirsInPathCount).toString() + "/"
						+ URLEncoder.encode(matrix.getName(), "UTF-8").replace("+", "%20") + "/"
						+ linkValue;
				
				// Making replacement for current 'a' tag
				// !!! In case of really large reports this could cause OutOfMemory exception
				// !!! or take too long time to process. Currently there is no another way to do such logic
				reportBody = reportBody.substring(0, linkTagStart) + linkTag.replace(linkValue, newLinkValue)
						+ reportBody.substring(linkTagStart + linkTag.length());
				searchFrom += newLinkValue.length() - linkValue.length();
			}
		}
		catch (Exception e)
		{
			MessageUtils.addErrorMessage("Unable to replace paths for downloadable files", "");
		}
		return reportBody;
	}
	
	
	public String getReportHead()
	{
		buildReport();
		return reportHead;
	}
	
	public void setReportHead(String reportHead)
	{
		this.reportHead = reportHead;
	}
	
	public String getReportBody()
	{
		return reportBody;
	}
	
	public void setReportBody(String reportBody)
	{
		this.reportBody = reportBody;
	}
}
