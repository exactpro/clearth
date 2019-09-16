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

package com.exactprosystems.clearth.web.beans;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.Executor;
import com.exactprosystems.clearth.automation.Matrix;
import com.exactprosystems.clearth.automation.MatrixData;
import com.exactprosystems.clearth.automation.Scheduler;
import com.exactprosystems.clearth.automation.report.html.RealTimeReport;
import com.exactprosystems.clearth.automation.report.ReportException;
import com.exactprosystems.clearth.utils.ExceptionUtils;
import com.exactprosystems.clearth.utils.TagUtils;
import com.exactprosystems.clearth.web.misc.MessageUtils;
import com.exactprosystems.clearth.web.misc.UserInfoUtils;

import org.apache.commons.lang.StringUtils;

import javax.annotation.PostConstruct;
import javax.faces.context.FacesContext;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.exactprosystems.clearth.automation.matrix.linked.MatrixProvider.STORED_MATRIX_PREFIX;

public class ReportBean
{
	private String userName;
	
	private String reportHead;
	private String reportBody;
	
	@PostConstruct
	private void init()
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
		Map<String, List<String>> matrSteps = Executor.getStepsByMatricesMap(actionsReports);
		checkNames(matrSteps, scheduler);	

		RealTimeReport report;
		try
		{
			report = createReportWriter(matrix, userName, scheduler);
			report.writeReport(scheduler.getSteps(), matrSteps.get(matrixName), actionsReports, false);
		}
		catch (Exception e)
		{
			MessageUtils.addErrorMessage("An error occurred during building report", ExceptionUtils.getDetailedMessage(e));
			return;
		}
		
		String reportHtml = report.getHtmlContent().getBuffer().toString();
		setReportHead(TagUtils.getTagValue("head", reportHtml));
		setReportBody(TagUtils.getTagValue("body", reportHtml));
	}
	
		
	public String getReportHead() throws IOException, ReportException
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
	

	protected Scheduler getScheduler(String name)
	{
		if (StringUtils.isEmpty(name))
		{
			MessageUtils.addErrorMessage("No scheduler selected", "");
			return null;
		}
		
		Scheduler result = ClearThCore.getInstance().getSchedulersManager().getSchedulerByName(name, userName);
		if (result == null)
		{
			MessageUtils.addErrorMessage("No scheduler with name '" + name + "'", "");
			return null;
		}
		
		if (!result.isRunning())
		{
			MessageUtils.addErrorMessage("Scheduler is not running", "");
			return null;
		}
		
		return result;
	}
	
	protected Matrix getMatrix(String name, Scheduler scheduler)
	{
		if (StringUtils.isEmpty(name))
		{
			MessageUtils.addErrorMessage("No matrix selected", "");
			return null;
		}
		
		Matrix result = null;
		for (Matrix m : scheduler.getMatrices())
			if (m.getName().equals(name))
				result = m;
		
		if (result == null)
		{
			MessageUtils.addErrorMessage("No matrix with name '" + name + "'", "");
			return null;
		}
		else if (result.getStarted() == null)
		{
			MessageUtils.addErrorMessage("Matrix '" + name + "' is not running", "");
			return null;
		}
		return result;
	}
	
	protected RealTimeReport createReportWriter(Matrix matrix, String userName, Scheduler scheduler) throws IOException
	{
		return new RealTimeReport(matrix, userName, matrix.getName(), scheduler.getStartTime(), new Date());
	}
	
	private String getLinkedMatrixSrcName(String matrixDirName, Scheduler scheduler)
	{
		for (MatrixData md : scheduler.getMatricesData())
		{
			if (md.isLinked() && md.getFile().getName().equals(matrixDirName))
				return md.getName();
		}
		return null;
	}
	
	private void checkNames(Map<String, List<String>> matrSteps, Scheduler scheduler)
	{
		Set<String> matrDirs = new HashSet<String>(matrSteps.keySet());
		for (String mDir : matrDirs)
		{
			if (!mDir.contains(STORED_MATRIX_PREFIX))
				continue;
			List<String> stepsReports = matrSteps.get(mDir);
			String realName = getLinkedMatrixSrcName(mDir, scheduler);
			matrSteps.put(realName, stepsReports);
		}
	}
}
