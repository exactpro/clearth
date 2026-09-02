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

import static com.exactprosystems.clearth.woodpecker.utils.charts.DatasetType.DYNAMIC;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.exactprosystems.clearth.woodpecker.configuration.daemonsDictionary.DaemonDesc;
import com.exactprosystems.clearth.woodpecker.daemons.WoodpeckerDaemon;
import com.exactprosystems.clearth.woodpecker.utils.charts.BaseLoadingChartBuilder;
import com.exactprosystems.clearth.woodpecker.utils.charts.Dataset;

public class MessageDaemonChartBuilder extends BaseLoadingChartBuilder
{
	private static final String RESP_BY_OPS = "RespByOperations";
	private static final String RESP_BY_MSGS = "RespByMessages";
	private static final String SENT_BY_OPS = "SentByOperations";
	private static final String SENT_BY_MSGS = "SentByMessages";

	private static final String SCHEDULE_NAME = "Loading schedule (%s)";

	
	@Override
	protected List<Dataset> createDatasets(WoodpeckerDaemon daemon)
	{
		MessageDaemonDesc daemonDesc = (MessageDaemonDesc) daemon.getDescription();
		String pluralUnitName = daemonDesc.getPluralUnitName();
		MessageStatisticsSettings statisticsSettings =
				((MessageDaemonDesc) daemon.getDescription()).getStatisticsSettings();
		boolean isCountSent = statisticsSettings.isCountSentMessages();
		boolean isCountResponses = statisticsSettings.isCountResponses();

		List<Dataset> datasets = new ArrayList<>();		
		if (isCountSent)
		{
			datasets.add(new Dataset(SENT_BY_MSGS, sentMessagesLineTitle(isCountResponses), DYNAMIC));
			datasets.add(new Dataset(SENT_BY_OPS, sentOperationsLineTitle(isCountResponses, pluralUnitName), DYNAMIC));
		}
		if (isCountResponses)
		{
			datasets.add(new Dataset(RESP_BY_MSGS, messageResponsesLineTitle(isCountSent), DYNAMIC));
			datasets.add(new Dataset(RESP_BY_OPS, operationResponsesLineTitle(isCountSent, pluralUnitName), DYNAMIC));
		}
		return datasets;
	}

	@Override
	protected String createScheduleLineName(DaemonDesc desc)
	{
		MessageDaemonDesc daemonDesc = (MessageDaemonDesc) desc;
		String pluralUnitName = daemonDesc.getPluralUnitName();
		return format(SCHEDULE_NAME, pluralUnitName);
	}

	
	@Override
	protected Map<String, List<Integer>> getDistributionsByLineName(WoodpeckerDaemon daemon, TimeUnit periodUnit)
	{
		MessageLoadingStatistics statistics = (MessageLoadingStatistics) daemon.getLoadingStatistics();
		
		Map<String, List<Integer>> distributionsByName = new HashMap<>();
		MessageStatisticsSettings statisticsSettings =
				((MessageDaemonDesc) daemon.getDescription()).getStatisticsSettings();

		if (statisticsSettings.isCountSentMessages())
		{
			distributionsByName.put(SENT_BY_MSGS, (periodUnit == SECONDS)
					? statistics.getSentMessagesBySeconds()
					: statistics.getSentMessagesByMinutes());

			distributionsByName.put(SENT_BY_OPS, (periodUnit == SECONDS)
					? statistics.getSentOperationsBySeconds()
					: statistics.getSentOperationsByMinutes());
		}

		if (statisticsSettings.isCountResponses())
		{
			distributionsByName.put(RESP_BY_MSGS, (periodUnit == SECONDS)
					? statistics.getMessageResponsesBySeconds()
					: statistics.getMessageResponsesByMinutes());

			distributionsByName.put(RESP_BY_OPS, (periodUnit == SECONDS)
					? statistics.getOperationResponsesBySeconds()
					: statistics.getOperationResponsesByMinutes());
		}
		
		return distributionsByName;
	}

	
	private String sentMessagesLineTitle(boolean isCountResponses)
	{
		return isCountResponses ? "Sent messages" : "Actual loading (messages)";
	}

	private String sentOperationsLineTitle(boolean isCountResponses, String opName)
	{
		return isCountResponses ? "Sent operations" : format("Actual loading (%s)", opName);
	}

	private String messageResponsesLineTitle(boolean isCountSentMessages)
	{
		return isCountSentMessages ? "Responses for messages" : "Actual loading (messages)";
	}

	private String operationResponsesLineTitle(boolean isCountSentMessages, String opName)
	{
		return isCountSentMessages ? "Responses for operations" : format("Actual loading (%s)", opName);
	}
}
