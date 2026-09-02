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

package com.exactprosystems.clearth.woodpecker.daemons.message;

import java.io.IOException;

import org.apache.commons.csv.CSVPrinter;

import com.exactprosystems.clearth.woodpecker.configuration.settings.DaemonSettings;
import com.exactprosystems.clearth.woodpecker.reports.BaseDaemonExecutionReportWriter;
import com.exactprosystems.clearth.woodpecker.statistics.LoadingStatistics;

import static java.lang.String.format;

public class MessageDaemonExecutionReportWriter extends BaseDaemonExecutionReportWriter
{

	@Override
	protected void writeAdditionalSettings(CSVPrinter csvPrinter, DaemonSettings ds) throws IOException
	{
		MessageDaemonSettings settings = (MessageDaemonSettings) ds;

		writeRow(csvPrinter, "Min batch size", settings.getMinBatchSize());
		writeRow(csvPrinter, "Max batch size", settings.getMaxBatchSize());
		writeRow(csvPrinter, "Use strict min batch size", settings.isUseStrictMinBatchSize());
	}

	@Override
	protected void writeStatistics(CSVPrinter csvPrinter, LoadingStatistics ls) throws IOException
	{
		MessageLoadingStatistics statistics = (MessageLoadingStatistics) ls;

		writeRow(csvPrinter, "Total sent operations", statistics.getTotalSentOperations());
		writeRow(csvPrinter, "Total sent messages", statistics.getTotalSentMessages());

		for (String operationName : statistics.getOperationNames())
		{
			writeRow(csvPrinter, format("%s operations", operationName),
					statistics.getTotalSentOperationsByName(operationName));
			writeRow(csvPrinter, format("%s messages", operationName),
					statistics.getTotalSentMessagesByOperationName(operationName));
		}
	}
}
