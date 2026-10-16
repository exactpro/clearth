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

import com.exactprosystems.clearth.automation.MatrixFunctions;
import com.exactprosystems.clearth.generators.IncrementingValueGenerator;
import com.exactprosystems.clearth.tools.matrixmaker.initialdata.CsvInitialData;
import com.exactprosystems.clearth.tools.matrixmaker.initialdata.InitialData;
import com.exactprosystems.clearth.utils.LineBuilder;
import com.exactprosystems.clearth.utils.ObjectWrapper;
import com.exactprosystems.clearth.utils.Pair;
import com.exactprosystems.clearth.utils.TagUtils;
import org.apache.commons.lang3.StringUtils;

import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Functions
{
	public static final String FORMULA_START = "#{", FORMULA_END = "}";
	
	protected Map<String, Object> savedValues = new HashMap<>();
	protected Map<String, List<String>> savedList = new HashMap<String, List<String>>();
	
	protected Map<String, InitialData> initialData = new HashMap<String, InitialData>();
	protected final MatrixFunctions mf;

	public Functions(Map<String, InitialData> initialData)
	{
		this.initialData = initialData;
		this.mf = new MatrixFunctions(null, new Date(), null, false, new IncrementingValueGenerator(System.currentTimeMillis()));
	}

	public String generateValue(String expression) throws GenerateException
	{
		while (expression.contains(FORMULA_START))
		{
			int start = expression.indexOf(FORMULA_START);
			String before = expression.substring(0, start);
			String formula = expression.substring(start);
			int end = TagUtils.indexClosingTag(formula, FORMULA_START, FORMULA_END);
			if (end == -1)
				throw new GenerateException("Expression has not '}'");
			String after = formula.substring(end + FORMULA_END.length());
			formula = formula.substring(FORMULA_START.length(), end);
			if(formula.contains(FORMULA_START))
				formula = generateValue(formula);
			String result = calculateExpression(formula);
			expression = before + result + after;
		}

		return expression;
	}
	
	protected String getSavedValue(String name)
	{
		return Objects.toString(savedValues.get(name), null);
	}

	protected String calculateExpression(String formula) throws GenerateException
	{
		String[] f = parsParams(formula);
		
		String function = f[0];
		String[] params = new String[f.length-1];
		System.arraycopy( f, 1, params, 0, params.length );
		Func func = Func.getByName(function);
		if (func == null)
		{
			InitialData initData = initialData.get(function);
			if (initData != null)
				return initData.getField(params);
			
			String savedValue = getSavedValue(function);
			if (savedValue != null)
				return savedValue;
			
			return evaluateMatrixFunction(formula);
		}
		else
		{
			switch (func)
			{
				case SAVE_THIS:
					return saveThis(function, params);
				case GET_SAVED:
					return getSaved(function, params);
				case GET_SAVED_Index:
					return getSavedIndex(function, params);
				case SAVE_THIS_N:
					return saveThisN(function, params);
				case GET_SAVED_N:
					return getSavedN(function, params);
				case ADD:
					return plus(function, params);
				case SUB:
					return minus(function, params);
				case MUL:
					return multiply(function, params);
				case DIV:
					return divide(function, params);
				case PADDING:
					return padding(function, params);
				case IF:
					return condition(function, params);
				case CLEAR_ID:
					return clearId(function, params);
				case CLEAR_IDS:
					return clearIds(function, params);
				case SPLIT_MATRIX:
					return params[0];
				case GET_CSV_COLUMNS:
					return getCsvColumns(function, params);
				default:
					InitialData initData = initialData.get(function);
					if (initData != null)
						return initData.getField(params);
					
					String savedValue = getSavedValue(function);
					if (savedValue != null)
						return savedValue;
					
					throw new GenerateException("Function '" + function + "' does not exist.");
			}
		}
	}

	protected String condition(String function, String[] params) throws GenerateException
	{
		verifyCountParameters(function, params, 4);
		
		String value = params[0];
		String pattern = params[1];
		String ifTrue = params[2];
		String ifFalse = params[3];

		Pattern pattern1 = Pattern.compile(pattern);
		Matcher matcher = pattern1.matcher(value);
		
		if(matcher.find())
			return ifTrue;
		else
			return ifFalse;
	}

	protected String padding(String function, String[] params) throws GenerateException
	{
		verifyCountParameters(function, params, 3);
		
		String value = params[0];
		int number;
		try
		{ 
			number = Integer.parseInt(params[1]);
		}
		catch(NumberFormatException e)
		{
			throw new GenerateException("Second parameter in function '" + function + "' must be a number");
		}
		String suffix = params[2];
				
		return StringUtils.rightPad(value, number, suffix);
	}

	public String clearId(String function, String[] params) throws GenerateException
	{
		savedList.put(params[0],new ArrayList<String>());
		return "";
	}

	public String clearIds(String function, String[] params) throws GenerateException
	{
		savedList.clear();
		return "";
	}

	protected String plus(String function, String[] params) throws GenerateException
	{
		
		Pair<Double, Double> values = parsDoubleValue(function, params);
		return formatDouble(values.getFirst() + values.getSecond());
	}

	protected String minus(String function, String[] params) throws GenerateException
	{
		Pair<Double, Double> values = parsDoubleValue(function, params);
		return formatDouble(values.getFirst() - values.getSecond());
	}

	protected String multiply(String function, String[] params) throws GenerateException
	{
		Pair<Double, Double> values = parsDoubleValue(function, params);
		return formatDouble(values.getFirst() * values.getSecond());
	}

	protected String divide(String function, String[] params) throws GenerateException
	{
		Pair<Double, Double> values = parsDoubleValue(function, params);
		return formatDouble(values.getFirst() / values.getSecond());
	}
	
	protected String formatDouble(double value)
	{
		DecimalFormat formatter = new DecimalFormat("#.##");
		return formatter.format(value);
	}
	
	protected Pair<Double, Double> parsDoubleValue(String function, String[] params) throws GenerateException
	{
		verifyCountParameters(function, params, 2);
		
		Double value1 = Double.parseDouble(params[0]);
		Double value2 = Double.parseDouble(params[1]);
		return new Pair<Double,Double>(value1, value2);
	}

	public String saveThis(String function, String[] params) throws GenerateException
	{
		verifyCountParameters(function, params, 2);
		savedValues.put(params[0], params[1]);
		return params[1];
	}
	
	protected String getSaved(String function, String[] params) throws GenerateException
	{
		verifyCountParameters(function, params, 1);
		
		String value = getSavedValue(params[0]);
		if(value == null)
			throw new GenerateException("Value for key '" + params[0] + "' is not saved");
		return value;
	}

	protected String getSavedIndex(String function, String[] params) throws GenerateException
	{
		verifyCountParameters(function, params, 2);
		
		if(savedList.get(params[0]) == null)
			throw new GenerateException("Value for key '" + params[0] + "' is not saved");
				
		int index = Integer.valueOf(params[1]) - 1;
		
		return savedList.get(params[0]).get(index);
	}
	
	protected String saveThisN(String function, String[] params) throws GenerateException
	{
		verifyCountParameters(function, params, 2);

		savedList.putIfAbsent(params[0], new ArrayList<String>());

		savedList.get(params[0]).add(params[1]);
		savedValues.put(params[0], params[1]);
		return params[1];
	}

	protected String getSavedN(String function, String[] params) throws GenerateException
	{
		verifyCountParameters(function, params, 2);
		
		String delimiter = params[1];
		String[] paramsArr = params[0].replace("'", "").split(",");
		String res = null;
		for (String p : paramsArr)
		{
			if(savedList.get(p) == null)
				throw new GenerateException("Values for key '" + p + "' are not saved");
			
			res = (StringUtils.isEmpty(res) ? "" : (res + delimiter)) + StringUtils.join(savedList.get(p), delimiter);
			savedList.put(p,new ArrayList<>());
		}
		return res;
	}
	
	protected String getCsvColumns(String function, String[] params) throws GenerateException
	{
		if (params.length < 3 || params.length > 4)
		{
			throw new GenerateException("Function '" + function + "' should have parameters: link_to_csv, regex_pattern,"
					+ " delimiter, [sorting_order]. sorting_order is optional, columns won't be sorted if it is omitted");
		}
		
		InitialData data = initialData.get(params[0]);
		if (!(data instanceof CsvInitialData))
			throw new GenerateException("Function '" + function + "' could process only CSV initial data");
		Pattern columnPattern = Pattern.compile(params[1]);
		String delimiter = params[2];
		SortingOrder sortingOrder = null;
		if (params.length == 4)
		{
			sortingOrder = SortingOrder.getByName(params[3]);
			if (sortingOrder == null)
			{
				throw new GenerateException("Sorting order for function '" + function + "' could be only '"
						+ SortingOrder.ASCENDING.name + "' or '" + SortingOrder.DESCENDING.name + "'");
			}
		}
		
		Map<String, String> row = ((CsvInitialData)data).getCurrentRow();
		List<String> selectedColumns = row.keySet().stream().filter(c -> columnPattern.matcher(c).find()).collect(Collectors.toList());
		if (sortingOrder != null)
			selectedColumns.sort(sortingOrder.comparator);
		LineBuilder builder = new LineBuilder(delimiter);
		selectedColumns.forEach(c -> builder.append(row.get(c)));
		return builder.toString();
	}
	
	public static String[] parsParams(String formula)
	{
		List<String> tokensList = new ArrayList<String>();
		boolean inQuotes = false;
		boolean escape = false;
		StringBuilder b = new StringBuilder();
		int openedBraces = 0;
		if (formula.startsWith(FORMULA_START))
		{
			formula = formula.substring(FORMULA_START.length());
		}
		
		for (char c : formula.toCharArray())
		{
			Operation r = Operation.getByName(String.valueOf(c));
			if (r == null)
				b.append(c);
			else
			{
				switch (r)
				{
					case CORNER:
						escape = true;
						break;
					case COMMA:
						if (inQuotes || openedBraces > 0)
						{
							b.append(c);
						} else {
							tokensList.add(b.toString());
							b = new StringBuilder();
						}
						break;
					case QUOTE:
						if (escape)
						{
							b.append(c);
							escape = false;
						}
						else {
							inQuotes = !inQuotes;
						}
						break;
					case OPEN_BRACE:
						if (!inQuotes)
							openedBraces++;
						b.append(c);
						break;
					case CLOSE_BRACE:
						if (!inQuotes)
							openedBraces--;
						b.append(c);
						break;
						
					default:
						b.append(c);
						break;
				}
			}
		}
		tokensList.add(b.toString());
		return tokensList.toArray(new String[0]);
	}

	
	protected void verifyCountParameters(String function, String[] params, int count) throws GenerateException
	{
		if(params.length != count)
			if(count == 1)
				throw new GenerateException("Function '" + function + "' must have " + count + " parameter. Function: " + function+" Parameters: " + Arrays.asList(params));
			else
				throw new GenerateException("Function '" + function + "' must have " + count + " parameters. Function: " + function+" Parameters: " + Arrays.asList(params));
	}
	
	protected String evaluateMatrixFunction(String f) throws GenerateException
	{
		try
		{
			Object result = mf.calculateExpression(MatrixFunctions.FORMULA_START+f+MatrixFunctions.FORMULA_END, 
					"mm_value", savedValues, null, null, new ObjectWrapper(0));
			return result != null ? result.toString() : null;
		}
		catch (Exception e)
		{
			throw new GenerateException("Could not evaluate function '"+f+"': "+e.getMessage(), e);
		}
	}
	
	/*protected String[] splitValue(String value)
	{
		String delimiter = ",";
		String starter = "{", ender = "}";
		char[] arr = value.toCharArray();
		return null;
	}*/
	
	enum Func
	{
		SAVE_THIS("saveThis"),
		GET_SAVED("getSaved"),
		SAVE_THIS_N("saveThisN"),
		GET_SAVED_N("getSavedN"),
		GET_SAVED_Index("getSavedIndex"),
		ADD("+"),
		SUB("-"),
		MUL("*"),
		DIV("/"),
		PADDING("padding"),
		IF("if"),
		CLEAR_ID("clearId"),
		CLEAR_IDS("clearIds"),
		SPLIT_MATRIX("splitMatrix"),
		GET_CSV_COLUMNS("getCsvColumns");
		
		String name;
		Func(String name)
		{
			this.name = name;
		}
		
		public static Func getByName(String name)
		{
			for (Func val : Func.values())
			{
				if (val.name.equals(name))
					return val;
			}
			return null;
		}
	}
	

	enum Operation
	{
		OPEN_BRACE("{"),
		CLOSE_BRACE("}"),
		COMMA(","),
		QUOTE("'"),
		CORNER("^");
		
		String name;
		Operation(String name)
		{
			this.name = name;
		}
		
		public static Operation getByName(String name)
		{
			for (Operation val : Operation.values())
			{
				if (val.name.equals(name))
					return val;
			}
			return null;
		}
	}
	
	
	enum SortingOrder
	{
		ASCENDING("asc", Comparator.naturalOrder()),
		DESCENDING("desc", Comparator.reverseOrder());
		
		String name;
		Comparator<String> comparator;
		
		SortingOrder(String name, Comparator<String> comparator)
		{
			this.name = name;
			this.comparator = comparator;
		}
		
		public static SortingOrder getByName(String name)
		{
			for (SortingOrder order : SortingOrder.values())
			{
				if (order.name.equalsIgnoreCase(name))
					return order;
			}
			return null;
		}
	}
}
