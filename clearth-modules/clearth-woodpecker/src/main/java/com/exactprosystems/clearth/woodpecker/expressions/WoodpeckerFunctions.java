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

package com.exactprosystems.clearth.woodpecker.expressions;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.ValueGenerator;
import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.MatrixFunctions;
import com.exactprosystems.clearth.automation.exceptions.FunctionException;
import com.exactprosystems.clearth.utils.ObjectWrapper;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerException;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerFunctionsException;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerNotEnoughInitialDataException;
import com.exactprosystems.clearth.woodpecker.initialdata.InitialData;
import com.exactprosystems.clearth.woodpecker.initialdata.collections.InitialDataCollection;
import com.exactprosystems.clearth.woodpecker.initialdata.queries.InitialDataQuery;
import com.exactprosystems.clearth.woodpecker.initialdata.tables.InitialDataTable;
import com.exactprosystems.clearth.woodpecker.initialdata.tables.internal.*;
import com.exactprosystems.clearth.woodpecker.misc.SchedulerSettingsForWoodpecker;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.mvel2.PropertyAccessException;
import org.mvel2.templates.CompiledTemplate;
import org.mvel2.templates.TemplateRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.exactprosystems.clearth.utils.CollectionUtils.join;
import static com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerFunctionsException.*;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.concurrent.ThreadLocalRandom.current;
import static org.apache.commons.lang3.StringUtils.rightPad;

public class WoodpeckerFunctions extends MatrixFunctions
{
	private static final Logger log = LoggerFactory.getLogger(WoodpeckerFunctions.class);

	public static final String DISABLED = "{disabled}";
	
	private static final Pattern UNRESOLVED_LINK_PATTERN = Pattern.compile("could not access: ([\\w\\d_]+); in class:"); 

	private final InitialData initialData;
	
	/*
	* Table can be updated between invocations of randomRowNum or nextRowNum and tableValueByRowNum.
	* So we need to store reference to table used in randomRowNum or nextRowNum
	* to use the same table in tableValueByRowNum.
	* */
	private final ThreadLocal<HashMap<String, Table>> tablesCacheRef = ThreadLocal.withInitial(HashMap::new);


	public WoodpeckerFunctions(SchedulerSettingsForWoodpecker settings, 
	                           InitialData initialData)
	{
		this(settings.getHolidays(),
				settings.useBusinessDay() ? settings.getBusinessDay() : null,
				settings.getBaseTime(),
				settings.isWeekendHoliday(),
				ClearThCore.commonGenerator(),
				initialData);
	}
	
	public WoodpeckerFunctions(Map<String, Boolean> holidays,
	                           Date businessDay,
	                           Date baseTime,
	                           boolean weekendHoliday,
	                           ValueGenerator valueGenerator,
	                           InitialData initialData)
	{
		super(holidays, businessDay, baseTime, weekendHoliday, valueGenerator);
		this.initialData = initialData;
	}


	/**
	 * This class extends MatrixFunctions only to provide ability 
	 * to use standard matrix functions in Woodpecker configs.
	 * Please use calculate method.
	 * @throws UnsupportedOperationException
	 */
	@Deprecated
	@Override
	public Object calculateExpression(String expression, String paramName, Map<String, Object> mvelVars, 
	                                  Map<String, String> fixedIDs, Action currentAction, ObjectWrapper iterationWrapper) throws Exception
	{
		throw new UnsupportedOperationException("Use calculate!");
	}

	public Object calculate(CompiledTemplate expression, Map<String, Object> vars) throws WoodpeckerException
	{
		try
		{
			return TemplateRuntime.execute(expression, this, vars);
		}
		catch (Exception e)
		{
			if (e instanceof PropertyAccessException)
			{
				Matcher m = UNRESOLVED_LINK_PATTERN.matcher(e.getMessage());
				if (m.find())
					throw new WoodpeckerFunctionsException(String.format("Cannot find parameter '%s'. " +
							"Available parameters: %s.", m.group(1), join(vars)));
			}
			
			Throwable causeOfCause = (e.getCause() != null) ? e.getCause().getCause() : null;
			if (causeOfCause instanceof WoodpeckerException)
				throw (WoodpeckerException) causeOfCause;
			else if (causeOfCause instanceof FunctionException)
				throw new WoodpeckerFunctionsException(e.getMessage(), causeOfCause);
			else 
				throw new WoodpeckerFunctionsException(e.getMessage(), e);
		}
	}


	@SuppressWarnings("unused")
	public String line(int minLength, int maxLength)
	{
		return line(current().nextInt(minLength, maxLength + 1));
	}

	@SuppressWarnings("unused")
	public String line(int length)
	{
		return RandomStringUtils.secure().nextAlphanumeric(length);
	}


	@SuppressWarnings("unused")
	public String string(int minLength, int maxLength)
	{
		return string(current().nextInt(minLength, maxLength + 1));
	}

	@SuppressWarnings("unused")
	public String string(int length)
	{
		return RandomStringUtils.secure().nextAlphabetic(length);
	}


	@SuppressWarnings("unused")
	public String upstring(int minLength, int maxLength)
	{
		return string(minLength, maxLength).toUpperCase();
	}

	@SuppressWarnings("unused")
	public String upstring(int length)
	{
		return string(length).toUpperCase();
	}


	@SuppressWarnings("unused")
	public String number(int maxLength)
	{
		long maxNumber = (long)Math.pow(10, maxLength) - 1;
		return Long.toString(current().nextLong(maxNumber));
	}

	@SuppressWarnings("unused")
	public String number(int minLength, int maxLength)
	{
		long minNumber = (long)Math.pow(10, minLength - 1);
		long maxNumber = (long)Math.pow(10, maxLength) - 1;
		return Long.toString(current().nextLong(minNumber, maxNumber));
	}


	@SuppressWarnings("unused")
	public String decimal(int maxCount, int maxDecimal)
	{
		String dec = RandomStringUtils.secure().nextNumeric(current().nextInt(maxDecimal + 1));
		if (dec.isEmpty() || Long.parseLong(dec) == 0)
			return RandomStringUtils.secure().nextNumeric(current().nextInt(maxCount) + 1);
		return RandomStringUtils.secure().nextNumeric(current().nextInt(maxCount) + 1) + "." + dec;
	}


	@SuppressWarnings("unused")
	public String random(long maxCount)
	{
		return Long.toString(current().nextLong(maxCount));
	}

	@SuppressWarnings("unused")
	public String random(long minCount, long maxCount)
	{
		return Long.toString(current().nextLong(minCount, maxCount));
	}


	@SuppressWarnings("unused")
	public String or(String ... params)
	{
		String param = params[current().nextInt(params.length)];
		if(StringUtils.equals(param, DISABLED))
			return null;
		return param;
	}


	@SuppressWarnings("unused")
	public String padding(String value, int number, String suffix)
	{
		return rightPad(value, number, suffix);
	}


	@SuppressWarnings("unused")
	public String abs(int value)
	{
		return Integer.toString(Math.abs(value));
	}


	@SuppressWarnings("unused")
	public String condition(String value, String patternStr, String ifTrue, String ifFalse)
	{
		Matcher matcher = Pattern.compile(patternStr).matcher(value);
		String result = matcher.matches() ? ifTrue : ifFalse;
		return StringUtils.equals(result, DISABLED) ? null : result;
	}


	@SuppressWarnings("unused")
	public String upperCase(String value)
	{
		return StringUtils.upperCase(value);
	}

	@SuppressWarnings("unused")
	public String lowerCase(String value)
	{
		return StringUtils.lowerCase(value);
	}
	
	
	public boolean equals(Object a, Object b)
	{
		return Objects.equals(a, b);
	}
	
	
	@SuppressWarnings("unused")
	public boolean containsText(String str, String searchStr)
	{
		return StringUtils.contains(str, searchStr);
	}


	////////// FUNCTIONS FOR WORK WITH INITIAL DATA //////////
	
	////////// Tables //////////
	
	@SuppressWarnings("unused")
	public KeyCondition notEq(String value)
	{
		return new KeyNotEquals(value);
	}

	@SuppressWarnings("unused")
	public KeyCondition in(String... values)
	{
		return new KeyIn(asList(values));
	}

	@SuppressWarnings("unused")
	public KeyCondition notIn(String... values)
	{
		Set<String> valuesSet = new HashSet<>();
		Collections.addAll(valuesSet, values);
		return new KeyNotIn(valuesSet);
	}
	
	@SuppressWarnings("unused")
	public String tableValue(String tableName, String columnName, Object... keyConditions) throws WoodpeckerException
	{
		Table table = findTable(tableName);
		
		Map<String, KeyCondition> conditions = parseConditions(keyConditions);
		
		int rowNum = table.selectRandomRowNumber(conditions);
		return table.selectColumnValueByRowNum(rowNum, columnName);
	}
	
	@SuppressWarnings("unused")
	public String nextTableValue(String tableName, String columnName, Object... keyConditions) throws WoodpeckerException
	{
		Table table = findTable(tableName);

		Map<String, KeyCondition> conditions = parseConditions(keyConditions);

		int rowNum = table.selectNextRowNumber(conditions);
		return table.selectColumnValueByRowNum(rowNum, columnName);
	}

	@SuppressWarnings("unused")
	public int randomRowNum(String tableName, Object... keyConditions) throws WoodpeckerException
	{
		Table table = findTable(tableName);
		tablesCacheRef.get().put(tableName, table);
		
		Map<String, KeyCondition> conditions = parseConditions(keyConditions);
		
		return table.selectRandomRowNumber(conditions);
	}	
	
	@SuppressWarnings("unused")
	public int nextRowNum(String tableName, Object... keyConditions) throws WoodpeckerException
	{
		Table table = findTable(tableName);
		tablesCacheRef.get().put(tableName, table);

		Map<String, KeyCondition> conditions = parseConditions(keyConditions);
		
		return table.selectNextRowNumber(conditions);
	}

	@SuppressWarnings("unused")
	public String tableValueByRowNum(String tableName, int rowNum, String columnName) throws WoodpeckerException
	{
		Table table = tablesCacheRef.get().get(tableName);
		if (table == null)
			table = findTable(tableName);
		return table.selectColumnValueByRowNum(rowNum, columnName);
	}
	
	protected final Table findTable(String tableName) throws WoodpeckerException
	{
		InitialDataTable table = initialData.getTable(tableName);
		if (table == null)
			throw whenTableNotFound(tableName, initialData);
		return table.getInternalTable();
	}
	
	protected final Map<String, Table> getTablesCache()
	{
		return tablesCacheRef.get();
	}
	
	private Map<String, KeyCondition> parseConditions(Object[] params) throws WoodpeckerException
	{
		if ((params == null) || (params.length == 0))
			return emptyMap();

		if (params.length % 2 == 1)
			throw new WoodpeckerFunctionsException(String.format("Value for key '%s' isn't specified.",
					params[params.length - 1]));

		Map<String, KeyCondition> conditions = new LinkedHashMap<>();
		for (int i = 0; i < params.length; i += 2)
		{
			String keyName = String.valueOf(params[i]);
			
			Object keyValue = params[i + 1];
			KeyCondition condition;
			if (keyValue instanceof KeyCondition)
				condition = (KeyCondition) keyValue;
			else 
				condition = new KeyEquals(String.valueOf(keyValue));
			
			conditions.put(keyName, condition);
		}
		return conditions;
	}
	

	////////// Queries //////////

	@SuppressWarnings("unused")
	public String queryValue(String queryName, String... params) throws WoodpeckerException
	{
		InitialDataQuery query = initialData.getQuery(queryName);
		if (query == null)
			throw whenQueryNotFound(queryName, initialData);
		return query.execute(paramsToMap(params));
	}


	private Map<String, String> paramsToMap(String[] params) throws WoodpeckerFunctionsException
	{
		if (params.length == 0)
			return emptyMap();

		if (params.length % 2 == 1)
			throw new WoodpeckerFunctionsException(String.format("Value for key '%s' isn't specified.",
					params[params.length - 1]));

		Map<String, String> map = new HashMap<>();
		for (int i = 0; i < params.length; i += 2)
		{
			map.put(params[i], params[i + 1]);
		}
		return map;
	}

	////////// Collections //////////

	@SuppressWarnings("unused")
	public String collectionValue(String collectionName) throws WoodpeckerException
	{
		InitialDataCollection collection = initialData.getCollection(collectionName);
		if (collection == null)
			throw whenCollectionNotFound(collectionName, initialData);
		
		String value = collection.nextValue();
		if (value != null)
			return value;
		else 
			throw new WoodpeckerNotEnoughInitialDataException(collectionName);
	}	
	
	@SuppressWarnings("unused")
	public String addToCollection(String collectionName, String value) throws WoodpeckerException
	{
		if (value == null)
			throw whenTryToAddNullToCollection(collectionName);
		
		InitialDataCollection collection = initialData.getCollection(collectionName);
		if (collection == null)
			throw whenCollectionNotFound(collectionName, initialData);
		
		if (!collection.addValue(value))
			log.warn("Unable to add value {} to collection '{}': collection is full.", value, collectionName);
		
		return value;
	}
}
