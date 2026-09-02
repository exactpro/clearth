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

package com.exactprosystems.clearth.woodpecker.daemons.action;

import com.exactprosystems.clearth.woodpecker.statistics.LoadingStatistics;
import com.exactprosystems.clearth.woodpecker.utils.RateMeter;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

import org.apache.commons.collections4.list.UnmodifiableList;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MINUTES;

public class ActionLoadingStatistics extends LoadingStatistics
{
	private final List<String> actionsNames = new ArrayList<>();

	private final AtomicInteger actionsInSecondCounter = new AtomicInteger();
	private final List<Integer> actionsBySecondDistribution = initDistribution();
	
	private final AtomicInteger actionsInMinuteCounter = new AtomicInteger();
	private final List<Integer> actionsByMinuteDistribution = initDistribution();
	
	private final LongAdder totalActionsCounter = new LongAdder();
	private final Map<String, LongAdder> totalActionsByName = new HashMap<>();
	
	private final RateMeter rateMeter = new RateMeter(1000);
	private volatile double rateInSecond;
	private volatile double rateInMinute;


	public ActionLoadingStatistics(Collection<String> actionNames)
	{
		requireNonNull(actionNames, "actionNames");
		
		this.actionsNames.addAll(actionNames);
		
		for (String name : actionNames)
		{
			totalActionsByName.put(name, new LongAdder());
		}
	}
	
	
	protected List<Integer> initDistribution()
	{
		return new CopyOnWriteArrayList<>();
	}


	public List<String> getActionsNames()
	{
		return actionsNames;
	}

	public List<Integer> getActionsBySecondDistribution()
	{
		return UnmodifiableList.unmodifiableList(actionsBySecondDistribution);
	}

	public List<Integer> getActionsByMinuteDistribution()
	{
		return UnmodifiableList.unmodifiableList(actionsByMinuteDistribution);
	}

	public long getTotalActionsCount()
	{
		return totalActionsCounter.longValue();
	}
	
	public long getTotalActionsByName(String actionName)
	{
		LongAdder counter = totalActionsByName.get(actionName);
		return (counter != null) ? counter.sum() : 0;
	}

	public double getRateInSecond()
	{
		return rateInSecond;
	}

	public double getRateInMinute()
	{
		return rateInMinute;
	}
	
	
	public void incActions(String actionName)
	{
		actionsInSecondCounter.incrementAndGet();
		totalActionsCounter.increment();
		
		LongAdder counter = totalActionsByName.get(actionName);
		if (counter != null)
			counter.increment();
		
		rateMeter.update(1);
	}
	

	@Override
	protected void update(TimeUnit periodUnit)
	{
		int count = actionsInSecondCounter.getAndSet(0);
		actionsBySecondDistribution.add(count);
		
		actionsInMinuteCounter.addAndGet(count);
		if (periodUnit == MINUTES)
			actionsByMinuteDistribution.add(actionsInMinuteCounter.getAndSet(0));
		
		rateInSecond = rateMeter.calculateRateInSecond();
		rateInMinute = rateMeter.calculateRateInMinute();
	}
}
