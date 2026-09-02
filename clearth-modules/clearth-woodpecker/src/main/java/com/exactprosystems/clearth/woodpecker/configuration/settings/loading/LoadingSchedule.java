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

package com.exactprosystems.clearth.woodpecker.configuration.settings.loading;

import static com.exactprosystems.clearth.woodpecker.utils.WoodpeckerMathUtils.OUTPUT_NUMBER_PATTERN;
import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.exactprosystems.clearth.utils.CommaBuilder;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerSettingsException;


public class LoadingSchedule implements Iterable<LoadingSchedule.Period>
{
	private final List<Period> periods = new ArrayList<>();
	
	
	public void addPeriod(double rate, int duration)
	{
		periods.add(new Period(rate, duration));
	}
	
	
	@Override
	public Iterator<Period> iterator()
	{
		return periods.iterator();
	}
	
	public int getPeriodsCount()
	{
		return periods.size();
	}
	
	public Period getPeriod(int index)
	{
		return periods.get(index);
	}
	
	
	@Override
	public String toString()
	{
		CommaBuilder cb = new CommaBuilder(" ");
		for (Period p : periods)
		{
			cb.append(p);
		}
		return cb.toString();
	}


	public static LoadingSchedule parse(String periodsStr) throws WoodpeckerSettingsException
	{
		LoadingSchedule schedule = new LoadingSchedule();
		if (isBlank(periodsStr))
			return schedule;

		String[] textPeriods = periodsStr.split("\\s+");
		int periodIndex = 1;
		for (String textPeriod : textPeriods)
		{
			if (isBlank(textPeriod))
				continue;
			schedule.periods.add(parsePeriod(textPeriod, periodIndex));
			periodIndex++;
		}
		return schedule;
	}
	
	private static Period parsePeriod(String textValue, int periodIndex) throws WoodpeckerSettingsException
	{
		textValue = textValue.trim();
		int lastIndex = textValue.length() - 1;
		
		int slashIndex = textValue.indexOf('/');
		if ((slashIndex == -1) || (slashIndex == 0) || (slashIndex == lastIndex))
			throw new WoodpeckerSettingsException("Period #%d has unexpected format - [%s]. " +
					"Valid format is [rate/duration]. Use spaces to separate periods.", periodIndex, textValue);
		
		String rateText = textValue.substring(0, slashIndex);
		String durationText = textValue.substring(slashIndex + 1);
		
		double rate;
		try
		{
			rate = parseDouble(rateText);
		}
		catch (NumberFormatException ignore)
		{
			throw new WoodpeckerSettingsException("Rate [%s] in period #%d [%s] should be integer or decimal with '.'. " +
					"Period format is [rate/duration]. Use spaces to separate periods.", 
					rateText, periodIndex, textValue);
		}
		
		int duration;
		try
		{
			duration = parseInt(durationText);
		}
		catch (NumberFormatException ignore)
		{
			throw new WoodpeckerSettingsException("Duration [%s] in period #%d [%s] should be integer number. " +
					"Period format is [rate/duration]. Use spaces to separate periods.",
					durationText, periodIndex, textValue);
		}
		
		return new Period(rate, duration);
	}


	public static class Period
	{
		private final double rate;
		private final int duration;

		public Period(double rate, int duration)
		{
			this.rate = rate;
			this.duration = duration;
		}

		public double getRate()
		{
			return rate;
		}

		public int getDuration()
		{
			return duration;
		}
		
		@Override
		public String toString()
		{
			DecimalFormat rateFormat = new DecimalFormat(OUTPUT_NUMBER_PATTERN);
			return rateFormat.format(rate) + '/' + duration;
		}
	}
}
