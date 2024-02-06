/******************************************************************************
 * Copyright 2009-2024 Exactpro Systems Limited
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

package com.exactprosystems.clearth.utils;

import com.exactprosystems.clearth.automation.MatrixFunctions;
import com.exactprosystems.clearth.automation.exceptions.UnbalancedExpressionException;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Integer.max;
import static java.lang.Integer.parseInt;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.apache.commons.lang.StringUtils.*;

public class StringOperationUtils
{	
	public static boolean needsQuotes(String value)
	{
		return (value.contains(",")) || (value.contains("\"")) || (value.startsWith(" ")) || (value.endsWith(" "));
	}
	
	public static String quote(String value)
	{
		if (!needsQuotes(value))
			return value;
		else
		{
			if (value.contains("\""))
				value = value.replace("\"", "\"\"");
			return "\"" + value + "\"";
		}
	}
	
	public static boolean checkUnquotedSymbol(String formula, int indexToCheck)
	{
		return checkUnquotedSymbol(formula, 0, indexToCheck);
	}

	public static boolean checkUnquotedSymbol(String text, int indexToCheck, int formulaStart)
	{
		// If '.' is located inside string literal - skip it
		int quoteCount = 0, quoteIndex = -1;
		while (((quoteIndex = text.indexOf('\'', max(quoteIndex+1, formulaStart))) > -1) && (quoteIndex < indexToCheck))
			quoteCount++;
		return (quoteCount % 2 == 0);  // Odd number of quotes means that symbol is inside of string literal - skipping it
	}
	
	public static String stringOfSpaces(int length)
	{
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < length; i++)
			sb.append(" ");
		return sb.toString();
	}
	
	public static String multilineString(String delimiter, String... lines)
	{
		if (lines == null)
			return null;
		
		if (lines.length == 0)
			return "";
		
		StringBuilder sb = new StringBuilder();
		for (String s : lines)
			sb.append(s).append(delimiter);
		return sb.toString();
	}
	
	public static int findString(String value, String[] basket)
	{
		for (int i = 0; i < basket.length; i++)
		{
			if (basket[i].equals(value))
				return i;
		}
		return -1;
	}
	
	/*
	 * "1..10", " 1 .. 10"  ->  Pair(1, 10)
	 * "5", " 5 "           ->  Pair(5, 5)
	 * 
	 * "", null             ->  null
	 * "abc"                ->  null
	 * "5..2"               ->  null
	 * "-1..0"              ->  null
	 */
	public static Pair<Integer, Integer> parseIntegerRange(String text)
	{
		if (isEmpty(text))
			return null;
		
		Matcher m = Pattern.compile("\\s*(\\d+)\\s*(\\.\\.\\s*(\\d+))?\\s*").matcher(text);
		if (!m.matches())
			return null;
		
		int from = parseInt(m.group(1));
		String toTxt = m.group(3);
		int to = (toTxt != null) ? parseInt(toTxt) : from;
		return (from <= to) ? new Pair<Integer, Integer>(from, to) : null;
	}

	public static Set<String> splitByCommasToSet(String value)
	{
		if (isBlank(value))
			return emptySet();

		String[] values = splitByCommas(value);
		Set<String> set = new LinkedHashSet<String>();
		Collections.addAll(set, values);
		return set;
	}
	
	public static List<String> splitByCommasToList(String value)
	{
		if (isBlank(value))
			return emptyList();
		
		String[] values = splitByCommas(value);
		List<String> list = new ArrayList<String>();
		Collections.addAll(list, values);
		return list;
	}

	public static String[] splitByCommas(String value)
	{
		if (isBlank(value))
			return new String[]{};
		return value.split(",\\s*");
	}
	
	/**
	 * Removes number comparison functions (asNumber(), isLessThan(), etc.) from given string
	 * @param s String to remove functions from
	 * @return argument of removed function(s). Normally, a number
	 */
	public static String stripNumericFunctions(String s)
	{
		int functionStart = -1;
		String functionName = null;
		
		boolean isFormula = s.startsWith(MatrixFunctions.FORMULA_START),
				isPreCalculated = s.startsWith("{");  //@{asNumber(X)} will come here like this: {asNumber(X)}
		for (String fn : ComparisonUtils.SPECIAL_NUMBER_FUNCTION_NAMES)
		{
			functionStart = s.indexOf(fn+"(");
			if ((functionStart == 0) 
					|| ((functionStart == 1) && isPreCalculated)
					|| ((functionStart == 2) && isFormula))
			{
				functionName = fn;
				break;
			}
		}
		
		if (functionStart == -1)
			return s;
		
		//                                             name + (
		int cutFrom = functionStart + functionName.length() + 1;
		
		int cutTo = indexOfAny(s, ",)");
		if (cutTo == -1)
			cutTo = s.length();
		
		String newD = removeQuotes(s.substring(cutFrom, cutTo));
		return stripNumericFunctions(newD);
	}
	
	
	private static String removeQuotes(String s)
	{
		return removeStart(removeEnd(s, "'"), "'");
	}

	public static void checkBracketsBalance(String function) throws UnbalancedExpressionException
	{
		int openBracket = 0;
		boolean openQuote = false;

		for(int i = 0; i < function.length(); i++)
		{
			char ch = function.charAt(i);

			if (ch == '\'' && (i == 0 || function.charAt(i - 1) != '\\'))
			{
				openQuote = !openQuote;
				continue;
			}
			if (!openQuote)
			{
				if (ch == '(')
				{
					openBracket++;
					continue;
				}
				if (ch == ')')
				{
					openBracket--;
					if (openBracket < 0)
						throw new UnbalancedExpressionException("Unbalanced closing bracket at position " + (i + 1));
				}
			}
		}
		if (openQuote)
			throw new UnbalancedExpressionException("Unbalanced quotes");

		if (openBracket != 0)
			throw new UnbalancedExpressionException("Unbalanced opening bracket");
	}
}
