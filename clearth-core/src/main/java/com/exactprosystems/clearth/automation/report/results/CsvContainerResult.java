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

package com.exactprosystems.clearth.automation.report.results;

import com.csvreader.CsvWriter;
import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.report.FailReason;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.ResultDetail;
import com.exactprosystems.clearth.utils.FileOperationUtils;
import com.exactprosystems.clearth.utils.Utils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@JsonIgnoreProperties({ "logger", "failReason", "tempReportFile", "csvWriter" })
public class CsvContainerResult extends ContainerResult implements AutoCloseable
{
	private static final long serialVersionUID = 5699150687216722251L;
	
	protected static final Logger logger = LoggerFactory.getLogger(CsvContainerResult.class);
	
	@JsonIgnore
	protected final String COLUMN_COMPARISON_NAME = "Comparison name",
			COLUMN_COMPARISON_RESULT = "Comparison result", COLUMN_ROW_KIND = "Row kind";
	
	protected int maxDisplayedRowsCount = 50, totalRowsCount = 0, passedRowsCount = 0;
	protected boolean writeCsvReportAnyway = false, onlyFailedInHtml = false, onlyFailedInCsv = false;
	
	protected File tempReportFile = null;
	private CsvWriter csvWriter = null;
	private String name;
	
	// Empty constructor is required for JSON-reports
	public CsvContainerResult()
	{
		super();
	}

	public CsvContainerResult(String name)
	{
		super();
		this.name = name;
	}

	protected CsvContainerResult(String header, boolean isBlockView)
	{
		super(header, isBlockView);
	}
	
	protected CsvContainerResult(String header, boolean isBlockView, String name)
	{
		super(header, isBlockView);
		this.name = name;
	}

	public static CsvContainerResult createPlainResult()
	{
		return new CsvContainerResult();
	}
	
	public static CsvContainerResult createPlainResult(String name)
	{
		return new CsvContainerResult(null, false, name);
	}

	public static CsvContainerResult createBlockResult(String header)
	{
		return new CsvContainerResult(header, true);
	}
	
	public static CsvContainerResult createBlockResult(String header, String name)
	{
		return new CsvContainerResult(header, true, name);
	}
	
	
	@Override
	public void addDetail(Result detail)
	{
		totalRowsCount++;
		if (detail.isSuccess())
			passedRowsCount++;
		else if (totalRowsCount - passedRowsCount == 1)
			setFailReason(detail.getFailReason());
		
		if ((!onlyFailedInHtml && totalRowsCount <= maxDisplayedRowsCount)
				|| (onlyFailedInHtml && !detail.isSuccess() && totalRowsCount - passedRowsCount <= maxDisplayedRowsCount))
			super.addDetail(detail);
		if (!onlyFailedInCsv || !detail.isSuccess())
			writeDetailToReportFile(detail);
	}
	
	@Override
	protected boolean checkDetails()
	{
		if (success)
			setSuccess(passedRowsCount == totalRowsCount);
		return success;
	}
	
	@Override
	public FailReason getFailReason()
	{
		return failReason;
	}
	
	@Override
	public void processDetails(File reportDir, Action linkedAction)
	{
		String headerMsg = buildHeader();
		
		if (tempReportFile != null)
		{
			if (writeCsvReportAnyway || (onlyFailedInHtml && (totalRowsCount - passedRowsCount > maxDisplayedRowsCount || passedRowsCount != 0))
					|| totalRowsCount > maxDisplayedRowsCount)
			{
				try
				{
					Path targetFilePath = createTargetFilePath(reportDir, linkedAction);
					Files.move(tempReportFile.toPath(), targetFilePath, StandardCopyOption.REPLACE_EXISTING);
					headerMsg += processWrittenFile(targetFilePath.toFile());
				}
				catch (IOException e)
				{
					logger.error("Couldn't move written CSV report file to necessary path", e);
					headerMsg += "<br>Error occurred while saving CSV report file. Please, see logs for details.";
				}
			}
			else
				tempReportFile.delete();
		}
		
		setHeader(headerMsg);
	}
	
	/**
	 * Closes writer object used to populate result details to separate CSV report.
	 */
	@Override
	public void close() throws IOException
	{
		Utils.closeResource(csvWriter);
	}
	
	
	protected String buildHeader()
	{
		return "Total rows: " + totalRowsCount + " / Displayed: " + Math.min(totalRowsCount, maxDisplayedRowsCount);
	}
	
	protected void writeDetailToReportFile(Result detail)
	{
		try
		{
			csvWriter = getCsvReportWriter();
			ContainerResult mainContainer = (ContainerResult)detail;
			DetailedResult detailedResult = (DetailedResult)mainContainer.getDetails().get(0);
			List<ResultDetail> resultDetails = detailedResult.getResultDetails();
			
			// Write header only if it's a first comparison row in file
			if (!onlyFailedInCsv && totalRowsCount == 1 || onlyFailedInCsv && totalRowsCount - passedRowsCount == 1)
			{
				csvWriter.write(COLUMN_COMPARISON_NAME);
				csvWriter.write(COLUMN_COMPARISON_RESULT);
				csvWriter.write(COLUMN_ROW_KIND);
				for (ResultDetail rd : resultDetails)
					csvWriter.write(rd.getParam());
				csvWriter.endRecord();
			}
			String compResult = detailedResult.isSuccess() ? "PASSED" : "FAILED";
			// Write expected comparison data
			csvWriter.write(mainContainer.getHeader());
			csvWriter.write(compResult);
			csvWriter.write("EXPECTED");
			for (ResultDetail rd : resultDetails)
				csvWriter.write(rd.getExpected());
			csvWriter.endRecord();
			// Write actual comparison data
			csvWriter.write(mainContainer.getHeader());
			csvWriter.write(compResult);
			csvWriter.write("ACTUAL");
			for (ResultDetail rd : resultDetails)
				csvWriter.write(rd.getActual());
			csvWriter.endRecord();
		}
		catch (IOException e)
		{
			logger.error("Error occurred while writing next result detail to the CSV report file", e);
		}
	}
	
	protected CsvWriter getCsvReportWriter() throws IOException
	{
		if (tempReportFile == null || csvWriter == null)
		{
			tempReportFile = Files.createTempFile(Paths.get(ClearThCore.tempPath()), "csvcontainerresult_report_", ".csv").toFile();
			csvWriter = new CsvWriter(new BufferedWriter(new FileWriter(tempReportFile)), ',');
		}
		return csvWriter;
	}
	
	protected Path createTargetFilePath(File reportDir, Action linkedAction) throws IOException
	{
		File detailsDir = new File(reportDir, DETAILS_DIR);
		detailsDir.mkdirs();
		return Files.createTempFile(detailsDir.toPath(), (linkedAction != null ? linkedAction.getStepName() + "_" + linkedAction.getIdInMatrix() + "_" + linkedAction.getName()
				: "noStep_noId_noAction") + "_", "_" + name + (onlyFailedInCsv ? "_(onlyfailed)" : "") + ".csv");
	}
	
	protected String processWrittenFile(File writtenFile) throws IOException
	{
		File archivedResult = new File(writtenFile.getParentFile(), FilenameUtils.removeExtension(writtenFile.getName()) + ".zip");
		FileOperationUtils.zipFiles(archivedResult, new File[] { writtenFile });
		writtenFile.delete();
		return "<br>Zip-archive with CSV report inside could be downloaded " +
				"<a href=\"" + archivedResult.getParentFile().getName() + File.separator + archivedResult.getName() + "\">here</a>";
	}
	
	
	public void setMaxDisplayedRowsCount(int maxDisplayedRowsCount)
	{
		this.maxDisplayedRowsCount = maxDisplayedRowsCount;
	}
	
	public int getMaxDisplayedRowsCount()
	{
		return maxDisplayedRowsCount;
	}
	
	public void setWriteCsvReportAnyway(boolean writeCsvReportAnyway)
	{
		this.writeCsvReportAnyway = writeCsvReportAnyway;
	}
	
	public boolean isWriteCsvReportAnyway()
	{
		return writeCsvReportAnyway;
	}
	
	public void setOnlyFailedInHtml(boolean onlyFailedInHtml)
	{
		this.onlyFailedInHtml = onlyFailedInHtml;
	}
	
	public boolean isOnlyFailedInHtml()
	{
		return onlyFailedInHtml;
	}
	
	public void setOnlyFailedInCsv(boolean onlyFailedInCsv)
	{
		this.onlyFailedInCsv = onlyFailedInCsv;
	}
	
	public boolean isOnlyFailedInCsv()
	{
		return onlyFailedInCsv;
	}
	
	
	public int getTotalRowsCount()
	{
		return totalRowsCount;
	}
	
	public int getPassedRowsCount()
	{
		return passedRowsCount;
	}
	
	
	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}
}
