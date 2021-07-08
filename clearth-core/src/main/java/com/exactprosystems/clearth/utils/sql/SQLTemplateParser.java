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

package com.exactprosystems.clearth.utils.sql;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.exactprosystems.clearth.utils.sql.SQLUtils.*;
import static org.apache.commons.lang.StringUtils.contains;

public class SQLTemplateParser
{
	private static final String END_OF_LINE_REGEX = "(\r\n|\r|\n|\u0085|\u2028|\u2029)";
	private static final Pattern TEMPLATE_PARAM_PATTERN = Pattern.compile("'?(?<![\\\\])[#$@][\\w]+'?");
	private static final String MULTI_PARAM_BEGINNER = "@";
	// regex for /* */
	private static final String MULTI_LINE_REGEX = "/\\*[\\s\\S]*?\\*/";
	// regex for --
	private static final String SINGLE_LINE_REGEX = "(--[\\s\\S]*?"+END_OF_LINE_REGEX+")|(--[\\s\\S]*)";
	// regex for ' ' or " " that considers \' or \" in them
	private static final String STRING_LITERAL_REGEX = "'[\\s\\S]*?[^\\\\]'|\"[\\s\\S]*?[^\\\\]\"";
	private static final Pattern SQL_COMMENTS_AND_LITERALS_REGEX = Pattern.compile(String.format("(%s)|(%s)|(%s)",
			MULTI_LINE_REGEX, SINGLE_LINE_REGEX, STRING_LITERAL_REGEX));

	public ParametrizedQuery parseParametrizedQueryTemplate(File templateFile) throws IOException
	{
		String templateText = FileUtils.readFileToString(templateFile, Charset.defaultCharset());
		if (templateText.isEmpty())
			throw new IOException(String.format("The file '%s' is empty", templateFile));
		return parseParametrizedQueryTemplate(templateText);
	}

	public ParametrizedQuery parseParametrizedQueryTemplate(String templateText, String multiParamsDelimiter)
	{
		templateText = removeComments(templateText);
		Matcher paramMatcher = TEMPLATE_PARAM_PATTERN.matcher(templateText);
		List<String> queryParams = new ArrayList<>();
		HashSet<String> multiParams = new HashSet<>();

		StringBuffer queryBuilder = new StringBuffer();
		while (paramMatcher.find())
		{
			String foundParam = getTemplateParamName(paramMatcher.group());
			if (contains(paramMatcher.group(), MULTI_PARAM_BEGINNER))
				multiParams.add(foundParam);

			queryParams.add(foundParam);
			paramMatcher.appendReplacement(queryBuilder, "?");
		}
		paramMatcher.appendTail(queryBuilder);

		replaceAll(queryBuilder, TABLE_NAME_WITH_DOLLAR.toString(), Character.toString(CONVERT_BEGINNER));
		replaceAll(queryBuilder,TABLE_NAME_WITH_SHARP.toString(),Character.toString(CUSTOM_BEGINNER));
		replaceAll(queryBuilder, "\\" + MULTI_PARAM_BEGINNER, MULTI_PARAM_BEGINNER);

		return new ParametrizedQuery(queryBuilder.toString(), queryParams, multiParams, multiParamsDelimiter);
	}

	public ParametrizedQuery parseParametrizedQueryTemplate(String templateText)
	{
		return parseParametrizedQueryTemplate(templateText, ",");
	}

	public static void replaceAll(StringBuffer builder, String from, String to)
	{
		int index = builder.indexOf(from);
		while (index != -1)
		{
			builder.replace(index, index + from.length(), to);
			index += to.length();
			index = builder.indexOf(from, index);
		}
	}

	protected String getTemplateParamName(String templateParam)
	{
		if (templateParam.length() < 2)
			throw new IllegalArgumentException("Template param should contain at least 2 characters.");

		return templateParam.replaceAll("[#$@']*", "").trim();
	}

	/**
	 * deletes single line and multiline comments;
	 * can handle strict sql strings (in ''/"") and situations like "--/*" and "/*--"
	 * @return query without comments
	 */
	public String removeComments(String query)
	{
		Matcher matcher = SQL_COMMENTS_AND_LITERALS_REGEX.matcher(query);
		
		if (!matcher.find())
			return query;
		
		StringBuilder result = new StringBuilder();
		int index = 0;
		while (index < query.length() && matcher.find(index))
		{
			result.append(query.subSequence(index, matcher.start()));
			index = matcher.end();
			
			String block = matcher.group();
			if (block.startsWith("/")) // multiline comment start
				result.append(" ");
			else if (block.startsWith("-")) // single line comment start
			{
				result.append(index >= query.length() ? "" :
						query.substring(index - 2, index).equals("\r\n") ? "\r\n" : query.charAt(index - 1));
			}
			else // string literal start
				result.append(block);
		}
		
		if (index < query.length())
			result.append(query.substring(index));
		return result.toString();
	}
}
