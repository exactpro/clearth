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

package com.exactprosystems.clearth.utils;

import com.exactprosystems.clearth.automation.MatrixFunctions;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isBlank;

public class TagUtils
{
	public static int getTagStart(String tagName, String text, int searchFrom)
	{
		int start = text.indexOf("<" + tagName, searchFrom);
		if (start < 0)
			return start;
		int withEnd, withParams;
		withEnd = text.indexOf("<" + tagName + ">", searchFrom);
		withParams = text.indexOf("<" + tagName + " ", searchFrom);
		if (withEnd >= 0 && withParams >= 0)
			return withEnd < withParams? withEnd : withParams;
		else
			return withEnd == -1 ? withParams : withEnd;
	}
	
	public static int getTagEnd(String tagName, String text, int searchFrom, boolean positionAfter)
	{
		int tagEnd = text.indexOf("</" + tagName + ">", searchFrom);
		if ((positionAfter) && (tagEnd > -1))
			tagEnd += tagName.length() + 3; // 3 is <, / and >
		return tagEnd;
	}
	
	public static String getTagValue(String tagName, String text)
	{
		int start = getTagStart(tagName, text, -1);
		if (start < 0)
			return null;
		start = text.indexOf(">", start)+1;
		
		int end = getTagEnd(tagName, text, start, false);
		if (end < 0)
			end = text.length();
		
		return text.substring(start, end);
	}
	
	public static String getPureTagValue(String tagValue)
	{
		if (!containsNestedTags(tagValue))
			return tagValue;
		
		int start = tagValue.indexOf(">")+1,
				end = tagValue.indexOf("</", start);
		if (end<0)
			end = tagValue.length();
		
		return getPureTagValue(tagValue.substring(start, end));
	}
	
	public static boolean containsNestedTags(String tagValue)
	{
		return (tagValue.indexOf("<") > -1) || (tagValue.indexOf(">") > -1);
	}
	
	public static boolean tagExists(String tagName, String text)
	{
		return getTagStart(tagName, text, -1) > -1;
	}
	
	public static Pair<String, Integer> getTagAndStart(String tagName, String text, int searchFrom)
	{
		int start = getTagStart(tagName, text, searchFrom);
		if (start < 0)
			return null;
		int end = getTagEnd(tagName, text, start, true);
		if (end < 0)
			return new Pair<String, Integer>(text.substring(start), start);
		
		// Correct search. It includes inner tags with the same name
		int inner = start + tagName.length() + 2, innerCount = 0, nextStart;
		while (((nextStart = getTagStart(tagName, text, inner)) > -1) && (nextStart < end))
		{
			inner = nextStart+tagName.length() + 2;
			innerCount++;
			end = getTagEnd(tagName, text, end, true);
			if (end < 0)
				return new Pair<String, Integer>(text.substring(start), start);
		}
		return new Pair<String, Integer>(text.substring(start, end), start);
	}
	
	public static Pair<String, Integer> getTagAndStart(String tagName, String text)
	{
		return getTagAndStart(tagName, text, -1);
	}
	
	public static String getTag(String tagName, String text, int searchFrom)
	{
		Pair<String, Integer> result = getTagAndStart(tagName, text, searchFrom);
		if (result != null)
			return result.getFirst();
		else
			return null;
	}
	
	public static String getTag(String tagName, String text)
	{
		return getTag(tagName, text, -1);
	}
	
	public static String getTagAttribute(String tagName, String attrName, String text)
	{
		int start = text.indexOf("<" + tagName + " ");
		if (start < 0)
			return null;
		start += tagName.length() + 2;
		
		start = text.indexOf(attrName + "=\"", start);
		if (start < 0)
			return null;
		start += attrName.length() + 2;
		
		int end = text.indexOf("\"", start);
		if (end < 0)
			return null;
		return text.substring(start, end);
	}
	
	public static String openTag(String tagName, String attributes, boolean empty)
	{
		if (attributes != null)
			return "<" + tagName + " " + attributes + (empty ? " />" : ">");
		else
			return "<" + tagName+(empty ? " />" : ">");
	}
	
	public static String openTag(String tagName, String attributes)
	{
		return openTag(tagName, attributes, false);
	}
	
	public static String closeTag(String tagName)
	{
		return "</" + tagName + ">";
	}
	

	public static String findFormula(String expression)
	{
		return findFormula(expression, MatrixFunctions.FORMULA_START, MatrixFunctions.FORMULA_END);
	}
	
	public static String findFormula(String expression, String openingTag, String closingTag)
	{
		int start = expression.indexOf(openingTag);
		if (start == -1)
			return null;
		int end = indexClosingTag(expression, openingTag, closingTag, start);
		if (end == -1)
			return null;
		return expression.substring(start + openingTag.length(), end);
	}


	/**
	 * Returns the index of closing tag corresponding to the specified opening tag.
	 * Search will be started from the specified findFromIndex. 
	 * 
	 * @param value         text value containing opening and closing tags. 
	 * @param openingTag    opening tag corresponding to closing tag.
	 * @param closingTag    closing tag to search.
	 * @param findFromIndex the index from which to start the search.
	 *                         
	 * @return              index of the first character of closingTag or -1
	 * 
	 * @throws IllegalArgumentException
	 * 
	 * @see com.exactprosystems.clearth.utils.TagUtilsTest for examples.
	 */
	@SuppressWarnings("OverlyComplexMethod") // - Complexity caused by parameters check.
	public static int indexClosingTag(String value, String openingTag, String closingTag, int findFromIndex)
	{
		if (isBlank(openingTag))
			throw new IllegalArgumentException(format("openingTag='%s'. Non-blank string is expected.", openingTag));
		if (isBlank(closingTag))
			throw new IllegalArgumentException(format("closingTag='%s'. Non-blank string is expected.", closingTag));
		if (openingTag.equals(closingTag))
			throw new IllegalArgumentException(format("openingTag=closingTag='%s'. Different values are expected.",
					openingTag));
		
		if ((value == null) || !value.contains(closingTag))
			return -1;
		
		
		int depth = 0;
		int currentIndex = findFromIndex;
		
		while (currentIndex < value.length())
		{
			int openingTagIndex = value.indexOf(openingTag, currentIndex);
			int closingTagIndex = value.indexOf(closingTag, currentIndex);
			
			if (closingTagIndex == -1)
				return -1;
			else if ((openingTagIndex == -1) || (closingTagIndex < openingTagIndex))
			{
				depth--;
				if (depth == 0)
					return closingTagIndex;
				else 
					currentIndex = closingTagIndex + 1;
			}
			else //openingTagIndex != -1
			{
				depth++;
				currentIndex = openingTagIndex + 1;
			}
		}
		
		return -1;
	}

	/**
	 * Returns the index of closing tag corresponding to the specified opening tag.
	 * 
	 * @param value         text value containing opening and closing tags.
	 * @param openingTag    opening tag corresponding to closing tag.
	 * @param closingTag    closing tag to search.
	 *                         
	 * @return              index of the first character of closingTag or -1
	 * 
	 * @throws IllegalArgumentException
	 * 
	 * @see com.exactprosystems.clearth.utils.TagUtilsTest for examples.
	 */
	public static int indexClosingTag(String value, String openingTag, String closingTag)
	{
		return indexClosingTag(value, openingTag, closingTag, 0);
	}
	
	
	public static boolean checkTagClosed(String tagName, String text) {
		int indexOfClosingTag = getTagEnd(tagName, text, -1, false);
		int indexOfNextOpenTag = getTagStart(tagName, text, getTagStart(tagName, text, -1) + tagName.length() + 2);
		if (indexOfClosingTag == -1 && indexOfNextOpenTag == -1) 
			return false;
		if (indexOfNextOpenTag > -1 && indexOfClosingTag == -1)
			return false;
		if (indexOfNextOpenTag > -1 && indexOfNextOpenTag < indexOfClosingTag)
			return false;
		return true;
	}
}
