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

package com.exactprosystems.clearth.automation.expressions;


import com.exactprosystems.clearth.utils.Pair;

import java.util.*;

import org.apache.commons.lang3.StringUtils;

import static com.exactprosystems.clearth.automation.ActionExecutor.*;
import static com.exactprosystems.clearth.automation.MatrixFunctions.FORMULA_END;
import static com.exactprosystems.clearth.automation.MatrixFunctions.FORMULA_START;
import static com.exactprosystems.clearth.automation.MatrixFunctions.STRING_CONSTANT_QUOTE;
import static com.exactprosystems.clearth.automation.expressions.MvelExpressionUtils.isValidActionIdChar;
import static com.exactprosystems.clearth.utils.StringOperationUtils.checkUnquotedSymbol;
import static com.exactprosystems.clearth.utils.TagUtils.indexClosingTag;
import static java.lang.Character.isJavaIdentifierPart;
import static java.lang.Character.isJavaIdentifierStart;
import static java.lang.Math.max;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

public class ActionReferenceFinder
{
	private static final Set<String> SPECIAL_REFS = new HashSet<>(asList(PARAMS_IN, PARAMS_OUT, VARKEY_ACTION));

	private final String text;

	private int expStartIndex;
	private int expEndIndex;
	private int idStartIndex;
	private int idEndIndex;
	private int paramNameStartIndex;
	private int paramNameEndIndex;

	private boolean searchStarted;
	private boolean searchFinished;

	public ActionReferenceFinder(String text)
	{
		requireNonNull(text);
		this.text = removeStringLiterals(text);
	}


	public boolean findNext()
	{
		if (searchFinished)
			return false;

		for (;;)
		{
			if (searchStarted && findNextReference())
				return true;

			if (!searchStarted)
				searchStarted = true;

			if (!findNextExpression())
			{
				searchFinished = true;
				return false;
			}
		}
	}

	public String nextActionId()
	{
		if (!searchStarted || searchFinished)
			return null;

		return text.substring(idStartIndex, idEndIndex);
	}

	public String nextParamName()
	{
		if (!searchStarted || searchFinished)
			return null;

		return text.substring(paramNameStartIndex, paramNameEndIndex);
	}

	public List<String> checkExpressionCorrectness()
	{
		List<String> result = new ArrayList<>();
		//in case of odd number of quotes parsing might not work
		//E. g. "@{.....'}a.b}" does not close nor contains any parameters
		if (StringUtils.countMatches(text, STRING_CONSTANT_QUOTE) % 2 != 0)
			result.add("Expression contains odd number of quotes");
		return result;
	}

	public Collection<Pair<String, String>> findAll()
	{
		Set<Pair<String, String>> references = new LinkedHashSet<>();
		while (findNext())
		{
			references.add(new Pair<>(nextActionId(), nextParamName()));
		}
		return references;
	}

	/**
	 * Trying to move pointers to next @{...} expression.
	 *
	 * Result: @{......................}
	 *           ^                     ^ 
	 *           expStartIndex         expEndIndex
	 *
	 * @return true if next expression is found.
	 */
	private boolean findNextExpression()
	{
		int findFromIndex = (expEndIndex == 0) ? 0 : expEndIndex + 1;

		expStartIndex = text.indexOf(FORMULA_START, findFromIndex);
		if (expStartIndex == -1)
			return false;

		expEndIndex = indexClosingTag(text, FORMULA_START, FORMULA_END, expStartIndex);

		expStartIndex += FORMULA_START.length();

		return expEndIndex != -1;
	}

	/**
	 * Trying to move pointers to next actionId.ParamName reference inside [expStartIndex, expEndIndex) bounds.
	 *
	 * Result:     actionId.out.ParamName
	 *             ^       ^    ^        ^
	 *  idStartIndex idEndIndex paramNameStartIndex paramNameEndIndex
	 *
	 * Notes:
	 *
	 * * Dots at the following places are ignored:
	 *   - first char in expression;
	 *   - last char in expression;
	 *   - next char after previous reference.
	 *
	 * * ParamName is a valid Java identifier so let's check it before action id.
	 *
	 * @return true if next reference is found.
	 */
	private boolean findNextReference()
	{
		int dotIndex = -1;
		for (;;)
		{
			int findFromIndex = max(max(expStartIndex, paramNameEndIndex), dotIndex) + 1;

			dotIndex = text.indexOf('.', findFromIndex);
			if ((dotIndex == -1) || (dotIndex > expEndIndex))
				return false;
			if (!checkUnquotedSymbol(text, dotIndex, expStartIndex))
				continue;

			if (!findNextParamName(dotIndex + 1))
				continue;

			if (findNextActionId(dotIndex))
				return true;
		}
	}

	/**
	 * Trying to move pointers to the next action id located before the specified findToIndex.
	 * Action id doesn't have to be a valid MVEL (Java) identifier.
	 * It can be started from digit or consisted only from digits.
	 *
	 * @param findToIndex index of next char after possible action id.
	 * @return true if next action id is found.
	 */
	private boolean findNextActionId(int findToIndex)
	{
		int startIndex = findToIndex;
		while (isValidActionIdChar(text.charAt(startIndex - 1)))
			startIndex--;

		if (startIndex == findToIndex)
			return false;

		idStartIndex = startIndex;
		idEndIndex = findToIndex;
		return true;

	}

	/**
	 * Trying to move pointers to the next parameter name skipping special references like 'in', 'out' and 'action'.
	 * Action parameter name is a valid Java identifier.
	 *
	 * Results:
	 *
	 *        actionId.ParamName
	 *                 ^        ^
	 * paramNameStartIndex     paramNameEndIndex
	 *
	 * or:
	 *        actionId.out.ParamName
	 *                     ^        ^
	 *     paramNameStartIndex     paramNameEndIndex
	 *
	 * If endIndex points to '(' it is method call.
	 *
	 * @param findFromIndex index of the first char of possible parameter name.
	 * @return true if next parameter name is found.
	 */
	private boolean findNextParamName(int findFromIndex)
	{
		int startIndex = findFromIndex;
		int endIndex = findIdentifierEndIndex(startIndex);
		if (endIndex == -1)
			return false;

		if ((text.charAt(endIndex) == '.') &&
				SPECIAL_REFS.contains(text.substring(startIndex, endIndex)))
		{
			startIndex = endIndex + 1;
			endIndex = findIdentifierEndIndex(startIndex);
			if (endIndex == -1)
				return false;
		}

		if (text.charAt(endIndex) == '(')
			return false;

		paramNameStartIndex = startIndex;
		paramNameEndIndex = endIndex;
		return true;
	}

	/**
	 * Trying to find end of identifier: parameter name or special reference ('in', 'out' or 'action')
	 * started from the specified index. Parameter name is valid Java identifier.
	 *
	 * @param findFromIndex  index of first char of possible identifier.
	 * @return index of next char after found identifier or -1 if there is no valid identifier 
	 * started at specified findFromIndex.
	 */
	private int findIdentifierEndIndex(int findFromIndex)
	{
		if (!isJavaIdentifierStart(text.charAt(findFromIndex)))
			return -1;

		int endIndex = findFromIndex + 1;
		while (isJavaIdentifierPart(text.charAt(endIndex)))
			endIndex++;

		return endIndex;
	}


	static private String removeStringLiterals(String text)
	{
		return text.replaceAll("'.*?'", "''");
	}

}
