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

import com.exactprosystems.clearth.woodpecker.daemons.WoodpeckerDaemon;
import com.exactprosystems.clearth.woodpecker.statistics.LoadingStatistics;
import com.exactprosystems.clearth.woodpecker.utils.RateMeter;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

import org.apache.commons.collections4.list.UnmodifiableList;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MINUTES;

public class MessageLoadingStatistics extends LoadingStatistics
{
	private final MessageStatisticsSettings settings;

	// Distribution by seconds
	private final List<Integer> sentOperationsBySeconds;
	private final List<Integer> sentMessagesBySeconds;
	private final List<Integer> operationResponsesBySeconds;
	private final List<Integer> messageResponsesBySeconds;
	// Distribution by minutes
	private final List<Integer> sentOperationsByMinutes;
	private final List<Integer> sentMessagesByMinutes;
	private final List<Integer> operationResponsesByMinutes;
	private final List<Integer> messageResponsesByMinutes;

	// Seconds
	private final AtomicInteger sentOpsInSecondCounter;
	private final AtomicInteger sentMsgsInSecondCounter;
	private final AtomicInteger opsRespInSecondCounter;
	private final AtomicInteger msgsRespInSecondCounter;
	// Minutes
	private final AtomicInteger sentOpsInMinuteCounter;
	private final AtomicInteger sentMsgsInMinuteCounter;
	private final AtomicInteger opsRespInMinuteCounter;
	private final AtomicInteger msgsRespInMinuteCounter;

	private final LongAdder totalSentOperations = new LongAdder();
	private final LongAdder totalSentMessages = new LongAdder();

	private final List<String> operationNames = new ArrayList<>();
	private final Map<String, LongAdder> totalSentOperationsByName = new HashMap<>();
	private final Map<String, LongAdder> totalSentMessagesByOpName = new HashMap<>();

	private final RateMeter opsRateMeter = new RateMeter(1000);
	private final RateMeter msgsRateMeter = new RateMeter(1000);

	private volatile double opsRateInSecond;
	private volatile double opsRateInMinute;

	private volatile double msgsRateInSecond;
	private volatile double msgsRateInMinute;


	public MessageLoadingStatistics(WoodpeckerDaemon daemon, Collection<String> operationNames)
	{
		requireNonNull(daemon, "daemon");
		requireNonNull(operationNames, "operationNames");

		this.operationNames.addAll(operationNames);

		for (String name : operationNames)
		{
			totalSentMessagesByOpName.put(name, new LongAdder());
			totalSentOperationsByName.put(name, new LongAdder());
		}

		settings = ((MessageDaemonDesc) daemon.getDescription()).getStatisticsSettings();

		boolean countSent = settings.isCountSentMessages();
		sentOperationsBySeconds = initDistribution(countSent);
		sentMessagesBySeconds = initDistribution(countSent);
		sentOperationsByMinutes = initDistribution(countSent);
		sentMessagesByMinutes = initDistribution(countSent);
		sentOpsInSecondCounter = initCounter(countSent);
		sentMsgsInSecondCounter = initCounter(countSent);
		sentOpsInMinuteCounter = initCounter(countSent);
		sentMsgsInMinuteCounter = initCounter(countSent);

		boolean countResponses = settings.isCountResponses();
		operationResponsesBySeconds = initDistribution(countResponses);
		messageResponsesBySeconds = initDistribution(countResponses);
		operationResponsesByMinutes = initDistribution(countResponses);
		messageResponsesByMinutes = initDistribution(countResponses);
		opsRespInSecondCounter = initCounter(countResponses);
		msgsRespInSecondCounter = initCounter(countResponses);
		opsRespInMinuteCounter = initCounter(countResponses);
		msgsRespInMinuteCounter = initCounter(countResponses);
	}

	protected List<Integer> initDistribution(boolean notEmpty)
	{
		return notEmpty ? new CopyOnWriteArrayList<>() : emptyList();
	}

	protected AtomicInteger initCounter(boolean isUsed)
	{
		return isUsed ? new AtomicInteger() : null;
	}


	public List<Integer> getSentOperationsBySeconds()
	{
		return UnmodifiableList.unmodifiableList(sentOperationsBySeconds);
	}

	public List<Integer> getSentMessagesBySeconds()
	{
		return UnmodifiableList.unmodifiableList(sentMessagesBySeconds);
	}

	public List<Integer> getOperationResponsesBySeconds()
	{
		return UnmodifiableList.unmodifiableList(operationResponsesBySeconds);
	}

	public List<Integer> getMessageResponsesBySeconds()
	{
		return UnmodifiableList.unmodifiableList(messageResponsesBySeconds);
	}

	public List<Integer> getSentOperationsByMinutes()
	{
		return UnmodifiableList.unmodifiableList(sentOperationsByMinutes);
	}

	public List<Integer> getSentMessagesByMinutes()
	{
		return UnmodifiableList.unmodifiableList(sentMessagesByMinutes);
	}

	public List<Integer> getOperationResponsesByMinutes()
	{
		return UnmodifiableList.unmodifiableList(operationResponsesByMinutes);
	}

	public List<Integer> getMessageResponsesByMinutes()
	{
		return UnmodifiableList.unmodifiableList(messageResponsesByMinutes);
	}


	public long getTotalSentOperations()
	{
		return totalSentOperations.longValue();
	}

	public long getTotalSentMessages()
	{
		return totalSentMessages.longValue();
	}


	public List<String> getOperationNames()
	{
		return operationNames;
	}

	public long getTotalSentOperationsByName(String operationName)
	{
		return getTotalSentByOpName(operationName, totalSentOperationsByName);
	}

	public long getTotalSentMessagesByOperationName(String operationName)
	{
		return getTotalSentByOpName(operationName, totalSentMessagesByOpName);
	}

	private long getTotalSentByOpName(String operationName, Map<String, LongAdder> counters)
	{
		LongAdder counter = counters.get(operationName);
		return (counter != null) ? counter.sum() : 0;
	}


	public double getOpsRateInSecond()
	{
		return opsRateInSecond;
	}

	public double getOpsRateInMinute()
	{
		return opsRateInMinute;
	}

	public double getMsgsRateInSecond()
	{
		return msgsRateInSecond;
	}

	public double getMsgsRateInMinute()
	{
		return msgsRateInMinute;
	}


	public void incSentMessages(String operationName)
	{
		addSentMessages(operationName, 1);
	}

	public void incMessageResponses(String operationName)
	{
		addMessageResponses(operationName, 1);
	}

	public void incSentOperations(String operationName)
	{
		addSentOperations(operationName, 1);
	}

	public void incOperationResponses(String operationName)
	{
		addOperationResponses(operationName, 1);
	}

	public void addSentMessages(String operationName, int count)
	{
		if (settings.isCountSentMessages())
		{
			sentMsgsInSecondCounter.addAndGet(count);
			if (!settings.isCountResponses())
				addMessages(operationName, count);
		}
	}

	public void addMessageResponses(String operationName, int count)
	{
		if (settings.isCountResponses())
		{
			msgsRespInSecondCounter.addAndGet(count);
			addMessages(operationName, count);
		}
	}

	private void addMessages(String operationName, int count)
	{
		totalSentMessages.add(count);
		updateCounterByOpName(operationName, totalSentMessagesByOpName, count);
		msgsRateMeter.update(count);
	}

	public void addSentOperations(String operationName, int count)
	{
		if (settings.isCountSentMessages())
		{
			sentOpsInSecondCounter.addAndGet(count);
			if (!settings.isCountResponses())
				addOperations(operationName, count);
		}
	}

	public void addOperationResponses(String operationName, int count)
	{
		if (settings.isCountResponses())
		{
			opsRespInSecondCounter.addAndGet(count);
			addOperations(operationName, count);
		}
	}

	private void addOperations(String operationName, int count)
	{
		totalSentOperations.add(count);
		updateCounterByOpName(operationName, totalSentOperationsByName, count);
		opsRateMeter.update(count);
	}

	private void updateCounterByOpName(String operationName, Map<String, LongAdder> counters, int count)
	{
		LongAdder counter = counters.get(operationName);
		if (counter != null)
			counter.add(count);
	}


	@Override
	protected void update(TimeUnit periodUnit)
	{
		updateAllDistributions(periodUnit);
		updateRates();
	}

	private void updateAllDistributions(TimeUnit periodUnit)
	{
		if (settings.isCountSentMessages())
		{
			updateDistributions(periodUnit, sentMsgsInSecondCounter, sentMsgsInMinuteCounter,
					sentMessagesBySeconds, sentMessagesByMinutes);
			updateDistributions(periodUnit, sentOpsInSecondCounter, sentOpsInMinuteCounter,
					sentOperationsBySeconds, sentOperationsByMinutes);
		}
		if (settings.isCountResponses())
		{
			updateDistributions(periodUnit, msgsRespInSecondCounter, msgsRespInMinuteCounter,
					messageResponsesBySeconds, messageResponsesByMinutes);
			updateDistributions(periodUnit, opsRespInSecondCounter, opsRespInMinuteCounter,
					operationResponsesBySeconds, operationResponsesByMinutes);
		}
	}

	private void updateDistributions(TimeUnit periodUnit, AtomicInteger counterInSecond, AtomicInteger counterInMinute,
	                                 List<Integer> distributionBySeconds, List<Integer> distributionByMinutes)
	{
		int count = counterInSecond.getAndSet(0);
		distributionBySeconds.add(count);
		counterInMinute.addAndGet(count);
		if (periodUnit == MINUTES)
			distributionByMinutes.add(counterInMinute.getAndSet(0));
	}

	private void updateRates()
	{
		opsRateInSecond = opsRateMeter.calculateRateInSecond();
		opsRateInMinute = opsRateMeter.calculateRateInMinute();

		msgsRateInSecond = msgsRateMeter.calculateRateInSecond();
		msgsRateInMinute = msgsRateMeter.calculateRateInMinute();
	}
}
