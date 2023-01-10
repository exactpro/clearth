/******************************************************************************
 * Copyright 2009-2023 Exactpro Systems Limited
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

import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.report.FailReason;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.comparisonwriters.ComparisonWriter;
import com.exactprosystems.clearth.automation.report.comparisonwriters.CsvComparisonWriter;
import com.exactprosystems.clearth.automation.report.results.resultReaders.CsvContainerResultReader;
import com.exactprosystems.clearth.utils.SettingsException;
import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.utils.tabledata.comparison.valuesComparators.ValuesComparator;
import com.exactprosystems.clearth.utils.tabledata.converters.ValueParser;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class CsvContainerResult extends ContainerResult implements AutoCloseable
{
	private static final long serialVersionUID = 5699150687216722251L;
	private static final Logger logger = LoggerFactory.getLogger(CsvContainerResult.class);
	
	public static final String COLUMN_COMPARISON_NAME = "Comparison name",
			COLUMN_COMPARISON_RESULT = "Comparison result", COLUMN_ROW_KIND = "Row kind";
	public static final String PASSED = "PASSED", FAILED = "FAILED", EXPECTED = "EXPECTED", ACTUAL = "ACTUAL";
	protected int maxDisplayedRowsCount = 50,
			maxStoredRowsCount = -1,
			storedRowsCount = 0,
			totalRowsCount = 0,
			passedRowsCount = 0;
	protected boolean writeCsvReportAnyway = false, onlyFailedInHtml = false, onlyFailedInCsv = false;
	protected String name;
	
	@JsonIgnore
	private ComparisonWriter<ContainerResult> comparisonWriter;
	@SuppressWarnings("rawtypes")
	protected ValuesComparator valuesComparator;
	@JsonIgnore
	@SuppressWarnings("rawtypes")
	protected ValueParser valueParser;
	
	
	@JsonIgnore
	protected File tempReportFile = null,
			reportFile = null;
	
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
		else if (totalRowsCount - passedRowsCount == 1 // The first failed detail
				|| failReason.ordinal() > detail.getFailReason().ordinal())
			failReason = detail.getFailReason();
		
		if ((!onlyFailedInHtml && totalRowsCount <= maxDisplayedRowsCount)
				|| (onlyFailedInHtml && !detail.isSuccess() && totalRowsCount - passedRowsCount <= maxDisplayedRowsCount))
			super.addDetail(detail);
		
		if (shouldBeStored(detail))
		{
			addToWriter(detail);
			storedRowsCount++;
		}
	}
	
	protected boolean shouldBeStored(Result detail)
	{
		// check if max limit to store reached
		if (maxStoredRowsCount > 0 && storedRowsCount >= maxStoredRowsCount)
			return false;
		
		return !detail.isSuccess() || !onlyFailedInCsv;
	}

	@Override
	protected boolean checkDetails()
	{
		return success = passedRowsCount == totalRowsCount;
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
		if (comparisonWriter != null)
		{
			try
			{
				boolean writeReportAnyway = isWriteCsvReportAnyway() || (isOnlyFailedInHtml() && passedRowsCount > 0);
				
				Path reportPath = comparisonWriter.finishReport(reportDir.toPath(), getReportFileNamePrefix(linkedAction),
						getReportFileNameSuffixNoExtension(), writeReportAnyway);
				if (reportPath != null)
				{
					reportFile = reportPath.toFile();
					headerMsg += buildDownloadLink(reportFile);
				}
			}
			catch (IOException e)
			{
				getLogger().error("Couldn't move written report file to necessary path", e);
				headerMsg += "<br>Error occurred while saving report file. Please, see logs for details.";
			}
		}
		setHeader(headerMsg);
	}

	protected void addToWriter(Result detail)
	{
		try
		{
			getComparisonWriter().addDetail((ContainerResult) detail);
		}
		catch (IOException e)
		{
			if (getLogger().isDebugEnabled())
				getLogger().error("Error occurred while writing result detail to the report file: " + detail, e);
			else
				getLogger().error("Error occurred while writing result detail to the report file", e);
		}
	}
	
	/**
	 * Closes writer object used to populate result details to separate CSV report.
	 */
	@Override
	public void close() throws IOException
	{
		Utils.closeResource(comparisonWriter);
	}
	
	protected ComparisonWriter<ContainerResult> getComparisonWriter()
	{
		if (comparisonWriter == null)
			comparisonWriter = createComparisonWriter();
		
		return comparisonWriter;
	}
	
	protected ComparisonWriter<ContainerResult> createComparisonWriter()
	{
		int maxBufferSize = maxDisplayedRowsCount;
		return new CsvComparisonWriter(maxBufferSize);
	}
	
	protected String buildHeader()
	{
		return String.format("Total rows: %d / Displayed: %d / Stored: %d",
				totalRowsCount,
				Math.min(totalRowsCount, maxDisplayedRowsCount),
				getStoredRowsCount());
	}
	
	protected String getReportFileNamePrefix(Action linkedAction)
	{
		return (linkedAction == null ? "noStep_noId_noAction" :
				String.format("%s_%s_%s", linkedAction.getStepName(), linkedAction.getIdInMatrix(),
						linkedAction.getName()) + "_");
	}
	
	protected String getReportFileNameSuffixNoExtension()
	{
		return "_" + name + (onlyFailedInCsv ? "_(onlyfailed)" : "");
	}

	protected String buildDownloadLink(File file) throws IOException
	{
		return "<br>Zip-archive with CSV report inside could be downloaded " +
				"<a href=\"" + file.getParentFile().getName() + File.separator + file.getName() + "\">here</a>";
	}
	
	@JsonIgnore
	@SuppressWarnings("unchecked")
	public CsvContainerResultReader getReader() throws SettingsException, IOException
	{
		if (valuesComparator == null)
			throw new SettingsException("Values comparator is required but not provided");
		if (valueParser == null)
			throw new SettingsException("Value parser is required but not provided");
		
		return new CsvContainerResultReader(reportFile, valuesComparator, valueParser);
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
	
	public void setName(String name)
	{
		this.name = name;
	}
	
	public String getName()
	{
		return name;
	}
	
	
	public int getTotalRowsCount()
	{
		return totalRowsCount;
	}
	
	public int getPassedRowsCount()
	{
		return passedRowsCount;
	}

	public int getStoredRowsCount()
	{
		return storedRowsCount;
	}

	public int getMaxStoredRowsCount()
	{
		return maxStoredRowsCount;
	}

	public void setMaxStoredRowsCount(int maxStoredRowsCount)
	{
		this.maxStoredRowsCount = maxStoredRowsCount;
	}
	
	public File getTempReportFile()
	{
		return tempReportFile;
	}
	
	public File getReportFile()
	{
		return reportFile;
	}

	public<A, B> void setValueHandlers(ValuesComparator<A, B> valuesComparator, ValueParser<A, B> valueParser)
	{
		this.valuesComparator = valuesComparator;
		this.valueParser = valueParser;
	}

	protected Logger getLogger()
	{
		return logger;
	}
}
