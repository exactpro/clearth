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

package com.exactprosystems.clearth.tools.matrixmaker;

import com.exactprosystems.clearth.tools.matrixmaker.initialdata.CsvInitialData;
import com.exactprosystems.clearth.tools.matrixmaker.initialdata.CsvInitialData.CsvInitialRow;
import com.exactprosystems.clearth.tools.matrixmaker.initialdata.CsvInitializer.FilterParam;
import com.exactprosystems.clearth.utils.KeyValueUtils;
import com.exactprosystems.clearth.utils.Pair;
import com.exactprosystems.clearth.utils.csv.readers.ClearThCsvReader;
import com.exactprosystems.clearth.utils.csv.readers.ClearThCsvReaderConfig;
import org.apache.commons.lang3.StringUtils;
import org.mvel2.MVEL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import static com.exactprosystems.clearth.tools.matrixmaker.MatrixGenerator.START_COMMENT;
import static com.exactprosystems.clearth.utils.csv.readers.ClearThCsvReaderConfig.withFirstLineAsHeader;

public class MMCsvUtils
{
	public static final Logger logger = LoggerFactory.getLogger(MMCsvUtils.class);
	private static final String NOT_FIND_FILE = "Did not find file '%s'",
								NOT_READ_FILE = "Cannot read file '%s'",
								SELECTED_ROW = "Selected row #{} by condition: '{}'",
								NOT_SELECTED_ROW = "Row #{} doesn't fit condition: '{}'";
	
	public static String findFile(File[] dataFiles, String fileName) throws GenerateException
	{
		for(File file : dataFiles)
		{
			if(StringUtils.equals(file.getName(),fileName))
				return file.getAbsolutePath();
		}
		throw new GenerateException("Did not find file '" + fileName + "'. Please upload this file.");
	}
	
	public static  List<CsvInitialRow> readCsv(String fileName, boolean randomScenarios, List<Integer> availableConfigRows) throws GenerateException
	{
		String fName = getFileName(fileName);
		logger.trace("Read file '{}'", fName);
		
		try (ClearThCsvReader reader = new ClearThCsvReader(fileName, createCsvReaderConfig(true)))
		{
			List<CsvInitialRow> result = new ArrayList<>();

			if (!reader.hasHeader())
				return result;

			int rowNum = 2, rowInFile = 1;
			String str = "not starts with '//'";

			while (reader.hasNext())
			{
				rowInFile++;
				checkHeader(rowInFile, reader, fName);
				if (!reader.getRawRecord().startsWith(START_COMMENT))
				{
					result.add(new CsvInitialRow(rowNum++, new LinkedHashMap<>(reader.getRecord())));
					logger.trace(SELECTED_ROW, rowInFile, str);
				}
				else
					logger.trace(NOT_SELECTED_ROW, rowInFile, str);
			}
			
			if (randomScenarios)
				return createRandomRowCsv(result, availableConfigRows, fileName);
			else
				return result;
		}
		catch (FileNotFoundException e)
		{
			throw throwGenerateException(String.format(NOT_FIND_FILE, fName), e);
		}
		catch (IOException e)
		{
			throw throwGenerateException(String.format(NOT_READ_FILE, fName), e);
		}
	}
	
	private static List<CsvInitialRow> createRandomRowCsv(List<CsvInitialRow> records, List<Integer> availableConfigRows, String fileName) throws GenerateException
	{
		if (records == null || records.size() < 2)
			return records;
		
		Random rn = new Random();
		int randomNum = 0;
		CsvInitialRow result = null;
		
		if (availableConfigRows != null)
		{

			HashMap<Integer,CsvInitialRow> rowMap = new HashMap<Integer,CsvInitialRow>();
			for (CsvInitialData.CsvInitialRow r : records)
				rowMap.put(r.getRowNumber(), r);
			
			//Check availableConfigRows
			List<Integer> unavailableRows = new ArrayList<Integer>();
			String rangeStr = availableConfigRows.isEmpty() ? "" 
					: (availableConfigRows.size() == 1 ? String.valueOf(availableConfigRows.get(0)) 
							: availableConfigRows.get(0) + "-" + availableConfigRows.get(availableConfigRows.size()-1)
					);
			for (Integer val : availableConfigRows)
			{
				if (!rowMap.containsKey(val))
					unavailableRows.add(val);
			}
			if (!unavailableRows.isEmpty())
				availableConfigRows.removeAll(unavailableRows);
			
			if (availableConfigRows.isEmpty())
			{
				throw new GenerateException("Cannot get random row from config: " + getFileName(fileName) + " for range: [" + rangeStr +"]");
			}
			
			int range = availableConfigRows.size();
			int num =  rn.nextInt(range);
			randomNum = availableConfigRows.get(num);
			result = rowMap.get(randomNum);
		}
		else
		{
			int range = records.size();
			randomNum = rn.nextInt(range);
			result = records.get(randomNum);
		}
		
		if (result == null)
			throw new GenerateException("Cannot get random row from config: " + getFileName(fileName));
		
		logger.debug("Random index: {}, Random row: {}", (randomNum + 1), result);
		return new ArrayList<CsvInitialRow>(Arrays.asList(result));
	}
	
	public static  List<CsvInitialRow> readUniqueCsv(String fileName, String columnName, boolean withoutEmpty) throws GenerateException
	{
		String fName = getFileName(fileName);
		logger.trace("Read file '{}' by Unique only", fName);
		
		try (ClearThCsvReader reader = new ClearThCsvReader(fileName, createCsvReaderConfig(true)))
		{
			List<CsvInitialRow> result = new ArrayList<>();

			if (!reader.hasHeader())
				return result;

			int rowNum = 2, rowInFile = 1;
			List<String> unique = new ArrayList<>();
			String str = String.format("unique '%s'", columnName);

			while(reader.hasNext())
			{
				rowInFile++;
				checkHeader(rowInFile, reader, fName);
				String value = reader.get(columnName);
				if((withoutEmpty && value.isEmpty()) || unique.contains(value))
				{
					logger.trace(NOT_SELECTED_ROW, rowInFile, str);
					continue;
				}
				unique.add(value);
				result.add(new CsvInitialRow(rowNum++, new LinkedHashMap<>(reader.getRecord())));
				logger.trace(SELECTED_ROW, rowInFile, str);
			}
			return result;
		}
		catch (FileNotFoundException e)
		{
			throw throwGenerateException(String.format(NOT_FIND_FILE, fName), e);
		}
		catch (IOException e)
		{
			throw throwGenerateException(String.format(NOT_READ_FILE, fName), e);
		}
	}

	public static List<CsvInitialRow> readCsvByFilter(String fileName, Map<String, FilterParam> filterParams, String[] paramsArr) throws GenerateException
	{
		String fName = getFileName(fileName);
		logger.trace("Read file '{}' by Filter", fName);

		try (ClearThCsvReader reader = new ClearThCsvReader(fileName, createCsvReaderConfig(true)))
		{
			List<CsvInitialRow> result = new ArrayList<>();

			if (!reader.hasHeader())
				return result;

			int rowNum = 2, rowInFile = 1;
			
			while(reader.hasNext())
			{
				rowInFile++;
				checkHeader(rowInFile, reader, fName);
				Map<String, String> rowMap = reader.getRecord();
				if (acceptsFilter(rowMap, filterParams))
				{
					result.add(new CsvInitialRow(rowNum++, new LinkedHashMap<>(rowMap)));
					if(logger.isTraceEnabled())
						logger.trace(SELECTED_ROW, rowInFile, Arrays.toString(paramsArr));
					continue;
				}
				if(logger.isTraceEnabled())
					logger.trace(NOT_SELECTED_ROW, rowInFile, Arrays.toString(paramsArr));
			}
			return result;
		}
		catch (FileNotFoundException e)
		{
			throw throwGenerateException(String.format(NOT_FIND_FILE, fName), e);
		}
		catch (IOException e)
		{
			throw throwGenerateException(String.format(NOT_READ_FILE, fName), e);
		}
	}
	
	public static List<CsvInitialRow> readCsvByMvel(String fileName, Serializable compiledExpression, String expression) throws GenerateException
	{
		String fName = getFileName(fileName);
		logger.trace("Read file '{}' by Mvel", fName);
		int rowInFile = 1;

		try (ClearThCsvReader reader = new ClearThCsvReader(fileName, createCsvReaderConfig(true)))
		{
			List<CsvInitialRow> result = new ArrayList<>();

			if (!reader.hasHeader())
				return result;
			
			int rowNum = 2;
			while (reader.hasNext())
			{
				rowInFile++;
				checkHeader(rowInFile, reader, fName);
				Map<String, String> row = reader.getRecord();
				Map<String, Object> vars = new HashMap<>();
				vars.put("row", row);
				if (MVEL.executeExpression(compiledExpression, vars, Boolean.class))
				{
					result.add(new CsvInitialRow(rowNum++, row));
					logger.trace(SELECTED_ROW, rowInFile, expression);
					continue;
				}
				logger.trace(NOT_SELECTED_ROW, rowInFile, expression);
			}
			return result;
		}
		catch (GenerateException e)
		{
			throw e;
		}
		catch (IOException e)
		{
			throw throwGenerateException(String.format(NOT_READ_FILE, fName), e);
		}
		catch (Exception e)
		{
			throw throwGenerateException(String.format("Error while reading row #%s from file '%s' with MVEL filter: %s", rowInFile, fName, expression), e);
		}
	}

	public static boolean acceptsFilter(Map<String, String> source, Map<String, FilterParam> filterParams)
	{
		for (Entry<String, FilterParam> keyEntry : filterParams.entrySet())
		{
			if (!source.containsKey(keyEntry.getKey()))
				return false;

			String sourceValue = source.get(keyEntry.getKey());
			if (!keyEntry.getValue().getValues().contains(sourceValue))
				return false;
		}
		return true;
	}

	/**
	 * keyValues array is supposed to be {"param1=va1", "param2=val2", ...}
	 */
	public static Map<String, String> parseKeyValues(String[] keyValues)
	{
		Map<String, String> result = new HashMap<>(keyValues.length);
		for (String keuValue : keyValues)
		{
			Pair<String, String> pair = KeyValueUtils.parseKeyValueString(keuValue);
			result.put(pair.getFirst(), pair.getSecond());
		}
		return result;
	}

	/**
	 * @param line is supposed to be "value1","value2",...,"valueN"
	 * Each value can be optionally surrounded by quotes.
	 * Value also could have commas inside if it is surrounded by quotes.
	 * @return array of values
	 * @throws GenerateException
	 */
	public static String[] splitLine(String line) throws GenerateException
	{
		try (ClearThCsvReader reader = new ClearThCsvReader(new StringReader(line), createCsvReaderConfig(false)))
			{
				if (reader.hasNext())
					return reader.getValues();
				return null;
			}
		catch (IOException e)
		{
			throw new GenerateException("Error while splitting line", e);
		}
	}

	private static ClearThCsvReaderConfig createCsvReaderConfig(boolean withHeader)
	{
		ClearThCsvReaderConfig config = withHeader ? withFirstLineAsHeader() : new ClearThCsvReaderConfig();
		config.setIgnoreSurroundingSpaces(true);
		return config;
	}

	public static Map<String, FilterParam> makeFilterParams(String[] paramsArr)
	{
		Map<String, FilterParam> filterParams = new HashMap<>();
		Arrays.stream(paramsArr).forEach(paramStr ->
		{
			Pair<String, String> pair = KeyValueUtils.parseKeyValueString(paramStr);
			String name = pair.getFirst();
			if (name == null)
				name = paramStr;
			String value = pair.getSecond();
			FilterParam filterParam = filterParams.get(name);
			if (filterParam == null)
			{
				filterParams.put(name, new FilterParam(name, value));
			} else
			{
				filterParam.add(value);
			}
		});
		return filterParams;
	}

	private static String getFileName(String file)
	{
		File f = new File(file);
		return f.getName();
	}
	
	private static void checkHeader(int rowNum, ClearThCsvReader reader, String fName) throws IOException, GenerateException
	{
		int header = reader.getHeader().size();
		int values = reader.getValues().length;
		
		if (header != values)
		{
			String message = String.format("File %s, row #%s: number of columns (%s) does not match header (%s)",
					fName, rowNum, values, header);
			throw new GenerateException(message);
		}
	}
	
	private static GenerateException throwGenerateException(String msg, Exception e)
	{
		return new GenerateException(msg, e);
	}
}
