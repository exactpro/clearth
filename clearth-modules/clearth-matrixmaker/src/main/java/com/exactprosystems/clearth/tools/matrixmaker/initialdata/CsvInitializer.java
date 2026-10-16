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

package com.exactprosystems.clearth.tools.matrixmaker.initialdata;

import com.exactprosystems.clearth.tools.matrixmaker.GenerateException;
import com.exactprosystems.clearth.tools.matrixmaker.MMCsvUtils;
import com.exactprosystems.clearth.tools.matrixmaker.initialdata.CsvInitialData.CsvInitialRow;
import org.apache.commons.lang3.StringUtils;
import org.mvel2.MVEL;

import java.io.File;
import java.io.Serializable;
import java.util.*;

import static com.exactprosystems.clearth.tools.matrixmaker.MMCsvUtils.makeFilterParams;

public class CsvInitializer
{
	public static final String UNIQUE_ONLY = "UNIQUE_ONLY",
			UNIQUE_ONLY_WITHOUT_EMPTY = "UNIQUE_ONLY_WITHOUT_EMPTY",
			FILTER = "FILTER", MVEL_FILTER = "MVEL",
			MAPPING = "mapping";
	
	protected boolean randomScenarios = false;
	protected List<Integer> availableConfigRows = null;
	
	public CsvInitialData initializeCsvConfig(String name, String function, String[] params, File[] dataFiles, boolean randomScenarios, List<Integer> availableConfigRows) throws GenerateException
	{
		this.randomScenarios = randomScenarios;
		this.availableConfigRows = availableConfigRows;
		String configName;
		if (dataFiles.length == 1)
			configName = dataFiles[0].getName();
		else
			configName = params[0];
		
		String absolutePath = findFile(dataFiles, configName);
		
		List<CsvInitialRow> rows = readConfig(function, dataFiles, absolutePath, params);
		
		return createInitialData(function, configName, rows, name, params);
		
	}
	
	protected CsvInitialData createInitialData(String function, String configName, List<CsvInitialRow> rows, String name, String[] params) throws GenerateException
	{
		if (isMapping(function))
			return createMappingData( function,  configName, rows,  name, params);
		else
			return createCsvInitialData(function, configName, rows, name, params);
	}
	
	protected CsvInitialData createCsvInitialData(String value, String configName, List<CsvInitialRow> rows, String name, String[] params) throws GenerateException
	{
		return new CsvInitialData(value, configName, randomScenarios, rows, name);
	}

	protected CsvInitialData createMappingData(String value, String configName, List<CsvInitialRow> rows, String name, String[] params) throws  GenerateException
	{
		if (params.length == 2)
			return new MappingInitialData(value, configName, rows, name, params[1]);
		else 
			throw new GenerateException("Function 'mapping' must have 2 parameters");
	}
	
	public static boolean isMapping(String function)
	{
		return (StringUtils.equals(function, MAPPING));
	}

	protected List<CsvInitialRow> readConfig(String function, File[] dataFiles, String absolutePath, String[] params) throws GenerateException
	{
		if (isMapping(function))
			return readCsv(absolutePath, availableConfigRows, false);
		
		if (params.length == 1)
			return readCsv(absolutePath, availableConfigRows, randomScenarios);
		else if (params.length > 1)
		{
			String mode = params[1], param = params[2];
			switch (mode)
			{
				case UNIQUE_ONLY:
					return readUniqueCsv(absolutePath, param, false);
				case UNIQUE_ONLY_WITHOUT_EMPTY:
					return readUniqueCsv(absolutePath, param, true);
				case FILTER:
					String[] paramsArr = Arrays.copyOfRange(params, 2, params.length);
					Map<String, FilterParam> filterParams = makeFilterParams(paramsArr);
					return readCsvByFilter(absolutePath, filterParams, paramsArr);
				case MVEL_FILTER:
					return readCsvByMvel(absolutePath, MVEL.compileExpression(param), param);
				default:
					throw new GenerateException("Does not support mode '" + mode + "'");
			}
		}
		else
			throw new GenerateException("Function 'csvFile' must have 1 or more parameters");
	}

	private String findFile(File[] dataFiles, String fileName) throws GenerateException
	{
		return MMCsvUtils.findFile(dataFiles, fileName);
	}
	
	private List<CsvInitialRow> readCsv(String fileName, List<Integer> availableConfigRows, boolean randomScenarios) throws GenerateException
	{
		return MMCsvUtils.readCsv(fileName, randomScenarios, availableConfigRows);
	}
	
	private List<CsvInitialRow> readUniqueCsv(String fileName, String columnName, boolean withoutEmpty) throws GenerateException
	{
		return MMCsvUtils.readUniqueCsv(fileName, columnName, withoutEmpty);
	}

	private List<CsvInitialRow> readCsvByFilter(String fileName, Map<String, FilterParam> keyFields, String[] paramsArr) throws GenerateException
	{
		return MMCsvUtils.readCsvByFilter(fileName, keyFields, paramsArr);
	}
	
	private List<CsvInitialRow> readCsvByMvel(String fileName, Serializable compiledExpression, String expression) throws GenerateException
	{
		return MMCsvUtils.readCsvByMvel(fileName, compiledExpression, expression);
	}
	
	
	public static class FilterParam
	{
		private final String name;
		private final Set<String> values;

		public FilterParam(String name, Set<String> values)
		{
			this.name = name;
			this.values = values;
		}

		public FilterParam(String name, String value)
		{
			this.name = name;
			values = new HashSet<>();
			values.add(value);
		}

		public void add(String value)
		{
			values.add(value);
		}

		public String getName()
		{
			return name;
		}

		public Set<String> getValues()
		{
			return values;
		}
	}
}
