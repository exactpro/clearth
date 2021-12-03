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

package com.exactprosystems.clearth.converters;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.exactprosystems.clearth.automation.ActionGenerator;
import com.exactprosystems.clearth.automation.actions.MessageAction;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.utils.Pair;

/**
 * Fills given message with fields from strings and maps
 * @author vladimir.panarin
 */
public class MessageFiller
{
	protected final Set<String> matrixServiceFields = new HashSet<String>();

	public MessageFiller()
	{
		matrixServiceFields.add(ActionGenerator.COLUMN_ACTION);
		matrixServiceFields.add(ActionGenerator.COLUMN_COMMENT);
		matrixServiceFields.add(ActionGenerator.COLUMN_GLOBALSTEP);
		matrixServiceFields.add(ActionGenerator.COLUMN_ID);
		matrixServiceFields.add(ActionGenerator.COLUMN_INVERT);
		matrixServiceFields.add(ActionGenerator.COLUMN_EXECUTE);
		matrixServiceFields.add(ActionGenerator.COLUMN_TIMEOUT);
		matrixServiceFields.add(MessageAction.CONNECTIONNAME.toLowerCase());
		matrixServiceFields.add(MessageAction.REPEATINGGROUPS.toLowerCase());
	}


	protected boolean isColumnToSkip(String columnName, List<String> includeList)
	{
		if (!columnName.startsWith(ActionGenerator.HEADER_DELIMITER))
			return true;

		String h = columnName.substring(1).toLowerCase();
		return matrixServiceFields.contains(h) && ((includeList == null) || (!includeList.contains(h)));
	}

	/**
	 * Fills message with values from script
	 * @param message to fill
	 * @param header line of script
	 * @param values line of script
	 * @param includeList script parameters that should be included in message anyway, even if they are service ones
	 */
	public void fillByHeaderAndValues(ClearThMessage<?> message, String[] header, String[] values, List<String> includeList)
	{

		for (int i = 0; i < header.length; i++)
		{
			String h = header[i];
			if (isColumnToSkip(h, includeList))
				continue;

			if (!h.isEmpty())
			{
				h = h.substring(1);
				if (h.equalsIgnoreCase(ActionGenerator.COLUMN_ID))
				{
					h = ActionGenerator.COLUMN_ID;
				}
			}

			message.addField(h, values[i]);
		}
	}

	/**
	 * Converts values line from script to Map
	 * @param header line of script
	 * @param values line of script
	 * @param includeList script parameters that should be included in message anyway, even if they are service ones
	 * @return Map of script values with saved order and without service and omitted parameters
	 */
	public Map<String, String> fillMapByHeaderAndValues(String[] header, String[] values, List<String> includeList)
	{
		Map<String, String> result = new LinkedHashMap<String, String>();
		for (int i = 0; i < header.length; i++)
		{
			String h = header[i];
			if (isColumnToSkip(h, includeList))
				continue;

			result.put(h.substring(1), values[i]);
		}
		return result;
	}

	/**
	 * Fills main fields of message (i.e. not fields in RGs) with default action parameter values and values from script
	 * @param message to fill
	 * @param actionParams default parameter values. Will be overridden with values from mainEntry
	 * @param mainEntry header and values lines of a script
	 */
	public void fillMainFields(ClearThMessage<?> message, Map<String, String> actionParams, Pair<String[], String[]> mainEntry)
	{
		//Setting-up the fields from default action parameters
		for (Entry<String, String> field : actionParams.entrySet())
			message.addField(field.getKey(), field.getValue());

		//Setting-up the fields from script, i.e. this may override default parameters
		fillByHeaderAndValues(message, mainEntry.getFirst(), mainEntry.getSecond(), null);
	}
}
