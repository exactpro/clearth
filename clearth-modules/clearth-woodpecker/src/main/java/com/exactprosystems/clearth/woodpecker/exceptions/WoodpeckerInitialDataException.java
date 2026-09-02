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

package com.exactprosystems.clearth.woodpecker.exceptions;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import static com.exactprosystems.clearth.utils.CollectionUtils.join;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.collections4.MapUtils.isEmpty;

public class WoodpeckerInitialDataException extends WoodpeckerException
{
	public WoodpeckerInitialDataException(String message)
	{
		super(message);
	}

	public WoodpeckerInitialDataException(String message, Throwable cause)
	{
		super(message, cause);
	}
	
	public WoodpeckerInitialDataException(String messageTemplate, Object... messageTemplateArgs)
	{
		super(format(messageTemplate, messageTemplateArgs));
	}
	
	public WoodpeckerInitialDataException(Throwable cause, String messageTemplate, Object... messageTemplateArgs)
	{
		super(format(messageTemplate, messageTemplateArgs), cause);
	}
	

	public static WoodpeckerInitialDataException whenTableContainsNulls(String tableName,
	                                                                    Collection<String> columnsWithNulls)
	{
		return new WoodpeckerInitialDataException(format("Table '%s' contains NULL values in the following not-null column(s): %s. " +
				"See logs for details.", tableName, join(", ", columnsWithNulls)));
	}
	
	public static WoodpeckerInitialDataException whenDataNotFoundByKeys(String tableName, String columnName, 
	                                                                    Map<String, String> keys)
	{
		return new WoodpeckerInitialDataException(format("Unable to find '%s' in data loaded by '%s' by the following keys: %s.",
				columnName, tableName, keys.entrySet().stream()
						.map(e -> e.getKey() + '=' + e.getValue()).collect(joining(", "))));
	}
	
	public static WoodpeckerInitialDataException whenUniqueDataNotFound(String tableName, String columnName)
	{
		return new WoodpeckerInitialDataException(format("Unable to find next unique '%s' in data loaded by '%s'.",
				columnName, tableName));
	}

	public static WoodpeckerInitialDataException whenUniqueDataNotFound(String tableName, String columnName,
	                                                                    Map<String, String> keys)
	{
		if (isEmpty(keys))
		{
			return new WoodpeckerInitialDataException(format("Unable to find next unique '%s' in data loaded by '%s'.",
					columnName, tableName));
		}
		else
		{
			return new WoodpeckerInitialDataException(format("Unable to find next unique '%s' in data loaded by '%s' by the following keys: %s.",
					columnName, tableName, join(keys)));
		}
	}
	
	public static WoodpeckerInitialDataException whenBlackListNotFound(String blackListName,
	                                                                   String referencingTableName,
	                                                                   Collection<String> availableBlackListsNames)
	{
		return new WoodpeckerInitialDataException(format("Black List '%s' referenced by table '%s' doesn't exist. %s",
				blackListName, referencingTableName,
				isNotEmpty(availableBlackListsNames)
						? format("Available Black Lists: %s.", join(availableBlackListsNames))
						: "No Black Lists available."));
	}
	
	public static WoodpeckerInitialDataException whenTableNotContainsColumn(String tableName, 
	                                                                        String columnName,
	                                                                        Collection<String> availableColumns)
	{
		return new WoodpeckerInitialDataException(format("Table '%s' doesn't contain column '%s'. Available columns: %s.", 
				tableName, columnName, join(availableColumns)));
	}

	public static WoodpeckerInitialDataException whenTableNotContainsColumns(String tableName,
	                                                                         Collection<String> absentColumns,
	                                                                         Collection<String> availableColumns)
	{
		return new WoodpeckerInitialDataException(format("Table '%s' doesn't contain column%s %s. Available columns: %s.", 
				tableName, 
				((availableColumns.size() > 1) ? "s" : ""), 
				join(absentColumns), 
				join(availableColumns)));
	}
	
	public static WoodpeckerInitialDataException fromIOException(IOException e, 
	                                                             String messageTemplate, 
	                                                             Object ... templateParams)
	{
		return new WoodpeckerInitialDataException(messageFromIOException(e, messageTemplate, templateParams), e);
	}
}
