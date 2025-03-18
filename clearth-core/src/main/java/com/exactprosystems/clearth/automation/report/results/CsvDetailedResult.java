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

package com.exactprosystems.clearth.automation.report.results;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.report.FailReason;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.comparisonwriters.CsvComparisonWriter;
import com.exactprosystems.clearth.automation.report.results.resultReaders.CsvDetailedResultReader;
import com.exactprosystems.clearth.utils.SettingsException;
import com.exactprosystems.clearth.utils.LineBuilder;
import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.utils.tabledata.comparison.ComparisonProcessor;
import com.exactprosystems.clearth.utils.tabledata.comparison.valuesComparators.ValuesComparator;
import com.exactprosystems.clearth.utils.tabledata.converters.ValueParser;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.collections4.list.UnmodifiableList;

import java.io.File;
import java.io.Serializable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CsvDetailedResult extends Result implements AutoCloseable, Serializable
{
	private static final long serialVersionUID = 5699150687216722252L;
	private static final Logger logger = LoggerFactory.getLogger(CsvDetailedResult.class);
	
	public static final String COLUMN_COMPARISON_NAME = "Comparison name",
			COLUMN_COMPARISON_RESULT = "Comparison result", COLUMN_ROW_KIND = "Row kind";
	public static final String PASSED = "PASSED", FAILED = "FAILED", EXPECTED = "EXPECTED", ACTUAL = "ACTUAL";
	protected int maxDisplayedRowsCount = ComparisonProcessor.DEFAULT_MAX_ROWS_TO_SHOW_COUNT,
			minStoredRowsCount = -1,
			maxStoredRowsCount = -1,
			storedRowsCount = 0,
			totalRowsCount = 0,
			passedRowsCount = 0;
	protected boolean writeCsvReportAnyway = false, 
			onlyFailedInHtml = false, 
			onlyFailedInCsv = false,
			listFailedColumns = false;
	protected String name;
	protected String header;
	protected List<DetailedResult> details;
	
	@JsonIgnore
	private CsvComparisonWriter comparisonWriter;
	@JsonIgnore
	@SuppressWarnings("rawtypes")
	protected ValuesComparator valuesComparator;
	@JsonIgnore
	@SuppressWarnings("rawtypes")
	protected ValueParser valueParser;
	
	
	@JsonIgnore
	protected File csvPath,
			tempReportFile = null,
			reportFile = null;
	
	// Empty constructor is required for JSON reports
	public CsvDetailedResult() 
	{
		this(null);
	}
	
	public CsvDetailedResult(String name)
	{
		this(name, new File(ClearThCore.tempPath()));
	}
	
	public CsvDetailedResult(String name, File csvPath)
	{
		this.details = new ArrayList<>();
		this.name = name;
		this.csvPath = csvPath;
	}
	
	
	public void addDetail(DetailedResult detail)
	{
		totalRowsCount++;
		if (detail.isSuccess())
			passedRowsCount++;
		else if (totalRowsCount - passedRowsCount == 1 // The first failed detail
				|| failReason.ordinal() > detail.getFailReason().ordinal())
			failReason = detail.getFailReason();
		
		checkDetails();
		if ((!onlyFailedInHtml && totalRowsCount <= maxDisplayedRowsCount)
				|| (onlyFailedInHtml && !detail.isSuccess() && totalRowsCount - passedRowsCount <= maxDisplayedRowsCount))
			details.add(detail);
		
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

	protected void addToWriter(DetailedResult detail)
	{
		try
		{
			getComparisonWriter().addDetail(detail, detail.getComment());
		}
		catch (IOException e)
		{
			if (getLogger().isDebugEnabled())
				getLogger().error("Error occurred while writing result detail to the report file: " + detail, e);
			else
				getLogger().error("Error occurred while writing result detail to the report file", e);
		}
	}

	@Override
	public void clearDetails()
	{
		details.clear();
	}
	
	/**
	 * Closes writer object used to populate result details to separate CSV report.
	 */
	@Override
	public void close() throws IOException
	{
		Utils.closeResource(comparisonWriter);
	}
	
	protected CsvComparisonWriter getComparisonWriter()
	{
		if (comparisonWriter == null)
			comparisonWriter = createComparisonWriter();
		
		return comparisonWriter;
	}
	
	protected CsvComparisonWriter createComparisonWriter()
	{
		//minStoredRowsCount < 0 is considered as minStoredRowsCount is not set
		//If minStoredRowsCount >= 0, passing minStoredRowsCount-1 as max buffer size for CsvComparisonWriter,
		//e.g. if minStoredRowsCount = 3 and buffer size becomes 2 (i.e. max buffer size), CSV report is not written.
		//When buffer size becomes 3, CSV report is written and buffer is dropped.
		int maxBufferSize = minStoredRowsCount < 0 ? maxDisplayedRowsCount : minStoredRowsCount-1;
		return new CsvComparisonWriter(maxBufferSize, listFailedColumns, csvPath);
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
	public CsvDetailedResultReader getReader() throws SettingsException, IOException
	{
		if (valuesComparator == null)
			throw new SettingsException("Values comparator is required but not provided");
		if (valueParser == null)
			throw new SettingsException("Value parser is required but not provided");
		
		return new CsvDetailedResultReader(reportFile, valuesComparator, valueParser);
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
	
	
	public boolean isListFailedColumns()
	{
		return listFailedColumns;
	}
	
	public void setListFailedColumns(boolean listFailedColumns)
	{
		this.listFailedColumns = listFailedColumns;
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
	
	
	public int getMinStoredRowsCount()
	{
		return minStoredRowsCount;
	}
	
	public void setMinStoredRowsCount(int minStoredRowsCount)
	{
		this.minStoredRowsCount = minStoredRowsCount;
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
	
	public List<DetailedResult> getDetails()
	{
		return UnmodifiableList.unmodifiableList(details);  // This prevents changes in the list that will make details and containers not linked
	}
	
	public void setHeader(String header)
	{
		this.header = header;
	}
	
	public String getHeader()
	{
		return header;
	}

	protected Logger getLogger()
	{
		return logger;
	}

	@Override
	public LineBuilder toLineBuilder(LineBuilder builder, String prefix)
	{
		super.toLineBuilder(builder, prefix);
		builder.add(prefix).add("Total row count: ").add(Integer.toString(totalRowsCount)).eol().add("Details (Identical / Exp. / Act.)").eol();
		for (Result detail : details)
			detail.toLineBuilder(builder, prefix + " ");
		return builder;
	}
}
